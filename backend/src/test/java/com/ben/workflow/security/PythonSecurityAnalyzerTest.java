package com.ben.workflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonSecurityAnalyzer 单元测试
 * 
 * @author 龙傲天
 * @version 1.0
 */
@Execution(ExecutionMode.CONCURRENT)
class PythonSecurityAnalyzerTest {
    
    private PythonSecurityAnalyzer analyzer;
    private PythonSecurityAnalyzer analyzerWithNetwork;
    
    @BeforeEach
    void setUp() {
        analyzer = new PythonSecurityAnalyzer(PythonSecurityConfig.createStrict());
        analyzerWithNetwork = new PythonSecurityAnalyzer(
            PythonSecurityConfig.createStrict().setNetworkEnabled(true)
        );
    }
    
    @Test
    @DisplayName("测试空代码")
    void testEmptyCode() {
        SecurityAnalysisResult result = analyzer.analyze("");
        assertTrue(result.isSafe());
        
        result = analyzer.analyze(null);
        assertTrue(result.isSafe());
        
        result = analyzer.analyze("   ");
        assertTrue(result.isSafe());
    }
    
    @Test
    @DisplayName("测试安全代码 - 简单计算")
    void testSafeCode_simpleMath() {
        String code = """
            import math
            result = math.sqrt(16) + math.sin(0.5)
            print(result)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertTrue(result.isSafe(), "简单数学计算应该安全：" + result.getViolations());
    }
    
    @Test
    @DisplayName("测试安全代码 - JSON 处理")
    void testSafeCode_json() {
        String code = """
            import json
            data = {"name": "test", "value": 42}
            json_str = json.dumps(data)
            parsed = json.loads(json_str)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertTrue(result.isSafe(), "JSON 处理应该安全：" + result.getViolations());
    }
    
    @Test
    @DisplayName("测试安全代码 - 正则表达式")
    void testSafeCode_regex() {
        String code = """
            import re
            pattern = r"\\d+"
            text = "abc123def456"
            matches = re.findall(pattern, text)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertTrue(result.isSafe(), "正则表达式应该安全：" + result.getViolations());
    }
    
    @Test
    @DisplayName("测试安全代码 - 日期时间")
    void testSafeCode_datetime() {
        String code = """
            from datetime import datetime, timedelta
            now = datetime.now()
            tomorrow = now + timedelta(days=1)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertTrue(result.isSafe(), "日期时间操作应该安全：" + result.getViolations());
    }
    
    @Test
    @DisplayName("测试安全代码 - numpy")
    void testSafeCode_numpy() {
        String code = """
            import numpy as np
            arr = np.array([1, 2, 3, 4, 5])
            mean = np.mean(arr)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertTrue(result.isSafe(), "numpy 操作应该安全：" + result.getViolations());
    }
    
    @Test
    @DisplayName("测试安全代码 - pandas")
    void testSafeCode_pandas() {
        String code = """
            import pandas as pd
            df = pd.DataFrame({'a': [1, 2, 3], 'b': [4, 5, 6]})
            result = df.sum()
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertTrue(result.isSafe(), "pandas 操作应该安全：" + result.getViolations());
    }
    
    @Test
    @DisplayName("测试危险代码 - eval")
    void testDangerousCode_eval() {
        String code = """
            user_input = "1 + 2"
            result = eval(user_input)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("eval")));
    }
    
    @Test
    @DisplayName("测试危险代码 - exec")
    void testDangerousCode_exec() {
        String code = """
            code = "print('hello')"
            exec(code)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("exec")));
    }
    
    @Test
    @DisplayName("测试危险代码 - os.system")
    void testDangerousCode_osSystem() {
        String code = """
            import os
            os.system("ls -la")
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("os.system")));
    }
    
    @Test
    @DisplayName("测试危险代码 - subprocess")
    void testDangerousCode_subprocess() {
        String code = """
            import subprocess
            result = subprocess.run(["ls", "-la"], capture_output=True)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("subprocess")));
    }
    
    @Test
    @DisplayName("测试危险代码 - open 文件操作")
    void testDangerousCode_open() {
        String code = """
            with open("/etc/passwd", "r") as f:
                content = f.read()
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("open")));
    }
    
    @Test
    @DisplayName("测试危险代码 - pickle")
    void testDangerousCode_pickle() {
        String code = """
            import pickle
            data = pickle.loads(serialized_data)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("pickle")));
    }
    
    @Test
    @DisplayName("测试危险代码 - __import__")
    void testDangerousCode_dynamicImport() {
        String code = """
            module_name = "os"
            os = __import__(module_name)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("__import__")));
    }
    
    @Test
    @DisplayName("测试未授权模块导入 - os")
    void testUnauthorizedImport_os() {
        String code = """
            import os
            print(os.getcwd())
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("os")));
    }
    
    @Test
    @DisplayName("测试未授权模块导入 - sys")
    void testUnauthorizedImport_sys() {
        String code = """
            import sys
            print(sys.path)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("sys")));
    }
    
    @Test
    @DisplayName("测试未授权模块导入 - socket")
    void testUnauthorizedImport_socket() {
        String code = """
            import socket
            s = socket.socket()
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("socket")));
    }
    
    @Test
    @DisplayName("测试网络模块 - 禁用网络时")
    void testNetworkModule_disabled() {
        String code = """
            import requests
            response = requests.get("https://example.com")
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("网络权限")));
    }
    
    @Test
    @DisplayName("测试网络模块 - 启用网络时")
    void testNetworkModule_enabled() {
        String code = """
            import requests
            response = requests.get("https://example.com")
            """;
        
        SecurityAnalysisResult result = analyzerWithNetwork.analyze(code);
        assertTrue(result.isSafe(), "启用网络后 requests 应该安全：" + result.getViolations());
    }
    
    @Test
    @DisplayName("测试代码注入风险")
    void testCodeInjection() {
        String code = """
            user_code = "print('hello')"
            eval(user_code + " + ' world'")
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试多个危险函数")
    void testMultipleDangerousFunctions() {
        String code = """
            import os
            eval("1+1")
            exec("print(2)")
            os.system("ls")
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().size() >= 3);
    }
    
    @Test
    @DisplayName("测试 isSafe 快捷方法")
    void testIsSafeShortcut() {
        String safeCode = "import math\nresult = math.sqrt(16)";
        assertTrue(analyzer.isSafe(safeCode));
        
        String dangerousCode = "eval('1+1')";
        assertFalse(analyzer.isSafe(dangerousCode));
    }
    
    @Test
    @DisplayName("测试获取导入列表")
    void testGetImports() {
        String code = """
            import math
            import json
            from datetime import datetime
            import numpy as np
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        List<String> imports = result.getImports();
        
        assertTrue(imports.contains("math"));
        assertTrue(imports.contains("json"));
        assertTrue(imports.contains("datetime"));
        assertTrue(imports.contains("numpy"));
    }
    
    @Test
    @DisplayName("测试警告信息")
    void testWarnings() {
        String code = """
            user_input = get_user_input()
            eval(user_input + " + 1")
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        // 代码注入风险应该产生警告
        assertFalse(result.getWarnings().isEmpty() || result.getViolations().isEmpty());
    }
    
    @Test
    @DisplayName("测试结果 toString")
    void testResultToString() {
        SecurityAnalysisResult result = analyzer.analyze("import math");
        String str = result.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("SecurityAnalysisResult"));
    }
    
    @Test
    @DisplayName("测试获取配置")
    void testGetConfig() {
        assertNotNull(analyzer.getConfig());
        assertEquals(PythonSecurityConfig.createStrict().getDangerousFunctions(), 
                     analyzer.getConfig().getDangerousFunctions());
    }
    
    @Test
    @DisplayName("测试绕过检测 - getattr")
    void testBypassDetection_getattr() {
        String code = """
            builtins = getattr(obj, '__builtins__')
            eval = getattr(builtins, 'eval')
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("getattr")));
    }
    
    @Test
    @DisplayName("测试绕过检测 - __class__")
    void testBypassDetection_class() {
        String code = """
            cls = obj.__class__
            subclasses = cls.__subclasses__()
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        // 应该产生警告
        assertFalse(result.getWarnings().isEmpty());
    }
    
    @Test
    @DisplayName("测试绕过检测 - __mro__")
    void testBypassDetection_mro() {
        String code = """
            mro = obj.__mro__
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.getWarnings().isEmpty());
    }
    
    @Test
    @DisplayName("测试绕过检测 - importlib")
    void testBypassDetection_importlib() {
        String code = """
            import importlib
            os = importlib.import_module('os')
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.contains("importlib")));
    }
    
    @Test
    @DisplayName("测试绕过检测 - __import__ 变体")
    void testBypassDetection_dunderImport() {
        String code = """
            os = __import__('os')
            os.system('ls')
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
        assertTrue(result.getViolations().size() >= 2);
    }
    
    @Test
    @DisplayName("测试危险函数 - os.popen")
    void testDangerousFunction_osPopen() {
        String code = """
            import os
            os.popen('ls')
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试危险函数 - subprocess.Popen")
    void testDangerousFunction_subprocessPopen() {
        String code = """
            import subprocess
            subprocess.Popen(['ls'])
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试危险函数 - pickle.loads")
    void testDangerousFunction_pickleLoads() {
        String code = """
            import pickle
            data = pickle.loads(serialized)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试危险函数 - compile")
    void testDangerousFunction_compile() {
        String code = """
            code = compile('1+1', '<string>', 'eval')
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试危险函数 - globals")
    void testDangerousFunction_globals() {
        String code = """
            g = globals()
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试危险函数 - locals")
    void testDangerousFunction_locals() {
        String code = """
            l = locals()
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试危险函数 - setattr")
    void testDangerousFunction_setattr() {
        String code = """
            setattr(obj, 'attr', value)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试危险函数 - delattr")
    void testDangerousFunction_delattr() {
        String code = """
            delattr(obj, 'attr')
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试未授权模块 - ctypes")
    void testUnauthorizedModule_ctypes() {
        String code = """
            import ctypes
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试未授权模块 - platform")
    void testUnauthorizedModule_platform() {
        String code = """
            import platform
            print(platform.system())
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试隐藏代码检测 - 字符串中的 eval")
    void testHiddenCode_stringEval() {
        String code = """
            malicious = "eval(user_input)"
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        // 应该产生警告
        assertFalse(result.getWarnings().isEmpty() || result.getViolations().isEmpty());
    }
    
    @Test
    @DisplayName("测试 format 注入检测")
    void testFormatInjection() {
        String code = """
            code = "print('{}')".format(user_input)
            eval(code)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试检测到的函数数量")
    void testDetectedFunctionsCount() {
        String code = """
            import math
            math.sqrt(16)
            math.sin(0.5)
            math.cos(0.5)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertTrue(result.getDetectedFunctionsCount() > 0);
    }
    
    @Test
    @DisplayName("测试复杂绕过 - 多层 getattr")
    void testComplexBypass_nestedGetattr() {
        String code = """
            builtins = getattr(obj, '__builtins__')
            eval_func = getattr(builtins, 'eval')
            eval_func('1+1')
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertFalse(result.isSafe());
    }
    
    @Test
    @DisplayName("测试注释中的危险代码被忽略")
    void testCommentIgnored() {
        String code = """
            # eval('1+1')
            # import os
            import math
            result = math.sqrt(16)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertTrue(result.isSafe());
    }
    
    @Test
    @DisplayName("测试多行字符串中的危险代码被忽略")
    void testMultilineStringIgnored() {
        String code = """
            doc = \"\"\"
            This is a docstring
            with some text
            \"\"\"
            import math
            result = math.sqrt(16)
            """;
        
        SecurityAnalysisResult result = analyzer.analyze(code);
        assertTrue(result.isSafe());
    }
}
