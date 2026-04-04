package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HttpRequestExecutor 单元测试
 */
public class HttpRequestExecutorTest {

    private HttpRequestExecutor executor;
    private static HttpServer server;
    private static int port;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();

        // GET endpoint
        server.createContext("/api/test", exchange -> {
            String method = exchange.getRequestMethod();
            String response;
            int code = 200;

            switch (method) {
                case "GET" -> response = "{\"message\":\"hello\"}";
                case "POST" -> {
                    byte[] body = exchange.getRequestBody().readAllBytes();
                    response = "{\"received\":\"" + new String(body) + "\"}";
                }
                case "PUT" -> {
                    byte[] body = exchange.getRequestBody().readAllBytes();
                    response = "{\"updated\":\"" + new String(body) + "\"}";
                }
                case "DELETE" -> response = "{\"deleted\":true}";
                default -> {
                    response = "Method not allowed";
                    code = 405;
                }
            }

            // Echo back custom header if present
            String customHeader = exchange.getRequestHeaders().getFirst("X-Custom");
            if (customHeader != null) {
                exchange.getResponseHeaders().add("X-Echo", customHeader);
            }

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // 404 endpoint
        server.createContext("/api/notfound", exchange -> {
            byte[] bytes = "not found".getBytes();
            exchange.sendResponseHeaders(404, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // Slow endpoint (for timeout test)
        server.createContext("/api/slow", exchange -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {}
            byte[] bytes = "slow".getBytes();
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
    void setUp() {
        executor = new HttpRequestExecutor();
    }

    // ===== 基本信息 =====

    @Test
    void testGetType() {
        assertEquals("http_request", executor.getType());
    }

    @Test
    void testGetName() {
        assertEquals("HTTP 请求", executor.getName());
    }

    @Test
    void testGetDescription() {
        assertNotNull(executor.getDescription());
    }

    // ===== GET 请求 =====

    @Test
    void testGetRequest() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/test");
        inputs.put("method", "GET");

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getOutput("status_code"));
        String body = (String) result.getOutput("response_body");
        assertTrue(body.contains("hello"));
    }

    @Test
    void testDefaultMethodIsGet() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/test");
        // 不设置 method，默认 GET

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getOutput("status_code"));
    }

    // ===== POST 请求 =====

    @Test
    void testPostRequest() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/test");
        inputs.put("method", "POST");
        inputs.put("body", "{\"name\":\"test\"}");

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getOutput("status_code"));
        String body = (String) result.getOutput("response_body");
        assertTrue(body.contains("received"));
    }

    // ===== PUT 请求 =====

    @Test
    void testPutRequest() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/test");
        inputs.put("method", "PUT");
        inputs.put("body", "{\"data\":\"update\"}");

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getOutput("status_code"));
        String body = (String) result.getOutput("response_body");
        assertTrue(body.contains("updated"));
    }

    // ===== DELETE 请求 =====

    @Test
    void testDeleteRequest() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/test");
        inputs.put("method", "DELETE");

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getOutput("status_code"));
        String body = (String) result.getOutput("response_body");
        assertTrue(body.contains("deleted"));
    }

    // ===== 自定义 Headers =====

    @Test
    void testCustomHeaders() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/test");
        inputs.put("method", "GET");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom", "my-value");
        inputs.put("headers", headers);

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, String> responseHeaders = (Map<String, String>) result.getOutput("response_headers");
        assertNotNull(responseHeaders);
        // server echoes X-Custom back as X-Echo (case may vary)
        String echoValue = responseHeaders.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase("X-Echo"))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertEquals("my-value", echoValue);
    }

    // ===== 错误场景 =====

    @Test
    void testMissingUrl() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        // 不设置 url

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isFailed());
        assertTrue(result.getErrorMessage().contains("url"));
    }

    @Test
    void testEmptyUrl() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "");

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isFailed());
    }

    @Test
    void testUnsupportedMethod() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/test");
        inputs.put("method", "PATCH");

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isFailed());
        assertTrue(result.getErrorMessage().contains("不支持"));
    }

    @Test
    void test404Response() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/notfound");

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        // 404 不是执行失败，只是 HTTP 状态码
        assertTrue(result.isSuccess());
        assertEquals(404, result.getOutput("status_code"));
    }

    @Test
    void testTimeout() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/slow");
        inputs.put("timeout", 1); // 1 秒超时

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isFailed());
    }

    @Test
    void testInvalidUrl() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "not-a-url");

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isFailed());
    }

    @Test
    void testConnectionRefused() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:19999/api/test"); // 不存在的端口
        inputs.put("timeout", 3);

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isFailed());
    }

    @Test
    void testNullContext() throws Exception {
        NodeExecutionResult result = executor.execute(null);
        assertTrue(result.isFailed());
    }

    // ===== 方法大小写兼容 =====

    @Test
    void testMethodCaseInsensitive() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/test");
        inputs.put("method", "get"); // 小写

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getOutput("status_code"));
    }

    // ===== 输出格式验证 =====

    @Test
    void testOutputContainsAllFields() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/test");

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
        assertNotNull(result.getOutput("status_code"));
        assertNotNull(result.getOutput("response_body"));
        assertNotNull(result.getOutput("response_headers"));
    }

    // ===== timeout 参数类型兼容 =====

    @Test
    void testTimeoutAsString() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("url", "http://localhost:" + port + "/api/test");
        inputs.put("timeout", "10"); // String 类型

        NodeExecutionContext ctx = new NodeExecutionContext("node-1", "node-1", "http_request", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isSuccess());
    }
}
