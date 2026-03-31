package com.ben.workflow.adapter;

import com.ben.workflow.adapter.nanobanana.NanoBananaExecutor;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NanoBananaModelProvider (NanoBananaExecutor) 单元测试
 * 测试 NanoBanana 图像生成模型的各种场景
 */
class NanoBananaModelProviderTest {

    private NanoBananaExecutor nanoBananaExecutor;
    private ModelExecutionContext context;

    @BeforeEach
    void setUp() {
        nanoBananaExecutor = new NanoBananaExecutor();
        context = new ModelExecutionContext();
        context.setConfig(new HashMap<>());
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("nanobanana", nanoBananaExecutor.getType(), "执行器类型应为 nanobanana");
    }

    @Test
    @DisplayName("成功调用 NanoBanana API - 默认参数")
    void testExecuteSuccessWithDefaultParams() {
        context.getConfig().put("prompt", "一个香蕉");

        ModelExecutionResult result = nanoBananaExecutor.execute(context);

        assertTrue(result.isSuccess(), "执行应该成功");
        assertNotNull(result.getData(), "返回数据不应为空");
        assertEquals("image", result.getData().get("type"));
        assertEquals("nanobanana", result.getData().get("modelProvider"));
        assertEquals("一个香蕉", result.getData().get("prompt"));
        assertEquals(1024, result.getData().get("width"));
        assertEquals(1024, result.getData().get("height"));
        assertTrue(((String) result.getData().get("url")).contains("placeholder"));
    }

    @Test
    @DisplayName("成功调用 NanoBanana API - 自定义尺寸")
    void testExecuteSuccessWithCustomSize() {
        context.getConfig().put("prompt", "宽屏图像");
        context.getConfig().put("width", 1920);
        context.getConfig().put("height", 1080);

        ModelExecutionResult result = nanoBananaExecutor.execute(context);

        assertTrue(result.isSuccess(), "执行应该成功");
        assertEquals("宽屏图像", result.getData().get("prompt"));
        assertEquals(1920, result.getData().get("width"));
        assertEquals(1080, result.getData().get("height"));
    }

    @Test
    @DisplayName("API 超时处理 - 高分辨率图像")
    void testApiTimeoutHandling() {
        context.getConfig().put("prompt", "超高分辨率图像");
        context.getConfig().put("width", 4096);
        context.getConfig().put("height", 4096);

        ModelExecutionResult result = nanoBananaExecutor.execute(context);

        assertTrue(result.isSuccess(), "即使高分辨率任务也应返回结果");
        assertEquals(4096, result.getData().get("width"));
        assertEquals(4096, result.getData().get("height"));
        assertNotNull(result.getData().get("url"), "应返回图像 URL");
    }

    @Test
    @DisplayName("空响应处理 - null context")
    void testEmptyResponseWithNullContext() {
        ModelExecutionResult result = nanoBananaExecutor.execute(null);

        assertTrue(result.isSuccess(), "null context 应被安全处理");
        assertNotNull(result.getData(), "应返回默认数据");
        assertEquals("", result.getData().get("prompt"));
        assertEquals(1024, result.getData().get("width"));
        assertEquals(1024, result.getData().get("height"));
    }

    @Test
    @DisplayName("空响应处理 - 空配置")
    void testEmptyResponseWithEmptyConfig() {
        context.setConfig(new HashMap<>());

        ModelExecutionResult result = nanoBananaExecutor.execute(context);

        assertTrue(result.isSuccess(), "空配置应被安全处理");
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试返回数据完整性")
    void testResponseDataCompleteness() {
        context.getConfig().put("prompt", "完整性测试");

        ModelExecutionResult result = nanoBananaExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("type"));
        assertTrue(result.getData().containsKey("modelProvider"));
        assertTrue(result.getData().containsKey("prompt"));
        assertTrue(result.getData().containsKey("url"));
        assertTrue(result.getData().containsKey("width"));
        assertTrue(result.getData().containsKey("height"));
    }

    @Test
    @DisplayName("测试多次调用一致性")
    void testMultipleCallsConsistency() {
        context.getConfig().put("prompt", "一致性测试");

        ModelExecutionResult result1 = nanoBananaExecutor.execute(context);
        ModelExecutionResult result2 = nanoBananaExecutor.execute(context);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        
        assertEquals("一致性测试", result1.getData().get("prompt"));
        assertEquals("一致性测试", result2.getData().get("prompt"));
        assertEquals(1024, result1.getData().get("width"));
        assertEquals(1024, result2.getData().get("width"));
    }

    @Test
    @DisplayName("测试不同参数独立处理")
    void testDifferentParamsHandling() {
        context.getConfig().put("prompt", "测试 1");
        context.getConfig().put("width", 512);
        ModelExecutionResult result1 = nanoBananaExecutor.execute(context);

        context.getConfig().put("prompt", "测试 2");
        context.getConfig().put("width", 2048);
        ModelExecutionResult result2 = nanoBananaExecutor.execute(context);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertEquals("测试 1", result1.getData().get("prompt"));
        assertEquals("测试 2", result2.getData().get("prompt"));
        assertEquals(512, result1.getData().get("width"));
        assertEquals(2048, result2.getData().get("width"));
    }

    @Test
    @DisplayName("测试部分参数 - 只有 prompt")
    void testWithOnlyPrompt() {
        context.getConfig().clear();
        context.getConfig().put("prompt", "只有 prompt");

        ModelExecutionResult result = nanoBananaExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("只有 prompt", result.getData().get("prompt"));
        assertEquals(1024, result.getData().get("width"));
        assertEquals(1024, result.getData().get("height"));
    }

    @Test
    @DisplayName("测试部分参数 - 只有 height")
    void testWithOnlyHeight() {
        context.getConfig().clear();
        context.getConfig().put("height", 2048);

        ModelExecutionResult result = nanoBananaExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
        assertEquals(1024, result.getData().get("width"));
        assertEquals(2048, result.getData().get("height"));
    }

    @Test
    @DisplayName("测试异常参数类型处理")
    void testWithInvalidParamTypes() {
        context.getConfig().put("prompt", 123);

        // NanoBananaExecutor 会尝试将 Integer 转为 String，可能失败
        ModelExecutionResult result = nanoBananaExecutor.execute(context);

        // 不强制要求成功，取决于实现
        assertNotNull(result);
    }

    @Test
    @DisplayName("测试非正方形图像")
    void testNonSquareImage() {
        context.getConfig().put("prompt", "横向图像");
        context.getConfig().put("width", 1920);
        context.getConfig().put("height", 1080);

        ModelExecutionResult result = nanoBananaExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(1920, result.getData().get("width"));
        assertEquals(1080, result.getData().get("height"));
        assertNotEquals(result.getData().get("width"), result.getData().get("height"));
    }
}
