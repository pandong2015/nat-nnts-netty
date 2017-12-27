package com.pandong.tool.nnts.client;

import com.pandong.tool.nnts.client.utils.ChannelFutureListenerFactory;
import com.pandong.tool.nnts.client.utils.ClientUtil;
import com.pandong.tool.nnts.client.utils.ConnectionUtils;
import com.pandong.tool.nnts.model.Server;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NNTSClient {

  public void start() {
    Server server = ClientUtil.getConfigServer();
    ConnectionUtils.ConnectionFactory.INSTANCE.connect(ConnectionUtils.ConnectType.PROXY,
            server.getIp(), server.getPort(), ChannelFutureListenerFactory.REGISTER);
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        ConnectionUtils.ConnectionFactory.INSTANCE.getWorkerGroup().shutdownGracefully();
        log.info("shutdown client...");
      }
    }));
  }

  public static void main(String[] args) {
    NNTSClient client = new NNTSClient();
    client.start();
  }
}
