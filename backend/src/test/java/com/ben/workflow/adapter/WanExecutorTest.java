package com.ben.workflow.adapter.wan;

import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WanExecutor 单元测试
 */
class WanExecutorTest {

    private WanExecutor wanExecutor;
    private ModelExecutionContext context;

    @BeforeEach
    void setUp() {
        wanExecutor = new WanExecutor();
        context = new ModelExecutionContext();
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("wan", wanExecutor.getType());
    }

    @Test
    @DisplayName("测试正常执行 - 带 prompt")
    void testExecuteWithPrompt() {
        context.setConfig(Map.of("prompt", "一只可爱的猫咪"));

        ModelExecutionResult result = wanExecutor.execute(context);

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
    @DisplayName("测试执行 - 空配置对象")
    void testExecuteWithEmptyConfigObject() {
        context.setConfig(new HashMap<>());

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
    }



    @Test
    @DisplayName("测试执行 - null prompt")
    void testExecuteWithNullPrompt() {
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", null);
        context.setConfig(config);

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("", result.getData().get("prompt"));
    }

    @Test
    @DisplayName("测试返回值包含图片 URL")
    void testExecuteReturnsImageUrl() {
        context.setConfig(Map.of("prompt", "图片测试"));

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().containsKey("url"));
        assertTrue(((String) result.getData().get("url")).contains("placeholder"));
    }

    @Test
    @DisplayName("测试返回固定步骤数")
    void testExecuteReturnsFixedSteps() {
        context.setConfig(Map.of("prompt", "步骤测试"));

        ModelExecutionResult result = wanExecutor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(30, result.getData().get("steps"));
    }
}
