package com.pandong.tool.nnts.client;

import com.alibaba.fastjson.JSON;
import com.pandong.tool.nnts.Global;
import com.pandong.tool.nnts.data.DBUtils;
import com.pandong.tool.nnts.model.Node;
import com.pandong.tool.nnts.model.Service;

import java.util.ArrayList;

public class TestDB {
  public static void main(String[] args) {
    String home = Global.Database.HOME_CLIENT;
    String db1 = Global.Database.DB_NAME_NODE;
    String db2 = Global.Database.DB_NAME_JKS;
    Service service = new Service();
    service.setName("callback");
    Node local = new Node();
    local.setIp("192.168.164.35");
    local.setPort(10010);
    service.setLocal(local);
    Node proxy = new Node();
    proxy.setPort(11010);
    service.setProxy(proxy);
    try {
      DBUtils db = new DBUtils(home, new ArrayList<String>() {{
        add(db1);
        add(db2);
      }});

      String key = Global.Database.NODE_KEY_PREFIX_SERVICE + Global.CongigName.NAME_SEPARATOR + service.hash();
      db.writeToDatabase(home, db1, key, JSON.toJSONString(service), false);

      System.out.println(db.readFromDatabaseString(home, db1, key));

      db.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
