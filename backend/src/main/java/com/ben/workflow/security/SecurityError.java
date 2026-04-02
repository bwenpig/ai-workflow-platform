package com.ben.workflow.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Python 代码安全异常类
 * 
 * 用于标识和分类 Python 代码执行过程中的各种安全违规行为。
 * 提供详细的错误码、违规类型和上下文信息。
 * 
 * <h2>错误码分类</h2>
 * <ul>
 *   <li>E001-E099: 危险函数调用</li>
 *   <li>E100-E199: 未授权模块导入</li>
 *   <li>E200-E299: 动态代码执行</li>
 *   <li>E300-E399: 资源滥用</li>
 *   <li>E400-E499: 网络访问违规</li>
 *   <li>E500-E599: 文件系统违规</li>
 *   <li>E900-E999: 其他安全违规</li>
 * </ul>
 * 
 * <h2>使用方法</h2>
 * <pre>{@code
 * // 抛出安全异常
 * throw new SecurityError(
 *     SecurityErrorCode.DANGEROUS_FUNCTION,
 *     "检测到危险函数 eval()",
 *     "eval"
 * );
 * 
 * // 带违规列表的异常
 * List<String> violations = Arrays.asList("eval", "exec");
 * throw new SecurityError(
 *     SecurityErrorCode.DANGEROUS_FUNCTION,
 *     "多个危险函数调用",
 *     violations
 * );
 * }</pre>
 * 
 * @author 龙傲天
 * @version 1.0
 * @since 2026-04-01
 */
public class SecurityError extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码
     */
    private final SecurityErrorCode errorCode;
    
    /**
     * 违规详情列表
     */
    private final List<String> violations;
    
    /**
     * 原始代码片段（如果有）
     */
    private final String codeSnippet;
    
    /**
     * 行号（如果可确定）
     */
    private final int lineNumber;
    
    /**
     * 严重程度
     */
    private final Severity severity;
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     */
    public SecurityError(SecurityErrorCode errorCode, String message) {
        this(errorCode, message, new ArrayList<>());
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param violation 单个违规项
     */
    public SecurityError(SecurityErrorCode errorCode, String message, String violation) {
        this(errorCode, message, violation != null ? Collections.singletonList(violation) : new ArrayList<>());
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param violations 违规列表
     */
    public SecurityError(SecurityErrorCode errorCode, String message, List<String> violations) {
        this(errorCode, message, violations, null, -1, Severity.HIGH);
    }
    
    /**
     * 完整构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param violations 违规列表
     * @param codeSnippet 代码片段
     * @param lineNumber 行号
     * @param severity 严重程度
     */
    public SecurityError(SecurityErrorCode errorCode, String message, List<String> violations,
                         String codeSnippet, int lineNumber, Severity severity) {
        super(String.format("[%s] %s", errorCode.getCode(), message));
        this.errorCode = errorCode;
        this.violations = violations != null ? new ArrayList<>(violations) : new ArrayList<>();
        this.codeSnippet = codeSnippet;
        this.lineNumber = lineNumber;
        this.severity = severity != null ? severity : Severity.HIGH;
    }
    
    /**
     * 构造函数（带 cause）
     * 
     * @param errorCode 错误码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public SecurityError(SecurityErrorCode errorCode, String message, Throwable cause) {
        super(String.format("[%s] %s", errorCode.getCode(), message), cause);
        this.errorCode = errorCode;
        this.violations = new ArrayList<>();
        this.codeSnippet = null;
        this.lineNumber = -1;
        this.severity = Severity.HIGH;
    }
    
    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public SecurityErrorCode getErrorCode() {
        return errorCode;
    }
    
    /**
     * 获取违规列表
     * 
     * @return 违规列表（不可变）
     */
    public List<String> getViolations() {
        return Collections.unmodifiableList(violations);
    }
    
    /**
     * 是否包含违规
     * 
     * @return true 表示包含违规
     */
    public boolean hasViolations() {
        return !violations.isEmpty();
    }
    
    /**
     * 获取违规数量
     * 
     * @return 违规数量
     */
    public int getViolationCount() {
        return violations.size();
    }
    
    /**
     * 获取代码片段
     * 
     * @return 代码片段
     */
    public String getCodeSnippet() {
        return codeSnippet;
    }
    
    /**
     * 获取行号
     * 
     * @return 行号，-1 表示未知
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * 获取严重程度
     * 
     * @return 严重程度
     */
    public Severity getSeverity() {
        return severity;
    }
    
    /**
     * 是否为高危违规
     * 
     * @return true 表示高危
     */
    public boolean isHighSeverity() {
        return severity == Severity.HIGH;
    }
    
    /**
     * 是否为中危违规
     * 
     * @return true 表示中危
     */
    public boolean isMediumSeverity() {
        return severity == Severity.MEDIUM;
    }
    
    /**
     * 是否为低危违规
     * 
     * @return true 表示低危
     */
    public boolean isLowSeverity() {
        return severity == Severity.LOW;
    }
    
    /**
     * 添加违规项
     * 
     * @param violation 违规项
     * @return 当前异常对象（支持链式调用）
     */
    public SecurityError addViolation(String violation) {
        if (violation != null && !violation.isEmpty()) {
            this.violations.add(violation);
        }
        return this;
    }
    
    /**
     * 构建详细错误报告
     * 
     * @return 详细错误报告
     */
    public String buildReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Python 安全违规报告 ===\n");
        sb.append("错误码：").append(errorCode.getCode()).append("\n");
        sb.append("级别：").append(severity).append("\n");
        sb.append("消息：").append(getMessage()).append("\n");
        
        if (!violations.isEmpty()) {
            sb.append("违规项 (").append(violations.size()).append("):\n");
            for (int i = 0; i < violations.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(violations.get(i)).append("\n");
            }
        }
        
        if (codeSnippet != null && !codeSnippet.isEmpty()) {
            sb.append("代码片段：\n");
            sb.append("  ").append(codeSnippet.replace("\n", "\n  ")).append("\n");
        }
        
        if (lineNumber >= 0) {
            sb.append("行号：").append(lineNumber).append("\n");
        }
        
        if (getCause() != null) {
            sb.append("原因：").append(getCause().getMessage()).append("\n");
        }
        
        sb.append("========================\n");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("SecurityError{%s, severity=%s, violations=%d}",
            errorCode.getCode(), severity, violations.size());
    }
    
    /**
     * 严重程度枚举
     */
    public enum Severity {
        /**
         * 高危 - 可能导致远程代码执行、系统入侵
         */
        HIGH("高危"),
        
        /**
         * 中危 - 可能导致信息泄露、资源滥用
         */
        MEDIUM("中危"),
        
        /**
         * 低危 - 潜在风险，需要关注
         */
        LOW("低危");
        
        private final String label;
        
        Severity(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }
}

/**
 * 安全错误码枚举
 * 
 * @author 龙傲天
 * @version 1.0
 */
enum SecurityErrorCode {
    
    // ===== 危险函数调用 (E001-E099) =====
    DANGEROUS_FUNCTION_EVAL("E001", "危险函数调用", "禁止使用 eval() 执行动态代码"),
    DANGEROUS_FUNCTION_EXEC("E002", "危险函数调用", "禁止使用 exec() 执行动态代码"),
    DANGEROUS_FUNCTION_IMPORT("E003", "危险函数调用", "禁止使用 __import__() 动态导入"),
    DANGEROUS_FUNCTION_COMPILE("E004", "危险函数调用", "禁止使用 compile() 编译代码"),
    DANGEROUS_FUNCTION_OPEN("E005", "危险函数调用", "禁止使用 open() 进行文件操作"),
    DANGEROUS_FUNCTION_GETATTR("E006", "危险函数调用", "禁止使用 getattr() 访问属性"),
    DANGEROUS_FUNCTION_SETATTR("E007", "危险函数调用", "禁止使用 setattr() 设置属性"),
    DANGEROUS_FUNCTION_GLOBALS("E008", "危险函数调用", "禁止使用 globals() 访问全局变量"),
    DANGEROUS_FUNCTION_LOCALS("E009", "危险函数调用", "禁止使用 locals() 访问局部变量"),
    
    // ===== 未授权模块导入 (E100-E199) =====
    UNAUTHORIZED_IMPORT_OS("E101", "未授权模块导入", "禁止导入 os 模块（系统操作）"),
    UNAUTHORIZED_IMPORT_SYS("E102", "未授权模块导入", "禁止导入 sys 模块（解释器控制）"),
    UNAUTHORIZED_IMPORT_SUBPROCESS("E103", "未授权模块导入", "禁止导入 subprocess 模块（进程管理）"),
    UNAUTHORIZED_IMPORT_SOCKET("E104", "未授权模块导入", "禁止导入 socket 模块（网络通信）"),
    UNAUTHORIZED_IMPORT_PICKLE("E105", "未授权模块导入", "禁止导入 pickle 模块（反序列化风险）"),
    UNAUTHORIZED_IMPORT_IMPORTLIB("E106", "未授权模块导入", "禁止导入 importlib 模块（动态导入）"),
    UNAUTHORIZED_IMPORT_PLATFORM("E107", "未授权模块导入", "禁止导入 platform 模块（信息泄露）"),
    UNAUTHORIZED_IMPORT_CTYPES("E108", "未授权模块导入", "禁止导入 ctypes 模块（本地调用）"),
    
    // ===== 动态代码执行 (E200-E299) =====
    DYNAMIC_EXECUTION_EVAL("E201", "动态代码执行", "检测到 eval() 动态执行"),
    DYNAMIC_EXECUTION_EXEC("E202", "动态代码执行", "检测到 exec() 动态执行"),
    DYNAMIC_EXECUTION_COMPILE("E203", "动态代码执行", "检测到 compile() 编译执行"),
    DYNAMIC_IMPORT("E204", "动态代码执行", "检测到 __import__() 动态导入"),
    CODE_INJECTION("E205", "动态代码执行", "检测到潜在的代码注入风险"),
    
    // ===== 资源滥用 (E300-E399) =====
    RESOURCE_TIMEOUT("E301", "资源滥用", "代码执行超时"),
    RESOURCE_MEMORY("E302", "资源滥用", "内存使用超限"),
    RESOURCE_CPU("E303", "资源滥用", "CPU 使用超限"),
    RESOURCE_DISK("E304", "资源滥用", "磁盘使用超限"),
    
    // ===== 网络访问违规 (E400-E499) =====
    NETWORK_UNAUTHORIZED("E401", "网络访问违规", "未授权的网络访问"),
    NETWORK_DISABLED("E402", "网络访问违规", "网络功能已禁用"),
    NETWORK_EXTERNAL_HOST("E403", "网络访问违规", "禁止访问外部主机"),
    
    // ===== 文件系统违规 (E500-E599) =====
    FILE_READ_FORBIDDEN("E501", "文件系统违规", "禁止读取文件"),
    FILE_WRITE_FORBIDDEN("E502", "文件系统违规", "禁止写入文件"),
    FILE_DELETE_FORBIDDEN("E503", "文件系统违规", "禁止删除文件"),
    FILE_PATH_TRAVERSAL("E504", "文件系统违规", "检测到路径遍历攻击"),
    
    // ===== 其他安全违规 (E900-E999) =====
    SECURITY_ANALYSIS_FAILED("E901", "其他安全违规", "安全分析失败"),
    SECURITY_POLICY_VIOLATION("E902", "其他安全违规", "违反安全策略"),
    SECURITY_UNKNOWN("E999", "其他安全违规", "未知安全错误");
    
    private final String code;
    private final String category;
    private final String description;
    
    SecurityErrorCode(String code, String category, String description) {
        this.code = code;
        this.category = category;
        this.description = description;
    }
    
    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 获取分类
     * 
     * @return 分类
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * 获取描述
     * 
     * @return 描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据错误码查找
     * 
     * @param code 错误码
     * @return 错误码枚举，未找到返回 SECURITY_UNKNOWN
     */
    public static SecurityErrorCode fromCode(String code) {
        for (SecurityErrorCode ec : values()) {
            if (ec.code.equals(code)) {
                return ec;
            }
        }
        return SECURITY_UNKNOWN;
    }
    
    /**
     * 是否属于危险函数调用类别
     * 
     * @return true 表示属于危险函数调用
     */
    public boolean isDangerousFunction() {
        return code.startsWith("E0");
    }
    
    /**
     * 是否属于未授权导入类别
     * 
     * @return true 表示属于未授权导入
     */
    public boolean isUnauthorizedImport() {
        return code.startsWith("E1");
    }
    
    /**
     * 是否属于动态执行类别
     * 
     * @return true 表示属于动态执行
     */
    public boolean isDynamicExecution() {
        return code.startsWith("E2");
    }
    
    /**
     * 是否属于高危错误
     * 
     * @return true 表示高危
     */
    public boolean isHighRisk() {
        return code.startsWith("E0") || code.startsWith("E1") || code.startsWith("E2");
    }
}
