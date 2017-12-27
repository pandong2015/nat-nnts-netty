package com.pandong.tool.nnts.model;

import lombok.Data;

import java.util.List;

@Data
public class Client extends Node{
  private List<Service> services;

  @Override
  public String toString() {
    return "Client{" +
            "id=" + getId() +
            ", name='" + getName() + '\'' +
            ", ip='" + getIp() + '\'' +
            ", port=" + getPort() +
            '}';
  }

}
