package com.pandong.tool.nnts.client.utils;

import com.pandong.tool.nnts.client.handler.ClientChannelHandler;
import com.pandong.tool.nnts.client.handler.RealServerChannelHandler;
import com.pandong.tool.nnts.handler.IdleHandler;
import com.pandong.tool.nnts.model.TransferProto;
import io.netty.bootstrap.Bootstrap;
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

@Slf4j
public class ConnectionUtils {
  private NioEventLoopGroup workerGroup = new NioEventLoopGroup();
  private Bootstrap proxyBootstrap;
  private Bootstrap realServerBootstrap;

  private ConnectionUtils() {
    proxyBootstrap = new Bootstrap();
    proxyBootstrap.group(workerGroup);
    proxyBootstrap.channel(NioSocketChannel.class);
    proxyBootstrap.handler(new ChannelInitializer<SocketChannel>() {
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

    realServerBootstrap = new Bootstrap();
    realServerBootstrap.group(workerGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel channel) throws Exception {
        channel.pipeline().addLast(new RealServerChannelHandler());
      }
    });
  }

  public Bootstrap getProxyBootstrap() {
    return proxyBootstrap;
  }

  public Bootstrap getRealServerBootstrap() {
    return realServerBootstrap;
  }

  public NioEventLoopGroup getWorkerGroup() {
    return workerGroup;
  }

  public enum ConnectType{
    PROXY,
    REAL
  }

  public enum ConnectionFactory {
    INSTANCE;

    private ConnectionUtils connectionUtils;

    ConnectionFactory() {
      connectionUtils = new ConnectionUtils();
    }

    public void connect(ConnectType type, String ip, int port, ChannelFutureListener channelFutureListener){
      switch (type){
        case REAL:
          connectRealServer(ip,port, channelFutureListener);
          break;
        case PROXY:
          connectProxyServer(ip,port, channelFutureListener);
          break;
      }
    }

    public void connectProxyServer(String ip, int port, ChannelFutureListener channelFutureListener) {
      connectionUtils.getProxyBootstrap().connect(ip, port).addListener(channelFutureListener);
    }

    public void connectRealServer(String ip, int port, ChannelFutureListener channelFutureListener){
      connectionUtils.getRealServerBootstrap().connect(ip, port).addListener(channelFutureListener);
    }

    public NioEventLoopGroup getWorkerGroup() {
      return connectionUtils.getWorkerGroup();
    }
  }
}
