package com.ben.workflow.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocketNotificationService 单元测试
 */
class WebSocketNotificationServiceTest {

    private WebSocketNotificationService notificationService;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        notificationService = new WebSocketNotificationService();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @org.junit.jupiter.api.AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("测试通知执行开始")
    void testNotifyExecutionStart() {
        notificationService.notifyExecutionStart("exec-123");

        String output = outContent.toString();
        assertTrue(output.contains("[WebSocket]"));
        assertTrue(output.contains("执行开始"));
        assertTrue(output.contains("exec-123"));
    }

    @Test
    @DisplayName("测试通知节点开始")
    void testNotifyNodeStart() {
        notificationService.notifyNodeStart("exec-123", "node-456");

        String output = outContent.toString();
        assertTrue(output.contains("[WebSocket]"));
        assertTrue(output.contains("节点开始"));
        assertTrue(output.contains("exec-123"));
        assertTrue(output.contains("node-456"));
    }

    @Test
    @DisplayName("测试通知节点完成")
    void testNotifyNodeComplete() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        
        notificationService.notifyNodeComplete("exec-123", "node-456", result);

        String output = outContent.toString();
        assertTrue(output.contains("[WebSocket]"));
        assertTrue(output.contains("节点完成"));
        assertTrue(output.contains("exec-123"));
        assertTrue(output.contains("node-456"));
    }

    @Test
    @DisplayName("测试通知节点失败")
    void testNotifyNodeFailed() {
        notificationService.notifyNodeFailed("exec-123", "node-456", "连接超时");

        String output = errContent.toString();
        assertTrue(output.contains("[WebSocket]"));
        assertTrue(output.contains("节点失败"));
        assertTrue(output.contains("exec-123"));
        assertTrue(output.contains("node-456"));
        assertTrue(output.contains("连接超时"));
    }

    @Test
    @DisplayName("测试通知执行完成")
    void testNotifyExecutionComplete() {
        Map<String, Object> result = new HashMap<>();
        result.put("output", "completed");
        
        notificationService.notifyExecutionComplete("exec-123", result);

        String output = outContent.toString();
        assertTrue(output.contains("[WebSocket]"));
        assertTrue(output.contains("执行完成"));
        assertTrue(output.contains("exec-123"));
    }

    @Test
    @DisplayName("测试多次通知")
    void testMultipleNotifications() {
        notificationService.notifyExecutionStart("exec-123");
        notificationService.notifyNodeStart("exec-123", "node-1");
        notificationService.notifyNodeComplete("exec-123", "node-1", new HashMap<>());
        notificationService.notifyExecutionComplete("exec-123", new HashMap<>());

        String output = outContent.toString();
        assertTrue(output.contains("执行开始"));
        assertTrue(output.contains("节点开始"));
        assertTrue(output.contains("节点完成"));
        assertTrue(output.contains("执行完成"));
    }

    @Test
    @DisplayName("测试通知空实例 ID")
    void testNotifyWithEmptyInstanceId() {
        notificationService.notifyExecutionStart("");

        String output = outContent.toString();
        assertTrue(output.contains("[WebSocket]"));
        assertTrue(output.contains("执行开始"));
    }

    @Test
    @DisplayName("测试通知 null 结果")
    void testNotifyNodeCompleteWithNullResult() {
        notificationService.notifyNodeComplete("exec-123", "node-456", null);

        String output = outContent.toString();
        assertTrue(output.contains("[WebSocket]"));
        assertTrue(output.contains("节点完成"));
    }

    @Test
    @DisplayName("测试服务实例化")
    void testServiceInstantiation() {
        WebSocketNotificationService service = new WebSocketNotificationService();
        assertNotNull(service);
    }

    @Test
    @DisplayName("测试实现 NotificationService 接口")
    void testImplementsNotificationService() {
        assertTrue(notificationService instanceof NotificationService);
    }
}
