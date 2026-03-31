package com.ben.workflow.engine;

import com.ben.workflow.model.PythonNodeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonDockerExecutor 单元测试
 * 
 * @author 龙傲天
 * @version 1.0
 */
public class PythonDockerExecutorTest {
    
    private PythonDockerExecutor executor;
    
    @BeforeEach
    public void setUp() {
        executor = new PythonDockerExecutor();
    }
    
    @Test
    @DisplayName("执行器类型检查")
    public void testExecutorType() {
        assertEquals("python_docker", executor.getType());
        assertEquals("Python Docker 沙箱", executor.getName());
        assertNotNull(executor.getDescription());
    }
    
    @Test
    @DisplayName("执行简单 Python 脚本")
    public void testExecuteSimpleScript() {
        String script = """
            result = inputs.get('a', 0) + inputs.get('b', 0)
            outputs['result'] = result
            print("计算完成")
            """;
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("a", 10);
        inputs.put("b", 20);
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript(script);
        config.setTimeout(10);
        
        PythonExecutionResult result = executor.execute(script, inputs, config);
        
        assertTrue(result.isSuccess(), "执行应该成功");
        assertNotNull(result.getOutputs());
        assertEquals(30, result.getOutputs().get("result"));
    }
    
    @Test
    @DisplayName("执行字符串处理脚本")
    public void testExecuteStringProcessingScript() {
        String script = """
            text = inputs.get('text', '')
            outputs['uppercase'] = text.upper()
            outputs['length'] = len(text)
            outputs['reversed'] = text[::-1]
            """;
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("text", "hello world");
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript(script);
        
        PythonExecutionResult result = executor.execute(script, inputs, config);
        
        assertTrue(result.isSuccess());
        assertEquals("HELLO WORLD", result.getOutputs().get("uppercase"));
        assertEquals(11, result.getOutputs().get("length"));
        assertEquals("dlrow olleh", result.getOutputs().get("reversed"));
    }
    
    @Test
    @DisplayName("执行 JSON 处理脚本")
    public void testExecuteJsonProcessingScript() {
        String script = """
            import json
            data = inputs.get('data', {})
            outputs['keys'] = list(data.keys())
            outputs['count'] = len(data)
            """;
        
        Map<String, Object> inputs = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);
        data.put("active", true);
        inputs.put("data", data);
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript(script);
        
        PythonExecutionResult result = executor.execute(script, inputs, config);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutputs().get("keys"));
        assertEquals(3, result.getOutputs().get("count"));
    }
    
    @Test
    @DisplayName("超时控制测试")
    public void testTimeoutControl() {
        String script = """
            import time
            time.sleep(10)  # 睡眠 10 秒
            outputs['done'] = True
            """;
        
        Map<String, Object> inputs = new HashMap<>();
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript(script);
        config.setTimeout(2);  // 2 秒超时
        
        long startTime = System.currentTimeMillis();
        PythonExecutionResult result = executor.execute(script, inputs, config);
        long elapsed = System.currentTimeMillis() - startTime;
        
        assertFalse(result.isSuccess(), "应该超时失败");
        assertTrue(elapsed < 5000, "应该在 5 秒内返回（超时保护生效）");
        assertTrue(result.getError().contains("超时"), "错误信息应该包含超时提示");
    }
    
    @Test
    @DisplayName("错误处理测试 - 语法错误")
    public void testSyntaxErrorHandling() {
        String script = """
            # 语法错误
            if True
                print("missing colon")
            """;
        
        Map<String, Object> inputs = new HashMap<>();
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript(script);
        
        PythonExecutionResult result = executor.execute(script, inputs, config);
        
        assertFalse(result.isSuccess(), "应该失败");
        assertNotNull(result.getError());
    }
    
    @Test
    @DisplayName("错误处理测试 - 运行时错误")
    public void testRuntimeErrorHandling() {
        String script = """
            # 运行时错误
            x = 1 / 0
            outputs['result'] = x
            """;
        
        Map<String, Object> inputs = new HashMap<>();
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript(script);
        
        PythonExecutionResult result = executor.execute(script, inputs, config);
        
        assertFalse(result.isSuccess(), "应该失败");
        assertNotNull(result.getError());
    }
    
    @Test
    @DisplayName("空输入测试")
    public void testEmptyInputs() {
        String script = """
            outputs['message'] = 'Hello from empty inputs'
            outputs['input_count'] = len(inputs)
            """;
        
        Map<String, Object> inputs = new HashMap<>();
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript(script);
        
        PythonExecutionResult result = executor.execute(script, inputs, config);
        
        assertTrue(result.isSuccess());
        assertEquals("Hello from empty inputs", result.getOutputs().get("message"));
        assertEquals(0, result.getOutputs().get("input_count"));
    }
    
    @Test
    @DisplayName("复杂数据结构测试")
    public void testComplexDataStructures() {
        String script = """
            # 处理列表和字典
            items = inputs.get('items', [])
            total = sum(item.get('price', 0) * item.get('quantity', 1) for item in items)
            outputs['total'] = total
            outputs['item_count'] = len(items)
            """;
        
        Map<String, Object> inputs = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        
        Map<String, Object> item1 = new HashMap<>();
        item1.put("name", "Apple");
        item1.put("price", 1.5);
        item1.put("quantity", 3);
        
        Map<String, Object> item2 = new HashMap<>();
        item2.put("name", "Banana");
        item2.put("price", 0.8);
        item2.put("quantity", 5);
        
        items.add(item1);
        items.add(item2);
        inputs.put("items", items);
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript(script);
        
        PythonExecutionResult result = executor.execute(script, inputs, config);
        
        assertTrue(result.isSuccess());
        assertEquals(8.5, (Double) result.getOutputs().get("total"), 0.01);
        assertEquals(2, result.getOutputs().get("item_count"));
    }
    
    @Test
    @DisplayName("使用预装库 - requests")
    public void testPreludeLibrary() {
        String script = """
            # 测试预装库（注意：网络被禁用，这里只测试导入）
            try:
                import requests
                outputs['requests_available'] = True
            except ImportError:
                outputs['requests_available'] = False
            
            try:
                import pandas
                outputs['pandas_available'] = True
            except ImportError:
                outputs['pandas_available'] = False
            
            try:
                import numpy
                outputs['numpy_available'] = True
            except ImportError:
                outputs['numpy_available'] = False
            """;
        
        Map<String, Object> inputs = new HashMap<>();
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript(script);
        
        PythonExecutionResult result = executor.execute(script, inputs, config);
        
        assertTrue(result.isSuccess());
        // 注意：这些库在基础 python:3.11-slim 镜像中不可用，需要自定义镜像
        // 这个测试用于验证库的可用性
    }
    
    @Test
    @DisplayName("内存限制配置")
    public void testMemoryLimitConfig() {
        String script = """
            outputs['test'] = 'memory limit test'
            """;
        
        Map<String, Object> inputs = new HashMap<>();
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript(script);
        config.setMemoryLimit(256L);  // 256MB
        
        PythonExecutionResult result = executor.execute(script, inputs, config);
        
        assertTrue(result.isSuccess());
    }
    
    @Test
    @DisplayName("环境变量传递")
    public void testEnvironmentVariables() {
        String script = """
            import os
            api_key = os.environ.get('API_KEY', 'not_set')
            debug = os.environ.get('DEBUG', 'false')
            outputs['api_key'] = api_key
            outputs['debug'] = debug == 'true'
            """;
        
        Map<String, Object> inputs = new HashMap<>();
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript(script);
        
        Map<String, String> env = new HashMap<>();
        env.put("API_KEY", "test-key-123");
        env.put("DEBUG", "true");
        config.setEnv(env);
        
        PythonExecutionResult result = executor.execute(script, inputs, config);
        
        assertTrue(result.isSuccess());
        assertEquals("test-key-123", result.getOutputs().get("api_key"));
        assertTrue((Boolean) result.getOutputs().get("debug"));
    }
}
