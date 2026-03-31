package com.ben.workflow.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ModelExecutor SPI 集成测试
 */
@SpringBootTest
public class ModelExecutorTest {

    @Autowired
    private ExecutorRegistry executorRegistry;

    @Test
    @DisplayName("测试 ExecutorRegistry 初始化")
    public void testRegistryInitialization() {
        assertNotNull(executorRegistry);
        Set<String> types = executorRegistry.getRegisteredTypes();
        assertNotNull(types);
        System.out.println("已注册执行器类型：" + types);
    }

    @Test
    @DisplayName("测试 KlingExecutor 注册")
    public void testKlingExecutorRegistered() {
        ModelExecutor executor = executorRegistry.getExecutor("kling");
        assertNotNull(executor);
        assertEquals("kling", executor.getType());
    }

    @Test
    @DisplayName("测试 WanExecutor 注册")
    public void testWanExecutorRegistered() {
        ModelExecutor executor = executorRegistry.getExecutor("wan");
        assertNotNull(executor);
        assertEquals("wan", executor.getType());
    }

    @Test
    @DisplayName("测试 SeedanceExecutor 注册")
    public void testSeedanceExecutorRegistered() {
        ModelExecutor executor = executorRegistry.getExecutor("seedance");
        assertNotNull(executor);
        assertEquals("seedance", executor.getType());
    }

    @Test
    @DisplayName("测试 NanoBananaExecutor 注册")
    public void testNanoBananaExecutorRegistered() {
        ModelExecutor executor = executorRegistry.getExecutor("nanobanana");
        assertNotNull(executor);
        assertEquals("nanobanana", executor.getType());
    }

    @Test
    @DisplayName("测试 PythonScriptExecutor 注册")
    public void testPythonScriptExecutorRegistered() {
        ModelExecutor executor = executorRegistry.getExecutor("python_script");
        assertNotNull(executor);
        assertEquals("python_script", executor.getType());
    }

    @Test
    @DisplayName("测试 hasExecutor")
    public void testHasExecutor() {
        assertTrue(executorRegistry.hasExecutor("kling"));
        assertTrue(executorRegistry.hasExecutor("wan"));
        assertFalse(executorRegistry.hasExecutor("unknown"));
    }
}
