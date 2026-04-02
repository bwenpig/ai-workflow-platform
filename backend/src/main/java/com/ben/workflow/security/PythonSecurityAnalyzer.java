package com.ben.workflow.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Python 代码安全分析器
 * 
 * 使用正则表达式和模式匹配对 Python 代码进行静态分析（模拟 AST 分析），检测：
 * - 危险函数调用（40+ 个危险函数）
 * - 非法模块导入
 * - 动态代码执行
 * - 代码注入风险
 * - 绕过尝试检测
 * 
 * <h2>分析方法</h2>
 * <ul>
 *   <li>词法分析：提取函数调用、导入语句、属性访问</li>
 *   <li>模式匹配：匹配危险函数、模块、代码模式</li>
 *   <li>上下文分析：检测注释/字符串中的伪装代码</li>
 *   <li>绕过检测：检测 getattr、importlib 等绕过手法</li>
 * </ul>
 * 
 * <h2>使用方法</h2>
 * <pre>{@code
 * PythonSecurityConfig config = PythonSecurityConfig.createStrict();
 * PythonSecurityAnalyzer analyzer = new PythonSecurityAnalyzer(config);
 * 
 * SecurityAnalysisResult result = analyzer.analyze(code);
 * if (!result.isSafe()) {
 *     System.err.println("代码不安全：" + result.getViolations());
 * }
 * }</pre>
 * 
 * @author 龙傲天
 * @version 2.0
 * @since 2026-04-01
 */
public class PythonSecurityAnalyzer {
    
    private final PythonSecurityConfig config;
    
    // ===== 词法分析正则 =====
    
    /**
     * 函数调用匹配：匹配 obj.method() 和 func() 形式
     */
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile(
        "\\b([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\s*\\(",
        Pattern.MULTILINE
    );
    
    /**
     * import 语句匹配
     */
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
        "^\\s*(?:import\\s+([^\\n]+)|from\\s+([a-zA-Z_][a-zA-Z0-9_.]*)\\s+import)",
        Pattern.MULTILINE
    );
    
    /**
     * 属性访问匹配：obj.attr 形式
     */
    private static final Pattern ATTRIBUTE_ACCESS_PATTERN = Pattern.compile(
        "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\.\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\b",
        Pattern.MULTILINE
    );
    
    /**
     * getattr 调用匹配：getattr(obj, 'attr')
     */
    private static final Pattern GETATTR_PATTERN = Pattern.compile(
        "\\bgetattr\\s*\\(\\s*([^,]+)\\s*,\\s*[\"']([^\"']+)[\"']",
        Pattern.MULTILINE
    );
    
    /**
     * 字符串中的代码匹配
     */
    private static final Pattern STRING_CODE_PATTERN = Pattern.compile(
        "[\"']([^\"']*?(?:eval|exec|__import__|os\\.system|subprocess)[^\"']*?)[\"']",
        Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 注释匹配（用于排除）
     */
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
        "#.*$",
        Pattern.MULTILINE
    );
    
    /**
     * 多行字符串匹配（用于排除）
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
    public PythonSecurityAnalyzer(PythonSecurityConfig config) {
        this.config = config;
    }
    
    /**
     * 分析 Python 代码的安全性
     * 
     * @param code Python 代码
     * @return 分析结果
     */
    public SecurityAnalysisResult analyze(String code) {
        if (code == null || code.trim().isEmpty()) {
            return SecurityAnalysisResult.safe("代码为空");
        }
        
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> imports = new ArrayList<>();
        Set<String> detectedFunctions = new HashSet<>();
        
        // 1. 清理代码（移除注释和字符串，用于准确分析）
        String cleanCode = removeCommentsAndStrings(code);
        
        // 2. AST 风格分析 - 检测危险函数调用
        detectDangerousFunctions(cleanCode, violations, detectedFunctions);
        
        // 3. AST 风格分析 - 检测模块导入
        detectImports(code, imports, violations, warnings);
        
        // 4. 检测动态执行（eval/exec/compile）
        detectDynamicExecution(cleanCode, violations);
        
        // 5. 检测 getattr/setattr 绕过
        detectBypassAttempts(cleanCode, violations, warnings);
        
        // 6. 检测代码注入风险
        detectCodeInjection(code, warnings);
        
        // 7. 检测隐藏的危险代码（字符串中）
        detectHiddenDangerousCode(code, warnings);
        
        boolean isSafe = violations.isEmpty();
        
        return new SecurityAnalysisResult(isSafe, violations, warnings, imports, detectedFunctions.size());
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
     * 检测危险函数调用（AST 风格分析）
     * 
     * @param code 清理后的代码
     * @param violations 违规列表
     * @param detectedFunctions 已检测函数集合（去重）
     */
    private void detectDangerousFunctions(String code, List<String> violations, Set<String> detectedFunctions) {
        Matcher matcher = FUNCTION_CALL_PATTERN.matcher(code);
        
        while (matcher.find()) {
            String functionName = matcher.group(1);
            
            // 去重检测
            if (detectedFunctions.contains(functionName)) {
                continue;
            }
            detectedFunctions.add(functionName);
            
            // 检查是否在黑名单中
            if (config.isDangerousFunction(functionName)) {
                violations.add("检测到危险函数调用：" + functionName + "()");
            }
        }
        
        // 检测属性访问中的危险函数（如 os.system）
        matcher = ATTRIBUTE_ACCESS_PATTERN.matcher(code);
        while (matcher.find()) {
            String obj = matcher.group(1);
            String attr = matcher.group(2);
            String fullName = obj + "." + attr;
            
            if (config.isDangerousFunction(fullName) && !detectedFunctions.contains(fullName)) {
                violations.add("检测到危险属性访问：" + fullName);
                detectedFunctions.add(fullName);
            }
        }
    }
    
    /**
     * 检测模块导入
     * 
     * @param code 原始代码
     * @param imports 导入列表
     * @param violations 违规列表
     * @param warnings 警告列表
     */
    private void detectImports(String code, List<String> imports, List<String> violations, List<String> warnings) {
        Matcher matcher = IMPORT_PATTERN.matcher(code);
        
        while (matcher.find()) {
            // import module1, module2
            if (matcher.group(1) != null) {
                String importLine = matcher.group(1);
                String[] modules = importLine.split("\\s*,\\s*");
                for (String module : modules) {
                    String moduleName = module.trim().split("\\s+as\\s+")[0].trim();
                    if (!moduleName.isEmpty()) {
                        imports.add(moduleName);
                        checkModuleSecurity(moduleName, violations, warnings);
                    }
                }
            }
            // from module import ...
            else if (matcher.group(2) != null) {
                String moduleName = matcher.group(2).trim();
                imports.add(moduleName);
                checkModuleSecurity(moduleName, violations, warnings);
            }
        }
    }
    
    /**
     * 检查模块安全性
     * 
     * @param moduleName 模块名称
     * @param violations 违规列表
     * @param warnings 警告列表
     */
    private void checkModuleSecurity(String moduleName, List<String> violations, List<String> warnings) {
        if (!config.isSafeModule(moduleName)) {
            violations.add("检测到未授权的模块导入：" + moduleName);
        } else if (config.requiresNetworkPermission(moduleName) && !config.isNetworkEnabled()) {
            violations.add("模块 " + moduleName + " 需要网络权限，但当前配置已禁用网络");
        }
    }
    
    /**
     * 检测动态执行
     * 
     * @param code 清理后的代码
     * @param violations 违规列表
     */
    private void detectDynamicExecution(String code, List<String> violations) {
        // 检测 eval/exec/compile
        Pattern dynamicExecPattern = Pattern.compile(
            "\\b(eval|exec|compile)\\s*\\(",
            Pattern.MULTILINE
        );
        Matcher matcher = dynamicExecPattern.matcher(code);
        while (matcher.find()) {
            String funcName = matcher.group(1);
            violations.add("检测到动态代码执行：" + funcName + "() - 高危操作");
        }
        
        // 检测 __import__
        Pattern dynamicImportPattern = Pattern.compile(
            "\\b__import__\\s*\\(",
            Pattern.MULTILINE
        );
        matcher = dynamicImportPattern.matcher(code);
        while (matcher.find()) {
            violations.add("检测到动态导入：__import__() - 高危操作");
        }
        
        // 检测 importlib 动态导入
        Pattern importlibPattern = Pattern.compile(
            "\\bimportlib\\.(import_module|reload|find_spec)\\s*\\(",
            Pattern.MULTILINE
        );
        matcher = importlibPattern.matcher(code);
        while (matcher.find()) {
            violations.add("检测到 importlib 动态导入：" + matcher.group(1) + "() - 高危操作");
        }
    }
    
    /**
     * 检测绕过尝试（getattr、__class__、__mro__等）
     * 
     * @param code 清理后的代码
     * @param violations 违规列表
     * @param warnings 警告列表
     */
    private void detectBypassAttempts(String code, List<String> violations, List<String> warnings) {
        // 检测 getattr 绕过
        Matcher getattrMatcher = GETATTR_PATTERN.matcher(code);
        while (getattrMatcher.find()) {
            String attrName = getattrMatcher.group(2);
            if (isDangerousAttributeName(attrName)) {
                violations.add("检测到 getattr 绕过尝试：getattr(_, '" + attrName + "')");
            }
        }
        
        // 检测 __class__、__mro__、__subclasses__ 等绕过
        Pattern bypassPattern = Pattern.compile(
            "\\b__(class|mro|subclasses__|globals__|builtins__|import__)__",
            Pattern.MULTILINE
        );
        Matcher bypassMatcher = bypassPattern.matcher(code);
        while (bypassMatcher.find()) {
            warnings.add("警告：检测到潜在的绕过手法：" + bypassMatcher.group(0));
        }
        
        // 检测 exec 的变体
        Pattern execVariantPattern = Pattern.compile(
            "\\b(execfile|apply|builtin|__builtin__)\\b",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
        );
        Matcher execVariantMatcher = execVariantPattern.matcher(code);
        while (execVariantMatcher.find()) {
            violations.add("检测到危险标识符：" + execVariantMatcher.group(0));
        }
    }
    
    /**
     * 检查属性名是否危险
     * 
     * @param attrName 属性名
     * @return true 表示危险
     */
    private boolean isDangerousAttributeName(String attrName) {
        Set<String> dangerousAttrs = Set.of(
            "__class__", "__mro__", "__subclasses__", "__globals__", 
            "__builtins__", "__import__", "func_globals", "gi_frame",
            "f_globals", "f_builtins"
        );
        return dangerousAttrs.contains(attrName);
    }
    
    /**
     * 检测代码注入风险
     * 
     * @param code 原始代码
     * @param warnings 警告列表
     */
    private void detectCodeInjection(String code, List<String> warnings) {
        // 检测 eval/exec 与字符串拼接
        Pattern concatPattern = Pattern.compile(
            "(?:eval|exec)\\s*\\(\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*[\\+\\%\\.]",
            Pattern.MULTILINE
        );
        Matcher matcher = concatPattern.matcher(code);
        if (matcher.find()) {
            warnings.add("警告：检测到代码注入风险（字符串拼接后执行）");
        }
        
        // 检测 format 注入
        Pattern formatPattern = Pattern.compile(
            "(?:eval|exec)\\s*\\(.*\\.format\\s*\\(",
            Pattern.MULTILINE
        );
        matcher = formatPattern.matcher(code);
        if (matcher.find()) {
            warnings.add("警告：检测到潜在的 format 注入风险");
        }
    }
    
    /**
     * 检测隐藏在字符串中的危险代码
     * 
     * @param code 原始代码
     * @param warnings 警告列表
     */
    private void detectHiddenDangerousCode(String code, List<String> warnings) {
        Matcher matcher = STRING_CODE_PATTERN.matcher(code);
        if (matcher.find()) {
            warnings.add("警告：字符串中包含危险代码片段：" + matcher.group(1).substring(0, Math.min(50, matcher.group(1).length())) + "...");
        }
    }
    
    /**
     * 快速验证代码是否安全
     * 
     * @param code Python 代码
     * @return true 表示安全，false 表示存在风险
     */
    public boolean isSafe(String code) {
        return analyze(code).isSafe();
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
