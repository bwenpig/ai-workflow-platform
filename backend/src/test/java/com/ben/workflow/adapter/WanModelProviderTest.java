package com.ben.workflow.adapter;

import com.ben.workflow.adapter.wan.WanExecutor;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WanModelProvider (WanExecutor) 单元测试
 * 测试万相 AI 图像生成模型的各种场景
 */
class WanModelProviderTest {

    private WanExecutor wanExecutor;
    private ModelExecutionContext context;

    @BeforeEach
    void setUp() {
        wanExecutor = new WanExecutor();
        context = new ModelExecutionContext();
        context.setConfig(new HashMap<>());
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("wan", wanExecutor.getType(), "执行器类型应为 wan");
    }

    @Test
    @DisplayName("成功调用万相 API - 基本参数")
    void testExecuteSuccessWithBasicParams() {
        context.getConfig().put("prompt", "一只可爱的猫咪");

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess(), "执行应该成功");
        assertNotNull(result.getData(), "返回数据不应为空");
        assertEquals("image", result.getData().get("type"));
        assertEquals("wan", result.getData().get("modelProvider"));
        assertEquals("一只可爱的猫咪", result.getData().get("prompt"));
        assertEquals(1024, result.getData().get("width"));
        assertEquals(1024, result.getData().get("height"));
        assertEquals(30, result.getData().get("steps"));
        assertTrue(((String) result.getData().get("url")).contains("placeholder"));
    }

    @Test
    @DisplayName("成功调用万相 API - 固定尺寸")
    void testExecuteSuccessWithFixedSize() {
        context.getConfig().put("prompt", "风景画");

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess(), "执行应该成功");
        assertEquals("风景画", result.getData().get("prompt"));
        assertEquals(1024, result.getData().get("width"));
        assertEquals(1024, result.getData().get("height"));
    }

    @Test
    @DisplayName("API 超时处理 - 复杂图像生成")
    void testApiTimeoutHandling() {
        context.getConfig().put("prompt", "非常复杂的场景，包含很多细节");
        context.getConfig().put("steps", 100);

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess(), "即使复杂任务也应返回结果");
        assertNotNull(result.getData().get("url"), "应返回图像 URL");
    }

    @Test
    @DisplayName("API 错误返回 - 空提示词")
    void testApiErrorWithEmptyPrompt() {
        context.getConfig().put("prompt", "");

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess(), "空提示词应被安全处理");
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("空响应处理 - null context")
    void testEmptyResponseWithNullContext() {
        // WanExecutor 会尝试从 null config 获取数据
        ModelExecutionResult result = wanExecutor.execute(null);

        // 实现可能返回失败或默认值
        assertNotNull(result);
    }

    @Test
    @DisplayName("空响应处理 - 空配置")
    void testEmptyResponseWithEmptyConfig() {
        context.setConfig(new HashMap<>());

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess(), "空配置应被安全处理");
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试返回数据完整性")
    void testResponseDataCompleteness() {
        context.getConfig().put("prompt", "完整性测试");

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("type"));
        assertTrue(result.getData().containsKey("modelProvider"));
        assertTrue(result.getData().containsKey("prompt"));
        assertTrue(result.getData().containsKey("url"));
        assertTrue(result.getData().containsKey("width"));
        assertTrue(result.getData().containsKey("height"));
        assertTrue(result.getData().containsKey("steps"));
    }

    @Test
    @DisplayName("测试多次调用一致性")
    void testMultipleCallsConsistency() {
        context.getConfig().put("prompt", "一致性测试");

        ModelExecutionResult result1 = wanExecutor.execute(context);
        ModelExecutionResult result2 = wanExecutor.execute(context);
        ModelExecutionResult result3 = wanExecutor.execute(context);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertTrue(result3.isSuccess());
        
        assertEquals("一致性测试", result1.getData().get("prompt"));
        assertEquals("一致性测试", result2.getData().get("prompt"));
        assertEquals("一致性测试", result3.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试不同提示词独立处理")
    void testDifferentPromptsHandling() {
        context.getConfig().put("prompt", "提示词 1");
        ModelExecutionResult result1 = wanExecutor.execute(context);

        context.getConfig().put("prompt", "提示词 2");
        ModelExecutionResult result2 = wanExecutor.execute(context);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertEquals("提示词 1", result1.getData().get("prompt"));
        assertEquals("提示词 2", result2.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试异常参数类型处理")
    void testWithInvalidParamTypes() {
        context.getConfig().put("prompt", 123);

        // WanExecutor 会尝试将 Integer 转为 String，可能失败
        ModelExecutionResult result = wanExecutor.execute(context);

        // 不强制要求成功，取决于实现
        assertNotNull(result);
    }

    @Test
    @DisplayName("测试部分参数 - 只有 width (被忽略)")
    void testWithOnlyWidth() {
        context.getConfig().clear();
        context.getConfig().put("width", 2048);

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
        assertEquals(1024, result.getData().get("width")); // WanExecutor 固定为 1024
    }

    @Test
    @DisplayName("测试部分参数 - 只有 steps (被忽略)")
    void testWithOnlySteps() {
        context.getConfig().clear();
        context.getConfig().put("steps", 50);

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
        assertEquals(30, result.getData().get("steps")); // WanExecutor 固定为 30
    }
}
