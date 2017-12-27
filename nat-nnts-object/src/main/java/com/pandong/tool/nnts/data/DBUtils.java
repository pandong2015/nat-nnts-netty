package com.pandong.tool.nnts.data;

import com.google.common.collect.Maps;
import com.pandong.common.units.FileUtils;
import com.pandong.tool.nnts.Global;
import com.sleepycat.je.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

@Slf4j
public class DBUtils {
  private Map<String, Environment> environmentMap = Maps.newConcurrentMap();
  private Map<String, Database> databaseMap = Maps.newConcurrentMap();

  public DBUtils(String home, List<String> database) throws DatabaseException {
    database(home, database);
  }

  public synchronized Environment environment(String home) throws DatabaseException {
    Environment environment = null;
    if (environmentMap.containsKey(home)) {
      environment = environmentMap.get(home);
    } else {
      String homePath = getEnvironmentPath(home);
      FileUtils.checkDirectory(homePath, true);
      environment = new Environment(new File(homePath), getEnvironmentConfig());
      environmentMap.put(home, environment);
    }
    return environment;
  }


  public void database(String home, List<String> names) throws DatabaseException {
    for (String db : names) {
      database(home, db);
    }
  }

  public void database(String home, String name) throws DatabaseException {
    String dbKey = getDatabaseKey(home, name);
    databaseMap.put(dbKey,
            environment(home).openDatabase(null, name, getDatabaseConfig()));
  }

  public Database getDatabase(String home, String name, boolean isCreate) throws DatabaseException {
    String dbKey = getDatabaseKey(home, name);
    if (!databaseMap.containsKey(dbKey) && isCreate) {
      database(home, name);
    }
    return databaseMap.get(dbKey);
  }

  public void close() {
    for (Map.Entry<String, Database> entry : databaseMap.entrySet()) {
      try {
        entry.getValue().close();
      } catch (DatabaseException e) {
        log.error(e.getMessage());
      }
    }
    for (Map.Entry<String, Environment> entry : environmentMap.entrySet()) {
      try {
        entry.getValue().close();
      } catch (DatabaseException e) {
        log.error(e.getMessage());
      }
    }
  }

  public Map<String, String> readAllDataFromDatabasr(String home, String db) throws DatabaseException, UnsupportedEncodingException {
    Database database = getDatabase(home, db, true);
    Map<String, String> resultMap = Maps.newHashMap();
    CursorConfig cc = new CursorConfig();
    Cursor myCursor = database.openCursor(null, cc);
    DatabaseEntry foundKey = new DatabaseEntry();
    DatabaseEntry foundData = new DatabaseEntry();
    if (myCursor.getFirst(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
      String theKey = new String(foundKey.getData(), Global.SYSTEM_ENCODING);
      String theData = new String(foundData.getData(), Global.SYSTEM_ENCODING);
      resultMap.put(theKey, theData);
      while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS){
        theKey = new String(foundKey.getData(), Global.SYSTEM_ENCODING);
        theData = new String(foundData.getData(), Global.SYSTEM_ENCODING);
        resultMap.put(theKey, theData);
      }
    }
    myCursor.close();
    return resultMap;
  }

  public String readFromDatabaseString(String home, String db, String key) throws DatabaseException, UnsupportedEncodingException {
    byte[] data = readFromDatabase(home, db, key);
    if (data != null) {
      return new String(data, Global.SYSTEM_ENCODING);
    }
    return null;
  }

  public byte[] readFromDatabase(String home, String db, String key) throws DatabaseException, UnsupportedEncodingException {
    Environment environment = environment(home);
    Database database = getDatabase(home, db, true);
    DatabaseEntry theKey = new DatabaseEntry(key.trim().getBytes(Global.SYSTEM_ENCODING));
    DatabaseEntry theData = new DatabaseEntry();
    TransactionConfig txConfig = new TransactionConfig();
    txConfig.setSerializableIsolation(true);
    Transaction txn = environment.beginTransaction(null, txConfig);
    OperationStatus res = database.get(txn, theKey, theData, LockMode.DEFAULT);
    txn.commit();
    byte[] result = null;
    if (res == OperationStatus.SUCCESS) {
      result = theData.getData();
      log.info("Load " + home + "-" + db + " insert " + key + " success.");
    } else {
      log.info("Load " + home + "-" + db + " insert " + key + " fail.");
    }
    return result;
  }

  public boolean writeToDatabase(String home, String db, String key, String value, boolean isOverwrite) throws DatabaseException, UnsupportedEncodingException {
    return writeToDatabase(home, db, key, value.getBytes(Global.SYSTEM_ENCODING), isOverwrite);
  }

  public boolean writeToDatabase(String home, String db, String key, byte[] value, boolean isOverwrite) throws DatabaseException, UnsupportedEncodingException {
    Environment environment = environment(home);
    Database database = getDatabase(home, db, true);
    DatabaseEntry theKey = new DatabaseEntry(key.trim().getBytes(Global.SYSTEM_ENCODING));
    DatabaseEntry theData = new DatabaseEntry(value);
    OperationStatus res = null;
    boolean result = false;
    TransactionConfig txConfig = new TransactionConfig();
    txConfig.setSerializableIsolation(true);
    Transaction txn = environment.beginTransaction(null, txConfig);
    if (isOverwrite) {
      res = database.put(txn, theKey, theData);
    } else {
      res = database.putNoOverwrite(txn, theKey, theData);
    }
    txn.commit();

    if (res == OperationStatus.SUCCESS) {
      log.info(home + "-" + db + " insert " + key + "-" + value + " SUCCESS.");
      result = true;
    } else if (res == OperationStatus.KEYEXIST) {
      log.info(home + "-" + db + " insert " + key + "-" + value + " fail, key is exist.");
    } else {
      log.info(home + "-" + db + " insert " + key + "-" + value + " fail.");
    }
    return result;
  }

  private String getEnvironmentPath(String name) {
    return Global.FileSystemValue.dataDirectory() + Global.CongigName.FILE_SEPARATOR + name;
  }

  private String getDatabaseKey(String home, String name) {
    return home + Global.CongigName.NAME_SEPARATOR + name;
  }

  private EnvironmentConfig getEnvironmentConfig() {
    EnvironmentConfig envCfg = new EnvironmentConfig();
    envCfg.setAllowCreate(true);
    envCfg.setReadOnly(false);
    envCfg.setTransactional(true);
    return envCfg;
  }

  private DatabaseConfig getDatabaseConfig() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setAllowCreate(true);
    databaseConfig.setReadOnly(false);
    databaseConfig.setTransactional(true);
    return databaseConfig;
  }

}
