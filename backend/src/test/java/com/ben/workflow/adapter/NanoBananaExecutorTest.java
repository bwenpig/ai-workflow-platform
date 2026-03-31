package com.ben.workflow.adapter.nanobanana;

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
 * NanoBananaExecutor 单元测试
 */
@ExtendWith(MockitoExtension.class)
class NanoBananaExecutorTest {

    private NanoBananaExecutor nanoBananaExecutor;

    @Mock
    private ModelExecutionContext mockContext;

    @BeforeEach
    void setUp() {
        nanoBananaExecutor = new NanoBananaExecutor();
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("nanobanana", nanoBananaExecutor.getType());
    }

    @Test
    @DisplayName("测试正常执行 - 默认参数")
    void testExecuteWithDefaultParams() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "一朵盛开的花");
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = nanoBananaExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("image", result.getData().get("type"));
        assertEquals("nanobanana", result.getData().get("modelProvider"));
        assertEquals("一朵盛开的花", result.getData().get("prompt"));
        assertEquals(1024, result.getData().get("width"));
        assertEquals(1024, result.getData().get("height"));
    }

    @Test
    @DisplayName("测试正常执行 - 自定义尺寸")
    void testExecuteWithCustomSize() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "自定义尺寸");
        config.put("width", 512);
        config.put("height", 768);
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = nanoBananaExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertEquals(512, result.getData().get("width"));
        assertEquals(768, result.getData().get("height"));
    }

    @Test
    @DisplayName("测试执行 - 空配置")
    void testExecuteWithNullContext() {
        ModelExecutionResult result = nanoBananaExecutor.execute(null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("", result.getData().get("prompt"));
        assertEquals(1024, result.getData().get("width"));
        assertEquals(1024, result.getData().get("height"));
    }

    @Test
    @DisplayName("测试执行 - 空配置对象")
    void testExecuteWithEmptyConfig() {
        Map<String, Object> config = new HashMap<>();
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = nanoBananaExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试执行 - 部分参数")
    void testExecuteWithPartialParams() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "测试图片");
        config.put("width", 2048);
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = nanoBananaExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertEquals("测试图片", result.getData().get("prompt"));
        assertEquals(2048, result.getData().get("width"));
        assertEquals(1024, result.getData().get("height")); // 默认值
    }

    @Test
    @DisplayName("测试错误处理 - 异常捕获")
    void testExecuteWithErrorHandling() {
        ModelExecutionContext errorContext = mock(ModelExecutionContext.class);
        when(errorContext.getConfig()).thenThrow(new RuntimeException("NanoBanana 测试异常"));

        ModelExecutionResult result = nanoBananaExecutor.execute(errorContext);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("NanoBanana 执行失败"));
        assertTrue(result.getError().contains("NanoBanana 测试异常"));
    }

    @Test
    @DisplayName("测试返回值包含图片 URL")
    void testExecuteReturnsImageUrl() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "图片测试");
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = nanoBananaExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("url"));
        assertTrue(((String) result.getData().get("url")).contains("placeholder"));
    }

    @Test
    @DisplayName("测试不同尺寸组合")
    void testExecuteWithDifferentSizes() {
        int[][] sizes = {{512, 512}, {1024, 1024}, {1920, 1080}, {768, 1024}};
        
        for (int[] size : sizes) {
            Map<String, Object> config = new HashMap<>();
            config.put("prompt", "尺寸测试");
            config.put("width", size[0]);
            config.put("height", size[1]);
            when(mockContext.getConfig()).thenReturn(config);

            ModelExecutionResult result = nanoBananaExecutor.execute(mockContext);

            assertTrue(result.isSuccess());
            assertEquals(size[0], result.getData().get("width"));
            assertEquals(size[1], result.getData().get("height"));
        }
    }
}
