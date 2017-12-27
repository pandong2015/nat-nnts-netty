package com.pandong.tool.nnts.client.handler;

import com.pandong.tool.nnts.Global;
import com.pandong.tool.nnts.client.utils.ClientUtil;
import com.pandong.tool.nnts.model.Service;
import com.pandong.tool.nnts.model.utils.TransferGenerate;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RealServerChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    Channel realChannel = ctx.channel();
    Channel proxyChannel = realChannel.attr(Global.ChannelAttribute.PROXY_CHANNEL).get();
    long requestId = realChannel.attr(Global.ChannelAttribute.REQUEST_ID).get();
    if (proxyChannel == null) {
      realChannel.close();
    } else {
      byte[] bytes = new byte[msg.readableBytes()];
      msg.readBytes(bytes);
      log.debug("request id[" + requestId + "], read real server data size --> " + bytes.length);
      proxyChannel.writeAndFlush(TransferGenerate.generateTransferRequest(requestId, ClientUtil.getClient(), bytes));
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    Channel realChannel = ctx.channel();
    Channel proxyChannel = realChannel.attr(Global.ChannelAttribute.PROXY_CHANNEL).get();
    Service service = realChannel.attr(Global.ChannelAttribute.SERVICE).get();
    long requestId = realChannel.attr(Global.ChannelAttribute.REQUEST_ID).get();
    log.debug("request id["+requestId+"], send disconnect commend.");
    if (proxyChannel != null) {
      ClientUtil.cleanRequest(requestId);
      proxyChannel.writeAndFlush(TransferGenerate.generateDisconnectRequest(requestId, ClientUtil.getClient(), service));
    }
    super.channelInactive(ctx);
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    Channel realChannel = ctx.channel();
    Channel proxyChannel = realChannel.attr(Global.ChannelAttribute.PROXY_CHANNEL).get();
    if (proxyChannel != null) {
      proxyChannel.config().setOption(ChannelOption.AUTO_READ, realChannel.isWritable());
    }
    super.channelWritabilityChanged(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error(cause.getMessage(), cause);
    super.exceptionCaught(ctx, cause);
  }
}
