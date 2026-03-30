package com.ben.workflow.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Python 节点配置测试
 */
public class PythonNodeConfigTest {
    
    @Test
    @DisplayName("创建默认配置")
    public void testDefaultConfig() {
        PythonNodeConfig config = new PythonNodeConfig();
        
        assertEquals(30, config.getTimeout());
        assertEquals("3.9", config.getPythonVersion());
        assertFalse(config.getNetworkEnabled());
        assertNull(config.getScript());
        assertNull(config.getScriptPath());
        assertNull(config.getRequirements());
        assertNull(config.getEnv());
    }
    
    @Test
    @DisplayName("设置脚本内容")
    public void testSetScript() {
        PythonNodeConfig config = new PythonNodeConfig();
        String script = "print('Hello World')";
        
        config.setScript(script);
        
        assertEquals(script, config.getScript());
    }
    
    @Test
    @DisplayName("设置脚本路径")
    public void testSetScriptPath() {
        PythonNodeConfig config = new PythonNodeConfig();
        String path = "/path/to/script.py";
        
        config.setScriptPath(path);
        
        assertEquals(path, config.getScriptPath());
    }
    
    @Test
    @DisplayName("设置超时时间")
    public void testSetTimeout() {
        PythonNodeConfig config = new PythonNodeConfig();
        
        config.setTimeout(60);
        
        assertEquals(60, config.getTimeout());
    }
    
    @Test
    @DisplayName("设置依赖包列表")
    public void testSetRequirements() {
        PythonNodeConfig config = new PythonNodeConfig();
        List<String> requirements = Arrays.asList("requests", "numpy", "pandas");
        
        config.setRequirements(requirements);
        
        assertEquals(3, config.getRequirements().size());
        assertTrue(config.getRequirements().contains("requests"));
        assertTrue(config.getRequirements().contains("numpy"));
        assertTrue(config.getRequirements().contains("pandas"));
    }
    
    @Test
    @DisplayName("设置 Python 版本")
    public void testSetPythonVersion() {
        PythonNodeConfig config = new PythonNodeConfig();
        
        config.setPythonVersion("3.11");
        
        assertEquals("3.11", config.getPythonVersion());
    }
    
    @Test
    @DisplayName("设置环境变量")
    public void testSetEnv() {
        PythonNodeConfig config = new PythonNodeConfig();
        Map<String, String> env = new HashMap<>();
        env.put("API_KEY", "test-key");
        env.put("DEBUG", "true");
        
        config.setEnv(env);
        
        assertEquals(2, config.getEnv().size());
        assertEquals("test-key", config.getEnv().get("API_KEY"));
        assertEquals("true", config.getEnv().get("DEBUG"));
    }
    
    @Test
    @DisplayName("设置网络访问权限")
    public void testSetNetworkEnabled() {
        PythonNodeConfig config = new PythonNodeConfig();
        
        config.setNetworkEnabled(true);
        
        assertTrue(config.getNetworkEnabled());
        
        config.setNetworkEnabled(false);
        
        assertFalse(config.getNetworkEnabled());
    }
    
    @Test
    @DisplayName("完整配置测试")
    public void testFullConfig() {
        PythonNodeConfig config = new PythonNodeConfig();
        
        config.setScript("print('test')");
        config.setScriptPath("/tmp/test.py");
        config.setTimeout(120);
        config.setRequirements(Arrays.asList("requests", "flask"));
        config.setPythonVersion("3.10");
        config.setEnv(Map.of("KEY", "value"));
        config.setNetworkEnabled(true);
        
        assertEquals("print('test')", config.getScript());
        assertEquals("/tmp/test.py", config.getScriptPath());
        assertEquals(120, config.getTimeout());
        assertEquals(2, config.getRequirements().size());
        assertEquals("3.10", config.getPythonVersion());
        assertEquals("value", config.getEnv().get("KEY"));
        assertTrue(config.getNetworkEnabled());
    }
}
