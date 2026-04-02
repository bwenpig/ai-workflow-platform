package com.ben.workflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImportWhitelistChecker 单元测试
 * 
 * @author 龙傲天
 * @version 1.0
 */
@Execution(ExecutionMode.CONCURRENT)
class ImportWhitelistCheckerTest {
    
    private ImportWhitelistChecker checker;
    private ImportWhitelistChecker checkerWithNetwork;
    
    @BeforeEach
    void setUp() {
        checker = new ImportWhitelistChecker(PythonSecurityConfig.createStrict());
        checkerWithNetwork = new ImportWhitelistChecker(
            PythonSecurityConfig.createStrict().setNetworkEnabled(true)
        );
    }
    
    @Test
    @DisplayName("测试空代码")
    void testEmptyCode() {
        ImportCheckResult result = checker.checkImports("");
        assertTrue(result.isAllAllowed());
        assertEquals(0, result.getTotalCount());
        
        result = checker.checkImports(null);
        assertTrue(result.isAllAllowed());
    }
    
    @Test
    @DisplayName("测试简单 import 语句")
    void testSimpleImport() {
        String code = "import math";
        ImportCheckResult result = checker.checkImports(code);
        
        assertTrue(result.isAllAllowed());
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getAllowedImports().size());
    }
    
    @Test
    @DisplayName("测试多个 import 语句")
    void testMultipleImports() {
        String code = """
            import math
            import json
            import re
            """;
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertTrue(result.isAllAllowed());
        assertEquals(3, result.getTotalCount());
        assertEquals(3, result.getAllowedImports().size());
    }
    
    @Test
    @DisplayName("测试逗号分隔的 import")
    void testCommaSeparatedImports() {
        String code = "import math, json, re";
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertTrue(result.isAllAllowed());
        assertEquals(3, result.getTotalCount());
    }
    
    @Test
    @DisplayName("测试带别名的 import")
    void testImportWithAlias() {
        String code = """
            import numpy as np
            import pandas as pd
            import matplotlib.pyplot as plt
            """;
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertTrue(result.isAllAllowed());
        assertEquals(3, result.getTotalCount());
        
        List<ImportStatement> imports = result.getAllowedImports();
        assertTrue(imports.stream().anyMatch(i -> i.hasAlias() && i.getAlias().equals("np")));
        assertTrue(imports.stream().anyMatch(i -> i.hasAlias() && i.getAlias().equals("pd")));
    }
    
    @Test
    @DisplayName("测试 from ... import 语句")
    void testFromImport() {
        String code = """
            from datetime import datetime
            from collections import defaultdict
            """;
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertTrue(result.isAllAllowed());
        assertEquals(2, result.getTotalCount());
        assertEquals("datetime", result.getAllImports().get(0).getModuleName());
        assertEquals("collections", result.getAllImports().get(1).getModuleName());
    }
    
    @Test
    @DisplayName("测试子模块导入")
    void testSubmoduleImport() {
        String code = """
            import numpy.random
            from PIL import Image
            """;
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertTrue(result.isAllAllowed());
        assertEquals(2, result.getTotalCount());
    }
    
    @Test
    @DisplayName("测试未授权模块 - os")
    void testUnauthorizedModule_os() {
        String code = "import os";
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertFalse(result.isAllAllowed());
        assertEquals(1, result.getUnauthorizedCount());
        assertEquals("os", result.getUnauthorizedImports().get(0).getModuleName());
    }
    
    @Test
    @DisplayName("测试未授权模块 - sys")
    void testUnauthorizedModule_sys() {
        String code = "import sys";
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertFalse(result.isAllAllowed());
        assertEquals(1, result.getUnauthorizedCount());
    }
    
    @Test
    @DisplayName("测试未授权模块 - subprocess")
    void testUnauthorizedModule_subprocess() {
        String code = "import subprocess";
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertFalse(result.isAllAllowed());
        assertEquals(1, result.getUnauthorizedCount());
    }
    
    @Test
    @DisplayName("测试混合授权和未授权")
    void testMixedImports() {
        String code = """
            import math
            import os
            import json
            import sys
            """;
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertFalse(result.isAllAllowed());
        assertEquals(4, result.getTotalCount());
        assertEquals(2, result.getAllowedCount());
        assertEquals(2, result.getUnauthorizedCount());
    }
    
    @Test
    @DisplayName("测试网络模块 - 禁用网络")
    void testNetworkModule_disabled() {
        String code = "import requests";
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertFalse(result.isAllAllowed());
        assertEquals(1, result.getUnauthorizedCount());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("网络权限")));
    }
    
    @Test
    @DisplayName("测试网络模块 - 启用网络")
    void testNetworkModule_enabled() {
        String code = "import requests";
        
        ImportCheckResult result = checkerWithNetwork.checkImports(code);
        
        assertTrue(result.isAllAllowed());
        assertEquals(1, result.getAllowedImports().size());
    }
    
    @Test
    @DisplayName("测试注释中的 import 被忽略")
    void testImportInComment() {
        String code = """
            # import os
            import math
            """;
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertTrue(result.isAllAllowed());
        assertEquals(1, result.getTotalCount());
        assertEquals("math", result.getAllImports().get(0).getModuleName());
    }
    
    @Test
    @DisplayName("测试字符串中的 import 被忽略")
    void testImportInString() {
        String code = """
            code = "import os"
            import math
            """;
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertTrue(result.isAllAllowed());
        assertEquals(1, result.getTotalCount());
    }
    
    @Test
    @DisplayName("测试多行字符串中的 import 被忽略")
    void testImportInMultilineString() {
        String code = """
            docstring = '''
            import os
            import sys
            This is a docstring
            '''
            import math
            """;
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertTrue(result.isAllAllowed());
        assertEquals(1, result.getTotalCount());
    }
    
    @Test
    @DisplayName("测试 isAllAllowed 快捷方法")
    void testIsAllAllowedShortcut() {
        String safeCode = "import math\nimport json";
        assertTrue(checker.isAllAllowed(safeCode));
        
        String dangerousCode = "import os";
        assertFalse(checker.isAllAllowed(dangerousCode));
    }
    
    @Test
    @DisplayName("测试获取配置")
    void testGetConfig() {
        assertNotNull(checker.getConfig());
    }
    
    @Test
    @DisplayName("测试 ImportStatement - 基本属性")
    void testImportStatement() {
        ImportStatement stmt = new ImportStatement("math", 0, "import");
        
        assertEquals("math", stmt.getModuleName());
        assertEquals(0, stmt.getPosition());
        assertEquals("import", stmt.getImportType());
        assertFalse(stmt.hasAlias());
        assertNull(stmt.getAlias());
    }
    
    @Test
    @DisplayName("测试 ImportStatement - 带别名")
    void testImportStatementWithAlias() {
        ImportStatement stmt = new ImportStatement("numpy", 10, "import", "np");
        
        assertEquals("numpy", stmt.getModuleName());
        assertEquals("np", stmt.getAlias());
        assertTrue(stmt.hasAlias());
    }
    
    @Test
    @DisplayName("测试 ImportStatement - equals 和 hashCode")
    void testImportStatementEquals() {
        ImportStatement stmt1 = new ImportStatement("math", 0, "import");
        ImportStatement stmt2 = new ImportStatement("math", 10, "import");
        ImportStatement stmt3 = new ImportStatement("json", 0, "import");
        
        assertEquals(stmt1, stmt2); // 相同模块名
        assertNotEquals(stmt1, stmt3); // 不同模块名
        assertEquals(stmt1.hashCode(), stmt2.hashCode());
    }
    
    @Test
    @DisplayName("测试 ImportStatement - toString")
    void testImportStatementToString() {
        ImportStatement stmt1 = new ImportStatement("math", 0, "import");
        assertEquals("import math", stmt1.toString());
        
        ImportStatement stmt2 = new ImportStatement("numpy", 0, "import", "np");
        assertEquals("import numpy as np", stmt2.toString());
        
        ImportStatement stmt3 = new ImportStatement("datetime", 0, "from");
        assertEquals("from datetime", stmt3.toString());
    }
    
    @Test
    @DisplayName("测试 ImportCheckResult - 空结果")
    void testEmptyResult() {
        ImportCheckResult result = ImportCheckResult.empty();
        
        assertTrue(result.isAllAllowed());
        assertEquals(0, result.getTotalCount());
        assertEquals(0, result.getUnauthorizedCount());
    }
    
    @Test
    @DisplayName("测试 ImportCheckResult - toString")
    void testImportCheckResultToString() {
        ImportCheckResult result = checker.checkImports("import math");
        String str = result.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("ImportCheckResult"));
        assertTrue(str.contains("total=1"));
    }
    
    @Test
    @DisplayName("测试复杂代码场景")
    void testComplexCode() {
        String code = """
            # 数据分析脚本
            import numpy as np
            import pandas as pd
            from matplotlib import pyplot as plt
            import json
            import re
            
            # 注释中的 import os 不应该被检测
            # import sys
            
            data = {"value": 42}
            json_str = json.dumps(data)
            arr = np.array([1, 2, 3])
            """;
        
        ImportCheckResult result = checker.checkImports(code);
        
        assertTrue(result.isAllAllowed());
        assertEquals(5, result.getTotalCount());
        assertEquals(5, result.getAllowedImports().size());
        assertEquals(0, result.getUnauthorizedCount());
    }
}
