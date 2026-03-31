package com.ben.workflow.spi;

import com.ben.workflow.adapter.seedance.SeedanceExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SeedanceExecutor 单元测试
 */
public class SeedanceExecutorTest {
    private final SeedanceExecutor executor = new SeedanceExecutor();

    @Test @DisplayName("测试执行器类型")
    public void testExecutorType() { assertEquals("seedance", executor.getType()); }

    @Test @DisplayName("测试执行 - 基本参数")
    public void testExecuteBasic() {
        ModelExecutionContext context = new ModelExecutionContext();
        context.setConfig(Map.of("prompt", "test", "duration", 10.0));
        ModelExecutionResult result = executor.execute(context);
        assertTrue(result.isSuccess());
        assertEquals("video", result.getData().get("type"));
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
        context.setInputs(Map.of("music", "audio.mp3"));
        context.setConfig(Map.of("prompt", "dance"));
        assertTrue(executor.execute(context).isSuccess());
    }
}
