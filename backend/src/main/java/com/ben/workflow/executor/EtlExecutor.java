package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.workflow.executor.extension.BaseExecutor;
import com.ben.workflow.executor.extension.ExecutorMeta;
import com.ben.workflow.executor.extension.ParameterSchema;
import com.ben.workflow.executor.extension.ValidationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ETL 清洗执行器
 * <p>
 * 将多个数据源的异构数据归一化为统一格式。
 * 支持 HackerNews、Reddit、GitHub Trending、36kr 等数据源。
 * <p>
 * 输入：上游多个节点的 response_body（JSON 字符串或已解析对象）
 * 输出：统一格式的 items 列表，每项包含 title、url、source、publishedAt、summary、tags
 */
@ExecutorMeta(
    type = "etl",
    name = "ETL 数据清洗",
    description = "归一化多数据源内容格式",
    category = "data",
    icon = "🔄",
    version = "1.0.0"
)
public class EtlExecutor extends BaseExecutor {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Override
    public String getType() {
        return "etl";
    }

    @Override
    public String getName() {
        return "ETL 数据清洗";
    }

    @Override
    public String getDescription() {
        return "归一化多数据源内容格式，输出统一字段：title、url、source、publishedAt、summary、tags";
    }

    @Override
    protected void doValidate(NodeExecutionContext context) throws ValidationException {
        super.doValidate(context);
        // ETL 节点接收上游汇聚的数据，不需要特定的必填字段
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> doExecute(NodeExecutionContext context) throws Exception {
        Map<String, Object> inputs = context.getInputs();
        log.info("[ETL] 开始清洗数据，输入字段: {}", inputs.keySet());

        List<Map<String, Object>> allItems = new ArrayList<>();

        // 遍历所有输入，尝试解析各数据源
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 跳过非数据字段
            if (value == null) continue;

            String rawData = null;
            if (value instanceof String) {
                rawData = (String) value;
            } else if ("response_body".equals(key)) {
                rawData = value.toString();
            }

            if (rawData != null && !rawData.isBlank()) {
                List<Map<String, Object>> parsed = parseDataSource(key, rawData);
                allItems.addAll(parsed);
            }
        }

        // 如果输入包含嵌套的 response_body，也尝试解析
        // 适配 Join 节点汇聚后的结构
        tryParseNestedResponses(inputs, allItems);

        // 去重（按 URL）
        Set<String> seenUrls = new HashSet<>();
        List<Map<String, Object>> dedupedItems = new ArrayList<>();
        for (Map<String, Object> item : allItems) {
            String url = (String) item.get("url");
            if (url != null && seenUrls.add(url)) {
                dedupedItems.add(item);
            } else if (url == null) {
                dedupedItems.add(item);
            }
        }

        log.info("[ETL] 清洗完成，共 {} 条数据（去重后 {}）", allItems.size(), dedupedItems.size());

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("items", dedupedItems);
        outputs.put("totalCount", dedupedItems.size());
        outputs.put("sources", extractSources(dedupedItems));
        outputs.put("processedAt", Instant.now().toString());
        
        // 生成格式化文本（用于推送）
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < Math.min(dedupedItems.size(), 3); i++) {
            Map<String, Object> item = dedupedItems.get(i);
            String title = (String) item.get("title");
            String url = (String) item.get("url");
            String source = (String) item.get("source");
            String summary = (String) item.get("summary");
            if (title != null) {
                formatted.append(i + 1).append(". ").append(title).append("\n");
                if (url != null) formatted.append("   ").append(url).append("\n");
                if (source != null) formatted.append("   来源: ").append(source);
                if (summary != null) formatted.append(" | ").append(summary);
                formatted.append("\n\n");
            }
        }
        outputs.put("formatted", formatted.toString().trim());
        
        return outputs;
    }

    /**
     * 尝试解析嵌套的响应数据（Join 汇聚后的多源数据）
     */
    @SuppressWarnings("unchecked")
    private void tryParseNestedResponses(Map<String, Object> inputs, List<Map<String, Object>> allItems) {
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map<String, Object> nested = (Map<String, Object>) value;
                Object responseBody = nested.get("response_body");
                if (responseBody instanceof String) {
                    String sourceHint = entry.getKey();
                    List<Map<String, Object>> parsed = parseDataSource(sourceHint, (String) responseBody);
                    allItems.addAll(parsed);
                }
            }
        }
    }

    /**
     * 根据数据内容自动检测并解析数据源
     */
    private List<Map<String, Object>> parseDataSource(String keyHint, String rawData) {
        List<Map<String, Object>> items = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawData);

            // HackerNews: 返回 ID 数组或 story 对象
            if (root.isArray() && root.size() > 0 && root.get(0).isNumber()) {
                // HN top stories 返回的是 ID 数组，不直接包含内容
                // 此时 rawData 可能已经是处理后的对象
                return items;
            }

            // HackerNews story 对象
            if (root.has("by") && root.has("title") && root.has("url")) {
                items.add(normalizeHackerNewsItem(root));
                return items;
            }

            // HackerNews: 多个 story 数组
            if (root.isArray()) {
                for (JsonNode node : root) {
                    if (node.has("by") && node.has("title")) {
                        items.add(normalizeHackerNewsItem(node));
                    } else if (node.has("full_name") || node.has("repo") || node.has("repositoryName")) {
                        // GitHub 结构
                        items.add(normalizeGitHubItem(node));
                    } else if (node.has("data") && node.path("data").has("title")) {
                        // Reddit 结构
                        items.add(normalizeRedditItem(node.path("data")));
                    } else {
                        // 通用结构
                        items.add(normalizeGenericItem(node, keyHint));
                    }
                }
                return items;
            }

            // Reddit: { data: { children: [...] } }
            if (root.has("data") && root.path("data").has("children")) {
                JsonNode children = root.path("data").path("children");
                for (JsonNode child : children) {
                    JsonNode data = child.path("data");
                    if (!data.isMissingNode()) {
                        items.add(normalizeRedditItem(data));
                    }
                }
                return items;
            }

            // 36kr: { data: { itemList: [...] } } 或 { data: { hotRankList: [...] } }
            if (root.has("data")) {
                JsonNode data = root.path("data");
                JsonNode itemList = data.has("itemList") ? data.path("itemList") :
                                    data.has("hotRankList") ? data.path("hotRankList") :
                                    data.has("items") ? data.path("items") : null;
                if (itemList != null && itemList.isArray()) {
                    for (JsonNode item : itemList) {
                        items.add(normalize36krItem(item));
                    }
                    return items;
                }
            }

            // GitHub Trending: HTML 或 JSON 数组
            if (root.isArray()) {
                for (JsonNode node : root) {
                    if (node.has("repo") || node.has("full_name") || node.has("repositoryName")) {
                        items.add(normalizeGitHubItem(node));
                    }
                }
                if (!items.isEmpty()) return items;
            }

            // 尝试通用解析
            if (root.isObject()) {
                items.add(normalizeGenericItem(root, keyHint));
            }

        } catch (Exception e) {
            log.warn("[ETL] 解析数据失败 (key={}): {}", keyHint, e.getMessage());
            // 尝试作为纯文本处理（GitHub Trending HTML）
            items.addAll(parseGitHubTrendingHtml(rawData));
        }
        return items;
    }

    // ===== 各数据源归一化 =====

    private Map<String, Object> normalizeHackerNewsItem(JsonNode node) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", node.path("title").asText(""));
        item.put("url", node.has("url") ? node.path("url").asText("") :
                "https://news.ycombinator.com/item?id=" + node.path("id").asText(""));
        item.put("source", "HackerNews");
        item.put("publishedAt", node.has("time") ?
                Instant.ofEpochSecond(node.path("time").asLong()).toString() :
                Instant.now().toString());
        item.put("summary", String.format("Score: %d | Comments: %d | By: %s",
                node.path("score").asInt(0),
                node.path("descendants").asInt(0),
                node.path("by").asText("")));
        item.put("tags", List.of("tech", "hackernews", node.path("type").asText("story")));
        return item;
    }

    private Map<String, Object> normalizeRedditItem(JsonNode data) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", data.path("title").asText(""));
        String url = data.path("url").asText("");
        if (url.isEmpty() || url.startsWith("/r/")) {
            url = "https://reddit.com" + data.path("permalink").asText("");
        }
        item.put("url", url);
        item.put("source", "Reddit r/technology");
        item.put("publishedAt", data.has("created_utc") ?
                Instant.ofEpochSecond(data.path("created_utc").asLong()).toString() :
                Instant.now().toString());
        item.put("summary", String.format("Upvotes: %d | Comments: %d | %s",
                data.path("ups").asInt(0),
                data.path("num_comments").asInt(0),
                truncate(data.path("selftext").asText(""), 200)));
        List<String> tags = new ArrayList<>(List.of("tech", "reddit"));
        if (data.has("link_flair_text") && !data.path("link_flair_text").isNull()) {
            tags.add(data.path("link_flair_text").asText());
        }
        item.put("tags", tags);
        return item;
    }

    private Map<String, Object> normalizeGitHubItem(JsonNode node) {
        Map<String, Object> item = new LinkedHashMap<>();
        String repoName = node.has("full_name") ? node.path("full_name").asText("") :
                          node.has("repo") ? node.path("repo").asText("") :
                          node.path("repositoryName").asText("");
        item.put("title", repoName + (node.has("description") ? " - " + node.path("description").asText("") : ""));
        item.put("url", node.has("html_url") ? node.path("html_url").asText("") :
                "https://github.com/" + repoName);
        item.put("source", "GitHub Trending");
        item.put("publishedAt", Instant.now().toString());
        item.put("summary", String.format("⭐ %s | Language: %s | %s",
                node.path("stars").asText(node.path("stargazers_count").asText("0")),
                node.path("language").asText("Unknown"),
                truncate(node.path("description").asText(""), 200)));
        List<String> tags = new ArrayList<>(List.of("opensource", "github"));
        if (node.has("language") && !node.path("language").isNull()) {
            tags.add(node.path("language").asText().toLowerCase());
        }
        item.put("tags", tags);
        return item;
    }

    private Map<String, Object> normalize36krItem(JsonNode node) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", node.path("title").asText(node.path("templateMaterial").path("widgetTitle").asText("")));
        String itemId = node.path("itemId").asText(node.path("id").asText(""));
        item.put("url", node.has("url") ? node.path("url").asText("") :
                "https://36kr.com/p/" + itemId);
        item.put("source", "36kr");
        item.put("publishedAt", node.has("publishTime") ? node.path("publishTime").asText("") :
                node.has("publishedAt") ? node.path("publishedAt").asText("") :
                Instant.now().toString());
        item.put("summary", truncate(
                node.path("summary").asText(
                    node.path("description").asText(
                        node.path("templateMaterial").path("widgetContent").asText(""))),
                300));
        List<String> tags = new ArrayList<>(List.of("tech", "36kr", "中文"));
        item.put("tags", tags);
        return item;
    }

    private Map<String, Object> normalizeGenericItem(JsonNode node, String sourceHint) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", node.has("title") ? node.path("title").asText("") :
                          node.has("name") ? node.path("name").asText("") : "Untitled");
        item.put("url", node.has("url") ? node.path("url").asText("") :
                         node.has("link") ? node.path("link").asText("") : "");
        item.put("source", sourceHint != null ? sourceHint : "unknown");
        item.put("publishedAt", Instant.now().toString());
        item.put("summary", node.has("description") ? truncate(node.path("description").asText(""), 300) :
                             node.has("summary") ? truncate(node.path("summary").asText(""), 300) : "");
        item.put("tags", List.of("tech"));
        return item;
    }

    /**
     * 解析 GitHub Trending HTML 页面
     */
    private List<Map<String, Object>> parseGitHubTrendingHtml(String html) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (html == null || !html.contains("github.com")) return items;

        // 简单正则提取 repo 链接
        Pattern pattern = Pattern.compile("<h2[^>]*>\\s*<a[^>]*href=\"(/[^\"]+)\"[^>]*>([^<]+)</a>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            Map<String, Object> item = new LinkedHashMap<>();
            String path = matcher.group(1).trim();
            String name = matcher.group(2).trim().replaceAll("\\s+", "");
            item.put("title", name);
            item.put("url", "https://github.com" + path);
            item.put("source", "GitHub Trending");
            item.put("publishedAt", Instant.now().toString());
            item.put("summary", "Trending repository: " + name);
            item.put("tags", List.of("opensource", "github", "trending"));
            items.add(item);
        }
        return items;
    }

    // ===== 工具方法 =====

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private List<String> extractSources(List<Map<String, Object>> items) {
        Set<String> sources = new LinkedHashSet<>();
        for (Map<String, Object> item : items) {
            Object source = item.get("source");
            if (source != null) sources.add(source.toString());
        }
        return new ArrayList<>(sources);
    }

    // ===== 参数定义 =====

    @Override
    protected List<ParameterSchema> defineInputParams() {
        return List.of(
            ParameterSchema.builder()
                .name("response_body").type("string").label("数据源响应")
                .description("上游 HTTP 请求节点的响应数据（JSON）")
                .required(false).build()
        );
    }

    @Override
    protected List<ParameterSchema> defineOutputParams() {
        return List.of(
            ParameterSchema.builder()
                .name("items").type("array").label("清洗结果")
                .description("归一化后的数据列表").build(),
            ParameterSchema.builder()
                .name("totalCount").type("number").label("总条数")
                .description("数据总条数").build(),
            ParameterSchema.builder()
                .name("sources").type("array").label("数据源")
                .description("包含的数据源列表").build(),
            ParameterSchema.builder()
                .name("processedAt").type("string").label("处理时间")
                .description("数据处理时间").build()
        );
    }
}
