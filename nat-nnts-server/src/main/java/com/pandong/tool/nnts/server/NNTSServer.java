package com.pandong.tool.nnts.server;

import com.pandong.tool.nnts.model.Server;
import com.pandong.tool.nnts.model.TransferProto;
import com.pandong.tool.nnts.server.handler.ServerChannelHandler;
import com.pandong.tool.nnts.server.utils.ServerUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NNTSServer {
  private ServerBootstrap bootstrap;

  public NNTSServer() {
    bind();
  }

  private void bind() {
    bootstrap = new ServerBootstrap();
    bootstrap.group(ServerUtils.getMasterGroup(), ServerUtils.getWorkerGroup()).channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 100)
            .childHandler(new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel channel) throws Exception {
                channel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                channel.pipeline().addLast(new ProtobufDecoder(TransferProto.Transfer.getDefaultInstance()));
                channel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                channel.pipeline().addLast(new ProtobufEncoder());
//                channel.pipeline().addLast(new MetricsHandler());
                channel.pipeline().addLast(new ServerChannelHandler());
              }
            });
  }

  public void start(){
    Server self = ServerUtils.server();
    try {
      log.info("proxy server start on port " + self.getPort());
      bootstrap.bind(self.getPort()).get();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run()
      {
        ServerUtils.getMasterGroup().shutdownGracefully();
        ServerUtils.getWorkerGroup().shutdownGracefully();
        log.info("shutdown server...");
      }
    }));
  }

  public static void main(String[] args) {
    NNTSServer server = new NNTSServer();
    server.start();
  }
}
