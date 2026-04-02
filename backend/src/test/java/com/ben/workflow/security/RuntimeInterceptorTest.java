package com.ben.workflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuntimeInterceptor 单元测试
 * 
 * @author 龙傲天
 * @version 1.0
 */
@Execution(ExecutionMode.CONCURRENT)
class RuntimeInterceptorTest {
    
    private RuntimeInterceptor interceptor;
    private RuntimeInterceptor interceptorWithNetwork;
    
    @BeforeEach
    void setUp() {
        interceptor = new RuntimeInterceptor(PythonSecurityConfig.createStrict());
        interceptorWithNetwork = new RuntimeInterceptor(
            PythonSecurityConfig.createStrict().setNetworkEnabled(true)
        );
    }
    
    @Test
    @DisplayName("测试空代码包装")
    void testEmptyCode() {
        String wrapped = interceptor.wrapUserCode("");
        assertNotNull(wrapped);
        
        wrapped = interceptor.wrapUserCode(null);
        assertNull(wrapped);
        
        wrapped = interceptor.wrapUserCode("   ");
        assertNotNull(wrapped);
    }
    
    @Test
    @DisplayName("测试安全代码包装")
    void testSafeCodeWrap() {
        String safeCode = """
            import math
            result = math.sqrt(16)
            print(result)
            """;
        
        String wrapped = interceptor.wrapUserCode(safeCode);
        
        assertNotNull(wrapped);
        assertTrue(wrapped.length() > safeCode.length());
        assertTrue(wrapped.contains("SecurityInterceptor"));
        assertTrue(wrapped.contains(safeCode));
    }
    
    @Test
    @DisplayName("测试危险代码包装 - eval")
    void testDangerousCode_eval() {
        String dangerousCode = "eval('1+1')";
        
        assertThrows(SecurityViolationException.class, () -> {
            interceptor.wrapUserCode(dangerousCode);
        });
    }
    
    @Test
    @DisplayName("测试危险代码包装 - exec")
    void testDangerousCode_exec() {
        String dangerousCode = "exec('print(1)')";
        
        assertThrows(SecurityViolationException.class, () -> {
            interceptor.wrapUserCode(dangerousCode);
        });
    }
    
    @Test
    @DisplayName("测试危险代码包装 - os.system")
    void testDangerousCode_osSystem() {
        String dangerousCode = """
            import os
            os.system('ls')
            """;
        
        assertThrows(SecurityViolationException.class, () -> {
            interceptor.wrapUserCode(dangerousCode);
        });
    }
    
    @Test
    @DisplayName("测试危险代码包装 - open")
    void testDangerousCode_open() {
        String dangerousCode = """
            with open('/etc/passwd', 'r') as f:
                content = f.read()
            """;
        
        assertThrows(SecurityViolationException.class, () -> {
            interceptor.wrapUserCode(dangerousCode);
        });
    }
    
    @Test
    @DisplayName("测试危险代码包装 - subprocess")
    void testDangerousCode_subprocess() {
        String dangerousCode = """
            import subprocess
            subprocess.run(['ls'])
            """;
        
        assertThrows(SecurityViolationException.class, () -> {
            interceptor.wrapUserCode(dangerousCode);
        });
    }
    
    @Test
    @DisplayName("测试危险代码包装 - pickle")
    void testDangerousCode_pickle() {
        String dangerousCode = """
            import pickle
            data = pickle.loads(serialized)
            """;
        
        assertThrows(SecurityViolationException.class, () -> {
            interceptor.wrapUserCode(dangerousCode);
        });
    }
    
    @Test
    @DisplayName("测试未授权模块包装")
    void testUnauthorizedModule() {
        String code = """
            import os
            print(os.getcwd())
            """;
        
        assertThrows(SecurityViolationException.class, () -> {
            interceptor.wrapUserCode(code);
        });
    }
    
    @Test
    @DisplayName("测试网络模块 - 禁用网络")
    void testNetworkModule_disabled() {
        String code = """
            import requests
            requests.get('https://example.com')
            """;
        
        assertThrows(SecurityViolationException.class, () -> {
            interceptor.wrapUserCode(code);
        });
    }
    
    @Test
    @DisplayName("测试网络模块 - 启用网络")
    void testNetworkModule_enabled() {
        String code = """
            import requests
            response = requests.get('https://example.com')
            """;
        
        assertDoesNotThrow(() -> {
            String wrapped = interceptorWithNetwork.wrapUserCode(code);
            assertNotNull(wrapped);
        });
    }
    
    @Test
    @DisplayName("测试 validateAndWrap 方法")
    void testValidateAndWrap() {
        String safeCode = "import math\nresult = math.sqrt(16)";
        
        String wrapped = assertDoesNotThrow(() -> {
            return interceptor.validateAndWrap(safeCode);
        });
        
        assertNotNull(wrapped);
        
        String dangerousCode = "eval('1+1')";
        
        assertThrows(SecurityViolationException.class, () -> {
            interceptor.validateAndWrap(dangerousCode);
        });
    }
    
    @Test
    @DisplayName("测试获取配置")
    void testGetConfig() {
        assertNotNull(interceptor.getConfig());
    }
    
    @Test
    @DisplayName("测试获取分析器")
    void testGetAnalyzer() {
        assertNotNull(interceptor.getAnalyzer());
    }
    
    @Test
    @DisplayName("测试 SecurityViolationException - 基本构造")
    void testSecurityViolationException_basic() {
        SecurityViolationException ex = new SecurityViolationException("Test error");
        
        assertEquals("Test error", ex.getMessage());
        assertTrue(ex.getViolations().isEmpty());
        assertFalse(ex.hasViolations());
    }
    
    @Test
    @DisplayName("测试 SecurityViolationException - 带违规列表")
    void testSecurityViolationException_withViolations() {
        java.util.List<String> violations = java.util.Arrays.asList(
            "Violation 1",
            "Violation 2"
        );
        
        SecurityViolationException ex = new SecurityViolationException("Multiple violations", violations);
        
        assertEquals("Multiple violations", ex.getMessage());
        assertEquals(2, ex.getViolations().size());
        assertTrue(ex.hasViolations());
        assertEquals("Violation 1", ex.getViolations().get(0));
    }
    
    @Test
    @DisplayName("测试 SecurityViolationException - null 违规列表")
    void testSecurityViolationException_nullViolations() {
        SecurityViolationException ex = new SecurityViolationException("Error", null);
        
        assertNotNull(ex.getViolations());
        assertTrue(ex.getViolations().isEmpty());
        assertFalse(ex.hasViolations());
    }
    
    @Test
    @DisplayName("测试包装后代码包含拦截器")
    void testWrappedCodeContainsInterceptor() {
        String safeCode = "import json\ndata = json.dumps({'key': 'value'})";
        
        String wrapped = interceptor.wrapUserCode(safeCode);
        
        assertTrue(wrapped.contains("SecurityInterceptor"));
        assertTrue(wrapped.contains("dangerous_funcs"));
        assertTrue(wrapped.contains("safe_import"));
        assertTrue(wrapped.contains("blocked_eval"));
        assertTrue(wrapped.contains("blocked_exec"));
    }
    
    @Test
    @DisplayName("测试包装后代码保留原始代码")
    void testWrappedCodePreservesOriginal() {
        String originalCode = """
            import math
            import json
            
            def calculate():
                result = math.sqrt(16)
                return json.dumps({'result': result})
            
            output = calculate()
            """;
        
        String wrapped = interceptor.wrapUserCode(originalCode);
        
        assertTrue(wrapped.contains(originalCode));
    }
    
    @Test
    @DisplayName("测试复杂安全代码")
    void testComplexSafeCode() {
        String code = """
            import numpy as np
            import pandas as pd
            from datetime import datetime
            import json
            import re
            
            # 数据处理
            data = {'values': [1, 2, 3, 4, 5]}
            arr = np.array(data['values'])
            mean = np.mean(arr)
            
            # JSON 序列化
            result = json.dumps({'mean': mean})
            
            # 正则匹配
            pattern = r'\\d+'
            matches = re.findall(pattern, 'abc123def456')
            """;
        
        assertDoesNotThrow(() -> {
            String wrapped = interceptor.wrapUserCode(code);
            assertNotNull(wrapped);
        });
    }
    
    @Test
    @DisplayName("测试多个危险函数检测")
    void testMultipleDangerousFunctions() {
        String code = """
            eval('1+1')
            exec('print(2)')
            __import__('os')
            """;
        
        SecurityViolationException ex = assertThrows(SecurityViolationException.class, () -> {
            interceptor.wrapUserCode(code);
        });
        
        assertTrue(ex.getMessage().contains("安全检查失败"));
    }
    
    @Test
    @DisplayName("测试注入资源限制方法存在")
    void testInjectResourceLimitsExists() {
        String code = "import math";
        
        String limited = interceptor.injectResourceLimits(code, 30, 128);
        
        assertNotNull(limited);
        assertTrue(limited.contains("signal"));
        assertTrue(limited.contains("resource"));
        assertTrue(limited.contains("SIGALRM"));
        assertTrue(limited.contains("RLIMIT_AS"));
    }
    
    @Test
    @DisplayName("测试注入资源限制 - 空代码")
    void testInjectResourceLimits_emptyCode() {
        String limited = interceptor.injectResourceLimits("", 30, 128);
        assertEquals("", limited);
        
        limited = interceptor.injectResourceLimits(null, 30, 128);
        assertNull(limited);
    }
}
