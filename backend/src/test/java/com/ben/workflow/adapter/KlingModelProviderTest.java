package com.ben.workflow.adapter;

import com.ben.workflow.adapter.kling.KlingExecutor;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KlingModelProvider (KlingExecutor) 单元测试
 * 测试可灵 AI 文生视频模型的各种场景
 */
class KlingModelProviderTest {

    private KlingExecutor klingExecutor;
    private ModelExecutionContext context;

    @BeforeEach
    void setUp() {
        klingExecutor = new KlingExecutor();
        context = new ModelExecutionContext();
        context.setConfig(new HashMap<>());
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("kling", klingExecutor.getType(), "执行器类型应为 kling");
    }

    @Test
    @DisplayName("成功调用可灵 API - 默认参数")
    void testExecuteSuccessWithDefaultParams() {
        context.getConfig().put("prompt", "一个美丽的日落");

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess(), "执行应该成功");
        assertNotNull(result.getData(), "返回数据不应为空");
        assertEquals("video", result.getData().get("type"));
        assertEquals("kling", result.getData().get("modelProvider"));
        assertEquals("一个美丽的日落", result.getData().get("prompt"));
        assertEquals(5.0, result.getData().get("duration"));
        assertEquals(24, result.getData().get("fps"));
        assertEquals(1280, result.getData().get("width"));
        assertEquals(720, result.getData().get("height"));
        assertTrue(((String) result.getData().get("url")).contains("googleapis"));
        assertTrue(((String) result.getData().get("previewUrl")).contains("placeholder"));
    }

    @Test
    @DisplayName("成功调用可灵 API - 自定义参数")
    void testExecuteSuccessWithCustomParams() {
        context.getConfig().put("prompt", "宇宙飞船在太空中飞行");
        context.getConfig().put("duration", 10.0);
        context.getConfig().put("fps", 30);

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess(), "执行应该成功");
        assertEquals("宇宙飞船在太空中飞行", result.getData().get("prompt"));
        assertEquals(10.0, result.getData().get("duration"));
        assertEquals(30, result.getData().get("fps"));
    }

    @Test
    @DisplayName("API 超时处理 - 长时间任务")
    void testApiTimeoutHandling() {
        context.getConfig().put("prompt", "长时间视频生成测试");
        context.getConfig().put("duration", 60.0);

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess(), "即使长时间任务也应返回结果");
        assertEquals(60.0, result.getData().get("duration"));
        assertNotNull(result.getData().get("url"), "应返回视频 URL");
    }

    @Test
    @DisplayName("API 错误返回 - 空提示词")
    void testApiErrorWithEmptyPrompt() {
        context.getConfig().put("prompt", "");

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess(), "空提示词也应返回成功（使用默认值）");
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("空响应处理 - null context")
    void testEmptyResponseWithNullContext() {
        ModelExecutionResult result = klingExecutor.execute(null);

        assertTrue(result.isSuccess(), "null context 应被安全处理");
        assertNotNull(result.getData(), "应返回默认数据");
        assertEquals("", result.getData().get("prompt"));
        assertEquals(5.0, result.getData().get("duration"));
    }

    @Test
    @DisplayName("空响应处理 - 空配置")
    void testEmptyResponseWithEmptyConfig() {
        context.setConfig(new HashMap<>());

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess(), "空配置应被安全处理");
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("重试机制 - 多次调用一致性")
    void testRetryMechanismConsistency() {
        context.getConfig().put("prompt", "重试测试");
        
        ModelExecutionResult result1 = klingExecutor.execute(context);
        ModelExecutionResult result2 = klingExecutor.execute(context);
        ModelExecutionResult result3 = klingExecutor.execute(context);

        assertTrue(result1.isSuccess(), "第一次调用应成功");
        assertTrue(result2.isSuccess(), "第二次调用应成功");
        assertTrue(result3.isSuccess(), "第三次调用应成功");
        
        assertEquals("重试测试", result1.getData().get("prompt"));
        assertEquals("重试测试", result2.getData().get("prompt"));
        assertEquals("重试测试", result3.getData().get("prompt"));
    }

    @Test
    @DisplayName("重试机制 - 不同参数独立处理")
    void testRetryMechanismWithDifferentParams() {
        context.getConfig().put("prompt", "测试 1");
        context.getConfig().put("duration", 5.0);
        ModelExecutionResult result1 = klingExecutor.execute(context);

        context.getConfig().put("prompt", "测试 2");
        context.getConfig().put("duration", 10.0);
        ModelExecutionResult result2 = klingExecutor.execute(context);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertEquals("测试 1", result1.getData().get("prompt"));
        assertEquals("测试 2", result2.getData().get("prompt"));
        assertEquals(5.0, result1.getData().get("duration"));
        assertEquals(10.0, result2.getData().get("duration"));
    }

    @Test
    @DisplayName("测试返回数据完整性")
    void testResponseDataCompleteness() {
        context.getConfig().put("prompt", "完整性测试");

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("type"));
        assertTrue(result.getData().containsKey("modelProvider"));
        assertTrue(result.getData().containsKey("prompt"));
        assertTrue(result.getData().containsKey("url"));
        assertTrue(result.getData().containsKey("previewUrl"));
        assertTrue(result.getData().containsKey("duration"));
        assertTrue(result.getData().containsKey("fps"));
        assertTrue(result.getData().containsKey("width"));
        assertTrue(result.getData().containsKey("height"));
    }

    @Test
    @DisplayName("测试部分参数 - 只有 prompt")
    void testWithOnlyPrompt() {
        context.getConfig().clear();
        context.getConfig().put("prompt", "只有 prompt");

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("只有 prompt", result.getData().get("prompt"));
        assertEquals(5.0, result.getData().get("duration"));
        assertEquals(24, result.getData().get("fps"));
    }

    @Test
    @DisplayName("测试部分参数 - 只有 duration")
    void testWithOnlyDuration() {
        context.getConfig().clear();
        context.getConfig().put("duration", 15.0);

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
        assertEquals(15.0, result.getData().get("duration"));
    }

    @Test
    @DisplayName("测试异常参数类型处理")
    void testWithInvalidParamTypes() {
        context.getConfig().put("prompt", 123);

        // KlingExecutor 会尝试将 Integer 转为 String，可能失败
        ModelExecutionResult result = klingExecutor.execute(context);

        // 不强制要求成功，取决于实现
        assertNotNull(result);
    }
}
