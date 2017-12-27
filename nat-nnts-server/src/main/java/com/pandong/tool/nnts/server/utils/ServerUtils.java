package com.pandong.tool.nnts.server.utils;

import com.pandong.common.units.Cache;
import com.pandong.common.units.Config;
import com.pandong.tool.nnts.Global;
import com.pandong.tool.nnts.model.Client;
import com.pandong.tool.nnts.model.Server;
import com.pandong.tool.nnts.model.Service;
import com.pandong.tool.nnts.server.handler.ProxyChannelHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ServerUtils {
  private static Config config = Global.loadConfig(Global.CongigName.CONFIG_FILE_NAME);
  private static Cache cache = Global.getCache(serverName()
          + Global.CongigName.NAME_SEPARATOR + Global.CongigName.CACHE_NAME);

  private static Server self;

  private static EventLoopGroup masterGroup = new NioEventLoopGroup();
  private static EventLoopGroup workerGroup = new NioEventLoopGroup();

  static {
    self = new Server();
    self.setPort(serverBindPort());
    self.setId(Global.getClientId());
    self.setIp(serverBindIp());
    self.setName(serverName());
  }

  public static EventLoopGroup getMasterGroup() {
    return masterGroup;
  }

  public static EventLoopGroup getWorkerGroup() {
    return workerGroup;
  }

  public static String serverName() {
    return config.stringValue(Global.CongigName.SERVER_NAME);
  }

  public static String serverBindIp() {
    return config.stringValue(Global.CongigName.SERVER_BIND_IP);
  }

  public static int serverBindPort() {
    return config.intValue(Global.CongigName.SERVER_BIND_PORT, Global.ConfigDefaultValue.SERVER_DEFAULT_PORT);
  }

  public static Server server() {
    return self;
  }

  public static void addClient(Client client, Channel channel) {
    Map<String, Client> ipMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_CLIENT_IPMAP);
    Map<String, Client> nameMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_CLIENT_NAMEMAP);
    Map<Long, Channel> channelMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_CLIENT_CHANNEL_MAP);
    ipMap.put(client.getIp(), client);
    nameMap.put(client.getName(), client);
    channelMap.put(client.getId(), channel);
    channel.attr(Global.ChannelAttribute.CLIENT_ID).set(client.getId());
    channel.attr(Global.ChannelAttribute.CLIENT).set(client);
    try {
      Global.getDB(Global.Database.HOME_SERVER)
              .writeToDatabase(Global.Database.HOME_SERVER, Global.Database.DB_NAME_NODE,
                      Global.Database.NODE_KEY_PREFIX_CLIENT + Global.CongigName.NAME_SEPARATOR + client.getId(),
                      client.json(), true);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

  }

  public static void addService(Channel channel, Service service) {
    Map<Integer, Channel> portChannelMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_CHANNEL_PORTMAP);
    Map<Integer, Service> portServiceMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_SERVICE_PORTMAP);
    if(!portServiceMap.containsKey(service.getProxy().getPort())) {
      portChannelMap.put(service.getProxy().getPort(), channel);
      portServiceMap.put(service.getProxy().getPort(), service);
      service.setStatus(true);
      startProxyServer(service);
    }
  }

  public static Channel getRequestChannel(long requestId){
    Map<Long, Channel> requestChannelMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_CHANNEL_REQUESTMAP);
    return requestChannelMap.get(requestId);
  }

  public static void addConnectProxy(long requestId, Channel requestChannel, Channel proxyChannel, Service service) {
    Map<Long, Channel> requestChannelMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_CHANNEL_REQUESTMAP);
    requestChannelMap.put(requestId, requestChannel);
    requestChannel.attr(Global.ChannelAttribute.REQUEST_ID).set(requestId);
    requestChannel.attr(Global.ChannelAttribute.SERVICE).set(service);
  }

  public static void cleanRequest(long requestId){
    Map<Long, Channel> requestChannelMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_CHANNEL_REQUESTMAP);
    requestChannelMap.remove(requestId);
  }

  public static Channel getProxyChannelWithPort(int port) {
    Map<Integer, Channel> portChannelMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_CHANNEL_PORTMAP);
    return portChannelMap.get(port);
  }

  public static Service getServiceWithPort(int port) {
    Map<Integer, Service> portServiceMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_SERVICE_PORTMAP);
    return portServiceMap.get(port);
  }

  public static void startProxyServer(Service service) {
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(getMasterGroup(), getWorkerGroup()).channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {

              @Override
              public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ProxyChannelHandler());
              }
            });
    try {
      bootstrap.bind(service.getProxy().getPort()).get();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    service.setStatus(true);
    log.info("start proxy server success!!!");
  }
}
