package com.ben.workflow.adapter.kling;

import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KlingExecutor 单元测试
 */
class KlingExecutorTest {

    private KlingExecutor klingExecutor;
    private ModelExecutionContext context;

    @BeforeEach
    void setUp() {
        klingExecutor = new KlingExecutor();
        context = new ModelExecutionContext();
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("kling", klingExecutor.getType());
    }

    @Test
    @DisplayName("测试正常执行 - 默认参数")
    void testExecuteWithDefaultParams() {
        context.setConfig(Map.of("prompt", "一个美丽的日落"));

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("video", result.getData().get("type"));
        assertEquals("kling", result.getData().get("modelProvider"));
        assertEquals("一个美丽的日落", result.getData().get("prompt"));
        assertEquals(5.0, result.getData().get("duration"));
        assertEquals(24, result.getData().get("fps"));
        assertEquals(1280, result.getData().get("width"));
        assertEquals(720, result.getData().get("height"));
    }

    @Test
    @DisplayName("测试正常执行 - 自定义参数")
    void testExecuteWithCustomParams() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "宇宙飞船");
        config.put("duration", 10.0);
        config.put("fps", 30);
        context.setConfig(config);

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(10.0, result.getData().get("duration"));
        assertEquals(30, result.getData().get("fps"));
    }

    @Test
    @DisplayName("测试执行 - 空配置")
    void testExecuteWithNullContext() {
        ModelExecutionResult result = klingExecutor.execute(null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("", result.getData().get("prompt"));
        assertEquals(5.0, result.getData().get("duration"));
    }

    @Test
    @DisplayName("测试执行 - 空配置对象")
    void testExecuteWithEmptyConfig() {
        context.setConfig(new HashMap<>());

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试执行 - 部分参数")
    void testExecuteWithPartialParams() {
        context.setConfig(Map.of("prompt", "测试视频", "duration", 15.0));

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("测试视频", result.getData().get("prompt"));
        assertEquals(15.0, result.getData().get("duration"));
        assertEquals(24, result.getData().get("fps"));
    }

    @Test
    @DisplayName("测试返回值包含预览 URL")
    void testExecuteReturnsPreviewUrl() {
        context.setConfig(Map.of("prompt", "预览测试"));

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("previewUrl"));
        assertTrue(((String) result.getData().get("previewUrl")).contains("placeholder"));
    }

    @Test
    @DisplayName("测试返回值包含视频 URL")
    void testExecuteReturnsVideoUrl() {
        context.setConfig(Map.of("prompt", "视频测试"));

        ModelExecutionResult result = klingExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("url"));
        assertTrue(((String) result.getData().get("url")).contains("googleapis"));
    }
}
