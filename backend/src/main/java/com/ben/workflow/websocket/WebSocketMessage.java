package com.ben.workflow.websocket;

import java.time.Instant;
import java.util.Map;

public class WebSocketMessage {
    private MessageType type;
    private String executionId;
    private String nodeId;
    private String status;
    private Integer progress;
    private String message;
    private Map<String, Object> data;
    private Instant timestamp;

    public enum MessageType { EXECUTION_START, EXECUTION_PROGRESS, NODE_START, NODE_COMPLETE, EXECUTION_COMPLETE, EXECUTION_FAILED, EXECUTION_CANCELLED }

    public WebSocketMessage() {}
    public WebSocketMessage(MessageType type, String executionId, String nodeId, String status, Integer progress, String message, Map<String, Object> data, Instant timestamp) {
        this.type = type; this.executionId = executionId; this.nodeId = nodeId; this.status = status; this.progress = progress; this.message = message; this.data = data; this.timestamp = timestamp;
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public static WebSocketMessage executionStart(String executionId) {
        return new WebSocketMessage(MessageType.EXECUTION_START, executionId, null, null, 0, "工作流执行开始", null, Instant.now());
    }
    public static WebSocketMessage nodeStart(String executionId, String nodeId) {
        return new WebSocketMessage(MessageType.NODE_START, executionId, nodeId, "RUNNING", null, "节点开始执行", null, Instant.now());
    }
    public static WebSocketMessage nodeComplete(String executionId, String nodeId, Map<String, Object> result) {
        return new WebSocketMessage(MessageType.NODE_COMPLETE, executionId, nodeId, "SUCCESS", 100, "节点执行完成", result, Instant.now());
    }
    public static WebSocketMessage executionComplete(String executionId, Map<String, Object> outputs) {
        return new WebSocketMessage(MessageType.EXECUTION_COMPLETE, executionId, null, "SUCCESS", 100, "工作流执行完成", outputs, Instant.now());
    }
    public static WebSocketMessage executionFailed(String executionId, String errorMessage) {
        return new WebSocketMessage(MessageType.EXECUTION_FAILED, executionId, null, "FAILED", null, errorMessage, null, Instant.now());
    }
}
