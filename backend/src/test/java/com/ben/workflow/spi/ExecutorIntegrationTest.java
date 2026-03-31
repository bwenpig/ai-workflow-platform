package com.ben.workflow.spi;

import com.ben.workflow.adapter.kling.KlingExecutor;
import com.ben.workflow.adapter.nanobanana.NanoBananaExecutor;
import com.ben.workflow.adapter.seedance.SeedanceExecutor;
import com.ben.workflow.adapter.wan.WanExecutor;
import com.ben.workflow.engine.PythonScriptExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 执行器集成测试
 * 测试 ExecutorRegistry 和所有 ModelExecutor 实现
 */
class ExecutorIntegrationTest {

    private ExecutorRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ExecutorRegistry();
    }

    /**
     * 测试场景 1: ExecutorRegistry 手动注册所有@NodeComponent 执行器
     */
    @Test
    void testManualRegistration() {
        // 手动注册所有执行器
        registry.register(new KlingExecutor());
        registry.register(new WanExecutor());
        registry.register(new SeedanceExecutor());
        registry.register(new NanoBananaExecutor());
        registry.register(new PythonScriptExecutor());
        
        // 验证所有执行器已注册
        Set<String> registeredTypes = registry.getRegisteredTypes();
        assertTrue(registeredTypes.contains("kling"), "应注册 kling 执行器");
        assertTrue(registeredTypes.contains("wan"), "应注册 wan 执行器");
        assertTrue(registeredTypes.contains("seedance"), "应注册 seedance 执行器");
        assertTrue(registeredTypes.contains("nanobanana"), "应注册 nanobanana 执行器");
        assertTrue(registeredTypes.contains("python_script"), "应注册 python_script 执行器");
        
        assertEquals(5, registeredTypes.size(), "应注册 5 个执行器");
    }

    /**
     * 测试场景 2: 通过类型获取执行器
     */
    @Test
    void testGetExecutorByType() {
        registry.register(new KlingExecutor());
        registry.register(new WanExecutor());
        registry.register(new SeedanceExecutor());
        
        ModelExecutor klingExecutor = registry.getExecutor("kling");
        assertNotNull(klingExecutor);
        assertEquals("kling", klingExecutor.getType());
        assertTrue(klingExecutor instanceof KlingExecutor);
        
        ModelExecutor wanExecutor = registry.getExecutor("wan");
        assertNotNull(wanExecutor);
        assertEquals("wan", wanExecutor.getType());
        assertTrue(wanExecutor instanceof WanExecutor);
        
        ModelExecutor seedanceExecutor = registry.getExecutor("seedance");
        assertNotNull(seedanceExecutor);
        assertEquals("seedance", seedanceExecutor.getType());
        assertTrue(seedanceExecutor instanceof SeedanceExecutor);
    }

    /**
     * 测试场景 3: 执行器执行（PythonScriptExecutor）
     */
    @Test
    void testPythonScriptExecutor() {
        PythonScriptExecutor executor = new PythonScriptExecutor();
        
        assertEquals("python_script", executor.getType());
        
        ModelExecutionContext context = new ModelExecutionContext();
        context.setNodeId("test-node");
        context.setInstanceId("test-instance");
        context.setNodeType("python_script");
        
        // 简单脚本测试
        Map<String, Object> config = new HashMap<>();
        config.put("script", "outputs['result'] = inputs.get('value', 0) * 2");
        context.setConfig(config);
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("value", 21);
        context.setInputs(inputs);
        
        ModelExecutionResult result = executor.execute(context);
        
        assertTrue(result.isSuccess(), "Python 脚本应执行成功");
        assertNotNull(result.getData(), "应返回结果数据");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(42, data.get("result"), "计算结果应为 42");
    }

    /**
     * 测试场景 4: 执行器执行（KlingExecutor）
     */
    @Test
    void testKlingExecutor() {
        KlingExecutor executor = new KlingExecutor();
        
        assertEquals("kling", executor.getType());
        
        ModelExecutionContext context = new ModelExecutionContext();
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "A beautiful sunset over the ocean");
        config.put("duration", 5.0);
        config.put("fps", 24);
        context.setConfig(config);
        
        ModelExecutionResult result = executor.execute(context);
        
        assertTrue(result.isSuccess(), "Kling 执行器应执行成功");
        assertNotNull(result.getData(), "应返回视频数据");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("video", data.get("type"));
        assertEquals("kling", data.get("modelProvider"));
        assertEquals("A beautiful sunset over the ocean", data.get("prompt"));
        assertNotNull(data.get("url"));
        assertNotNull(data.get("previewUrl"));
    }

    /**
     * 测试场景 5: 执行器执行（WanExecutor）
     */
    @Test
    void testWanExecutor() {
        WanExecutor executor = new WanExecutor();
        
        assertEquals("wan", executor.getType());
        
        ModelExecutionContext context = new ModelExecutionContext();
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "A cute cat playing with a ball");
        context.setConfig(config);
        
        ModelExecutionResult result = executor.execute(context);
        
        assertTrue(result.isSuccess(), "Wan 执行器应执行成功");
        assertNotNull(result.getData(), "应返回图像数据");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("image", data.get("type"));
        assertEquals("wan", data.get("modelProvider"));
        assertEquals("A cute cat playing with a ball", data.get("prompt"));
        assertNotNull(data.get("url"));
        assertEquals(1024, data.get("width"));
        assertEquals(1024, data.get("height"));
    }

    /**
     * 测试场景 6: 执行器执行（SeedanceExecutor）
     */
    @Test
    void testSeedanceExecutor() {
        SeedanceExecutor executor = new SeedanceExecutor();
        
        assertEquals("seedance", executor.getType());
        
        ModelExecutionContext context = new ModelExecutionContext();
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "A dancing robot in a futuristic city");
        config.put("duration", 10.0);
        config.put("fps", 30);
        context.setConfig(config);
        
        ModelExecutionResult result = executor.execute(context);
        
        assertTrue(result.isSuccess(), "Seedance 执行器应执行成功");
        assertNotNull(result.getData(), "应返回视频数据");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("video", data.get("type"));
        assertEquals("seedance", data.get("modelProvider"));
        assertEquals("A dancing robot in a futuristic city", data.get("prompt"));
        assertNotNull(data.get("url"));
        assertEquals(10.0, data.get("duration"));
        assertEquals(30, data.get("fps"));
    }

    /**
     * 测试场景 7: 执行器执行（NanoBananaExecutor）
     */
    @Test
    void testNanoBananaExecutor() {
        NanoBananaExecutor executor = new NanoBananaExecutor();
        
        assertEquals("nanobanana", executor.getType());
        
        ModelExecutionContext context = new ModelExecutionContext();
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "A colorful abstract painting");
        config.put("width", 512);
        config.put("height", 512);
        context.setConfig(config);
        
        ModelExecutionResult result = executor.execute(context);
        
        assertTrue(result.isSuccess(), "NanoBanana 执行器应执行成功");
        assertNotNull(result.getData(), "应返回图像数据");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("image", data.get("type"));
        assertEquals("nanobanana", data.get("modelProvider"));
        assertEquals("A colorful abstract painting", data.get("prompt"));
        assertNotNull(data.get("url"));
        assertEquals(512, data.get("width"));
        assertEquals(512, data.get("height"));
    }

    /**
     * 测试场景 8: 执行器错误处理
     */
    @Test
    void testExecutorErrorHandling() {
        // 测试 KlingExecutor 错误处理
        KlingExecutor klingExecutor = new KlingExecutor();
        ModelExecutionContext nullContext = null;
        
        // 空上下文不应抛出异常
        ModelExecutionResult result = klingExecutor.execute(nullContext);
        assertTrue(result.isSuccess(), "应处理空上下文情况");
        
        // 测试 WanExecutor 错误处理
        WanExecutor wanExecutor = new WanExecutor();
        ModelExecutionContext emptyContext = new ModelExecutionContext();
        emptyContext.setConfig(null);
        
        result = wanExecutor.execute(emptyContext);
        assertTrue(result.isSuccess(), "应处理空配置情况");
        
        // 测试 PythonScriptExecutor 错误处理
        PythonScriptExecutor pythonExecutor = new PythonScriptExecutor();
        ModelExecutionContext errorContext = new ModelExecutionContext();
        Map<String, Object> errorConfig = new HashMap<>();
        errorConfig.put("script", "raise Exception('Intentional error')");
        errorContext.setConfig(errorConfig);
        
        result = pythonExecutor.execute(errorContext);
        assertFalse(result.isSuccess(), "脚本错误应返回失败结果");
        assertNotNull(result.getError(), "应包含错误信息");
        // 错误信息可能包含异常堆栈或其他信息，只要不为空即可
        assertTrue(result.getError().length() > 0, "错误信息不应为空");
    }

    /**
     * 测试场景 9: 执行器 hasExecutor 检查
     */
    @Test
    void testHasExecutor() {
        registry.register(new KlingExecutor());
        
        assertTrue(registry.hasExecutor("kling"), "应存在 kling 执行器");
        assertFalse(registry.hasExecutor("nonexistent"), "不应存在不存在的执行器");
    }

    /**
     * 测试场景 10: 执行器注册幂等性
     */
    @Test
    void testRegistrationIdempotency() {
        KlingExecutor executor = new KlingExecutor();
        
        registry.register(executor);
        registry.register(executor); // 重复注册
        
        ModelExecutor retrieved = registry.getExecutor("kling");
        assertNotNull(retrieved);
        assertSame(executor, retrieved, "重复注册应返回同一实例");
    }
}
