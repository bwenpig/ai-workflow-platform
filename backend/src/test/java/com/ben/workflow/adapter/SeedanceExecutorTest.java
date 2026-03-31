package com.ben.workflow.adapter.seedance;

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
 * SeedanceExecutor 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SeedanceExecutorTest {

    private SeedanceExecutor seedanceExecutor;

    @Mock
    private ModelExecutionContext mockContext;

    @BeforeEach
    void setUp() {
        seedanceExecutor = new SeedanceExecutor();
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("seedance", seedanceExecutor.getType());
    }

    @Test
    @DisplayName("测试正常执行 - 默认参数")
    void testExecuteWithDefaultParams() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "一个未来城市");
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = seedanceExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("video", result.getData().get("type"));
        assertEquals("seedance", result.getData().get("modelProvider"));
        assertEquals("一个未来城市", result.getData().get("prompt"));
        assertEquals(10.0, result.getData().get("duration"));
        assertEquals(30, result.getData().get("fps"));
    }

    @Test
    @DisplayName("测试正常执行 - 自定义参数")
    void testExecuteWithCustomParams() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "海底世界");
        config.put("duration", 20.0);
        config.put("fps", 60);
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = seedanceExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertEquals(20.0, result.getData().get("duration"));
        assertEquals(60, result.getData().get("fps"));
    }

    @Test
    @DisplayName("测试执行 - 空配置")
    void testExecuteWithNullContext() {
        ModelExecutionResult result = seedanceExecutor.execute(null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("", result.getData().get("prompt"));
        assertEquals(10.0, result.getData().get("duration"));
    }

    @Test
    @DisplayName("测试执行 - 空配置对象")
    void testExecuteWithEmptyConfig() {
        Map<String, Object> config = new HashMap<>();
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = seedanceExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试执行 - 部分参数")
    void testExecuteWithPartialParams() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "测试视频");
        config.put("fps", 24);
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = seedanceExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertEquals("测试视频", result.getData().get("prompt"));
        assertEquals(10.0, result.getData().get("duration")); // 默认值
        assertEquals(24, result.getData().get("fps"));
    }

    @Test
    @DisplayName("测试错误处理 - 异常捕获")
    void testExecuteWithErrorHandling() {
        ModelExecutionContext errorContext = mock(ModelExecutionContext.class);
        when(errorContext.getConfig()).thenThrow(new RuntimeException("Seedance 测试异常"));

        ModelExecutionResult result = seedanceExecutor.execute(errorContext);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Seedance 执行失败"));
        assertTrue(result.getError().contains("Seedance 测试异常"));
    }

    @Test
    @DisplayName("测试返回值包含视频 URL")
    void testExecuteReturnsVideoUrl() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "视频测试");
        when(mockContext.getConfig()).thenReturn(config);

        ModelExecutionResult result = seedanceExecutor.execute(mockContext);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("url"));
        assertTrue(((String) result.getData().get("url")).contains("googleapis"));
    }
}
