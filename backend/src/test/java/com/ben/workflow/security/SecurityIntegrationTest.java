package com.ben.workflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全集成测试
 * 
 * 测试 Python 安全分析器的集成能力：
 * - 危险函数调用应被检测并抛出 SecurityViolationException
 * - 禁止的模块导入应被拦截
 * - 允许的模块应正常通过检查
 * 
 * 注意：本测试专注于安全分析层（PythonSecurityAnalyzer + RuntimeInterceptor），
 * 不依赖 Docker 环境。PythonDockerExecutor 的完整集成测试需要 Docker 运行。
 * 
 * @author 龙傲天
 * @version 1.0
 */
@Execution(ExecutionMode.CONCURRENT)
class SecurityIntegrationTest {
    
    private PythonSecurityAnalyzer analyzer;
    private PythonSecurityConfig config;
    private RuntimeInterceptor interceptor;
    
    @BeforeEach
    void setUp() {
        config = PythonSecurityConfig.createStrict();
        analyzer = new PythonSecurityAnalyzer(config);
        interceptor = new RuntimeInterceptor(config);
    }
    
    // ===== 测试场景 1: 危险函数调用应抛出 SecurityViolationException =====
    
    @Test
    @DisplayName("集成测试 - eval 危险函数应抛出 SecurityViolationException")
    void testDangerousFunction_eval() {
        String dangerousCode = """
            user_input = "1 + 2"
            result = eval(user_input)
            outputs['result'] = result
            """;
        
        // 安全分析层应该检测到危险
        SecurityAnalysisResult analysisResult = analyzer.analyze(dangerousCode);
        assertFalse(analysisResult.isSafe(), "eval 应该被检测为危险");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("eval")), "应该包含 eval 违规信息");
        
        // 运行时拦截器包装时应该抛出 SecurityViolationException
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(dangerousCode),
            "包装危险代码应该抛出 SecurityViolationException"
        );
        assertTrue(exception.getMessage().contains("eval"), "异常信息应该包含 eval");
    }
    
    @Test
    @DisplayName("集成测试 - exec 危险函数应抛出 SecurityViolationException")
    void testDangerousFunction_exec() {
        String dangerousCode = """
            code = "print('hello')"
            exec(code)
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(dangerousCode);
        assertFalse(analysisResult.isSafe(), "exec 应该被检测为危险");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("exec")), "应该包含 exec 违规信息");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(dangerousCode)
        );
        assertTrue(exception.getMessage().contains("exec"));
    }
    
    @Test
    @DisplayName("集成测试 - os.system 危险函数应抛出 SecurityViolationException")
    void testDangerousFunction_osSystem() {
        String dangerousCode = """
            import os
            os.system("ls -la")
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(dangerousCode);
        assertFalse(analysisResult.isSafe(), "os.system 应该被检测为危险");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("os.system")), "应该包含 os.system 违规信息");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(dangerousCode)
        );
        assertTrue(exception.getMessage().contains("os.system"));
    }
    
    @Test
    @DisplayName("集成测试 - subprocess 危险函数应抛出 SecurityViolationException")
    void testDangerousFunction_subprocess() {
        String dangerousCode = """
            import subprocess
            result = subprocess.run(["ls", "-la"], capture_output=True)
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(dangerousCode);
        assertFalse(analysisResult.isSafe(), "subprocess 应该被检测为危险");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(dangerousCode)
        );
        assertTrue(exception.getMessage().contains("subprocess"));
    }
    
    @Test
    @DisplayName("集成测试 - open 文件操作应抛出 SecurityViolationException")
    void testDangerousFunction_open() {
        String dangerousCode = """
            with open("/etc/passwd", "r") as f:
                content = f.read()
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(dangerousCode);
        assertFalse(analysisResult.isSafe(), "open 应该被检测为危险");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("open")), "应该包含 open 违规信息");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(dangerousCode)
        );
        assertTrue(exception.getMessage().contains("open"));
    }
    
    @Test
    @DisplayName("集成测试 - __import__ 动态导入应抛出 SecurityViolationException")
    void testDangerousFunction_dynamicImport() {
        String dangerousCode = """
            module_name = "os"
            os = __import__(module_name)
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(dangerousCode);
        assertFalse(analysisResult.isSafe(), "__import__ 应该被检测为危险");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("__import__")), "应该包含 __import__ 违规信息");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(dangerousCode)
        );
        assertTrue(exception.getMessage().contains("__import__"));
    }
    
    // ===== 测试场景 2: 禁止的模块导入应被拦截 =====
    
    @Test
    @DisplayName("集成测试 - os 模块导入应抛出 SecurityViolationException")
    void testBlockedModule_os() {
        String codeWithBlockedModule = """
            import os
            print(os.getcwd())
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(codeWithBlockedModule);
        assertFalse(analysisResult.isSafe(), "os 模块应该被拦截");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("os")), "应该包含 os 违规信息");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(codeWithBlockedModule)
        );
        assertTrue(exception.getMessage().contains("os"));
    }
    
    @Test
    @DisplayName("集成测试 - sys 模块导入应抛出 SecurityViolationException")
    void testBlockedModule_sys() {
        String codeWithBlockedModule = """
            import sys
            print(sys.path)
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(codeWithBlockedModule);
        assertFalse(analysisResult.isSafe(), "sys 模块应该被拦截");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("sys")), "应该包含 sys 违规信息");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(codeWithBlockedModule)
        );
        assertTrue(exception.getMessage().contains("sys"));
    }
    
    @Test
    @DisplayName("集成测试 - socket 模块导入应抛出 SecurityViolationException")
    void testBlockedModule_socket() {
        String codeWithBlockedModule = """
            import socket
            s = socket.socket()
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(codeWithBlockedModule);
        assertFalse(analysisResult.isSafe(), "socket 模块应该被拦截");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("socket")), "应该包含 socket 违规信息");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(codeWithBlockedModule)
        );
        assertTrue(exception.getMessage().contains("socket"));
    }
    
    @Test
    @DisplayName("集成测试 - pickle 模块导入应抛出 SecurityViolationException")
    void testBlockedModule_pickle() {
        String codeWithBlockedModule = """
            import pickle
            data = pickle.loads(serialized_data)
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(codeWithBlockedModule);
        assertFalse(analysisResult.isSafe(), "pickle 模块应该被拦截");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("pickle")), "应该包含 pickle 违规信息");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(codeWithBlockedModule)
        );
        assertTrue(exception.getMessage().contains("pickle"));
    }
    
    @Test
    @DisplayName("集成测试 - requests 模块在无网络权限时应抛出 SecurityViolationException")
    void testBlockedModule_requests_noNetwork() {
        String codeWithBlockedModule = """
            import requests
            response = requests.get("https://example.com")
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(codeWithBlockedModule);
        assertFalse(analysisResult.isSafe(), "requests 模块在无网络权限时应该被拦截");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("网络权限")), "应该包含网络权限违规信息");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(codeWithBlockedModule)
        );
        assertTrue(exception.getMessage().contains("网络权限") || exception.getMessage().contains("requests"));
    }
    
    // ===== 测试场景 3: 允许的模块应正常通过安全检查 =====
    
    @Test
    @DisplayName("集成测试 - math 模块应正常通过安全检查")
    void testAllowedModule_math() {
        String safeCode = """
            import math
            result = math.sqrt(16) + math.sin(0.5)
            outputs['result'] = result
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(safeCode);
        assertTrue(analysisResult.isSafe(), "math 模块应该安全：" + analysisResult.getViolations());
        
        // 运行时拦截器应该能成功包装（不抛出异常）
        String wrappedCode = assertDoesNotThrow(
            () -> interceptor.wrapUserCode(safeCode),
            "安全代码应该能成功包装"
        );
        assertNotNull(wrappedCode);
        assertTrue(wrappedCode.contains("import math"));
    }
    
    @Test
    @DisplayName("集成测试 - json 模块应正常通过安全检查")
    void testAllowedModule_json() {
        String safeCode = """
            import json
            data = {"name": "test", "value": 42}
            json_str = json.dumps(data)
            parsed = json.loads(json_str)
            outputs['parsed'] = parsed
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(safeCode);
        assertTrue(analysisResult.isSafe(), "json 模块应该安全：" + analysisResult.getViolations());
        
        String wrappedCode = assertDoesNotThrow(
            () -> interceptor.wrapUserCode(safeCode)
        );
        assertNotNull(wrappedCode);
    }
    
    @Test
    @DisplayName("集成测试 - datetime 模块应正常通过安全检查")
    void testAllowedModule_datetime() {
        String safeCode = """
            from datetime import datetime, timedelta
            now = datetime.now()
            tomorrow = now + timedelta(days=1)
            outputs['today'] = now.strftime('%Y-%m-%d')
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(safeCode);
        assertTrue(analysisResult.isSafe(), "datetime 模块应该安全：" + analysisResult.getViolations());
        
        String wrappedCode = assertDoesNotThrow(
            () -> interceptor.wrapUserCode(safeCode)
        );
        assertNotNull(wrappedCode);
    }
    
    @Test
    @DisplayName("集成测试 - numpy 模块应正常通过安全检查")
    void testAllowedModule_numpy() {
        String safeCode = """
            import numpy as np
            arr = np.array([1, 2, 3, 4, 5])
            mean = np.mean(arr)
            outputs['mean'] = float(mean)
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(safeCode);
        assertTrue(analysisResult.isSafe(), "numpy 模块应该安全：" + analysisResult.getViolations());
        
        String wrappedCode = assertDoesNotThrow(
            () -> interceptor.wrapUserCode(safeCode)
        );
        assertNotNull(wrappedCode);
    }
    
    @Test
    @DisplayName("集成测试 - pandas 模块应正常通过安全检查")
    void testAllowedModule_pandas() {
        String safeCode = """
            import pandas as pd
            df = pd.DataFrame({'a': [1, 2, 3], 'b': [4, 5, 6]})
            result = df.sum()
            outputs['sum_a'] = int(result['a'])
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(safeCode);
        assertTrue(analysisResult.isSafe(), "pandas 模块应该安全：" + analysisResult.getViolations());
        
        String wrappedCode = assertDoesNotThrow(
            () -> interceptor.wrapUserCode(safeCode)
        );
        assertNotNull(wrappedCode);
    }
    
    @Test
    @DisplayName("集成测试 - re 正则表达式模块应正常通过安全检查")
    void testAllowedModule_re() {
        String safeCode = """
            import re
            pattern = r"\\d+"
            text = "abc123def456"
            matches = re.findall(pattern, text)
            outputs['matches'] = matches
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(safeCode);
        assertTrue(analysisResult.isSafe(), "re 模块应该安全：" + analysisResult.getViolations());
        
        String wrappedCode = assertDoesNotThrow(
            () -> interceptor.wrapUserCode(safeCode)
        );
        assertNotNull(wrappedCode);
    }
    
    // ===== 测试场景 4: 复杂绕过手法应被检测 =====
    
    @Test
    @DisplayName("集成测试 - getattr 绕过应抛出 SecurityViolationException")
    void testBypassAttempt_getattr() {
        String bypassCode = """
            builtins = getattr(obj, '__builtins__')
            eval = getattr(builtins, 'eval')
            eval('1+1')
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(bypassCode);
        assertFalse(analysisResult.isSafe(), "getattr 绕过应该被检测");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("getattr")), "应该包含 getattr 违规信息");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(bypassCode)
        );
        assertTrue(exception.getMessage().contains("getattr"));
    }
    
    @Test
    @DisplayName("集成测试 - importlib 绕过应抛出 SecurityViolationException")
    void testBypassAttempt_importlib() {
        String bypassCode = """
            import importlib
            os = importlib.import_module('os')
            os.system('ls')
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(bypassCode);
        assertFalse(analysisResult.isSafe(), "importlib 绕过应该被检测");
        assertTrue(analysisResult.getViolations().stream()
                .anyMatch(v -> v.contains("importlib")), "应该包含 importlib 违规信息");
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(bypassCode)
        );
        assertTrue(exception.getMessage().contains("importlib"));
    }
    
    @Test
    @DisplayName("集成测试 - 多个危险操作应全部被检测")
    void testMultipleViolations() {
        String multipleDangerousCode = """
            import os
            import subprocess
            eval("1+1")
            exec("print(2)")
            os.system("ls")
            """;
        
        SecurityAnalysisResult analysisResult = analyzer.analyze(multipleDangerousCode);
        assertFalse(analysisResult.isSafe(), "多个危险操作应该被检测");
        assertTrue(analysisResult.getViolations().size() >= 3, 
                   "应该检测到至少 3 个违规，实际：" + analysisResult.getViolations().size());
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(multipleDangerousCode)
        );
        assertTrue(exception.getViolationCount() >= 3);
    }
    
    // ===== 测试场景 5: SecurityViolationException 特性验证 =====
    
    @Test
    @DisplayName("集成测试 - SecurityViolationException 包含违规列表")
    void testSecurityViolationException_details() {
        String dangerousCode = """
            import os
            os.system("ls")
            """;
        
        SecurityViolationException exception = assertThrows(
            SecurityViolationException.class,
            () -> interceptor.wrapUserCode(dangerousCode)
        );
        
        // 验证异常包含违规详情
        assertTrue(exception.hasViolations());
        assertTrue(exception.getViolationCount() >= 1);
        assertFalse(exception.getViolations().isEmpty());
        
        // 验证 toString 包含关键信息
        String exceptionStr = exception.toString();
        assertNotNull(exceptionStr);
        assertTrue(exceptionStr.contains("SecurityViolationException"));
    }
    
    @Test
    @DisplayName("集成测试 - 安全代码包装后包含拦截器")
    void testWrappedCode_containsInterceptor() {
        String safeCode = "import math\nresult = math.sqrt(16)";
        
        String wrappedCode = interceptor.wrapUserCode(safeCode);
        
        // 验证包装后的代码包含安全拦截器
        assertTrue(wrappedCode.contains("__SecurityInterceptor__"));
        assertTrue(wrappedCode.contains("_safe_import"));
        assertTrue(wrappedCode.contains("_blocked_eval"));
        assertTrue(wrappedCode.contains("import math"));
    }
    
    @Test
    @DisplayName("集成测试 - 空代码应正常处理")
    void testEmptyCode() {
        String emptyCode = "";
        
        SecurityAnalysisResult result = analyzer.analyze(emptyCode);
        assertTrue(result.isSafe());
        
        String wrappedCode = interceptor.wrapUserCode(emptyCode);
        assertEquals(emptyCode, wrappedCode);
    }
    
    @Test
    @DisplayName("集成测试 - null 代码应正常处理")
    void testNullCode() {
        SecurityAnalysisResult result = analyzer.analyze(null);
        assertTrue(result.isSafe());
        
        String wrappedCode = interceptor.wrapUserCode(null);
        assertNull(wrappedCode);
    }
}
