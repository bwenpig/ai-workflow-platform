package com.ben.workflow.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Python 代码安全配置类
 * 
 * 定义危险函数黑名单和安全模块白名单，用于代码静态分析和运行时拦截。
 * 
 * <h2>危险函数黑名单</h2>
 * <p>禁止使用的危险函数，包括：</p>
 * <ul>
 *   <li>代码执行类：eval(), exec(), __import__()</li>
 *   <li>文件操作类：open()</li>
 *   <li>系统命令类：os.system(), os.popen()</li>
 *   <li>进程管理类：subprocess.*</li>
 *   <li>网络通信类：socket.*</li>
 *   <li>反序列化类：pickle.loads()</li>
 * </ul>
 * 
 * <h2>模块导入白名单</h2>
 * <p>允许导入的安全模块，包括：</p>
 * <ul>
 *   <li>标准库：math, random, datetime, json, re, collections, itertools, functools</li>
 *   <li>字符串处理：string, textwrap, unicodedata</li>
 *   <li>预装库：numpy, pandas, Pillow</li>
 *   <li>网络库：requests（需要网络权限配置）</li>
 * </ul>
 * 
 * @author 龙傲天
 * @version 1.0
 * @since 2026-04-01
 */
public class PythonSecurityConfig {
    
    /**
     * 危险函数黑名单（完全限定名）- 40+ 个危险函数
     */
    private static final Set<String> DANGEROUS_FUNCTIONS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            // ===== 代码执行类 (高危) =====
            "eval",
            "exec",
            "__import__",
            "compile",
            "builtin.compile",
            "builtins.compile",
            
            // ===== 文件操作类 (高危) =====
            "open",
            "io.open",
            "builtins.open",
            "file",
            
            // ===== 系统命令类 (高危) =====
            "os.system",
            "os.popen",
            "os.popen2",
            "os.popen3",
            "os.popen4",
            "os.execv",
            "os.execve",
            "os.execvp",
            "os.execvpe",
            "os.execle",
            "os.execl",
            "os.execlp",
            "os.execlpe",
            "os.spawnl",
            "os.spawnle",
            "os.spawnlp",
            "os.spawnlpe",
            "os.spawnv",
            "os.spawnve",
            "os.spawnvp",
            "os.spawnvpe",
            "os.fork",
            "os.forkpty",
            "os.kill",
            "os.killpg",
            "os.putenv",
            "os.unsetenv",
            "os.setuid",
            "os.setgid",
            "os.setreuid",
            "os.setregid",
            "os.setresuid",
            "os.setresgid",
            "os.chroot",
            "os.chmod",
            "os.chown",
            "os.lchmod",
            "os.lchown",
            "os.link",
            "os.symlink",
            "os.readlink",
            "os.remove",
            "os.unlink",
            "os.rmdir",
            "os.removedirs",
            "os.rename",
            "os.renames",
            "os.mkdir",
            "os.makedirs",
            "os.listdir",
            "os.walk",
            
            // ===== 进程管理类 (高危) =====
            "subprocess.call",
            "subprocess.check_call",
            "subprocess.check_output",
            "subprocess.run",
            "subprocess.Popen",
            "subprocess.getoutput",
            "subprocess.getstatusoutput",
            
            // ===== 网络通信类 (高危) =====
            "socket.socket",
            "socket.create_connection",
            "socket.gethostbyname",
            "socket.gethostname",
            "socket.gethostbyaddr",
            "socket.getaddrinfo",
            "urllib.request.urlopen",
            "urllib.request.Request",
            "urllib.request.urlretrieve",
            "urllib.request.urlcleanup",
            "http.client.HTTPConnection",
            "http.client.HTTPSConnection",
            "ftplib.FTP",
            "smtplib.SMTP",
            "poplib.POP3",
            "imaplib.IMAP4",
            "telnetlib.Telnet",
            
            // ===== 反序列化类 (高危) =====
            "pickle.loads",
            "pickle.load",
            "cPickle.loads",
            "cPickle.load",
            "marshal.loads",
            "marshal.load",
            "shelve.open",
            "yaml.load",
            "yaml.safe_load",
            "yaml.unsafe_load",
            
            // ===== 动态导入类 (高危) =====
            "importlib.import_module",
            "importlib.__import__",
            "importlib.reload",
            "importlib.util.find_spec",
            "pkgutil.find_loader",
            "pkgutil.get_loader",
            
            // ===== 反射/内省类 (中高危) =====
            "getattr",
            "setattr",
            "delattr",
            "hasattr",
            "globals",
            "locals",
            "vars",
            "dir",
            "type",
            "object.__subclasses__",
            "classmethod",
            "staticmethod",
            "super",
            
            // ===== 输入类 (中危) =====
            "input",
            "raw_input",
            "getpass.getpass",
            
            // ===== 代码对象类 (高危) =====
            "types.CodeType",
            "types.FunctionType",
            "types.LambdaType",
            "types.GeneratorType",
            "codeop.compile_command",
            "codeop.compile_command",
            
            // ===== 其他危险函数 =====
            "breakpoint",
            "pdb.set_trace",
            "traceback.print_stack",
            "inspect.stack",
            "inspect.currentframe",
            "sys._getframe",
            "sys.settrace",
            "sys.setprofile",
            "gc.get_objects",
            "weakref.proxy",
            "copyreg.pickle",
            "atexit.register",
            "signal.signal",
            "threading._start_new_thread",
            "_thread.start_new_thread",
            "multiprocessing.Process",
            "concurrent.futures.ProcessPoolExecutor",
            "concurrent.futures.ThreadPoolExecutor"
        ))
    );
    
    /**
     * 安全模块白名单 - 20+ 个安全模块
     */
    private static final Set<String> SAFE_MODULES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            // ===== 基础标准库 =====
            "math",
            "cmath",
            "random",
            "secrets",
            "statistics",
            
            // ===== 日期时间 =====
            "datetime",
            "time",
            "calendar",
            "zoneinfo",
            
            // ===== 字符串处理 =====
            "string",
            "textwrap",
            "unicodedata",
            "re",
            "difflib",
            
            // ===== 数据结构 =====
            "collections",
            "itertools",
            "functools",
            "operator",
            "array",
            "bisect",
            "heapq",
            "queue",
            "dataclasses",
            
            // ===== 数据处理 =====
            "json",
            "csv",
            "copy",
            "pprint",
            "enum",
            "decimal",
            "fractions",
            "numbers",
            
            // ===== 日志和调试 =====
            "logging",
            "warnings",
            "contextlib",
            
            // ===== 预装库（数据科学） =====
            "numpy",
            "pandas",
            "PIL",
            "pillow",
            "matplotlib",
            "scipy",
            "sklearn",
            
            // ===== 网络库（需要网络权限配置） =====
            "requests",
            "urllib3"
        ))
    );
    
    /**
     * 需要网络权限的模块
     */
    private static final Set<String> NETWORK_MODULES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "requests",
            "urllib",
            "http",
            "socket",
            "ftplib",
            "poplib",
            "imaplib",
            "smtplib",
            "telnetlib"
        ))
    );
    
    /**
     * 是否启用网络访问
     */
    private boolean networkEnabled = false;
    
    /**
     * 是否启用文件读取（只读）
     */
    private boolean fileReadEnabled = false;
    
    /**
     * 是否启用文件写入
     */
    private boolean fileWriteEnabled = false;
    
    /**
     * 最大执行时间（秒）
     */
    private int maxExecutionTimeSeconds = 30;
    
    /**
     * 最大内存使用（MB）
     */
    private long maxMemoryMB = 128;
    
    /**
     * 默认构造函数，使用安全默认配置
     */
    public PythonSecurityConfig() {
        // 默认禁用网络和文件写入
    }
    
    /**
     * 检查函数是否在黑名单中
     * 
     * @param functionName 函数名称（可以是完全限定名，如 "os.system"）
     * @return 如果在黑名单中返回 true，否则返回 false
     */
    public boolean isDangerousFunction(String functionName) {
        if (functionName == null || functionName.trim().isEmpty()) {
            return false;
        }
        
        String normalized = functionName.trim();
        
        // 直接匹配
        if (DANGEROUS_FUNCTIONS.contains(normalized)) {
            return true;
        }
        
        // 检查后缀匹配（例如 "system" 匹配 "os.system"）
        for (String dangerous : DANGEROUS_FUNCTIONS) {
            if (normalized.endsWith(dangerous)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查模块是否在白名单中
     * 
     * @param moduleName 模块名称
     * @return 如果在白名单中返回 true，否则返回 false
     */
    public boolean isSafeModule(String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            return false;
        }
        
        String normalized = moduleName.trim();
        
        // 直接匹配
        if (SAFE_MODULES.contains(normalized)) {
            return true;
        }
        
        // 检查子模块（例如 "numpy.random" 匹配 "numpy"）
        String baseModule = normalized.split("\\.")[0];
        return SAFE_MODULES.contains(baseModule);
    }
    
    /**
     * 检查模块是否需要网络权限
     * 
     * @param moduleName 模块名称
     * @return 如果需要网络权限返回 true，否则返回 false
     */
    public boolean requiresNetworkPermission(String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            return false;
        }
        
        String normalized = moduleName.trim();
        String baseModule = normalized.split("\\.")[0];
        
        return NETWORK_MODULES.contains(baseModule);
    }
    
    /**
     * 获取危险函数黑名单
     * 
     * @return 不可修改的危险函数集合
     */
    public Set<String> getDangerousFunctions() {
        return DANGEROUS_FUNCTIONS;
    }
    
    /**
     * 获取安全模块白名单
     * 
     * @return 不可修改的安全模块集合
     */
    public Set<String> getSafeModules() {
        return SAFE_MODULES;
    }
    
    /**
     * 获取需要网络权限的模块列表
     * 
     * @return 不可修改的网络模块集合
     */
    public Set<String> getNetworkModules() {
        return NETWORK_MODULES;
    }
    
    /**
     * 是否启用网络访问
     * 
     * @return true 表示启用，false 表示禁用
     */
    public boolean isNetworkEnabled() {
        return networkEnabled;
    }
    
    /**
     * 设置是否启用网络访问
     * 
     * @param networkEnabled true 表示启用，false 表示禁用
     * @return 当前配置对象（支持链式调用）
     */
    public PythonSecurityConfig setNetworkEnabled(boolean networkEnabled) {
        this.networkEnabled = networkEnabled;
        return this;
    }
    
    /**
     * 是否启用文件读取
     * 
     * @return true 表示启用，false 表示禁用
     */
    public boolean isFileReadEnabled() {
        return fileReadEnabled;
    }
    
    /**
     * 设置是否启用文件读取
     * 
     * @param fileReadEnabled true 表示启用，false 表示禁用
     * @return 当前配置对象（支持链式调用）
     */
    public PythonSecurityConfig setFileReadEnabled(boolean fileReadEnabled) {
        this.fileReadEnabled = fileReadEnabled;
        return this;
    }
    
    /**
     * 是否启用文件写入
     * 
     * @return true 表示启用，false 表示禁用
     */
    public boolean isFileWriteEnabled() {
        return fileWriteEnabled;
    }
    
    /**
     * 设置是否启用文件写入
     * 
     * @param fileWriteEnabled true 表示启用，false 表示禁用
     * @return 当前配置对象（支持链式调用）
     */
    public PythonSecurityConfig setFileWriteEnabled(boolean fileWriteEnabled) {
        this.fileWriteEnabled = fileWriteEnabled;
        return this;
    }
    
    /**
     * 获取最大执行时间（秒）
     * 
     * @return 最大执行时间（秒）
     */
    public int getMaxExecutionTimeSeconds() {
        return maxExecutionTimeSeconds;
    }
    
    /**
     * 设置最大执行时间（秒）
     * 
     * @param maxExecutionTimeSeconds 最大执行时间（秒）
     * @return 当前配置对象（支持链式调用）
     */
    public PythonSecurityConfig setMaxExecutionTimeSeconds(int maxExecutionTimeSeconds) {
        this.maxExecutionTimeSeconds = maxExecutionTimeSeconds;
        return this;
    }
    
    /**
     * 获取最大内存使用（MB）
     * 
     * @return 最大内存使用（MB）
     */
    public long getMaxMemoryMB() {
        return maxMemoryMB;
    }
    
    /**
     * 设置最大内存使用（MB）
     * 
     * @param maxMemoryMB 最大内存使用（MB）
     * @return 当前配置对象（支持链式调用）
     */
    public PythonSecurityConfig setMaxMemoryMB(long maxMemoryMB) {
        this.maxMemoryMB = maxMemoryMB;
        return this;
    }
    
    /**
     * 验证配置的有效性
     * 
     * @throws IllegalStateException 当配置无效时抛出
     */
    public void validate() {
        if (maxExecutionTimeSeconds <= 0) {
            throw new IllegalStateException("最大执行时间必须大于 0");
        }
        
        if (maxMemoryMB <= 0) {
            throw new IllegalStateException("最大内存使用必须大于 0");
        }
        
        // 文件写入启用时，必须启用文件读取
        if (fileWriteEnabled && !fileReadEnabled) {
            throw new IllegalStateException("启用文件写入时必须同时启用文件读取");
        }
    }
    
    /**
     * 创建严格安全配置（默认推荐）
     * 
     * @return 严格安全配置实例
     */
    public static PythonSecurityConfig createStrict() {
        return new PythonSecurityConfig()
                .setNetworkEnabled(false)
                .setFileReadEnabled(false)
                .setFileWriteEnabled(false)
                .setMaxExecutionTimeSeconds(30)
                .setMaxMemoryMB(128);
    }
    
    /**
     * 创建宽松安全配置（仅用于测试）
     * 
     * @return 宽松安全配置实例
     */
    public static PythonSecurityConfig createLenient() {
        return new PythonSecurityConfig()
                .setNetworkEnabled(true)
                .setFileReadEnabled(true)
                .setFileWriteEnabled(false)
                .setMaxExecutionTimeSeconds(60)
                .setMaxMemoryMB(256);
    }
}
