package com.ben.workflow.adapter.kling;

import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * KlingExecutor 单元测试
 */
@ExtendWith(MockitoExtension.class)
class KlingExecutorTest {

    private KlingExecutor klingExecutor;

    @Mock
    private ModelExecutionContext mockContext;

    @BeforeEach
    void setUp() {
        klingExecutor = new KlingExecutor();
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("kling", klingExecutor.getType());
    }

    @Test
    @DisplayName("测试正常执行 - 默认参数")
    void testExecuteWithDefaultParams() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "一个美丽的日落");
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = klingExecutor.execute(mockContext);

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
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = klingExecutor.execute(mockContext);

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
        Map<String, Object> config = new HashMap<>();
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = klingExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试执行 - 部分参数")
    void testExecuteWithPartialParams() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "测试视频");
        config.put("duration", 15.0);
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = klingExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertEquals("测试视频", result.getData().get("prompt"));
        assertEquals(15.0, result.getData().get("duration"));
        assertEquals(24, result.getData().get("fps")); // 默认值
    }

    @Test
    @DisplayName("测试错误处理 - 异常捕获")
    void testExecuteWithErrorHandling() {
        ModelExecutionContext errorContext = mock(ModelExecutionContext.class);
        when(errorContext.getConfig()).thenThrow(new RuntimeException("测试异常"));

        ModelExecutionResult result = klingExecutor.execute(errorContext);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Kling 执行失败"));
        assertTrue(result.getError().contains("测试异常"));
    }

    @Test
    @DisplayName("测试返回值包含预览 URL")
    void testExecuteReturnsPreviewUrl() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "预览测试");
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = klingExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("previewUrl"));
        assertTrue(((String) result.getData().get("previewUrl")).contains("placeholder"));
    }
}
