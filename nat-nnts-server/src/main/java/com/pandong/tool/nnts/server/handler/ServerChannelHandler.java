package com.pandong.tool.nnts.server.handler;

import com.alibaba.fastjson.JSON;
import com.pandong.tool.nnts.Global;
import com.pandong.tool.nnts.model.Client;
import com.pandong.tool.nnts.model.Server;
import com.pandong.tool.nnts.model.Service;
import com.pandong.tool.nnts.model.TransferProto;
import com.pandong.tool.nnts.model.utils.TransferGenerate;
import com.pandong.tool.nnts.server.utils.ServerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class ServerChannelHandler extends SimpleChannelInboundHandler<TransferProto.Transfer> {
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TransferProto.Transfer msg) throws Exception {
    log.info("remoteAddress --> " + ctx.channel().remoteAddress() + " -- " + msg.getOperation().name());
    switch (msg.getOperation()) {
      case HEARTBEAT:
        ctx.writeAndFlush(TransferGenerate.generateHeartbeat(msg.getRequestId()));
        break;
      case SERVICE_REGISTER:
        registreService(ctx, msg);
        break;
      case CONNECT:
        connect(ctx, msg);
        break;
      case DISCONNECT:
        disconnect(ctx, msg);
        break;
      case TRANSFER:
        transfer(ctx, msg);
        break;
      case REGISTER:
        registre(ctx, msg);
        break;
      default:
        break;
    }
  }

  private void transfer(ChannelHandlerContext ctx, TransferProto.Transfer msg) {
    long requestId = msg.getRequestId();
    Channel requestChannel = ServerUtils.getRequestChannel(requestId);
    if (requestChannel != null) {
      ByteBuf buf = ctx.alloc().buffer(msg.getData().size());
      buf.writeBytes(msg.getData().toByteArray());
      requestChannel.writeAndFlush(buf);
    }
  }

  private void disconnect(ChannelHandlerContext ctx, TransferProto.Transfer msg) {
    log.debug("disconnect request, request id --> " + msg.getRequestId());
    Channel requestChannel = ServerUtils.getRequestChannel(msg.getRequestId());
    if (requestChannel != null) {
      ServerUtils.cleanRequest(msg.getRequestId());
      requestChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

  private void connect(ChannelHandlerContext ctx, TransferProto.Transfer msg) {
    long requestId = msg.getRequestId();
    log.debug("request id --> " + requestId);
    Channel requestChannel = ServerUtils.getRequestChannel(requestId);
    if (requestChannel != null) {
      log.debug("bind request & proxy channel");
      requestChannel.attr(Global.ChannelAttribute.PROXY_CHANNEL).set(ctx.channel());
      ctx.channel().attr(Global.ChannelAttribute.REQUEST_CHANNEL).set(requestChannel);
      requestChannel.config().setOption(ChannelOption.AUTO_READ, true);
    }

  }

  private void registreService(ChannelHandlerContext ctx, TransferProto.Transfer msg) {
    String data = msg.getData().toStringUtf8();
    log.info("registre new service --> " + data);
    Service service = JSON.parseObject(data, Service.class);
    Server server = ServerUtils.server();
    service.getProxy().setIp(server.getIp());
    ServerUtils.addService(ctx.channel(), service);
    ctx.writeAndFlush(TransferGenerate.generateRegistreServiceRequest(msg.getRequestId(), server, service));
  }

  private void registre(ChannelHandlerContext ctx, TransferProto.Transfer msg) {
    String data = msg.getData().toStringUtf8();
    log.info("registre new client --> " + data);
    Client client = JSON.parseObject(data, Client.class);
    ServerUtils.addClient(client, ctx.channel());
    Server self = ServerUtils.server();
    InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
    self.setIp(inetSocketAddress.getAddress().getHostAddress());
    self.setPort(inetSocketAddress.getPort());
    ctx.writeAndFlush(TransferGenerate.generateRegistre(msg.getRequestId(), self));
  }
}
