package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LlmRecommendationExecutor 单元测试
 */
class LlmRecommendationExecutorTest {

    private LlmRecommendationExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        executor = new LlmRecommendationExecutor();
        executor.init();
    }

    @Test
    void testGetType() {
        assertEquals("llm_recommendation", executor.getType());
    }

    @Test
    void testGetName() {
        assertEquals("LLM 智能推荐", executor.getName());
    }

    @Test
    void testGetMetadata() {
        var metadata = executor.getMetadata();
        assertNotNull(metadata);
        assertEquals("llm_recommendation", metadata.getType());
        assertEquals("LLM 智能推荐", metadata.getName());
        assertEquals("ai", metadata.getCategory());
        assertEquals("🎯", metadata.getIcon());
    }

    @Test
    void testExecuteWithMockMode() throws Exception {
        // 无 API Key 时应进入 Mock 模式
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of(
            "title", "AI 新框架发布",
            "url", "https://example.com/ai",
            "source", "HackerNews",
            "publishedAt", "2026-04-08T00:00:00Z",
            "summary", "一个新的 AI 框架",
            "tags", List.of("ai", "framework")
        ));
        items.add(Map.of(
            "title", "Java 21 新特性",
            "url", "https://example.com/java",
            "source", "Reddit",
            "publishedAt", "2026-04-08T01:00:00Z",
            "summary", "Java 21 带来了虚拟线程",
            "tags", List.of("java", "programming")
        ));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);

        NodeExecutionContext context = new NodeExecutionContext("rec-1", "rec-1", "llm_recommendation", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        Map<String, Object> outputs = result.getOutputs();
        assertNotNull(outputs);

        // Mock 模式也应返回推荐结果
        assertNotNull(outputs.get("recommendation"));
        assertEquals(2, outputs.get("itemCount"));
        assertNotNull(outputs.get("userProfile"));
    }

    @Test
    void testDefaultUserProfile() throws Exception {
        List<Map<String, Object>> items = List.of(
            Map.of("title", "Test", "url", "https://test.com", "source", "Test", 
                   "publishedAt", "2026-04-08", "summary", "test", "tags", List.of("test"))
        );

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);

        NodeExecutionContext context = new NodeExecutionContext("rec-2", "rec-2", "llm_recommendation", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> userProfile = (Map<String, Object>) result.getOutputs().get("userProfile");
        assertNotNull(userProfile);
        assertEquals("Java 工程师 + 技术 Leader", userProfile.get("profession"));
        assertEquals("AI 生图、AI 生视频", userProfile.get("businessFocus"));

        @SuppressWarnings("unchecked")
        List<String> interests = (List<String>) userProfile.get("interests");
        assertTrue(interests.contains("数码爱好者"));
        assertTrue(interests.contains("游戏爱好者"));
        assertTrue(interests.contains("爱狗人士"));
        assertTrue(interests.contains("业余拳击运动"));
    }

    @Test
    void testCustomUserProfile() throws Exception {
        List<Map<String, Object>> items = List.of(
            Map.of("title", "Test", "url", "https://test.com", "source", "Test",
                   "publishedAt", "2026-04-08", "summary", "test", "tags", List.of("test"))
        );

        Map<String, Object> customProfile = Map.of(
            "profession", "前端工程师",
            "businessFocus", "Web3"
        );

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);
        inputs.put("userProfile", customProfile);

        NodeExecutionContext context = new NodeExecutionContext("rec-3", "rec-3", "llm_recommendation", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> userProfile = (Map<String, Object>) result.getOutputs().get("userProfile");
        assertEquals("前端工程师", userProfile.get("profession"));
        assertEquals("Web3", userProfile.get("businessFocus"));
        // 默认的兴趣爱好仍然保留（因为 custom 没有覆盖）
        assertNotNull(userProfile.get("interests"));
    }

    @Test
    void testEmptyItems() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", new ArrayList<>());

        NodeExecutionContext context = new NodeExecutionContext("rec-4", "rec-4", "llm_recommendation", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getOutputs().get("itemCount"));
    }

    @Test
    void testInputParamsDefined() {
        var inputParams = executor.getMetadata().getInputParams();
        assertNotNull(inputParams);
        assertFalse(inputParams.isEmpty());

        // 检查 items 参数
        var itemsParam = inputParams.stream().filter(p -> "items".equals(p.getName())).findFirst();
        assertTrue(itemsParam.isPresent());
        assertTrue(itemsParam.get().isRequired());
    }

    @Test
    void testOutputParamsDefined() {
        var outputParams = executor.getMetadata().getOutputParams();
        assertNotNull(outputParams);
        assertFalse(outputParams.isEmpty());

        var paramNames = outputParams.stream().map(p -> p.getName()).toList();
        assertTrue(paramNames.contains("recommendation"));
        assertTrue(paramNames.contains("model"));
        assertTrue(paramNames.contains("itemCount"));
        assertTrue(paramNames.contains("userProfile"));
    }
}
