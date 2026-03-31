package com.ben.workflow.adapter.wan;

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
 * WanExecutor 单元测试
 */
@ExtendWith(MockitoExtension.class)
class WanExecutorTest {

    private WanExecutor wanExecutor;

    @Mock
    private ModelExecutionContext mockContext;

    @BeforeEach
    void setUp() {
        wanExecutor = new WanExecutor();
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("wan", wanExecutor.getType());
    }

    @Test
    @DisplayName("测试正常执行 - 带 prompt")
    void testExecuteWithPrompt() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "一只可爱的猫咪");
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = wanExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("image", result.getData().get("type"));
        assertEquals("wan", result.getData().get("modelProvider"));
        assertEquals("一只可爱的猫咪", result.getData().get("prompt"));
        assertEquals(1024, result.getData().get("width"));
        assertEquals(1024, result.getData().get("height"));
        assertEquals(30, result.getData().get("steps"));
    }

    @Test
    @DisplayName("测试执行 - 空配置")
    void testExecuteWithNullContext() {
        ModelExecutionResult result = wanExecutor.execute(null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试执行 - 空配置对象")
    void testExecuteWithEmptyConfig() {
        Map<String, Object> config = new HashMap<>();
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = wanExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试执行 - null prompt")
    void testExecuteWithNullPrompt() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", null);
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = wanExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试错误处理 - 异常捕获")
    void testExecuteWithErrorHandling() {
        ModelExecutionContext errorContext = mock(ModelExecutionContext.class);
        when(errorContext.getConfig()).thenThrow(new RuntimeException("Wan 测试异常"));

        ModelExecutionResult result = wanExecutor.execute(errorContext);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Wan 执行失败"));
        assertTrue(result.getError().contains("Wan 测试异常"));
    }

    @Test
    @DisplayName("测试返回值包含图片 URL")
    void testExecuteReturnsImageUrl() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "图片测试");
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = wanExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("url"));
        assertTrue(((String) result.getData().get("url")).contains("placeholder"));
    }

    @Test
    @DisplayName("测试返回固定步骤数")
    void testExecuteReturnsFixedSteps() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "步骤测试");
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = wanExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertEquals(30, result.getData().get("steps"));
    }
}
