package com.ben.workflow.adapter.seedance;

import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SeedanceExecutor 单元测试
 */
class SeedanceExecutorTest {

    private SeedanceExecutor seedanceExecutor;
    private ModelExecutionContext context;

    @BeforeEach
    void setUp() {
        seedanceExecutor = new SeedanceExecutor();
        context = new ModelExecutionContext();
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("seedance", seedanceExecutor.getType());
    }

    @Test
    @DisplayName("测试正常执行 - 默认参数")
    void testExecuteWithDefaultParams() {
        context.setConfig(Map.of("prompt", "一个未来城市"));

        ModelExecutionResult result = seedanceExecutor.execute(context);

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
        context.setConfig(config);

        ModelExecutionResult result = seedanceExecutor.execute(context);

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
        context.setConfig(new HashMap<>());

        ModelExecutionResult result = seedanceExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试执行 - 部分参数")
    void testExecuteWithPartialParams() {
        context.setConfig(Map.of("prompt", "测试视频", "fps", 24));

        ModelExecutionResult result = seedanceExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("测试视频", result.getData().get("prompt"));
        assertEquals(10.0, result.getData().get("duration"));
        assertEquals(24, result.getData().get("fps"));
    }

    @Test
    @DisplayName("测试返回值包含视频 URL")
    void testExecuteReturnsVideoUrl() {
        context.setConfig(Map.of("prompt", "视频测试"));

        ModelExecutionResult result = seedanceExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("url"));
        assertTrue(((String) result.getData().get("url")).contains("googleapis"));
    }
}
