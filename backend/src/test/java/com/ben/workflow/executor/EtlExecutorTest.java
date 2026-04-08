package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EtlExecutor 单元测试
 */
class EtlExecutorTest {

    private EtlExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        executor = new EtlExecutor();
        executor.init();
    }

    @Test
    void testGetType() {
        assertEquals("etl", executor.getType());
    }

    @Test
    void testGetName() {
        assertEquals("ETL 数据清洗", executor.getName());
    }

    @Test
    void testGetMetadata() {
        var metadata = executor.getMetadata();
        assertNotNull(metadata);
        assertEquals("etl", metadata.getType());
        assertEquals("ETL 数据清洗", metadata.getName());
        assertEquals("data", metadata.getCategory());
        assertEquals("🔄", metadata.getIcon());
    }

    @Test
    void testParseHackerNewsData() throws Exception {
        // 模拟 HackerNews story 对象数组
        String hnJson = """
            [
              {
                "id": 123,
                "by": "testuser",
                "title": "Show HN: A new AI framework",
                "url": "https://example.com/ai-framework",
                "score": 150,
                "descendants": 45,
                "time": 1712534400,
                "type": "story"
              },
              {
                "id": 124,
                "by": "user2",
                "title": "Rust is taking over systems programming",
                "url": "https://example.com/rust",
                "score": 200,
                "descendants": 80,
                "time": 1712534500,
                "type": "story"
              }
            ]
            """;

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("response_body", hnJson);

        NodeExecutionContext context = new NodeExecutionContext("etl-1", "etl-1", "etl", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        Map<String, Object> outputs = result.getOutputs();
        assertNotNull(outputs);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) outputs.get("items");
        assertNotNull(items);
        assertEquals(2, items.size());

        // 验证第一条
        Map<String, Object> first = items.get(0);
        assertEquals("Show HN: A new AI framework", first.get("title"));
        assertEquals("https://example.com/ai-framework", first.get("url"));
        assertEquals("HackerNews", first.get("source"));
        assertNotNull(first.get("publishedAt"));
        assertNotNull(first.get("tags"));
    }

    @Test
    void testParseRedditData() throws Exception {
        String redditJson = """
            {
              "data": {
                "children": [
                  {
                    "data": {
                      "title": "Apple announces new M5 chip",
                      "url": "https://apple.com/m5",
                      "permalink": "/r/technology/comments/abc123/apple_m5/",
                      "ups": 5000,
                      "num_comments": 300,
                      "selftext": "Apple just announced...",
                      "created_utc": 1712534400,
                      "link_flair_text": "Hardware"
                    }
                  }
                ]
              }
            }
            """;

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("response_body", redditJson);

        NodeExecutionContext context = new NodeExecutionContext("etl-2", "etl-2", "etl", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.getOutputs().get("items");
        assertNotNull(items);
        assertEquals(1, items.size());

        Map<String, Object> item = items.get(0);
        assertEquals("Apple announces new M5 chip", item.get("title"));
        assertEquals("Reddit r/technology", item.get("source"));
        assertTrue(((String) item.get("summary")).contains("5000"));
    }

    @Test
    void testParseGitHubData() throws Exception {
        String githubJson = """
            [
              {
                "full_name": "openai/whisper",
                "html_url": "https://github.com/openai/whisper",
                "description": "Robust Speech Recognition via Large-Scale Weak Supervision",
                "stargazers_count": 60000,
                "language": "Python"
              }
            ]
            """;

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("response_body", githubJson);

        NodeExecutionContext context = new NodeExecutionContext("etl-3", "etl-3", "etl", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.getOutputs().get("items");
        assertNotNull(items);
        assertEquals(1, items.size());

        Map<String, Object> item = items.get(0);
        assertTrue(((String) item.get("title")).contains("openai/whisper"));
        assertEquals("GitHub Trending", item.get("source"));
    }

    @Test
    void testParse36krData() throws Exception {
        String kr36Json = """
            {
              "data": {
                "hotRankList": [
                  {
                    "itemId": "999",
                    "title": "AI 大模型市场竞争白热化",
                    "summary": "各大厂商纷纷推出新模型...",
                    "publishTime": "2026-04-08T00:00:00Z"
                  }
                ]
              }
            }
            """;

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("response_body", kr36Json);

        NodeExecutionContext context = new NodeExecutionContext("etl-4", "etl-4", "etl", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.getOutputs().get("items");
        assertNotNull(items);
        assertEquals(1, items.size());

        Map<String, Object> item = items.get(0);
        assertEquals("AI 大模型市场竞争白热化", item.get("title"));
        assertEquals("36kr", item.get("source"));
    }

    @Test
    void testDeduplication() throws Exception {
        // 两条相同 URL 的数据应去重
        String json = """
            [
              {
                "by": "user1",
                "title": "Same article",
                "url": "https://example.com/same",
                "score": 100,
                "time": 1712534400,
                "type": "story"
              },
              {
                "by": "user2",
                "title": "Same article duplicate",
                "url": "https://example.com/same",
                "score": 50,
                "time": 1712534500,
                "type": "story"
              }
            ]
            """;

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("response_body", json);

        NodeExecutionContext context = new NodeExecutionContext("etl-5", "etl-5", "etl", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.getOutputs().get("items");
        assertEquals(1, items.size()); // 去重后只有 1 条
    }

    @Test
    void testEmptyInput() throws Exception {
        Map<String, Object> inputs = new HashMap<>();

        NodeExecutionContext context = new NodeExecutionContext("etl-6", "etl-6", "etl", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.getOutputs().get("items");
        assertNotNull(items);
        assertEquals(0, items.size());
        assertEquals(0, result.getOutputs().get("totalCount"));
    }

    @Test
    void testMixedMultiSourceData() throws Exception {
        // 模拟 Join 节点汇聚后的结构
        Map<String, Object> hnOutput = new HashMap<>();
        hnOutput.put("response_body", """
            [{"by":"u1","title":"HN Story","url":"https://hn.example.com","score":100,"time":1712534400,"type":"story"}]
            """);

        Map<String, Object> redditOutput = new HashMap<>();
        redditOutput.put("response_body", """
            {"data":{"children":[{"data":{"title":"Reddit Post","url":"https://reddit.example.com","ups":500,"num_comments":50,"created_utc":1712534400}}]}}
            """);

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("hackernews", hnOutput);
        inputs.put("reddit", redditOutput);

        NodeExecutionContext context = new NodeExecutionContext("etl-7", "etl-7", "etl", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.getOutputs().get("items");
        assertNotNull(items);
        assertEquals(2, items.size());

        // 验证来源不同
        @SuppressWarnings("unchecked")
        List<String> sources = (List<String>) result.getOutputs().get("sources");
        assertTrue(sources.size() >= 2);
    }

    @Test
    void testOutputFields() throws Exception {
        String json = """
            [{"by":"u","title":"Test","url":"https://test.com","score":1,"time":1712534400,"type":"story"}]
            """;

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("response_body", json);

        NodeExecutionContext context = new NodeExecutionContext("etl-8", "etl-8", "etl", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        Map<String, Object> outputs = result.getOutputs();

        // 验证所有输出字段
        assertNotNull(outputs.get("items"));
        assertNotNull(outputs.get("totalCount"));
        assertNotNull(outputs.get("sources"));
        assertNotNull(outputs.get("processedAt"));

        // 验证每条记录的统一字段
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) outputs.get("items");
        for (Map<String, Object> item : items) {
            assertNotNull(item.get("title"), "title should not be null");
            assertNotNull(item.get("url"), "url should not be null");
            assertNotNull(item.get("source"), "source should not be null");
            assertNotNull(item.get("publishedAt"), "publishedAt should not be null");
            assertNotNull(item.get("summary"), "summary should not be null");
            assertNotNull(item.get("tags"), "tags should not be null");
        }
    }
}
