package com.pandong.tool.nnts;

import com.google.common.collect.Maps;
import com.pandong.common.generater.IdGenerate;
import com.pandong.common.network.NetworkUtils;
import com.pandong.common.units.Cache;
import com.pandong.common.units.Config;
import com.pandong.common.units.FileUtils;
import com.pandong.common.units.HashUtil;
import com.pandong.tool.nnts.data.DBUtils;
import com.pandong.tool.nnts.model.Client;
import com.pandong.tool.nnts.model.Server;
import com.pandong.tool.nnts.model.Service;
import com.sleepycat.je.DatabaseException;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class Global {
  public static final String SYSTEM_ENCODING = "UTF-8";

  private static Map<String, AtomicLong> sequenceMap = Maps.newConcurrentMap();
  private static Map<String, Cache> cacheMap = Maps.newConcurrentMap();

  private static DBUtils dbUtils;

  public static DBUtils getDB(String home) {
    if (dbUtils == null) {
      try {
        dbUtils = new DBUtils(home, new ArrayList<String>() {{
          add(Database.DB_NAME_NODE);
          add(Database.DB_NAME_JKS);
        }});
      } catch (DatabaseException e) {
        log.error(e.getMessage(), e);
      }
    }
    return dbUtils;
  }


  public static AtomicLong getSequence(String name) {
    if (!sequenceMap.containsKey(name)) {
      sequenceMap.put(name, new AtomicLong(0));
    }
    return sequenceMap.get(name);
  }

  public static long getSequenceValue(String name) {
    return getSequence(name).getAndIncrement();
  }

  public static long getClientId() {
    return HashUtil.hashByMD5(NetworkUtils.getHardwareAddress());
  }

  public static long getId(String name) {
    return IdGenerate.generate(getSequenceValue(name));
  }

  public static Config loadConfig(String name) {
    return new Config(name);
  }

  public static Cache getCache(String name) {
    if (!cacheMap.containsKey(name)) {
      cacheMap.put(name, new Cache(name));
    }
    return cacheMap.get(name);
  }

  @Slf4j
  public static class FileSystemValue {
    public static final String USER_HOME = System.getProperty("user.home");
    public static final String DATA = "/.nnts";

    public static String dataDirectory() {
      return USER_HOME + CongigName.FILE_SEPARATOR + DATA;
    }

    public static Path getDataDirectory() {
      Path path = Paths.get(dataDirectory());
      if (FileUtils.checkDirectory(path, true)) {
        return path;
      }
      return null;
    }
  }

  public interface SequenceName {
    String SEQUENCE_NAME_IDLE = "idle-key";
    String SEQUENCE_NAME_REQUEST = "request-key";
    String SEQUENCE_NAME_REGISTER = "register-key";
  }

  public interface Database {
    String HOME_CLIENT = "client";
    String HOME_SERVER = "server";
    String DB_NAME_NODE = "node";
    String DB_NAME_JKS = "jks";

    String NODE_KEY_PREFIX_CLIENT = "client";
    String NODE_KEY_PREFIX_SERVER = "server";
    String NODE_KEY_PREFIX_SERVICE = "service";
  }

  public interface CongigName {
    String FILE_SEPARATOR = File.separator;
    String URL_SEPARATOR = "/";
    String NAME_SEPARATOR = "_";

    String CONFIG_FILE_NAME = "config";

    String CACHE_NAME = "cache";
    String CACHE_VALUE_NAME_SERVER_NAMEMAP = "server-name-map";
    String CACHE_VALUE_NAME_SERVER_IPMAP = "server-ip-map";
    String CACHE_VALUE_NAME_CLIENT_CHANNEL_MAP = "client-channel-map";
    String CACHE_VALUE_NAME_SERVER_CHANNEL_MAP = "server-channel-map";
    String CACHE_VALUE_NAME_SERVER_PORT_MAP = "server-port-map";
    String CACHE_VALUE_NAME_CLIENT_NAMEMAP = "client-name-map";
    String CACHE_VALUE_NAME_CLIENT_IPMAP = "client-ip-map";

    String CACHE_VALUE_NAME_SERVICE_MAP = "service-map";
    String CACHE_VALUE_NAME_REQUEST_CHANNEL_MAP = "request-channel-map";

    String CACHE_VALUE_NAME_CHANNEL_PORTMAP = "channel-port-map";
    String CACHE_VALUE_NAME_SERVICE_PORTMAP = "service-port-map";
    String CACHE_VALUE_NAME_CHANNEL_REQUESTMAP = "channel-request-map";

    String CACHE_VALUE_NAME_PROXY_SERVER_PORTMAP = "proxy-server-port-map";

    String SERVER_IP = "nat.nnts.server.ip";
    String SERVER_PORT = "nat.nnts.server.port";
    String SERVER_BIND_IP = "nat.nnts.server.bind.ip";
    String SERVER_BIND_PORT = "nat.nnts.server.bind.port";
    String SERVER_NAME = "nat.nnts.server.name";
    String CLIENT_NAME = "nat.nnts.client.name";
  }

  public interface ConfigDefaultValue {
    int SERVER_DEFAULT_PORT = 18080;
  }

  public interface ChannelAttribute {
    AttributeKey<Client> CLIENT = AttributeKey.newInstance("client");
    AttributeKey<Long> CLIENT_ID = AttributeKey.newInstance("client_id");
    AttributeKey<Server> SERVER = AttributeKey.newInstance("server");
    AttributeKey<Long> SERVER_ID = AttributeKey.newInstance("server_id");
    AttributeKey<Service> SERVICE = AttributeKey.newInstance("service");
    AttributeKey<Channel> PROXY_CHANNEL = AttributeKey.newInstance("proxy_channel");
    AttributeKey<Channel> REQUEST_CHANNEL = AttributeKey.newInstance("request_channel");
    AttributeKey<Long> REQUEST_ID = AttributeKey.newInstance("request_id");
  }
}
