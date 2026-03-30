package com.ben.workflow.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Python 执行结果测试
 */
public class PythonExecutionResultTest {
    
    @Test
    @DisplayName("创建默认结果")
    public void testDefaultResult() {
        PythonExecutionResult result = new PythonExecutionResult();
        
        assertFalse(result.isSuccess());
        assertNull(result.getOutputs());
        assertNull(result.getLogs());
        assertNull(result.getError());
        assertNull(result.getDuration());
    }
    
    @Test
    @DisplayName("创建成功结果")
    public void testSuccessResult() {
        Map<String, Object> outputs = Map.of("key", "value");
        String logs = "Execution completed";
        
        PythonExecutionResult result = PythonExecutionResult.success(outputs, logs);
        
        assertTrue(result.isSuccess());
        assertEquals(outputs, result.getOutputs());
        assertEquals(logs, result.getLogs());
        assertNull(result.getError());
    }
    
    @Test
    @DisplayName("创建失败结果")
    public void testFailureResult() {
        String error = "Something went wrong";
        
        PythonExecutionResult result = PythonExecutionResult.failure(error);
        
        assertFalse(result.isSuccess());
        assertNull(result.getOutputs());
        assertNull(result.getLogs());
        assertEquals(error, result.getError());
    }
    
    @Test
    @DisplayName("设置成功状态")
    public void testSetSuccess() {
        PythonExecutionResult result = new PythonExecutionResult();
        
        result.setSuccess(true);
        
        assertTrue(result.isSuccess());
        
        result.setSuccess(false);
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    @DisplayName("设置输出数据")
    public void testSetOutputs() {
        PythonExecutionResult result = new PythonExecutionResult();
        Map<String, Object> outputs = Map.of("result", "test");
        
        result.setOutputs(outputs);
        
        assertEquals(outputs, result.getOutputs());
    }
    
    @Test
    @DisplayName("设置日志")
    public void testSetLogs() {
        PythonExecutionResult result = new PythonExecutionResult();
        String logs = "Log message";
        
        result.setLogs(logs);
        
        assertEquals(logs, result.getLogs());
    }
    
    @Test
    @DisplayName("设置错误信息")
    public void testSetError() {
        PythonExecutionResult result = new PythonExecutionResult();
        String error = "Error message";
        
        result.setError(error);
        
        assertEquals(error, result.getError());
    }
    
    @Test
    @DisplayName("设置执行时长")
    public void testSetDuration() {
        PythonExecutionResult result = new PythonExecutionResult();
        Long duration = 1234L;
        
        result.setDuration(duration);
        
        assertEquals(duration, result.getDuration());
    }
    
    @Test
    @DisplayName("完整结果对象测试")
    public void testFullResult() {
        Map<String, Object> outputs = Map.of("output", "data");
        String logs = "Execution logs";
        String error = "Error info";
        Long duration = 5000L;
        
        PythonExecutionResult result = new PythonExecutionResult(true, outputs, logs, error, duration);
        
        assertTrue(result.isSuccess());
        assertEquals(outputs, result.getOutputs());
        assertEquals(logs, result.getLogs());
        assertEquals(error, result.getError());
        assertEquals(duration, result.getDuration());
    }
    
    @Test
    @DisplayName("空输出测试")
    public void testEmptyOutputs() {
        Map<String, Object> emptyOutputs = new HashMap<>();
        
        PythonExecutionResult result = PythonExecutionResult.success(emptyOutputs, "logs");
        
        assertTrue(result.isSuccess());
        assertTrue(result.getOutputs().isEmpty());
    }
}
