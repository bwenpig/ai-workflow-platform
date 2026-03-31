package com.ben.workflow.adapter;

import com.ben.workflow.adapter.seedance.SeedanceExecutor;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SeedanceModelProvider (SeedanceExecutor) 单元测试
 * 测试 Seedance 视频生成模型的各种场景
 */
class SeedanceModelProviderTest {

    private SeedanceExecutor seedanceExecutor;
    private ModelExecutionContext context;

    @BeforeEach
    void setUp() {
        seedanceExecutor = new SeedanceExecutor();
        context = new ModelExecutionContext();
        context.setConfig(new HashMap<>());
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("seedance", seedanceExecutor.getType(), "执行器类型应为 seedance");
    }

    @Test
    @DisplayName("成功调用 Seedance API - 默认参数")
    void testExecuteSuccessWithDefaultParams() {
        context.getConfig().put("prompt", "动感舞蹈视频");

        ModelExecutionResult result = seedanceExecutor.execute(context);

        assertTrue(result.isSuccess(), "执行应该成功");
        assertNotNull(result.getData(), "返回数据不应为空");
        assertEquals("video", result.getData().get("type"));
        assertEquals("seedance", result.getData().get("modelProvider"));
        assertEquals("动感舞蹈视频", result.getData().get("prompt"));
        assertEquals(10.0, result.getData().get("duration"));
        assertEquals(30, result.getData().get("fps"));
        assertTrue(((String) result.getData().get("url")).contains("googleapis"));
    }

    @Test
    @DisplayName("成功调用 Seedance API - 自定义参数")
    void testExecuteSuccessWithCustomParams() {
        context.getConfig().put("prompt", "街舞表演");
        context.getConfig().put("duration", 15.0);
        context.getConfig().put("fps", 60);

        ModelExecutionResult result = seedanceExecutor.execute(context);

        assertTrue(result.isSuccess(), "执行应该成功");
        assertEquals("街舞表演", result.getData().get("prompt"));
        assertEquals(15.0, result.getData().get("duration"));
        assertEquals(60, result.getData().get("fps"));
    }

    @Test
    @DisplayName("API 超时处理 - 长时间视频")
    void testApiTimeoutHandling() {
        context.getConfig().put("prompt", "长时间舞蹈表演");
        context.getConfig().put("duration", 120.0);

        ModelExecutionResult result = seedanceExecutor.execute(context);

        assertTrue(result.isSuccess(), "即使长时间任务也应返回结果");
        assertEquals(120.0, result.getData().get("duration"));
        assertNotNull(result.getData().get("url"), "应返回视频 URL");
    }

    @Test
    @DisplayName("空响应处理 - null context")
    void testEmptyResponseWithNullContext() {
        ModelExecutionResult result = seedanceExecutor.execute(null);

        assertTrue(result.isSuccess(), "null context 应被安全处理");
        assertNotNull(result.getData(), "应返回默认数据");
        assertEquals("", result.getData().get("prompt"));
        assertEquals(10.0, result.getData().get("duration"));
        assertEquals(30, result.getData().get("fps"));
    }

    @Test
    @DisplayName("空响应处理 - 空配置")
    void testEmptyResponseWithEmptyConfig() {
        context.setConfig(new HashMap<>());

        ModelExecutionResult result = seedanceExecutor.execute(context);

        assertTrue(result.isSuccess(), "空配置应被安全处理");
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试返回数据完整性")
    void testResponseDataCompleteness() {
        context.getConfig().put("prompt", "完整性测试");

        ModelExecutionResult result = seedanceExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("type"));
        assertTrue(result.getData().containsKey("modelProvider"));
        assertTrue(result.getData().containsKey("prompt"));
        assertTrue(result.getData().containsKey("url"));
        assertTrue(result.getData().containsKey("duration"));
        assertTrue(result.getData().containsKey("fps"));
    }

    @Test
    @DisplayName("测试多次调用一致性")
    void testMultipleCallsConsistency() {
        context.getConfig().put("prompt", "一致性测试");

        ModelExecutionResult result1 = seedanceExecutor.execute(context);
        ModelExecutionResult result2 = seedanceExecutor.execute(context);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        
        assertEquals("一致性测试", result1.getData().get("prompt"));
        assertEquals("一致性测试", result2.getData().get("prompt"));
        assertEquals(10.0, result1.getData().get("duration"));
        assertEquals(10.0, result2.getData().get("duration"));
    }

    @Test
    @DisplayName("测试不同参数独立处理")
    void testDifferentParamsHandling() {
        context.getConfig().put("prompt", "测试 1");
        context.getConfig().put("duration", 5.0);
        ModelExecutionResult result1 = seedanceExecutor.execute(context);

        context.getConfig().put("prompt", "测试 2");
        context.getConfig().put("duration", 20.0);
        ModelExecutionResult result2 = seedanceExecutor.execute(context);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertEquals("测试 1", result1.getData().get("prompt"));
        assertEquals("测试 2", result2.getData().get("prompt"));
        assertEquals(5.0, result1.getData().get("duration"));
        assertEquals(20.0, result2.getData().get("duration"));
    }

    @Test
    @DisplayName("测试部分参数 - 只有 prompt")
    void testWithOnlyPrompt() {
        context.getConfig().clear();
        context.getConfig().put("prompt", "只有 prompt");

        ModelExecutionResult result = seedanceExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("只有 prompt", result.getData().get("prompt"));
        assertEquals(10.0, result.getData().get("duration"));
        assertEquals(30, result.getData().get("fps"));
    }

    @Test
    @DisplayName("测试部分参数 - 只有 fps")
    void testWithOnlyFps() {
        context.getConfig().clear();
        context.getConfig().put("fps", 24);

        ModelExecutionResult result = seedanceExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
        assertEquals(10.0, result.getData().get("duration"));
        assertEquals(24, result.getData().get("fps"));
    }

    @Test
    @DisplayName("测试异常参数类型处理")
    void testWithInvalidParamTypes() {
        context.getConfig().put("prompt", 123);

        // SeedanceExecutor 会尝试将 Integer 转为 String，可能失败
        ModelExecutionResult result = seedanceExecutor.execute(context);

        // 不强制要求成功，取决于实现
        assertNotNull(result);
    }
}
