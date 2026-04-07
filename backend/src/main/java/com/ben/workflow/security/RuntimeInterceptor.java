package com.ben.workflow.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Python 代码运行时拦截层
 * 
 * 在代码执行前进行拦截和过滤，注入安全限制代码，防止：
 * - 危险函数调用
 * - 非法模块导入
 * - 资源滥用
 * - 绕过尝试
 * 
 * <h2>工作原理</h2>
 * <p>通过代码包装（wrapping）技术，在用户代码外层包裹安全监控代码，
 * 在运行时拦截危险操作。拦截层包括：</p>
 * <ul>
 *   <li>覆盖 __import__ 函数，拦截模块导入</li>
 *   <li>覆盖 eval/exec 函数，禁止动态执行</li>
 *   <li>覆盖 open 函数，禁止文件操作（I/O 框架白名单路径除外）</li>
 *   <li>注入 SecurityError 异常类</li>
 *   <li>可选：注入资源限制（超时、内存）</li>
 * </ul>
 * 
 * <h2>使用方法</h2>
 * <pre>{@code
 * PythonSecurityConfig config = PythonSecurityConfig.createStrict();
 * RuntimeInterceptor interceptor = new RuntimeInterceptor(config);
 * 
 * String safeCode = interceptor.wrapUserCode(userCode);
 * // 执行 safeCode 而非原始 userCode
 * }</pre>
 * 
 * @author 龙傲天
 * @version 2.1
 * @since 2026-04-01
 */
public class RuntimeInterceptor {
    
    private final PythonSecurityConfig config;
    private final PythonSecurityAnalyzer analyzer;
    
    /**
     * 安全拦截器 Python 模板
     * 
     * 注意：
     * - open() 拦截使用白名单机制，允许 I/O 框架读写 inputs.json / outputs.json
     * - import 拦截放行相对导入和 Python 内部模块（_开头），仅检查顶层绝对导入
     */
    private static final String INTERCEPTOR_TEMPLATE = """
# =============================================================================
# Security Runtime Interceptor - DO NOT MODIFY
# 安全运行时拦截层 - 禁止修改
# =============================================================================
import builtins
import sys

class __SecurityInterceptor__:
    \"\"\"安全拦截器 - 监控所有危险操作\"\"\"
    
    def __init__(self, dangerous_funcs, allowed_imports):
        self.dangerous_funcs = dangerous_funcs
        self.allowed_imports = allowed_imports
        
    def check_function_call(self, func_name):
        \"\"\"检查函数调用是否安全\"\"\"
        if func_name in self.dangerous_funcs:
            raise __SecurityError__("禁止调用危险函数：" + func_name)
    
    def check_import(self, module_name):
        \"\"\"检查模块导入是否安全\"\"\"
        base_module = module_name.split('.')[0]
        if base_module not in self.allowed_imports:
            raise __SecurityError__("禁止导入未授权模块：" + module_name)

# 创建拦截器实例
_interceptor = __SecurityInterceptor__(
    dangerous_funcs=%s,
    allowed_imports=%s
)

# -----------------------------------------------------------------------------
# 覆盖内置危险函数
# -----------------------------------------------------------------------------

# 覆盖 __import__ 函数
_original_import = builtins.__import__

def _safe_import(name, globals=None, locals=None, fromlist=(), level=0):
    \"\"\"安全的导入函数 - 拦截未授权模块\"\"\"
    # 放行相对导入（level > 0）和 Python 内部模块（_开头 / importlib）
    # 这些是 Python 导入机制自身所需，不属于用户代码的导入行为
    if level == 0 and not name.startswith('_') and name != 'importlib':
        _interceptor.check_import(name)
    return _original_import(name, globals, locals, fromlist, level)

builtins.__import__ = _safe_import

# 覆盖 eval - 完全禁止
def _blocked_eval(*args, **kwargs):
    raise __SecurityError__("禁止使用 eval() - 动态代码执行风险")

builtins.eval = _blocked_eval

# 覆盖 exec - 完全禁止
def _blocked_exec(*args, **kwargs):
    raise __SecurityError__("禁止使用 exec() - 动态代码执行风险")

builtins.exec = _blocked_exec

# 覆盖 compile - 完全禁止
def _blocked_compile(*args, **kwargs):
    raise __SecurityError__("禁止使用 compile() - 代码编译风险")

builtins.compile = _blocked_compile

# 覆盖 open - 白名单路径放行，其余禁止
# 保存原始 open，供 I/O 框架白名单文件使用
_original_open = builtins.open

# I/O 框架白名单文件名（仅允许 PythonScriptUtils 框架所需的文件）
_ALLOWED_IO_BASENAMES = {'inputs.json', 'outputs.json'}

def _blocked_open(*args, **kwargs):
    \"\"\"安全的 open - 仅允许 I/O 框架白名单文件，其余全部拦截\"\"\"
    if args:
        filepath = str(args[0])
        # 提取文件名（不依赖 os 模块，纯字符串操作）
        basename = filepath.rsplit('/', 1)[-1].rsplit('\\\\', 1)[-1]
        if basename in _ALLOWED_IO_BASENAMES:
            return _original_open(*args, **kwargs)
    raise __SecurityError__("禁止使用 open() - 文件操作风险")

builtins.open = _blocked_open

# 覆盖 getattr（防止绕过）
_original_getattr = builtins.getattr

def _safe_getattr(obj, name, *args):
    \"\"\"安全的 getattr - 拦截危险属性访问\"\"\"
    dangerous_attrs = ['__class__', '__mro__', '__subclasses__', '__globals__', 
                       '__builtins__', '__import__', 'func_globals', 'gi_frame',
                       'f_globals', 'f_builtins']
    if name in dangerous_attrs:
        raise __SecurityError__("禁止访问危险属性：" + name)
    return _original_getattr(obj, name, *args)

builtins.getattr = _safe_getattr

# 覆盖 setattr（防止绕过）
def _blocked_setattr(obj, name, value):
    raise __SecurityError__("禁止使用 setattr() - 属性修改风险")

builtins.setattr = _blocked_setattr

# 覆盖 delattr（防止绕过）
def _blocked_delattr(obj, name):
    raise __SecurityError__("禁止使用 delattr() - 属性删除风险")

builtins.delattr = _blocked_delattr

# 覆盖 globals/locals（防止作用域泄露）
def _blocked_globals():
    raise __SecurityError__("禁止使用 globals() - 作用域泄露风险")

def _blocked_locals():
    raise __SecurityError__("禁止使用 locals() - 作用域泄露风险")

builtins.globals = _blocked_globals
builtins.locals = _blocked_locals

# -----------------------------------------------------------------------------
# 安全异常类
# -----------------------------------------------------------------------------

class __SecurityError__(Exception):
    \"\"\"安全异常 - 标识安全违规行为\"\"\"
    pass

# -----------------------------------------------------------------------------
# 用户代码从这里开始
# -----------------------------------------------------------------------------

""";
    
    /**
     * 构造函数
     * 
     * @param config 安全配置
     */
    public RuntimeInterceptor(PythonSecurityConfig config) {
        this.config = config;
        this.analyzer = new PythonSecurityAnalyzer(config);
    }
    
    /**
     * 包装用户代码，注入安全拦截逻辑
     * 
     * @param userCode 用户原始代码
     * @return 包装后的安全代码
     * @throws SecurityViolationException 当代码违反安全策略时抛出
     */
    public String wrapUserCode(String userCode) throws SecurityViolationException {
        if (userCode == null || userCode.trim().isEmpty()) {
            return userCode;
        }
        
        // 1. 先进行静态分析（AST 风格）
        SecurityAnalysisResult analysis = analyzer.analyze(userCode);
        if (!analysis.isSafe()) {
            throw new SecurityViolationException(
                "代码安全检查失败：" + String.join(", ", analysis.getViolations()),
                analysis.getViolations()
            );
        }
        
        // 2. 生成危险函数列表（Python 集合格式）
        String dangerousFuncs = pythonSet(config.getDangerousFunctions());
        String allowedImports = pythonSet(config.getSafeModules());
        
        // 3. 生成拦截代码
        String interceptorCode = String.format(INTERCEPTOR_TEMPLATE, dangerousFuncs, allowedImports);
        
        // 4. 包装用户代码
        return interceptorCode + "\n" + userCode;
    }
    
    /**
     * 生成 Python 集合字面量
     * 
     * @param items Java 集合
     * @return Python 集合字面量
     */
    private String pythonSet(Set<String> items) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (String item : items) {
            if (!first) sb.append(", ");
            sb.append("'").append(item.replace("'", "\\'")).append("'");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 注入资源限制代码
     * 
     * @param code 代码
     * @param timeoutSeconds 超时时间（秒）
     * @param maxMemoryMB 最大内存（MB）
     * @return 注入资源限制后的代码
     */
    public String injectResourceLimits(String code, int timeoutSeconds, long maxMemoryMB) {
        if (code == null || code.trim().isEmpty()) {
            return code;
        }
        
        StringBuilder resourceLimits = new StringBuilder();
        resourceLimits.append("\n# =============================================================================\n");
        resourceLimits.append("# Resource Limits - 资源限制\n");
        resourceLimits.append("# =============================================================================\n");
        resourceLimits.append("import signal\n");
        resourceLimits.append("import resource\n");
        resourceLimits.append("\n");
        resourceLimits.append("def _timeout_handler(signum, frame):\n");
        resourceLimits.append("    raise TimeoutError('代码执行超时 (").append(timeoutSeconds).append("秒)')\n");
        resourceLimits.append("\n");
        resourceLimits.append("signal.signal(signal.SIGALRM, _timeout_handler)\n");
        resourceLimits.append("signal.alarm(").append(timeoutSeconds).append(")\n");
        resourceLimits.append("\n");
        resourceLimits.append("# 内存限制 (").append(maxMemoryMB).append(" MB)\n");
        resourceLimits.append("resource.setrlimit(resource.RLIMIT_AS, (").append(maxMemoryMB)
            .append(" * 1024 * 1024, ").append(maxMemoryMB).append(" * 1024 * 1024))\n");
        resourceLimits.append("\n");
        
        return resourceLimits.toString() + code;
    }
    
    /**
     * 验证并包装代码（完整流程）
     * 
     * @param userCode 用户代码
     * @return 包装后的安全代码
     * @throws SecurityViolationException 当代码违反安全策略时抛出
     */
    public String validateAndWrap(String userCode) throws SecurityViolationException {
        return wrapUserCode(userCode);
    }
    
    /**
     * 仅进行静态分析（不包装）
     * 
     * @param userCode 用户代码
     * @return 分析结果
     */
    public SecurityAnalysisResult analyzeOnly(String userCode) {
        return analyzer.analyze(userCode);
    }
    
    /**
     * 获取配置
     * 
     * @return 安全配置
     */
    public PythonSecurityConfig getConfig() {
        return config;
    }
    
    /**
     * 获取分析器
     * 
     * @return 安全分析器
     */
    public PythonSecurityAnalyzer getAnalyzer() {
        return analyzer;
    }
}
