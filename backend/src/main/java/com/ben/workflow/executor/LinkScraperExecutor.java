package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.spi.NodeComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * 链接抓取节点执行器
 */
@NodeComponent(value = "link_scraper", name = "链接抓取", description = "批量抓取网页正文内容")
public class LinkScraperExecutor implements NodeExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(LinkScraperExecutor.class);
    private static final int DEFAULT_MAX_URLS = 10;
    private static final int DEFAULT_TIMEOUT = 15;
    private static final int DEFAULT_CONCURRENCY = 3;

    @Override
    public String getType() {
        return "link_scraper";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        LocalDateTime startTime = LocalDateTime.now();
        String nodeId = context != null ? context.getNodeId() : "unknown";
        
        log.info("[LinkScraper] Starting nodeId={}", nodeId);
        
        try {
            Map<String, Object> inputs = context != null ? context.getInputs() : Collections.emptyMap();
            if (inputs == null) inputs = Collections.emptyMap();
            
            log.info("[LinkScraper] Inputs keys: {}", inputs.keySet());
            
            // 获取配置
            String urlField = (String) inputs.getOrDefault("url_field", "url");
            int maxUrls = getInt(inputs.getOrDefault("max_urls", DEFAULT_MAX_URLS));
            int timeout = getInt(inputs.getOrDefault("timeout", DEFAULT_TIMEOUT));
            int concurrency = getInt(inputs.getOrDefault("concurrency", DEFAULT_CONCURRENCY));
            
            log.info("[LinkScraper] urlField={}, maxUrls={}, timeout={}, concurrency={}", urlField, maxUrls, timeout, concurrency);
            
            // 提取URL列表
            List<String> urls = extractUrls(inputs, urlField);
            log.info("[LinkScraper] Extracted {} URLs", urls.size());
            
            if (urls.isEmpty()) {
                log.warn("[LinkScraper] No URLs found in inputs!");
                return NodeExecutionResult.success(nodeId, 
                    Map.of("results", new ArrayList<>(), "count", 0, "error", "No URLs found"), 
                    startTime, LocalDateTime.now());
            }
            
            // 限制数量
            urls = urls.subList(0, Math.min(urls.size(), maxUrls));
            log.info("[LinkScraper] Fetching {} URLs", urls.size());
            
            // 并发抓取
            List<Map<String, Object>> results = fetchUrls(urls, timeout, concurrency);
            
            Map<String, Object> outputs = new LinkedHashMap<>();
            outputs.put("results", results);
            outputs.put("count", results.size());
            outputs.put("total_urls", urls.size());
            outputs.put("formatted", formatResults(results));
            
            log.info("[LinkScraper] Completed, fetched {} results", results.size());
            
            return NodeExecutionResult.success(nodeId, outputs, startTime, LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("[LinkScraper] Error: {}", e.getMessage(), e);
            return NodeExecutionResult.failed(nodeId, e.getMessage(), null, startTime, LocalDateTime.now());
        }
    }
    
    private List<String> extractUrls(Map<String, Object> inputs, String urlField) {
        List<String> urls = new ArrayList<>();
        
        // 从 items 数组中提取 URL
        Object itemsObj = inputs.get("items");
        log.info("[LinkScraper] items type: {}, value: {}", itemsObj != null ? itemsObj.getClass().getName() : "null", itemsObj);
        
        if (itemsObj instanceof List) {
            for (Object item : (List<?>) itemsObj) {
                if (item instanceof Map) {
                    Object url = ((Map<?, ?>) item).get(urlField);
                    if (url != null && url.toString().startsWith("http")) {
                        urls.add(url.toString());
                    }
                }
            }
        }
        
        // 尝试从 formatted 字段提取
        if (urls.isEmpty()) {
            Object formatted = inputs.get("formatted");
            log.info("[LinkScraper] formatted type: {}", formatted != null ? formatted.getClass().getName() : "null");
            if (formatted instanceof List) {
                for (Object item : (List<?>) formatted) {
                    if (item instanceof Map) {
                        Object url = ((Map<?, ?>) item).get(urlField);
                        if (url != null && url.toString().startsWith("http")) {
                            urls.add(url.toString());
                        }
                    }
                }
            }
        }
        
        return urls;
    }
    
    private String formatResults(List<Map<String, Object>> results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> r = results.get(i);
            String title = (String) r.getOrDefault("title", "");
            String url = (String) r.getOrDefault("url", "N/A");
            String content = (String) r.getOrDefault("content", "");
            String error = (String) r.getOrDefault("error", "");
            
            sb.append("【文章").append(i + 1).append("】\n");
            sb.append("标题: ").append(title.isBlank() ? "(无标题)" : title).append("\n");
            sb.append("URL: ").append(url).append("\n");
            
            // 如果有错误或内容为空，使用标题+URL作为后备
            if (!error.isBlank()) {
                sb.append("说明: 抓取失败(").append(error).append(")，使用标题替代\n");
                sb.append("摘要: ").append(title.isBlank() ? url : title).append("\n");
            } else if (content.isBlank()) {
                sb.append("内容: ").append(title.isBlank() ? "(无内容)" : title).append("\n");
            } else {
                if (content.length() > 500) {
                    content = content.substring(0, 500) + "...";
                }
                sb.append("内容: ").append(content).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    private List<Map<String, Object>> fetchUrls(List<String> urls, int timeout, int concurrency) {
        List<Map<String, Object>> results = Collections.synchronizedList(new ArrayList<>());
        
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(concurrency, 5));
        List<Future<Map<String, Object>>> futures = new ArrayList<>();
        
        for (String url : urls) {
            futures.add(executor.submit(() -> fetchUrl(url, timeout)));
        }
        
        for (Future<Map<String, Object>> future : futures) {
            try {
                results.add(future.get(timeout * 2, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.error("[LinkScraper] Error fetching URL: {}", e.getMessage());
                results.add(Map.of("url", "error", "error", e.getMessage()));
            }
        }
        
        executor.shutdown();
        return results;
    }
    
    private Map<String, Object> fetchUrl(String url, int timeout) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return Map.of("url", url, "status", response.statusCode(), "error", "HTTP " + response.statusCode());
            }
            
            String html = response.body();
            String title = extractTitle(html);
            String content = extractContent(html);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("url", url);
            result.put("title", title);
            result.put("content", content);
            result.put("status", 200);
            result.put("length", content.length());
            
            return result;
            
        } catch (Exception e) {
            return Map.of("url", url, "error", e.getMessage());
        }
    }
    
    private String extractTitle(String html) {
        Pattern pattern = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // 尝试从 meta og:title 获取
        pattern = Pattern.compile("<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
    
    private String extractContent(String html) {
        // 移除脚本和样式
        html = html.replaceAll("<script[^>]*>[\\s\\S]*?</script>", "");
        html = html.replaceAll("<style[^>]*>[\\s\\S]*?</style>", "");
        html = html.replaceAll("<!--[\\s\\S]*?-->", "");
        html = html.replaceAll("<nav[^>]*>[\\s\\S]*?</nav>", "");
        html = html.replaceAll("<header[^>]*>[\\s\\S]*?</header>", "");
        html = html.replaceAll("<footer[^>]*>[\\s\\S]*?</footer>", "");
        
        // 移除HTML标签
        html = html.replaceAll("<[^>]+>", " ");
        
        // 清理空白
        html = html.replaceAll("\\s+", " ").trim();
        
        // 限制长度
        if (html.length() > 3000) {
            html = html.substring(0, 3000) + "...";
        }
        
        return html;
    }
    
    private int getInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return DEFAULT_MAX_URLS;
    }
}
