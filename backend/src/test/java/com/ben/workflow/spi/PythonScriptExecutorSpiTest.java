package com.ben.workflow.spi;

import com.ben.workflow.engine.PythonScriptExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonScriptExecutor SPI 测试
 */
public class PythonScriptExecutorSpiTest {
    private final PythonScriptExecutor executor = new PythonScriptExecutor();

    @Test @DisplayName("测试执行器类型")
    public void testExecutorType() { assertEquals("python_script", executor.getType()); }

    @Test @DisplayName("测试 SPI 执行 - 简单脚本")
    public void testSpiExecuteSimple() {
        ModelExecutionContext context = new ModelExecutionContext();
        context.setConfig(Map.of("script", "outputs['result'] = {'type': 'text', 'content': 'Hello'}"));
        ModelExecutionResult result = executor.execute(context);
        assertTrue(result.isSuccess());
    }

    @Test @DisplayName("测试 SPI 执行 - 带输入")
    public void testSpiExecuteWithInputs() {
        ModelExecutionContext context = new ModelExecutionContext();
        context.setInputs(Map.of("text", "hello"));
        context.setConfig(Map.of("script", "outputs['result'] = inputs.get('text')"));
        ModelExecutionResult result = executor.execute(context);
        assertTrue(result.isSuccess());
    }

    @Test @DisplayName("测试 SPI 执行 - 错误脚本")
    public void testSpiExecuteError() {
        ModelExecutionContext context = new ModelExecutionContext();
        context.setConfig(Map.of("script", "raise Exception('error')"));
        ModelExecutionResult result = executor.execute(context);
        assertFalse(result.isSuccess());
    }

    @Test @DisplayName("测试 SPI 执行 - 超时")
    public void testSpiExecuteTimeout() {
        ModelExecutionContext context = new ModelExecutionContext();
        context.setConfig(Map.of("script", "import time; time.sleep(5)", "timeout", 2));
        ModelExecutionResult result = executor.execute(context);
        assertFalse(result.isSuccess());
    }
}
