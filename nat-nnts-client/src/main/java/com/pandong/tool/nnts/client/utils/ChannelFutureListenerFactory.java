package com.pandong.tool.nnts.client.utils;

import com.pandong.tool.nnts.model.utils.TransferGenerate;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public enum ChannelFutureListenerFactory implements ChannelFutureListener {
  REGISTER {
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
      if (future.isSuccess()) {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) future.channel().localAddress();
        com.pandong.tool.nnts.model.Client client = ClientUtil.getClient();
        client.setIp(inetSocketAddress.getAddress().getHostAddress());
        client.setPort(inetSocketAddress.getPort());
        future.channel().writeAndFlush(TransferGenerate.generateRegistre(client));
        log.info("connect proxy server success, {}", future.channel());
      } else {
        log.warn("connect proxy server failed", future.cause());
      }
    }
  }
}
