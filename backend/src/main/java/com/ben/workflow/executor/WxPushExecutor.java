package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.workflow.executor.extension.BaseExecutor;
import com.ben.workflow.executor.extension.ExecutorMeta;
import com.ben.workflow.executor.extension.ParameterSchema;
import com.ben.workflow.executor.extension.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

/**
 * 微信推送执行器
 * <p>
 * 通过 OpenClaw 微信插件 API 推送消息给指定用户，支持变量替换和静默推送。
 * <p>
 * config 参数说明：
 * <ul>
 *   <li>to (String, 可选) - 接收者微信ID，留空发给当前用户</li>
 *   <li>content (String, 必填) - 消息内容，支持变量替换如 {{node.output}}</li>
 *   <li>silent (Boolean, 可选) - 是否静默推送，默认 false</li>
 * </ul>
 * <p>
 * 执行器配置（ExecutorConfiguration）：
 * <ul>
 *   <li>apiUrl (String, 可选) - OpenClaw API 地址，默认 http://localhost:8080</li>
 *   <li>timeoutSeconds (int, 可选) - API 调用超时秒数，默认 10</li>
 * </ul>
 */
@ExecutorMeta(
    type = "wx_push",
    name = "Wechat Push",
    description = "Push message via WeChat",
    category = "integration",
    icon = "💬"
)
public class WxPushExecutor extends BaseExecutor {

    private static final Logger logger = LoggerFactory.getLogger(WxPushExecutor.class);

    private static final String DEFAULT_API_URL = "http://127.0.0.1:8080";
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final String API_PATH = "/api/message/send";

    private String wxApiUrl;
    private int timeoutSeconds;
    private WebClient webClient;

    @Override
    public String getType() {
        return "wx_push";
    }

    @Override
    public String getName() {
        return "Wechat Push";
    }

    @Override
    public String getDescription() {
        return "Push message via WeChat";
    }

    @Override
    protected void doInitialize() throws Exception {
        if (configuration != null) {
            this.wxApiUrl = configuration.getString("apiUrl", DEFAULT_API_URL);
            this.timeoutSeconds = Integer.parseInt(
                configuration.getString("timeoutSeconds", String.valueOf(DEFAULT_TIMEOUT_SECONDS))
            );
        } else {
            this.wxApiUrl = DEFAULT_API_URL;
            this.timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        }
        this.webClient = WebClient.builder()
                .baseUrl(wxApiUrl)
                .build();
        logger.info("WxPushExecutor initialized, apiUrl={}", wxApiUrl);
    }

    /**
     * 允许外部注入 WebClient（用于测试）
     */
    void setWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    protected void doValidate(NodeExecutionContext context) throws ValidationException {
        super.doValidate(context);
        requireInput(context, "content");
    }

    @Override
    protected Map<String, Object> doExecute(NodeExecutionContext context) throws Exception {
        String to = getInputString(context, "to", "");
        String content = getInputString(context, "content", "");
        boolean silent = Boolean.parseBoolean(
            String.valueOf(getInput(context, "silent") != null ? getInput(context, "silent") : "false")
        );

        logger.info("WxPush: sending to={}, silent={}, contentLength={}", to, silent, content.length());

        // 变量替换: 处理 {{xxx}} 模式
        content = resolveVariables(content, context);

        // 检查是否需要分割发送（支持多部分推送）
        boolean success = true;
        if (content.contains("--第一部分--") && content.contains("--第二部分--")) {
            // 分割内容，分两条发送
            String[] parts = content.split("--第二部分--");
            String part1 = parts[0].replace("--第一部分--", "").trim();
            String part2 = "--第二部分--" + parts[1];
            
            logger.info("WxPush: splitting into 2 messages");
            boolean r1 = doSendWxMessage(to, part1, silent);
            boolean r2 = doSendWxMessage(to, part2, silent);
            success = r1 && r2;
        } else {
            // 单一消息发送
            success = doSendWxMessage(to, content, silent);
        }

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("sent", success);
        outputs.put("to", to.isEmpty() ? "current_user" : to);
        outputs.put("content", content);
        outputs.put("silent", silent);
        outputs.put("timestamp", System.currentTimeMillis());
        return outputs;
    }

    /**
     * 通过后端 API 发送微信消息（内部调用 OpenClaw）
     */
    boolean doSendWxMessage(String to, String content, boolean silent) {
        try {
            // 构建请求体
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("content", content);
            payload.put("to", to != null && !to.isEmpty() ? to : "current_user");
            payload.put("silent", silent);
            
            // 调用后端消息 API
            String response = webClient.post()
                    .uri("/api/v1/message/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(timeoutSeconds));
            
            logger.info("WxPush API response: {}", response);
            return response != null && response.contains("success");
        } catch (Exception e) {
            logger.error("WxPush API call failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 简单的变量替换: {{key}} -> context 中对应的值
     */
    private String resolveVariables(String template, NodeExecutionContext context) {
        if (template == null || !template.contains("{{")) {
            return template;
        }
        String result = template;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{(.+?)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object val = getInput(context, key);
            if (val != null) {
                result = result.replace(matcher.group(0), val.toString());
            }
        }
        return result;
    }

    @Override
    protected List<ParameterSchema> defineInputParams() {
        return List.of(
            ParameterSchema.builder()
                .name("to").type("string").label("To")
                .description("Receiver WeChat ID (leave empty for current user)")
                .required(false).build(),
            ParameterSchema.builder()
                .name("content").type("string").label("Content")
                .description("Message content (supports {{variable}} replacement)")
                .required(true).build(),
            ParameterSchema.builder()
                .name("silent").type("boolean").label("Silent")
                .description("Silent push (no notification sound)")
                .required(false).build()
        );
    }

    @Override
    protected List<ParameterSchema> defineOutputParams() {
        return List.of(
            ParameterSchema.builder()
                .name("sent").type("boolean").label("Sent")
                .description("Whether message was sent successfully").build(),
            ParameterSchema.builder()
                .name("to").type("string").label("To")
                .description("Receiver ID").build(),
            ParameterSchema.builder()
                .name("content").type("string").label("Content")
                .description("Sent message content").build(),
            ParameterSchema.builder()
                .name("silent").type("boolean").label("Silent")
                .description("Whether silent push").build(),
            ParameterSchema.builder()
                .name("timestamp").type("number").label("Timestamp")
                .description("Send timestamp").build()
        );
    }
}
