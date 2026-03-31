package com.ben.workflow.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocketMessage 单元测试
 */
class WebSocketMessageTest {

    private WebSocketMessage webSocketMessage;

    @BeforeEach
    void setUp() {
        webSocketMessage = new WebSocketMessage();
    }

    @Test
    @DisplayName("测试默认构造函数")
    void testDefaultConstructor() {
        WebSocketMessage message = new WebSocketMessage();
        assertNotNull(message);
    }

    @Test
    @DisplayName("测试全参数构造函数")
    void testFullConstructor() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        Instant timestamp = Instant.now();

        WebSocketMessage message = new WebSocketMessage(
                WebSocketMessage.MessageType.EXECUTION_START,
                "exec-123",
                "node-456",
                "RUNNING",
                50,
                "测试消息",
                data,
                timestamp
        );

        assertEquals(WebSocketMessage.MessageType.EXECUTION_START, message.getType());
        assertEquals("exec-123", message.getExecutionId());
        assertEquals("node-456", message.getNodeId());
        assertEquals("RUNNING", message.getStatus());
        assertEquals(50, message.getProgress());
        assertEquals("测试消息", message.getMessage());
        assertEquals(data, message.getData());
        assertEquals(timestamp, message.getTimestamp());
    }

    @Test
    @DisplayName("测试 Setter 和 Getter")
    void testSettersAndGetters() {
        webSocketMessage.setType(WebSocketMessage.MessageType.NODE_START);
        webSocketMessage.setExecutionId("exec-123");
        webSocketMessage.setNodeId("node-456");
        webSocketMessage.setStatus("RUNNING");
        webSocketMessage.setProgress(75);
        webSocketMessage.setMessage("更新消息");
        webSocketMessage.setData(new HashMap<>());
        webSocketMessage.setTimestamp(Instant.now());

        assertEquals(WebSocketMessage.MessageType.NODE_START, webSocketMessage.getType());
        assertEquals("exec-123", webSocketMessage.getExecutionId());
        assertEquals("node-456", webSocketMessage.getNodeId());
        assertEquals("RUNNING", webSocketMessage.getStatus());
        assertEquals(75, webSocketMessage.getProgress());
        assertEquals("更新消息", webSocketMessage.getMessage());
        assertNotNull(webSocketMessage.getData());
        assertNotNull(webSocketMessage.getTimestamp());
    }

    @Test
    @DisplayName("测试 executionStart 静态方法")
    void testExecutionStartStaticMethod() {
        WebSocketMessage message = WebSocketMessage.executionStart("exec-123");

        assertEquals(WebSocketMessage.MessageType.EXECUTION_START, message.getType());
        assertEquals("exec-123", message.getExecutionId());
        assertNull(message.getNodeId());
        assertEquals(0, message.getProgress());
        assertEquals("工作流执行开始", message.getMessage());
        assertNotNull(message.getTimestamp());
    }

    @Test
    @DisplayName("测试 nodeStart 静态方法")
    void testNodeStartStaticMethod() {
        WebSocketMessage message = WebSocketMessage.nodeStart("exec-123", "node-456");

        assertEquals(WebSocketMessage.MessageType.NODE_START, message.getType());
        assertEquals("exec-123", message.getExecutionId());
        assertEquals("node-456", message.getNodeId());
        assertEquals("RUNNING", message.getStatus());
        assertNull(message.getProgress()); // nodeStart returns null for progress
        assertEquals("节点开始执行", message.getMessage());
    }

    @Test
    @DisplayName("测试 nodeComplete 静态方法")
    void testNodeCompleteStaticMethod() {
        Map<String, Object> result = new HashMap<>();
        result.put("output", "completed");
        
        WebSocketMessage message = WebSocketMessage.nodeComplete("exec-123", "node-456", result);

        assertEquals(WebSocketMessage.MessageType.NODE_COMPLETE, message.getType());
        assertEquals("exec-123", message.getExecutionId());
        assertEquals("node-456", message.getNodeId());
        assertEquals("SUCCESS", message.getStatus());
        assertEquals(100, message.getProgress());
        assertEquals("节点执行完成", message.getMessage());
        assertEquals(result, message.getData());
    }

    @Test
    @DisplayName("测试 executionComplete 静态方法")
    void testExecutionCompleteStaticMethod() {
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("final", "result");
        
        WebSocketMessage message = WebSocketMessage.executionComplete("exec-123", outputs);

        assertEquals(WebSocketMessage.MessageType.EXECUTION_COMPLETE, message.getType());
        assertEquals("exec-123", message.getExecutionId());
        assertNull(message.getNodeId());
        assertEquals("SUCCESS", message.getStatus());
        assertEquals(100, message.getProgress());
        assertEquals("工作流执行完成", message.getMessage());
        assertEquals(outputs, message.getData());
    }

    @Test
    @DisplayName("测试 executionFailed 静态方法")
    void testExecutionFailedStaticMethod() {
        WebSocketMessage message = WebSocketMessage.executionFailed("exec-123", "测试错误信息");

        assertEquals(WebSocketMessage.MessageType.EXECUTION_FAILED, message.getType());
        assertEquals("exec-123", message.getExecutionId());
        assertNull(message.getNodeId());
        assertEquals("FAILED", message.getStatus());
        assertEquals("测试错误信息", message.getMessage());
    }

    @Test
    @DisplayName("测试所有 MessageType 枚举值")
    void testAllMessageTypes() {
        WebSocketMessage.MessageType[] types = WebSocketMessage.MessageType.values();
        
        assertEquals(7, types.length);
        assertEquals(WebSocketMessage.MessageType.EXECUTION_START, types[0]);
        assertEquals(WebSocketMessage.MessageType.EXECUTION_PROGRESS, types[1]);
        assertEquals(WebSocketMessage.MessageType.NODE_START, types[2]);
        assertEquals(WebSocketMessage.MessageType.NODE_COMPLETE, types[3]);
        assertEquals(WebSocketMessage.MessageType.EXECUTION_COMPLETE, types[4]);
        assertEquals(WebSocketMessage.MessageType.EXECUTION_FAILED, types[5]);
        assertEquals(WebSocketMessage.MessageType.EXECUTION_CANCELLED, types[6]);
    }

    @Test
    @DisplayName("测试消息数据修改")
    void testMessageDataModification() {
        Map<String, Object> data = new HashMap<>();
        data.put("initial", "value");
        webSocketMessage.setData(data);
        
        data.put("modified", "data");
        assertEquals(2, webSocketMessage.getData().size());
    }

    @Test
    @DisplayName("测试 null 值设置")
    void testNullValueSetting() {
        webSocketMessage.setType(null);
        webSocketMessage.setExecutionId(null);
        webSocketMessage.setNodeId(null);
        webSocketMessage.setStatus(null);
        webSocketMessage.setProgress(null);
        webSocketMessage.setMessage(null);
        webSocketMessage.setData(null);
        webSocketMessage.setTimestamp(null);

        assertNull(webSocketMessage.getType());
        assertNull(webSocketMessage.getExecutionId());
        assertNull(webSocketMessage.getNodeId());
        assertNull(webSocketMessage.getStatus());
        assertNull(webSocketMessage.getProgress());
        assertNull(webSocketMessage.getMessage());
        assertNull(webSocketMessage.getData());
        assertNull(webSocketMessage.getTimestamp());
    }
}
