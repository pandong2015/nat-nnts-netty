package com.pandong.tool.nnts.handler;

import com.pandong.tool.nnts.model.TransferProto;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

@Slf4j
public class MetricsHandler extends ChannelDuplexHandler {

  @Override
  public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
    log.info("bind --> " + localAddress);
    super.bind(ctx, localAddress, promise);
  }

  @Override
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
    log.info("connect from --> " + remoteAddress);
    super.connect(ctx, remoteAddress, localAddress, promise);
  }

  @Override
  public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    log.info("disconnect");
    super.disconnect(ctx, promise);
  }

  @Override
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    log.info("close");
    super.close(ctx, promise);
  }

  @Override
  public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    log.info("deregister");
    super.deregister(ctx, promise);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    TransferProto.Transfer transfer = (TransferProto.Transfer) msg;
    log.info("read --> " + transfer.getSerializedSize());
    super.channelRead(ctx, msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    TransferProto.Transfer transfer = (TransferProto.Transfer) msg;
    log.info("write --> " + transfer.getSerializedSize());

  }

  @Override
  public void flush(ChannelHandlerContext ctx) throws Exception {
    log.info("flush");
    super.flush(ctx);
  }

}
