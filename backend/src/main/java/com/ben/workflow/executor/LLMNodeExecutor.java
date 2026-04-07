package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.spi.NodeComponent;
import com.ben.workflow.util.ConfigUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 节点执行器
 * <p>
 * 调用 OpenAI 兼容 API（支持 OpenAI、Anthropic、通义千问、DeepSeek 等）。
 * <p>
 * config 参数说明：
 * <ul>
 *   <li>model (String, 可选, 默认 "gpt-4o") - 模型名称</li>
 *   <li>systemPrompt (String, 可选) - 系统提示词</li>
 *   <li>userPrompt (String, 必填) - 用户提示词，支持 {{variable}} 变量替换</li>
 *   <li>temperature (Number, 可选, 默认 0.7) - 温度 0~2</li>
 *   <li>maxTokens (Integer, 可选, 默认 2048) - 最大 token 数</li>
 *   <li>apiKey (String, 可选) - API Key，优先级：config > 环境变量</li>
 *   <li>baseUrl (String, 可选) - API 地址，默认 https://api.openai.com/v1</li>
 * </ul>
 * <p>
 * 变量替换：userPrompt / systemPrompt 中的 {{variable}} 会从 inputs 中查找替换。
 * <p>
 * 输出：
 * <ul>
 *   <li>content (String) - LLM 回复文本</li>
 *   <li>model (String) - 实际使用的模型</li>
 *   <li>usage (Map) - token 用量：prompt_tokens, completion_tokens, total_tokens</li>
 *   <li>finish_reason (String) - 结束原因</li>
 * </ul>
 */
@NodeComponent(value = "llm", name = "LLM 大模型", description = "调用大语言模型（OpenAI/Anthropic/通义千问等）")
public class LLMNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(LLMNodeExecutor.class);

    // 腾讯云 Hunyuan LLM 配置
    private static final String DEFAULT_MODEL = "hunyuan-2.0-instruct";
    private static final String DEFAULT_BASE_URL = "https://api.lkeap.cloud.tencent.com/coding/v3";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 2048;
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    /** {{variable}} 匹配 */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    @Autowired(required = false)
    private WebClient.Builder webClientBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "llm";
    }

    @Override
    public String getName() {
        return "LLM 大模型";
    }

    @Override
    public String getDescription() {
        return "调用大语言模型（OpenAI/Anthropic/通义千问等）";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) throws Exception {
        LocalDateTime startTime = LocalDateTime.now();
        String nodeId = context != null ? context.getNodeId() : "unknown";

        try {
            Map<String, Object> inputs = context != null ? context.getInputs() : Collections.emptyMap();
            if (inputs == null) inputs = Collections.emptyMap();

            // ---- 解析配置（使用 ConfigUtils 消除重复） ----
            String model = ConfigUtils.getString(inputs, "model", DEFAULT_MODEL);
            String systemPrompt = ConfigUtils.getString(inputs, "systemPrompt", null);
            String userPrompt = ConfigUtils.getString(inputs, "userPrompt", null);
            double temperature = ConfigUtils.getDouble(inputs, "temperature", DEFAULT_TEMPERATURE);
            int maxTokens = ConfigUtils.getInt(inputs, "maxTokens", DEFAULT_MAX_TOKENS);
            String baseUrl = ConfigUtils.getString(inputs, "baseUrl", DEFAULT_BASE_URL);
            String apiKey = resolveApiKey(inputs, baseUrl);
            int timeout = ConfigUtils.getInt(inputs, "timeout", DEFAULT_TIMEOUT_SECONDS);

            // 校验
            if (userPrompt == null || userPrompt.isBlank()) {
                return NodeExecutionResult.failed(nodeId, "缺少必填参数: userPrompt", null, startTime, LocalDateTime.now());
            }
            // 如果没有 API Key，使用模拟返回（演示模式）
            if (apiKey == null || apiKey.isBlank()) {
                log.info("[LLM] Mock 模式：nodeId={}, prompt={}", nodeId, userPrompt);
                
                // 模拟 LLM 返回
                String mockResponse = "【模拟回复】您好！这是来自 " + model + " 的模拟响应。\n\n" 
                    + "原始问题：" + userPrompt + "\n\n"
                    + "(要使用真实 LLM，请在配置中设置 apiKey)";
                
                Map<String, Object> outputs = new LinkedHashMap<>();
                outputs.put("content", mockResponse);
                outputs.put("model", model);
                outputs.put("usage", Map.of("prompt_tokens", 10, "completion_tokens", 50, "total_tokens", 60));
                outputs.put("finish_reason", "stop");
                outputs.put("_mock", true);
                
                return NodeExecutionResult.success(nodeId, outputs, startTime, LocalDateTime.now());
            }

            // clamp temperature
            temperature = Math.max(0, Math.min(2, temperature));

            // ---- 变量替换 ----
            userPrompt = replaceVariables(userPrompt, inputs);
            if (systemPrompt != null) {
                systemPrompt = replaceVariables(systemPrompt, inputs);
            }

            // ---- 构建 messages ----
            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userPrompt));

            // ---- 构建请求体 ----
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            String endpoint = normalizeBaseUrl(baseUrl) + "/chat/completions";

            log.info("[LLM] node={}, model={}, endpoint={}, promptLen={}", nodeId, model, endpoint, userPrompt.length());

            // ---- 发起请求 ----
            WebClient client = (webClientBuilder != null ? webClientBuilder : WebClient.builder()).build();

            String responseJson = client.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeout))
                    .block();

            if (responseJson == null || responseJson.isBlank()) {
                return NodeExecutionResult.failed(nodeId, "LLM API 返回空响应", null, startTime, LocalDateTime.now());
            }

            // ---- 解析响应 ----
            JsonNode root = objectMapper.readTree(responseJson);

            // 错误检查
            if (root.has("error")) {
                String errMsg = root.path("error").path("message").asText("unknown error");
                return NodeExecutionResult.failed(nodeId, "LLM API 错误: " + errMsg, null, startTime, LocalDateTime.now());
            }

            // 提取回复
            JsonNode choices = root.path("choices");
            String content = "";
            String finishReason = "";
            if (choices.isArray() && choices.size() > 0) {
                JsonNode first = choices.get(0);
                content = first.path("message").path("content").asText("");
                finishReason = first.path("finish_reason").asText("");
            }

            // 提取 usage
            Map<String, Object> usage = new LinkedHashMap<>();
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode()) {
                usage.put("prompt_tokens", usageNode.path("prompt_tokens").asInt(0));
                usage.put("completion_tokens", usageNode.path("completion_tokens").asInt(0));
                usage.put("total_tokens", usageNode.path("total_tokens").asInt(0));
            }

            String actualModel = root.path("model").asText(model);

            log.info("[LLM] node={}, model={}, tokens={}, finishReason={}", nodeId, actualModel, usage, finishReason);

            // ---- 输出 ----
            Map<String, Object> outputs = new LinkedHashMap<>();
            outputs.put("content", content);
            outputs.put("model", actualModel);
            outputs.put("usage", usage);
            outputs.put("finish_reason", finishReason);

            return NodeExecutionResult.success(nodeId, outputs, startTime, LocalDateTime.now());

        } catch (Exception e) {
            log.error("[LLM] node={} 执行失败", nodeId, e);
            return NodeExecutionResult.failed(nodeId, e, startTime, LocalDateTime.now());
        }
    }

    // ========== 变量替换 ==========

    /**
     * 将 {{variable}} 替换为 inputs 中对应的值
     */
    String replaceVariables(String template, Map<String, Object> variables) {
        if (template == null || variables == null) return template;

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object val = variables.get(varName);
            String replacement = val != null ? val.toString() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // ========== API Key 解析 ==========

    private String resolveApiKey(Map<String, Object> inputs, String baseUrl) {
        // 1. config 中直接指定
        String key = ConfigUtils.getString(inputs, "apiKey", null);
        if (key != null && !key.isBlank()) return key;

        // 2. 根据 baseUrl 猜测环境变量
        if (baseUrl != null) {
            String lower = baseUrl.toLowerCase();
            if (lower.contains("dashscope") || lower.contains("aliyun")) {
                key = System.getenv("DASHSCOPE_API_KEY");
                if (key != null && !key.isBlank()) return key;
            }
            if (lower.contains("anthropic")) {
                key = System.getenv("ANTHROPIC_API_KEY");
                if (key != null && !key.isBlank()) return key;
            }
            if (lower.contains("deepseek")) {
                key = System.getenv("DEEPSEEK_API_KEY");
                if (key != null && !key.isBlank()) return key;
            }
            if (lower.contains("tencent")) {
                key = System.getenv("LLM_API_KEY");
                if (key != null && !key.isBlank()) return key;
            }
        }

        // 3. fallback: LLM_API_KEY
        key = System.getenv("LLM_API_KEY");
        if (key != null && !key.isBlank()) return key;
        
        // 4. fallback: 无 key 时返回 null（进入 Mock 模式）
        return null;
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) return DEFAULT_BASE_URL;
        // 去掉末尾 /
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }
}
