package com.pandong.tool.nnts.model;

import com.alibaba.fastjson.JSON;
import lombok.Data;

@Data
public class Node implements Json {
  private long id;
  private String name;
  private String ip;
  private int port;

  @Override
  public String toString() {
    return "Node{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", ip='" + ip + '\'' +
            ", port=" + port +
            '}';
  }

  @Override
  public String json() {
    return JSON.toJSONString(this);
  }
}
