package com.pandong.tool.nnts.model;

import lombok.Data;

@Data
public class Status<T extends Node> {
  private T t;
  private int status;
}
