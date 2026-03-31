package com.ben.workflow.spi;

import com.ben.workflow.adapter.nanobanana.NanoBananaExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NanoBananaExecutor 单元测试
 */
public class NanoBananaExecutorTest {
    private final NanoBananaExecutor executor = new NanoBananaExecutor();

    @Test @DisplayName("测试执行器类型")
    public void testExecutorType() { assertEquals("nanobanana", executor.getType()); }

    @Test @DisplayName("测试执行 - 基本参数")
    public void testExecuteBasic() {
        ModelExecutionContext context = new ModelExecutionContext();
        context.setConfig(Map.of("prompt", "test", "width", 1024));
        ModelExecutionResult result = executor.execute(context);
        assertTrue(result.isSuccess());
        assertEquals("image", result.getData().get("type"));
    }

    @Test @DisplayName("测试执行 - 空配置")
    public void testExecuteEmptyConfig() {
        ModelExecutionContext context = new ModelExecutionContext();
        context.setConfig(new HashMap<>());
        assertTrue(executor.execute(context).isSuccess());
    }

    @Test @DisplayName("测试执行 - null 配置")
    public void testExecuteNullConfig() {
        ModelExecutionContext context = new ModelExecutionContext();
        context.setConfig(null);
        assertTrue(executor.execute(context).isSuccess());
    }

    @Test @DisplayName("测试执行 - 带输入")
    public void testExecuteWithInputs() {
        ModelExecutionContext context = new ModelExecutionContext();
        context.setInputs(Map.of("sketch", "image.jpg"));
        context.setConfig(Map.of("prompt", "colorize"));
        assertTrue(executor.execute(context).isSuccess());
    }
}
