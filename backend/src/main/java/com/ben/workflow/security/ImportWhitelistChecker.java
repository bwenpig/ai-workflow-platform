package com.ben.workflow.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模块导入白名单检查器
 * 
 * 专门负责检查 Python 代码中的模块导入语句，确保只使用授权的模块。
 * 支持以下导入形式：
 * <ul>
 *   <li>import module</li>
 *   <li>import module as alias</li>
 *   <li>import module1, module2</li>
 *   <li>from module import something</li>
 *   <li>from module.submodule import something</li>
 * </ul>
 * 
 * <h2>使用方法</h2>
 * <pre>{@code
 * PythonSecurityConfig config = PythonSecurityConfig.createStrict();
 * ImportWhitelistChecker checker = new ImportWhitelistChecker(config);
 * 
 * ImportCheckResult result = checker.checkImports(code);
 * if (!result.isAllAllowed()) {
 *     System.err.println("检测到未授权导入：" + result.getUnauthorizedImports());
 * }
 * }</pre>
 * 
 * @author 龙傲天
 * @version 1.0
 * @since 2026-04-01
 */
public class ImportWhitelistChecker {
    
    private final PythonSecurityConfig config;
    
    /**
     * import 语句匹配模式
     */
    private static final Pattern IMPORT_STATEMENT_PATTERN = Pattern.compile(
        "^\\s*import\\s+([^\\n]+)",
        Pattern.MULTILINE
    );
    
    /**
     * from ... import 语句匹配模式
     */
    private static final Pattern FROM_IMPORT_PATTERN = Pattern.compile(
        "^\\s*from\\s+([a-zA-Z_][a-zA-Z0-9_.]*)\\s+import",
        Pattern.MULTILINE
    );
    
    /**
     * 注释匹配模式（用于排除注释中的导入）
     */
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
        "#.*$",
        Pattern.MULTILINE
    );
    
    /**
     * 多行字符串匹配模式（用于排除字符串中的导入）
     */
    private static final Pattern MULTILINE_STRING_PATTERN = Pattern.compile(
        "(\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?''')",
        Pattern.MULTILINE
    );
    
    /**
     * 构造函数
     * 
     * @param config 安全配置
     */
    public ImportWhitelistChecker(PythonSecurityConfig config) {
        this.config = config;
    }
    
    /**
     * 检查代码中的所有导入
     * 
     * @param code Python 代码
     * @return 导入检查结果
     */
    public ImportCheckResult checkImports(String code) {
        if (code == null || code.trim().isEmpty()) {
            return ImportCheckResult.empty();
        }
        
        // 1. 移除注释和多行字符串
        String cleanCode = removeCommentsAndStrings(code);
        
        // 2. 提取所有导入
        List<ImportStatement> imports = extractImports(cleanCode);
        
        // 3. 检查每个导入
        List<ImportStatement> allowed = new ArrayList<>();
        List<ImportStatement> unauthorized = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        for (ImportStatement importStmt : imports) {
            String moduleName = importStmt.getModuleName();
            
            if (config.isSafeModule(moduleName)) {
                allowed.add(importStmt);
                
                // 检查是否需要网络权限
                if (config.requiresNetworkPermission(moduleName) && !config.isNetworkEnabled()) {
                    warnings.add("模块 " + moduleName + " 需要网络权限，但当前配置已禁用网络");
                    unauthorized.add(importStmt);
                }
            } else {
                unauthorized.add(importStmt);
            }
        }
        
        return new ImportCheckResult(imports, allowed, unauthorized, warnings);
    }
    
    /**
     * 移除代码中的注释和字符串
     * 
     * @param code 原始代码
     * @return 清理后的代码
     */
    private String removeCommentsAndStrings(String code) {
        // 移除多行字符串
        String result = MULTILINE_STRING_PATTERN.matcher(code).replaceAll("");
        
        // 移除单行注释
        result = COMMENT_PATTERN.matcher(result).replaceAll("");
        
        return result;
    }
    
    /**
     * 提取所有导入语句
     * 
     * @param code 清理后的代码
     * @return 导入语句列表
     */
    private List<ImportStatement> extractImports(String code) {
        List<ImportStatement> imports = new ArrayList<>();
        
        // 提取 import 语句
        Matcher importMatcher = IMPORT_STATEMENT_PATTERN.matcher(code);
        while (importMatcher.find()) {
            String importLine = importMatcher.group(1);
            parseImportStatement(importLine, imports);
        }
        
        // 提取 from ... import 语句
        Matcher fromMatcher = FROM_IMPORT_PATTERN.matcher(code);
        while (fromMatcher.find()) {
            String moduleName = fromMatcher.group(1).trim();
            imports.add(new ImportStatement(moduleName, fromMatcher.start(), "from"));
        }
        
        return imports;
    }
    
    /**
     * 解析 import 语句
     * 
     * @param importLine import 语句内容（不包含 "import" 关键字）
     * @param imports 导入列表
     */
    private void parseImportStatement(String importLine, List<ImportStatement> imports) {
        // 分割多个导入（import module1, module2）
        String[] modules = importLine.split("\\s*,\\s*");
        
        for (String module : modules) {
            module = module.trim();
            
            // 处理别名（import module as alias）
            String[] parts = module.split("\\s+as\\s+");
            String moduleName = parts[0].trim();
            String alias = parts.length > 1 ? parts[1].trim() : null;
            
            if (!moduleName.isEmpty()) {
                imports.add(new ImportStatement(moduleName, 0, "import", alias));
            }
        }
    }
    
    /**
     * 快速检查代码是否只包含授权的导入
     * 
     * @param code Python 代码
     * @return true 表示所有导入都授权
     */
    public boolean isAllAllowed(String code) {
        return checkImports(code).isAllAllowed();
    }
    
    /**
     * 获取配置
     * 
     * @return 安全配置
     */
    public PythonSecurityConfig getConfig() {
        return config;
    }
}
