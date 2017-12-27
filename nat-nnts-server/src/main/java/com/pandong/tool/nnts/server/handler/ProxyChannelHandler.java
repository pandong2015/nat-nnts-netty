package com.pandong.tool.nnts.server.handler;

import com.pandong.tool.nnts.Global;
import com.pandong.tool.nnts.model.Service;
import com.pandong.tool.nnts.model.utils.TransferGenerate;
import com.pandong.tool.nnts.server.utils.ServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class ProxyChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    Channel requestChannel = ctx.channel();
    Channel proxyChannel = requestChannel.attr(Global.ChannelAttribute.PROXY_CHANNEL).get();
    long requestId = requestChannel.attr(Global.ChannelAttribute.REQUEST_ID).get();
    if(proxyChannel==null){
      log.debug("request id --> " + requestId + ", no proxy channel.");
      requestChannel.close();
    }else {
      byte[] bytes = new byte[msg.readableBytes()];
      msg.readBytes(bytes);
      log.debug("request id --> " + requestId + ", read byte size --> "+ bytes.length);
      proxyChannel.writeAndFlush(TransferGenerate.generateTransferRequest(requestId, ServerUtils.server(), bytes));
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
    int port = inetSocketAddress.getPort();
    Channel proxyChannel = ServerUtils.getProxyChannelWithPort(port);
    Service service = ServerUtils.getServiceWithPort(port);
    if (proxyChannel == null) {
      //没有client端的连接
      ctx.channel().close();
    } else {
      //向client端发起连接请求
      long requestId = Global.getId(Global.SequenceName.SEQUENCE_NAME_REQUEST);
      log.debug("create new request, requestId --> "+requestId);
      //根据requestID，缓存channel，方便在传输数据时使用
      ServerUtils.addConnectProxy(requestId, ctx.channel(), proxyChannel, service);
      //在client端连接道真实服务前，暂停读取数据
      ctx.channel().config().setOption(ChannelOption.AUTO_READ, false);
      log.debug("send connect commend...");
      proxyChannel.writeAndFlush(TransferGenerate
              .generateConnectRequest(requestId, ServerUtils.server(), ServerUtils.getServiceWithPort(port)));
    }
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    Channel requestChannel = ctx.channel();
    Channel proxyChannel = requestChannel.attr(Global.ChannelAttribute.PROXY_CHANNEL).get();
    long requestId = requestChannel.attr(Global.ChannelAttribute.REQUEST_ID).get();
    Service service = requestChannel.attr(Global.ChannelAttribute.SERVICE).get();
    if(proxyChannel==null){
      requestChannel.close();
    }else{
      log.debug("close request, reuqest id --> "+requestId);
      proxyChannel.attr(Global.ChannelAttribute.REQUEST_CHANNEL).set(null);
      proxyChannel.writeAndFlush(TransferGenerate.generateDisconnectRequest(requestId, ServerUtils.server(), service));
    }
    super.channelInactive(ctx);
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    Channel requestChannel = ctx.channel();
    Channel proxyChannel = requestChannel.attr(Global.ChannelAttribute.PROXY_CHANNEL).get();
    if(proxyChannel==null){
      requestChannel.close();
    }else {
      proxyChannel.config().setOption(ChannelOption.AUTO_READ, requestChannel.isWritable());
    }
    super.channelWritabilityChanged(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    super.exceptionCaught(ctx, cause);
    ctx.close();
  }
}
