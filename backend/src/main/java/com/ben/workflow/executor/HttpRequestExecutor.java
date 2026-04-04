package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.spi.NodeComponent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * HTTP 请求节点执行器
 * <p>
 * 支持 GET/POST/PUT/DELETE 请求，自定义 headers、body、timeout。
 * <p>
 * config 参数说明：
 * <ul>
 *   <li>url (String, 必填) - 请求地址</li>
 *   <li>method (String, 可选, 默认 GET) - 请求方法: GET/POST/PUT/DELETE</li>
 *   <li>headers (Map&lt;String, String&gt;, 可选) - 自定义请求头</li>
 *   <li>body (String, 可选) - 请求体（POST/PUT 时使用）</li>
 *   <li>timeout (Integer, 可选, 默认 30) - 超时秒数</li>
 * </ul>
 * <p>
 * 输出：
 * <ul>
 *   <li>status_code (int) - HTTP 响应状态码</li>
 *   <li>response_body (String) - 响应体内容</li>
 *   <li>response_headers (Map) - 响应头</li>
 * </ul>
 */
@NodeComponent(value = "http_request", name = "HTTP 请求", description = "发起 HTTP/HTTPS 请求")
public class HttpRequestExecutor implements NodeExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "DELETE");

    @Override
    public String getType() {
        return "http_request";
    }

    @Override
    public String getName() {
        return "HTTP 请求";
    }

    @Override
    public String getDescription() {
        return "发起 HTTP/HTTPS 请求（GET/POST/PUT/DELETE）";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) throws Exception {
        LocalDateTime startTime = LocalDateTime.now();
        String nodeId = context != null ? context.getNodeId() : "unknown";

        try {
            Map<String, Object> inputs = context != null ? context.getInputs() : Collections.emptyMap();
            if (inputs == null) inputs = Collections.emptyMap();

            // 解析参数
            String url = getStringParam(inputs, "url", null);
            if (url == null || url.isBlank()) {
                return NodeExecutionResult.failed(nodeId, "缺少必填参数: url", null, startTime, LocalDateTime.now());
            }

            String method = getStringParam(inputs, "method", "GET").toUpperCase();
            if (!ALLOWED_METHODS.contains(method)) {
                return NodeExecutionResult.failed(nodeId, "不支持的 HTTP 方法: " + method + "，仅支持 " + ALLOWED_METHODS, null, startTime, LocalDateTime.now());
            }

            String body = getStringParam(inputs, "body", null);
            int timeout = getIntParam(inputs, "timeout", DEFAULT_TIMEOUT_SECONDS);

            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) inputs.get("headers");

            // 构建请求
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout));

            // 设置 headers
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        requestBuilder.header(entry.getKey(), entry.getValue());
                    }
                }
            }

            // 设置方法和 body
            HttpRequest.BodyPublisher bodyPublisher = (body != null && !body.isEmpty())
                    ? HttpRequest.BodyPublishers.ofString(body)
                    : HttpRequest.BodyPublishers.noBody();

            switch (method) {
                case "GET" -> requestBuilder.GET();
                case "POST" -> requestBuilder.POST(bodyPublisher);
                case "PUT" -> requestBuilder.PUT(bodyPublisher);
                case "DELETE" -> requestBuilder.DELETE();
            }

            HttpRequest request = requestBuilder.build();

            // 发送请求
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 收集响应头
            Map<String, String> responseHeaders = new LinkedHashMap<>();
            response.headers().map().forEach((key, values) -> {
                if (key != null && values != null && !values.isEmpty()) {
                    responseHeaders.put(key, String.join(", ", values));
                }
            });

            // 构建输出
            Map<String, Object> outputs = new LinkedHashMap<>();
            outputs.put("status_code", response.statusCode());
            outputs.put("response_body", response.body());
            outputs.put("response_headers", responseHeaders);

            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.success(nodeId, outputs, startTime, endTime);

        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.failed(nodeId, e, startTime, endTime);
        }
    }

    private String getStringParam(Map<String, Object> inputs, String key, String defaultValue) {
        Object val = inputs.get(key);
        if (val == null) return defaultValue;
        return val.toString();
    }

    private int getIntParam(Map<String, Object> inputs, String key, int defaultValue) {
        Object val = inputs.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
