package com.ben.workflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonSecurityConfig 单元测试
 * 
 * @author 龙傲天
 * @version 1.0
 */
@Execution(ExecutionMode.CONCURRENT)
class PythonSecurityConfigTest {
    
    private PythonSecurityConfig config;
    
    @BeforeEach
    void setUp() {
        config = PythonSecurityConfig.createStrict();
    }
    
    @Test
    @DisplayName("测试危险函数检测 - eval")
    void testDangerousFunction_eval() {
        assertTrue(config.isDangerousFunction("eval"));
    }
    
    @Test
    @DisplayName("测试危险函数检测 - exec")
    void testDangerousFunction_exec() {
        assertTrue(config.isDangerousFunction("exec"));
    }
    
    @Test
    @DisplayName("测试危险函数检测 - __import__")
    void testDangerousFunction_import() {
        assertTrue(config.isDangerousFunction("__import__"));
    }
    
    @Test
    @DisplayName("测试危险函数检测 - open")
    void testDangerousFunction_open() {
        assertTrue(config.isDangerousFunction("open"));
    }
    
    @Test
    @DisplayName("测试危险函数检测 - os.system")
    void testDangerousFunction_osSystem() {
        assertTrue(config.isDangerousFunction("os.system"));
    }
    
    @Test
    @DisplayName("测试危险函数检测 - subprocess")
    void testDangerousFunction_subprocess() {
        assertTrue(config.isDangerousFunction("subprocess.call"));
        assertTrue(config.isDangerousFunction("subprocess.Popen"));
    }
    
    @Test
    @DisplayName("测试危险函数检测 - socket")
    void testDangerousFunction_socket() {
        assertTrue(config.isDangerousFunction("socket.socket"));
        assertTrue(config.isDangerousFunction("socket.create_connection"));
    }
    
    @Test
    @DisplayName("测试危险函数检测 - pickle")
    void testDangerousFunction_pickle() {
        assertTrue(config.isDangerousFunction("pickle.loads"));
        assertTrue(config.isDangerousFunction("pickle.load"));
    }
    
    @Test
    @DisplayName("测试安全函数 - math")
    void testSafeFunction_math() {
        assertFalse(config.isDangerousFunction("math.sqrt"));
        assertFalse(config.isDangerousFunction("math.sin"));
    }
    
    @Test
    @DisplayName("测试安全函数 - json")
    void testSafeFunction_json() {
        assertFalse(config.isDangerousFunction("json.loads"));
        assertFalse(config.isDangerousFunction("json.dumps"));
    }
    
    @Test
    @DisplayName("测试空函数名")
    void testEmptyFunctionName() {
        assertFalse(config.isDangerousFunction(""));
        assertFalse(config.isDangerousFunction(null));
        assertFalse(config.isDangerousFunction("   "));
    }
    
    @Test
    @DisplayName("测试安全模块 - math")
    void testSafeModule_math() {
        assertTrue(config.isSafeModule("math"));
    }
    
    @Test
    @DisplayName("测试安全模块 - random")
    void testSafeModule_random() {
        assertTrue(config.isSafeModule("random"));
    }
    
    @Test
    @DisplayName("测试安全模块 - datetime")
    void testSafeModule_datetime() {
        assertTrue(config.isSafeModule("datetime"));
    }
    
    @Test
    @DisplayName("测试安全模块 - json")
    void testSafeModule_json() {
        assertTrue(config.isSafeModule("json"));
    }
    
    @Test
    @DisplayName("测试安全模块 - re")
    void testSafeModule_re() {
        assertTrue(config.isSafeModule("re"));
    }
    
    @Test
    @DisplayName("测试安全模块 - collections")
    void testSafeModule_collections() {
        assertTrue(config.isSafeModule("collections"));
    }
    
    @Test
    @DisplayName("测试安全模块 - numpy")
    void testSafeModule_numpy() {
        assertTrue(config.isSafeModule("numpy"));
        assertTrue(config.isSafeModule("numpy.random"));
        assertTrue(config.isSafeModule("numpy.ndarray"));
    }
    
    @Test
    @DisplayName("测试安全模块 - pandas")
    void testSafeModule_pandas() {
        assertTrue(config.isSafeModule("pandas"));
        assertTrue(config.isSafeModule("pandas.DataFrame"));
    }
    
    @Test
    @DisplayName("测试安全模块 - PIL")
    void testSafeModule_pil() {
        assertTrue(config.isSafeModule("PIL"));
        assertTrue(config.isSafeModule("PIL.Image"));
    }
    
    @Test
    @DisplayName("测试未授权模块 - os")
    void testUnauthorizedModule_os() {
        assertFalse(config.isSafeModule("os"));
    }
    
    @Test
    @DisplayName("测试未授权模块 - sys")
    void testUnauthorizedModule_sys() {
        assertFalse(config.isSafeModule("sys"));
    }
    
    @Test
    @DisplayName("测试未授权模块 - subprocess")
    void testUnauthorizedModule_subprocess() {
        assertFalse(config.isSafeModule("subprocess"));
    }
    
    @Test
    @DisplayName("测试未授权模块 - socket")
    void testUnauthorizedModule_socket() {
        assertFalse(config.isSafeModule("socket"));
    }
    
    @Test
    @DisplayName("测试空模块名")
    void testEmptyModuleName() {
        assertFalse(config.isSafeModule(""));
        assertFalse(config.isSafeModule(null));
        assertFalse(config.isSafeModule("   "));
    }
    
    @Test
    @DisplayName("测试网络模块检测 - requests")
    void testNetworkModule_requests() {
        assertTrue(config.requiresNetworkPermission("requests"));
    }
    
    @Test
    @DisplayName("测试网络模块检测 - urllib")
    void testNetworkModule_urllib() {
        assertTrue(config.requiresNetworkPermission("urllib"));
    }
    
    @Test
    @DisplayName("测试网络模块检测 - socket")
    void testNetworkModule_socket() {
        assertTrue(config.requiresNetworkPermission("socket"));
    }
    
    @Test
    @DisplayName("测试非网络模块")
    void testNonNetworkModule() {
        assertFalse(config.requiresNetworkPermission("math"));
        assertFalse(config.requiresNetworkPermission("json"));
        assertFalse(config.requiresNetworkPermission("numpy"));
    }
    
    @Test
    @DisplayName("测试默认配置 - 网络禁用")
    void testDefaultConfig_networkDisabled() {
        assertFalse(config.isNetworkEnabled());
    }
    
    @Test
    @DisplayName("测试默认配置 - 文件读取禁用")
    void testDefaultConfig_fileReadDisabled() {
        assertFalse(config.isFileReadEnabled());
    }
    
    @Test
    @DisplayName("测试默认配置 - 文件写入禁用")
    void testDefaultConfig_fileWriteDisabled() {
        assertFalse(config.isFileWriteEnabled());
    }
    
    @Test
    @DisplayName("测试默认配置 - 执行时间 30 秒")
    void testDefaultConfig_executionTime() {
        assertEquals(30, config.getMaxExecutionTimeSeconds());
    }
    
    @Test
    @DisplayName("测试默认配置 - 内存 128MB")
    void testDefaultConfig_memory() {
        assertEquals(128, config.getMaxMemoryMB());
    }
    
    @Test
    @DisplayName("测试配置修改 - 启用网络")
    void testConfigModification_enableNetwork() {
        config.setNetworkEnabled(true);
        assertTrue(config.isNetworkEnabled());
    }
    
    @Test
    @DisplayName("测试配置修改 - 链式调用")
    void testConfigModification_chain() {
        PythonSecurityConfig modified = config
                .setNetworkEnabled(true)
                .setFileReadEnabled(true)
                .setMaxExecutionTimeSeconds(60);
        
        assertTrue(modified.isNetworkEnabled());
        assertTrue(modified.isFileReadEnabled());
        assertEquals(60, modified.getMaxExecutionTimeSeconds());
    }
    
    @Test
    @DisplayName("测试严格配置")
    void testStrictConfig() {
        PythonSecurityConfig strict = PythonSecurityConfig.createStrict();
        
        assertFalse(strict.isNetworkEnabled());
        assertFalse(strict.isFileReadEnabled());
        assertFalse(strict.isFileWriteEnabled());
        assertEquals(30, strict.getMaxExecutionTimeSeconds());
        assertEquals(128, strict.getMaxMemoryMB());
    }
    
    @Test
    @DisplayName("测试宽松配置")
    void testLenientConfig() {
        PythonSecurityConfig lenient = PythonSecurityConfig.createLenient();
        
        assertTrue(lenient.isNetworkEnabled());
        assertTrue(lenient.isFileReadEnabled());
        assertFalse(lenient.isFileWriteEnabled());
        assertEquals(60, lenient.getMaxExecutionTimeSeconds());
        assertEquals(256, lenient.getMaxMemoryMB());
    }
    
    @Test
    @DisplayName("测试配置验证 - 有效配置")
    void testConfigValidation_valid() {
        assertDoesNotThrow(() -> config.validate());
    }
    
    @Test
    @DisplayName("测试配置验证 - 无效执行时间")
    void testConfigValidation_invalidExecutionTime() {
        config.setMaxExecutionTimeSeconds(0);
        assertThrows(IllegalStateException.class, () -> config.validate());
    }
    
    @Test
    @DisplayName("测试配置验证 - 无效内存")
    void testConfigValidation_invalidMemory() {
        config.setMaxMemoryMB(0);
        assertThrows(IllegalStateException.class, () -> config.validate());
    }
    
    @Test
    @DisplayName("测试配置验证 - 文件写入无读取")
    void testConfigValidation_writeWithoutRead() {
        config.setFileWriteEnabled(true);
        assertThrows(IllegalStateException.class, () -> config.validate());
    }
    
    @Test
    @DisplayName("测试获取危险函数列表")
    void testGetDangerousFunctions() {
        Set<String> dangerous = config.getDangerousFunctions();
        
        assertNotNull(dangerous);
        assertFalse(dangerous.isEmpty());
        assertTrue(dangerous.contains("eval"));
        assertTrue(dangerous.contains("exec"));
        assertTrue(dangerous.contains("open"));
    }
    
    @Test
    @DisplayName("测试获取安全模块列表")
    void testGetSafeModules() {
        Set<String> safe = config.getSafeModules();
        
        assertNotNull(safe);
        assertFalse(safe.isEmpty());
        assertTrue(safe.contains("math"));
        assertTrue(safe.contains("json"));
        assertTrue(safe.contains("numpy"));
    }
    
    @Test
    @DisplayName("测试获取网络模块列表")
    void testGetNetworkModules() {
        Set<String> network = config.getNetworkModules();
        
        assertNotNull(network);
        assertFalse(network.isEmpty());
        assertTrue(network.contains("requests"));
        assertTrue(network.contains("socket"));
    }
}
