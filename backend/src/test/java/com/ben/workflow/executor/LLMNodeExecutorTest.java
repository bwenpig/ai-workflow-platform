package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLMNodeExecutor 单元测试
 */
class LLMNodeExecutorTest {

    private LLMNodeExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new LLMNodeExecutor();
    }

    @Test
    void testGetType() {
        assertEquals("llm", executor.getType());
    }

    @Test
    void testGetName() {
        assertEquals("LLM 大模型", executor.getName());
    }

    // ========== 变量替换测试 ==========

    @Test
    void testReplaceVariables_basic() {
        Map<String, Object> vars = Map.of("name", "Alice", "topic", "AI");
        String result = executor.replaceVariables("Hello {{name}}, let's talk about {{topic}}.", vars);
        assertEquals("Hello Alice, let's talk about AI.", result);
    }

    @Test
    void testReplaceVariables_withSpaces() {
        Map<String, Object> vars = Map.of("x", "42");
        String result = executor.replaceVariables("Value: {{ x }}", vars);
        assertEquals("Value: 42", result);
    }

    @Test
    void testReplaceVariables_missingVar() {
        Map<String, Object> vars = Map.of("a", "1");
        String result = executor.replaceVariables("{{a}} and {{b}}", vars);
        assertEquals("1 and ", result);
    }

    @Test
    void testReplaceVariables_noVars() {
        String result = executor.replaceVariables("no variables here", Map.of());
        assertEquals("no variables here", result);
    }

    @Test
    void testReplaceVariables_nullTemplate() {
        assertNull(executor.replaceVariables(null, Map.of("a", "1")));
    }

    @Test
    void testReplaceVariables_nullVars() {
        assertEquals("{{x}}", executor.replaceVariables("{{x}}", null));
    }

    @Test
    void testReplaceVariables_numericValue() {
        Map<String, Object> vars = Map.of("count", 100, "price", 9.99);
        String result = executor.replaceVariables("Count={{count}}, Price={{price}}", vars);
        assertEquals("Count=100, Price=9.99", result);
    }

    // ========== 参数校验测试 ==========

    @Test
    void testExecute_missingUserPrompt() throws Exception {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("model", "gpt-4o");
        inputs.put("apiKey", "sk-test");
        // no userPrompt

        NodeExecutionContext ctx = new NodeExecutionContext("node1", "test", "llm", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        assertTrue(result.isFailed());
        assertTrue(result.getErrorMessage().contains("userPrompt"));
    }

    @Test
    void testExecute_missingApiKey_mockMode() throws Exception {
        // 当无 API Key 时进入 Mock 模式：返回成功 + _mock=true
        // 清除环境变量影响：即使环境中有 key，inputs 中不传 apiKey 且 baseUrl 用不匹配任何环境变量的值
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("userPrompt", "hello");
        inputs.put("baseUrl", "https://mock.test.local/v1"); // 不匹配任何环境变量检测
        // no apiKey

        String envLlmKey = System.getenv("LLM_API_KEY");
        if (envLlmKey != null && !envLlmKey.isBlank()) {
            // 环境中有 LLM_API_KEY，Mock 模式不会触发，跳过此测试
            return;
        }

        NodeExecutionContext ctx = new NodeExecutionContext("node1", "test", "llm", inputs);
        NodeExecutionResult result = executor.execute(ctx);

        // Mock 模式：成功返回
        assertTrue(result.isSuccess(), "Mock 模式应返回 success");
        assertFalse(result.isFailed(), "Mock 模式不应 failed");

        // 验证 outputs
        Map<String, Object> outputs = result.getOutputs();
        assertNotNull(outputs);
        assertEquals(true, outputs.get("_mock"), "outputs 应包含 _mock=true");
        assertNotNull(outputs.get("content"), "outputs 应包含 content");
        assertNotNull(outputs.get("model"), "outputs 应包含 model");
        assertEquals("stop", outputs.get("finish_reason"));
    }

    @Test
    void testExecute_nullContext() throws Exception {
        // 不应抛异常，应返回 failed
        NodeExecutionResult result = executor.execute(null);
        assertTrue(result.isFailed());
    }
}
