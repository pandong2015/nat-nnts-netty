package com.pandong.tool.nnts.client;

import com.pandong.tool.nnts.client.handler.ClientChannelHandler;
import com.pandong.tool.nnts.client.utils.ClientUtil;
import com.pandong.tool.nnts.handler.IdleHandler;
import com.pandong.tool.nnts.model.Client;
import com.pandong.tool.nnts.model.TransferProto;
import com.pandong.tool.nnts.model.utils.TransferGenerate;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class Test {
  public static void main(String[] args){
    Bootstrap bootstrap = new Bootstrap();
    NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    bootstrap.group(workerGroup);
    bootstrap.channel(NioSocketChannel.class);
    bootstrap.handler(new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel channel) throws Exception {
        channel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
        channel.pipeline().addLast(new ProtobufDecoder(TransferProto.Transfer.getDefaultInstance()));
        channel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
        channel.pipeline().addLast(new ProtobufEncoder());
        channel.pipeline().addLast(new IdleHandler());
        channel.pipeline().addLast(new ClientChannelHandler());
      }
    });
    bootstrap.connect("localhost", 18888).addListener(new ChannelFutureListener() {

      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
          InetSocketAddress inetSocketAddress = (InetSocketAddress) future.channel().localAddress();
          Client client = ClientUtil.getClient();
          client.setIp(inetSocketAddress.getAddress().getHostAddress());
          client.setPort(inetSocketAddress.getPort());
          future.channel().writeAndFlush(TransferGenerate.generateRegistre(client));
          log.info("connect proxy server success, {}", future.channel());
        } else {
          log.warn("connect proxy server failed", future.cause());
        }
      }
    });
  }
}
