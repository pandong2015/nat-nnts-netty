package com.pandong.tool.nnts.model.utils;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import com.pandong.tool.nnts.Global;
import com.pandong.tool.nnts.model.*;

public class TransferGenerate {

  public static TransferProto.Transfer generateHeartbeat() {
    return generateHeartbeat(Global.getId(Global.SequenceName.SEQUENCE_NAME_IDLE));
  }

  public static TransferProto.Transfer generateHeartbeat(long requestId) {
    return generateHeartbeat(Global.getClientId(), requestId);
  }

  public static TransferProto.Transfer generateHeartbeat(long clientId, long requestId) {
    return TransferProto.Transfer.newBuilder()
            .setOperation(TransferProto.OperationType.HEARTBEAT)
            .setClientId(clientId)
            .setRequestId(requestId)
            .build();
  }

  public static <T extends Node> TransferProto.Transfer generateRegistre(T node) {
    return generateRegistre(Global.getId(Global.SequenceName.SEQUENCE_NAME_REGISTER), node);
  }

  public static <T extends Node> TransferProto.Transfer generateRegistre(long requestId, T node) {
    return generateRegistre(requestId, node, node);
  }

  public static <T extends Node> TransferProto.Transfer generateRegistre(long requestId, T node, Object data) {
    return TransferProto.Transfer.newBuilder()
            .setOperation(TransferProto.OperationType.REGISTER)
            .setClientId(node.getId())
            .setRequestId(requestId)
            .setServiceName(node.getName())
            .setData(ByteString.copyFromUtf8(JSON.toJSONString(data)))
            .build();
  }

  public static TransferProto.Transfer generateRegistreServiceRequest(Client client, Service service) {
    return TransferProto.Transfer.newBuilder()
            .setOperation(TransferProto.OperationType.SERVICE_REGISTER)
            .setClientId(client.getId())
            .setRequestId(Global.getId(Global.SequenceName.SEQUENCE_NAME_REGISTER))
            .setServiceName(client.getName())
            .setData(ByteString.copyFromUtf8(JSON.toJSONString(service)))
            .build();
  }

  public static TransferProto.Transfer generateRegistreServiceRequest(long requestId, Server server, Service service) {
    return TransferProto.Transfer.newBuilder()
            .setOperation(TransferProto.OperationType.SERVICE_REGISTER)
            .setClientId(server.getId())
            .setRequestId(requestId)
            .setServiceName(server.getName())
            .setData(ByteString.copyFromUtf8(JSON.toJSONString(service)))
            .build();
  }

  public static TransferProto.Transfer generateConnectRequest(long requestId, Node node, Service service) {
    return TransferProto.Transfer.newBuilder()
            .setOperation(TransferProto.OperationType.CONNECT)
            .setClientId(node.getId())
            .setRequestId(requestId)
            .setServiceName(node.getName())
            .setData(ByteString.copyFromUtf8(JSON.toJSONString(service)))
            .build();
  }

  public static TransferProto.Transfer generateDisconnectRequest(long requestId, Node node, Service service) {
    return TransferProto.Transfer.newBuilder()
            .setOperation(TransferProto.OperationType.DISCONNECT)
            .setClientId(node.getId())
            .setRequestId(requestId)
            .setServiceName(node.getName())
            .setData(ByteString.copyFromUtf8(JSON.toJSONString(service)))
            .build();
  }

  public static TransferProto.Transfer generateTransferRequest(long requestId, Node node, byte[] data) {
    return TransferProto.Transfer.newBuilder()
            .setOperation(TransferProto.OperationType.TRANSFER)
            .setClientId(node.getId())
            .setRequestId(requestId)
            .setServiceName(node.getName())
            .setData(ByteString.copyFrom(data))
            .build();
  }
}
