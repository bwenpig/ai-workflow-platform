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
    void testExecute_missingApiKey() throws Exception {
        // 确保环境变量不存在时才会失败
        // 注意：如果环境中配置了 OPENAI_API_KEY 则此测试会通过 API Key 检查
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey == null || envKey.isBlank()) {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("userPrompt", "hello");
            // no apiKey, no env

            NodeExecutionContext ctx = new NodeExecutionContext("node1", "test", "llm", inputs);
            NodeExecutionResult result = executor.execute(ctx);

            assertTrue(result.isFailed());
            assertTrue(result.getErrorMessage().contains("API Key"));
        }
    }

    @Test
    void testExecute_nullContext() throws Exception {
        // 不应抛异常，应返回 failed
        NodeExecutionResult result = executor.execute(null);
        assertTrue(result.isFailed());
    }
}
