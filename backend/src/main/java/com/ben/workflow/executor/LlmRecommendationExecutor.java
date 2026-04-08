package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.workflow.executor.extension.BaseExecutor;
import com.ben.workflow.executor.extension.ExecutorMeta;
import com.ben.workflow.executor.extension.ParameterSchema;
import com.ben.workflow.executor.extension.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * LLM 推荐执行器
 * <p>
 * 根据 ETL 清洗后的资讯内容，结合用户画像，调用 LLM 生成个性化推荐。
 * <p>
 * 内置用户画像：
 * - 职业：Java 工程师 + 技术 Leader
 * - 业务方向：AI 生图、AI 生视频
 * - 兴趣：数码爱好者、游戏爱好者、爱狗人士、业余拳击运动
 * <p>
 * 输入：
 * <ul>
 *   <li>items (List) - ETL 清洗后的资讯列表</li>
 *   <li>userProfile (Map, 可选) - 自定义用户画像，覆盖默认值</li>
 *   <li>model (String, 可选) - LLM 模型名称</li>
 *   <li>apiKey (String, 可选) - API Key</li>
 *   <li>baseUrl (String, 可选) - API 地址</li>
 * </ul>
 * <p>
 * 输出：
 * <ul>
 *   <li>recommendation (String) - LLM 推荐文本（中文）</li>
 *   <li>model (String) - 使用的模型</li>
 *   <li>itemCount (int) - 输入资讯数量</li>
 * </ul>
 */
@ExecutorMeta(
    type = "llm_recommendation",
    name = "LLM 智能推荐",
    description = "结合用户画像对资讯进行个性化推荐",
    category = "ai",
    icon = "🎯",
    version = "1.0.0"
)
public class LlmRecommendationExecutor extends BaseExecutor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 默认用户画像 */
    private static final Map<String, Object> DEFAULT_USER_PROFILE = Map.of(
        "profession", "Java 工程师 + 技术 Leader",
        "businessFocus", "AI 生图、AI 生视频",
        "interests", List.of("数码爱好者", "游戏爱好者", "爱狗人士", "业余拳击运动"),
        "language", "中文"
    );

    @Override
    public String getType() {
        return "llm_recommendation";
    }

    @Override
    public String getName() {
        return "LLM 智能推荐";
    }

    @Override
    public String getDescription() {
        return "结合用户画像对资讯进行个性化推荐";
    }

    @Override
    protected void doValidate(NodeExecutionContext context) throws ValidationException {
        super.doValidate(context);
        // items 可能来自上游 ETL 节点，也可能直接传入
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> doExecute(NodeExecutionContext context) throws Exception {
        Map<String, Object> inputs = context.getInputs();
        log.info("[LLM-Recommendation] 开始生成推荐，输入字段: {}", inputs.keySet());

        // 获取 ETL 清洗后的数据
        List<Map<String, Object>> items = extractItems(inputs);
        log.info("[LLM-Recommendation] 资讯数量: {}", items.size());

        // 获取用户画像（支持自定义覆盖）
        Map<String, Object> userProfile = extractUserProfile(inputs);

        // 构建 LLM 提示词
        String systemPrompt = buildSystemPrompt(userProfile);
        String userPrompt = buildUserPrompt(items, userProfile);

        // 获取 LLM 配置
        String model = getInputString(context, "model", "hunyuan-2.0-instruct");
        String apiKey = getInputString(context, "apiKey", null);
        String baseUrl = getInputString(context, "baseUrl", null);
        int maxTokens = getInputInt(context, "maxTokens", 4096);
        double temperature = getInputDouble(context, "temperature", 0.7);

        // 构造 LLM 节点的输入参数（复用现有 LLMNodeExecutor 的参数格式）
        Map<String, Object> llmInputs = new LinkedHashMap<>();
        llmInputs.put("model", model);
        llmInputs.put("systemPrompt", systemPrompt);
        llmInputs.put("userPrompt", userPrompt);
        llmInputs.put("temperature", temperature);
        llmInputs.put("maxTokens", maxTokens);
        if (apiKey != null && !apiKey.isBlank()) {
            llmInputs.put("apiKey", apiKey);
        }
        if (baseUrl != null && !baseUrl.isBlank()) {
            llmInputs.put("baseUrl", baseUrl);
        }

        // 使用 LLMNodeExecutor 执行
        LLMNodeExecutor llmExecutor = new LLMNodeExecutor();
        NodeExecutionContext llmContext = new NodeExecutionContext(
            context.getNodeId() + "_llm",
            context.getNodeId(),
            "llm",
            llmInputs,
            0
        );

        com.ben.dagscheduler.spi.NodeExecutionResult llmResult = llmExecutor.execute(llmContext);

        Map<String, Object> outputs = new LinkedHashMap<>();
        if (llmResult.isSuccess()) {
            Map<String, Object> llmOutputs = llmResult.getOutputs();
            outputs.put("recommendation", llmOutputs.getOrDefault("content", "推荐生成失败"));
            outputs.put("model", llmOutputs.getOrDefault("model", model));
            outputs.put("usage", llmOutputs.get("usage"));
        } else {
            outputs.put("recommendation", "推荐生成失败: " + llmResult.getErrorMessage());
            outputs.put("model", model);
        }
        outputs.put("itemCount", items.size());
        outputs.put("userProfile", userProfile);
        return outputs;
    }

    /**
     * 从输入中提取资讯列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Map<String, Object> inputs) {
        // 优先从 items 字段获取
        Object itemsObj = inputs.get("items");
        if (itemsObj instanceof List) {
            return (List<Map<String, Object>>) itemsObj;
        }

        // 尝试从 JSON 字符串解析
        if (itemsObj instanceof String) {
            try {
                return objectMapper.readValue((String) itemsObj,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            } catch (JsonProcessingException e) {
                log.warn("[LLM-Recommendation] 无法解析 items 字符串", e);
            }
        }

        // 如果没有 items，尝试从整个输入中提取
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                if (nested.containsKey("title") && nested.containsKey("url")) {
                    items.add(nested);
                }
            }
        }
        return items;
    }

    /**
     * 提取用户画像
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractUserProfile(Map<String, Object> inputs) {
        Map<String, Object> profile = new LinkedHashMap<>(DEFAULT_USER_PROFILE);
        Object customProfile = inputs.get("userProfile");
        if (customProfile instanceof Map) {
            profile.putAll((Map<String, Object>) customProfile);
        }
        return profile;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(Map<String, Object> userProfile) {
        return """
            你是一个专业的科技资讯推荐助手。你的任务是根据用户画像，从一批科技资讯中筛选出最值得关注的内容，并给出个性化推荐。
            
            ## 用户画像
            - 职业：%s
            - 业务方向：%s
            - 兴趣爱好：%s
            
            ## 推荐要求
            1. 从提供的资讯中挑选最匹配用户画像的 TOP 内容（5-10 条）
            2. 每条推荐需要包含：标题、来源、推荐理由（结合用户画像说明为什么推荐）
            3. 按推荐优先级排序
            4. 最后给出一段总结性的今日科技动态概览
            5. 全部使用中文输出
            6. 推荐理由要具体，说明和用户的职业、业务方向或兴趣的关联
            """.formatted(
                userProfile.getOrDefault("profession", "技术人员"),
                userProfile.getOrDefault("businessFocus", "AI"),
                userProfile.getOrDefault("interests", List.of("技术"))
            );
    }

    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(List<Map<String, Object>> items, Map<String, Object> userProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是今日收集到的科技资讯，请根据我的画像进行推荐：\n\n");

        int index = 1;
        for (Map<String, Object> item : items) {
            sb.append(String.format("### %d. %s\n", index++, item.getOrDefault("title", "无标题")));
            sb.append(String.format("- 来源：%s\n", item.getOrDefault("source", "未知")));
            sb.append(String.format("- 链接：%s\n", item.getOrDefault("url", "")));
            sb.append(String.format("- 摘要：%s\n", item.getOrDefault("summary", "")));
            Object tags = item.get("tags");
            if (tags instanceof List) {
                sb.append(String.format("- 标签：%s\n", String.join(", ", ((List<?>) tags).stream().map(Object::toString).toList())));
            }
            sb.append("\n");

            // 限制最多 50 条，避免 token 超限
            if (index > 50) {
                sb.append("... 以及更多 ").append(items.size() - 50).append(" 条资讯\n\n");
                break;
            }
        }

        sb.append("请给出你的个性化推荐。");
        return sb.toString();
    }

    // ===== 参数定义 =====

    @Override
    protected List<ParameterSchema> defineInputParams() {
        return List.of(
            ParameterSchema.builder()
                .name("items").type("array").label("资讯列表")
                .description("ETL 清洗后的资讯数据").required(true).build(),
            ParameterSchema.builder()
                .name("userProfile").type("object").label("用户画像")
                .description("自定义用户画像（可选，默认使用内置画像）").required(false).build(),
            ParameterSchema.builder()
                .name("model").type("string").label("模型")
                .description("LLM 模型名称").required(false).build(),
            ParameterSchema.builder()
                .name("apiKey").type("string").label("API Key")
                .description("LLM API Key").required(false).build(),
            ParameterSchema.builder()
                .name("baseUrl").type("string").label("API 地址")
                .description("LLM API 地址").required(false).build(),
            ParameterSchema.builder()
                .name("maxTokens").type("number").label("最大 Tokens")
                .description("LLM 最大输出 tokens").required(false).build(),
            ParameterSchema.builder()
                .name("temperature").type("number").label("温度")
                .description("LLM 温度参数").required(false).build()
        );
    }

    @Override
    protected List<ParameterSchema> defineOutputParams() {
        return List.of(
            ParameterSchema.builder()
                .name("recommendation").type("string").label("推荐结果")
                .description("LLM 生成的个性化推荐文本").build(),
            ParameterSchema.builder()
                .name("model").type("string").label("模型")
                .description("实际使用的模型").build(),
            ParameterSchema.builder()
                .name("itemCount").type("number").label("资讯数量")
                .description("输入的资讯数量").build(),
            ParameterSchema.builder()
                .name("userProfile").type("object").label("用户画像")
                .description("使用的用户画像").build()
        );
    }
}
