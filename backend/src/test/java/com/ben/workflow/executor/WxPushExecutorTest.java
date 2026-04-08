package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.executor.extension.ExecutorConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WxPushExecutor 单元测试
 * <p>
 * 使用内嵌 HTTP 服务器模拟 OpenClaw 微信 API，验证真实 HTTP 调用。
 */
class WxPushExecutorTest {

    private WxPushExecutor executor;
    private static HttpServer server;
    private static int port;
    private static final ObjectMapper mapper = new ObjectMapper();

    /** 记录收到的请求 payload，便于断言 */
    private static final CopyOnWriteArrayList<Map<String, Object>> receivedRequests = new CopyOnWriteArrayList<>();

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();

        // 模拟 OpenClaw /api/message/send 端点
        server.createContext("/api/message/send", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            byte[] body = exchange.getRequestBody().readAllBytes();
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = mapper.readValue(body, Map.class);
                receivedRequests.add(payload);
            } catch (Exception ignored) {
            }

            String response = "{\"success\":true}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.setExecutor(null);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.stop(0);
    }

    @BeforeEach
    void setUp() throws Exception {
        receivedRequests.clear();
        executor = new WxPushExecutor();
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("apiUrl", "http://localhost:" + port);
        ExecutorConfiguration config = ExecutorConfiguration.builder()
                .params(configMap)
                .build();
        executor.setConfiguration(config);
        executor.init();
    }

    // ===== 基本信息 =====

    @Test
    @DisplayName("应返回正确的类型")
    void shouldReturnCorrectType() {
        assertEquals("wx_push", executor.getType());
    }

    @Test
    @DisplayName("应返回正确的名称")
    void shouldReturnCorrectName() {
        assertEquals("Wechat Push", executor.getName());
    }

    @Test
    @DisplayName("应返回正确的描述")
    void shouldReturnCorrectDescription() {
        assertEquals("Push message via WeChat", executor.getDescription());
    }

    // ===== 成功场景 =====

    @Test
    @DisplayName("发送消息成功应返回正确输出")
    void shouldExecuteSuccessfully() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("to", "test_user_123");
        inputs.put("content", "Hello from workflow!");
        inputs.put("silent", false);

        NodeExecutionContext ctx = new NodeExecutionContext("wx-node-1", "wx-node-1", "wx_push", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        Map<String, Object> outputs = result.getOutputs();
        assertEquals(true, outputs.get("sent"));
        assertEquals("test_user_123", outputs.get("to"));
        assertEquals("Hello from workflow!", outputs.get("content"));
        assertEquals(false, outputs.get("silent"));
        assertNotNull(outputs.get("timestamp"));
    }

    @Test
    @DisplayName("API 应收到正确的请求参数")
    void shouldSendCorrectPayload() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("to", "wx_user_456");
        inputs.put("content", "Test payload");
        inputs.put("silent", true);

        NodeExecutionContext ctx = new NodeExecutionContext("wx-node-1", "wx-node-1", "wx_push", inputs);
        executor.execute(ctx);

        assertEquals(1, receivedRequests.size());
        Map<String, Object> payload = receivedRequests.get(0);
        assertEquals("wx_user_456", payload.get("recipient"));
        assertEquals("Test payload", payload.get("content"));
        assertEquals(true, payload.get("silent"));
    }

    @Test
    @DisplayName("默认接收者应留空（发给当前用户）")
    void shouldDefaultToCurrentUser() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("content", "Test message");

        NodeExecutionContext ctx = new NodeExecutionContext("wx-node-1", "wx-node-1", "wx_push", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertEquals("current_user", result.getOutputs().get("to"));

        // API 端 recipient 应为空字符串
        assertEquals(1, receivedRequests.size());
        assertEquals("", receivedRequests.get(0).get("recipient"));
    }

    @Test
    @DisplayName("静默推送应正确处理")
    void shouldHandleSilentPush() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("content", "Silent message");
        inputs.put("silent", true);

        NodeExecutionContext ctx = new NodeExecutionContext("wx-node-1", "wx-node-1", "wx_push", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertEquals(true, result.getOutputs().get("silent"));
        assertEquals(true, result.getOutputs().get("sent"));

        // API 端 silent 应为 true
        assertEquals(true, receivedRequests.get(0).get("silent"));
    }

    // ===== 变量替换 =====

    @Test
    @DisplayName("变量替换应正常工作")
    void shouldResolveVariables() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("content", "处理结果: {{result}}");
        inputs.put("result", "success");

        NodeExecutionContext ctx = new NodeExecutionContext("wx-node-1", "wx-node-1", "wx_push", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertEquals("处理结果: success", result.getOutputs().get("content"));

        // API 端应收到替换后的内容
        assertEquals("处理结果: success", receivedRequests.get(0).get("content"));
    }

    @Test
    @DisplayName("无变量模板应原样返回")
    void shouldReturnPlainContent() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("content", "纯文本消息，没有变量");

        NodeExecutionContext ctx = new NodeExecutionContext("wx-node-1", "wx-node-1", "wx_push", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertEquals("纯文本消息，没有变量", result.getOutputs().get("content"));
    }

    @Test
    @DisplayName("多个变量应都被替换")
    void shouldResolveMultipleVariables() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("content", "用户 {{name}} 的订单 {{orderId}} 已完成");
        inputs.put("name", "张三");
        inputs.put("orderId", "ORD-2026-001");

        NodeExecutionContext ctx = new NodeExecutionContext("wx-node-1", "wx-node-1", "wx_push", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertEquals("用户 张三 的订单 ORD-2026-001 已完成", result.getOutputs().get("content"));
    }

    // ===== 验证失败 =====

    @Test
    @DisplayName("空内容应验证失败")
    void shouldFailOnEmptyContent() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("content", "");

        NodeExecutionContext ctx = new NodeExecutionContext("wx-node-1", "wx-node-1", "wx_push", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertFalse(result.isSuccess());
        // 不应发送 API 请求
        assertEquals(0, receivedRequests.size());
    }

    @Test
    @DisplayName("缺少 content 参数应验证失败")
    void shouldFailOnMissingContent() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("to", "user123");

        NodeExecutionContext ctx = new NodeExecutionContext("wx-node-1", "wx-node-1", "wx_push", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertFalse(result.isSuccess());
        assertEquals(0, receivedRequests.size());
    }

    // ===== API 调用失败 =====

    @Test
    @DisplayName("API 不可达时 sent 应为 false")
    void shouldHandleApiFailure() throws Exception {
        // 创建一个指向不存在端口的 executor
        WxPushExecutor badExecutor = new WxPushExecutor();
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("apiUrl", "http://localhost:19999");
        configMap.put("timeoutSeconds", "3");
        ExecutorConfiguration config = ExecutorConfiguration.builder()
                .params(configMap)
                .build();
        badExecutor.setConfiguration(config);
        badExecutor.init();

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("content", "This should fail");

        NodeExecutionContext ctx = new NodeExecutionContext("wx-node-1", "wx-node-1", "wx_push", inputs);
        NodeExecutionResult result = badExecutor.execute(ctx);

        // 执行本身不抛异常，但 sent=false
        assertTrue(result.isSuccess());
        assertEquals(false, result.getOutputs().get("sent"));
    }
}
