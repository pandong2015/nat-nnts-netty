package com.pandong.tool.nnts.model;

import com.pandong.common.units.HashUtil;
import lombok.Data;

@Data
public class Service {
  private String name;
  private Node local;
  private Node proxy;
  private boolean status;
  private boolean ssl;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Service service = (Service) o;

    return name.equals(service.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public long hash() {
    return HashUtil.hashByMD5(name);
  }
}
