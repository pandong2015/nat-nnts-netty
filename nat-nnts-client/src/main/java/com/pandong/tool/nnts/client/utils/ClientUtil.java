package com.pandong.tool.nnts.client.utils;

import com.pandong.common.units.Cache;
import com.pandong.common.units.Config;
import com.pandong.tool.nnts.Global;
import com.pandong.tool.nnts.model.Client;
import com.pandong.tool.nnts.model.Server;
import com.pandong.tool.nnts.model.Service;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ClientUtil {
  private static final Config config = Global.loadConfig(Global.CongigName.CONFIG_FILE_NAME);
  private static final Cache cache = Global.getCache(getClientName()
          + Global.CongigName.NAME_SEPARATOR + Global.CongigName.CACHE_NAME);
  private static Client self;

  static {
    self = new Client();
    self.setName(getClientName());
    self.setId(Global.getClientId());
  }

  public static Server getConfigServer() {
    Server server = new Server();
    server.setIp(config.stringValue(Global.CongigName.SERVER_IP));
    server.setPort(config.intValue(Global.CongigName.SERVER_PORT, Global.ConfigDefaultValue.SERVER_DEFAULT_PORT));
    return server;
  }

  public static Client getClient() {
    return self;
  }

  public static String getClientName() {
    return config.stringValue(Global.CongigName.CLIENT_NAME);
  }

  public static void cacheServer(Server server, Channel channel) {
    Map<String, Server> ipMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_SERVER_IPMAP);
    Map<String, Server> nameMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_SERVER_NAMEMAP);
    Map<Long, Channel> channelMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_SERVER_CHANNEL_MAP);
    Map<Integer, Server> portMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_SERVER_PORT_MAP);
    ipMap.put(server.getIp(), server);
    nameMap.put(server.getIp(), server);
    portMap.put(server.getPort(),server);
    channelMap.put(server.getId(), channel);
    channel.attr(Global.ChannelAttribute.SERVER_ID).set(server.getId());
    channel.attr(Global.ChannelAttribute.SERVER).set(server);
    try {
      Global.getDB(Global.Database.HOME_CLIENT)
              .writeToDatabase(Global.Database.HOME_CLIENT, Global.Database.DB_NAME_NODE,
              Global.Database.NODE_KEY_PREFIX_SERVER + Global.CongigName.NAME_SEPARATOR + server.getId(),
              server.json(), true);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  public static Server getServer(int port){
    Map<Integer, Server> portMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_SERVER_PORT_MAP);
    return portMap.get(port);
  }

  public static void cacheServiec(Channel channel, Service service){
    Map<Long, Service> serviceMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_SERVICE_MAP);
    serviceMap.put(service.hash(), service);
  }

  public static void cacheRequestChannel(Channel channel, Channel proxyChannel, long requestId, Service service){
    Map<Long, Channel> requestChannelMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_REQUEST_CHANNEL_MAP);
    channel.attr(Global.ChannelAttribute.REQUEST_ID).set(requestId);
    channel.attr(Global.ChannelAttribute.PROXY_CHANNEL).set(proxyChannel);
    channel.attr(Global.ChannelAttribute.SERVICE).set(service);
    requestChannelMap.put(requestId, channel);
  }

  public static void cleanRequest(long requestId){
    Map<Long, Channel> requestChannelMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_REQUEST_CHANNEL_MAP);
    requestChannelMap.remove(requestId);
  }

  public static Channel getRequestChannel(long requestId){
    Map<Long, Channel> requestChannelMap = cache.map(Global.CongigName.CACHE_VALUE_NAME_REQUEST_CHANNEL_MAP);
    return requestChannelMap.get(requestId);
  }
}
