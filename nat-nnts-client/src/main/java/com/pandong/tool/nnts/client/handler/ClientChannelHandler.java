package com.pandong.tool.nnts.client.handler;

import com.alibaba.fastjson.JSON;
import com.pandong.tool.nnts.Global;
import com.pandong.tool.nnts.client.utils.ClientUtil;
import com.pandong.tool.nnts.client.utils.ConnectionUtils;
import com.pandong.tool.nnts.model.Server;
import com.pandong.tool.nnts.model.Service;
import com.pandong.tool.nnts.model.TransferProto;
import com.pandong.tool.nnts.model.utils.TransferGenerate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ClientChannelHandler extends SimpleChannelInboundHandler<TransferProto.Transfer> {
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TransferProto.Transfer msg) throws Exception {
    log.debug("operation --> " + msg.getOperation().name());
    switch (msg.getOperation()) {
      case HEARTBEAT:
        break;
      case SERVICE_REGISTER:
        registerService(ctx, msg);
        break;
      case CONNECT:
        connect(ctx, msg);
        break;
      case TRANSFER:
        transfer(ctx, msg);
        break;
      case REGISTER:
        register(ctx, msg);
        break;
      case DISCONNECT:
        disconnect(ctx, msg);
        break;
      default:
        break;
    }
  }

  private void disconnect(ChannelHandlerContext ctx, TransferProto.Transfer msg) {
    log.debug("close connect, request id --> " + msg.getRequestId());
    Channel proxyChannel = ctx.channel();
    Channel realChannel = proxyChannel.attr(Global.ChannelAttribute.REQUEST_CHANNEL).get();
    if (realChannel != null) {
      proxyChannel.attr(Global.ChannelAttribute.REQUEST_CHANNEL).set(null);
      proxyChannel.close();
      realChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

  private void transfer(ChannelHandlerContext ctx, TransferProto.Transfer msg) {
    log.debug("begin transfer data, request id --> " + msg.getRequestId());
    Channel realChannel = ClientUtil.getRequestChannel(msg.getRequestId());
    if (realChannel != null) {
      ByteBuf buf = ctx.alloc().buffer(msg.getData().size());
      buf.writeBytes(msg.getData().toByteArray());
      realChannel.writeAndFlush(buf);
    }
  }

  private void connect(ChannelHandlerContext ctx, TransferProto.Transfer msg) {
    long requestId = msg.getRequestId();
    String data = msg.getData().toStringUtf8();
    Service service = JSON.parseObject(data, Service.class);
    Server server = ctx.channel().attr(Global.ChannelAttribute.SERVER).get();
    log.debug("request id --> " + requestId);
    log.debug("begin connect real server...");
    //连接后端服务
    ConnectionUtils.ConnectionFactory.INSTANCE.connect(ConnectionUtils.ConnectType.REAL, service.getLocal().getIp(),
            service.getLocal().getPort(), new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                  log.debug("connect real server success.");
                  Channel realChannel = future.channel();

                  //后端服务连接成功，发起新的连接至代理服务器，准备传输数据
                  log.debug("begin conect proxy server...");
                  ConnectionUtils.ConnectionFactory.INSTANCE.connect(ConnectionUtils.ConnectType.PROXY
                          , server.getIp(), server.getPort(), new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                              if (future.isSuccess()) {
                                log.debug("conect proxy server success.");
                                Channel newProxyChannel = future.channel();
                                //绑定两个channel，用于后续传输数据
                                newProxyChannel.attr(Global.ChannelAttribute.REQUEST_CHANNEL).set(realChannel);
                                realChannel.attr(Global.ChannelAttribute.PROXY_CHANNEL).set(newProxyChannel);

                                //回应connect指令，client准备完毕，准备传输
                                newProxyChannel.writeAndFlush(TransferGenerate.generateConnectRequest(requestId, ClientUtil.getClient(), service));

                                //后端服务开始读取数据
                                realChannel.config().setOption(ChannelOption.AUTO_READ, true);

                                //缓存requestId和后端服务连接的关系
                                ClientUtil.cacheRequestChannel(realChannel, newProxyChannel, requestId, service);
                              } else {
                                log.debug("conect proxy server fail.");
                                ctx.writeAndFlush(TransferGenerate.generateDisconnectRequest(requestId, ClientUtil.getClient(), service));
                              }
                            }
                          });
                } else {
                  //连接失败，发送断开连接指令
                  log.warn("Service[" + data + "] request fail.");
                  ctx.writeAndFlush(TransferGenerate.generateDisconnectRequest(requestId, ClientUtil.getClient(), service));
                }
              }
            });
  }

  private void registerService(ChannelHandlerContext ctx, TransferProto.Transfer msg) {
    String data = msg.getData().toStringUtf8();
    Service service = JSON.parseObject(data, Service.class);
    if (service.isStatus()) {
      log.info("service [" + service.getName() + "] register success");
      ClientUtil.cacheServiec(ctx.channel(), service);
    }
  }

  private void register(ChannelHandlerContext ctx, TransferProto.Transfer msg) {
    String data = msg.getData().toStringUtf8();
    Server server = JSON.parseObject(data, Server.class);
    ClientUtil.cacheServer(server, ctx.channel());

    //注册数据库中的所有service
    Map<String, String> allNode = null;
    try {
      allNode = Global.getDB(Global.Database.HOME_CLIENT)
              .readAllDataFromDatabasr(Global.Database.HOME_CLIENT, Global.Database.DB_NAME_NODE);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    if (allNode == null || allNode.isEmpty()) {
      return;
    }
    allNode.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(Global.Database.NODE_KEY_PREFIX_SERVICE))
            .map(Map.Entry::getValue)
            .map(str -> JSON.parseObject(str, Service.class))
            .forEach(service -> {
              ctx.writeAndFlush(TransferGenerate.generateRegistreServiceRequest(ClientUtil.getClient(), service));
            });

  }
}
