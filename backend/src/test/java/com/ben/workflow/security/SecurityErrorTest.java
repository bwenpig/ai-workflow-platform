package com.ben.workflow.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecurityError 单元测试
 * 
 * @author 龙傲天
 * @version 1.0
 */
@Execution(ExecutionMode.CONCURRENT)
class SecurityErrorTest {
    
    @Test
    @DisplayName("测试 SecurityError - 基本构造")
    void testSecurityError_basic() {
        SecurityError error = new SecurityError(SecurityErrorCode.DANGEROUS_FUNCTION_EVAL, "测试错误");
        
        assertEquals("E001", error.getErrorCode().getCode());
        assertTrue(error.getMessage().contains("测试错误"));
        assertTrue(error.getViolations().isEmpty());
        assertEquals(SecurityError.Severity.HIGH, error.getSeverity());
    }
    
    @Test
    @DisplayName("测试 SecurityError - 带单个违规")
    void testSecurityError_singleViolation() {
        SecurityError error = new SecurityError(
            SecurityErrorCode.DANGEROUS_FUNCTION_EXEC,
            "检测到 exec",
            "exec"
        );
        
        assertEquals(1, error.getViolationCount());
        assertEquals("exec", error.getViolations().get(0));
        assertTrue(error.hasViolations());
    }
    
    @Test
    @DisplayName("测试 SecurityError - 带多个违规")
    void testSecurityError_multipleViolations() {
        List<String> violations = Arrays.asList("eval", "exec", "__import__");
        SecurityError error = new SecurityError(
            SecurityErrorCode.DANGEROUS_FUNCTION_EVAL,
            "多个危险函数",
            violations
        );
        
        assertEquals(3, error.getViolationCount());
        assertTrue(error.hasViolations());
    }
    
    @Test
    @DisplayName("测试 SecurityError - 带代码片段")
    void testSecurityError_withCodeSnippet() {
        SecurityError error = new SecurityError(
            SecurityErrorCode.DANGEROUS_FUNCTION_EVAL,
            "检测到 eval",
            Collections.singletonList("eval"),
            "eval('1+1')",
            10,
            SecurityError.Severity.HIGH
        );
        
        assertEquals("eval('1+1')", error.getCodeSnippet());
        assertEquals(10, error.getLineNumber());
        assertTrue(error.isHighSeverity());
    }
    
    @Test
    @DisplayName("测试 SecurityError - 带 cause")
    void testSecurityError_withCause() {
        Exception cause = new RuntimeException("原始异常");
        SecurityError error = new SecurityError(
            SecurityErrorCode.SECURITY_ANALYSIS_FAILED,
            "分析失败",
            cause
        );
        
        assertEquals(cause, error.getCause());
        assertTrue(error.getMessage().contains("分析失败"));
    }
    
    @Test
    @DisplayName("测试 SecurityError - 添加违规")
    void testSecurityError_addViolation() {
        SecurityError error = new SecurityError(
            SecurityErrorCode.DANGEROUS_FUNCTION_EVAL,
            "初始错误"
        );
        
        error.addViolation("eval");
        error.addViolation("exec");
        
        assertEquals(2, error.getViolationCount());
    }
    
    @Test
    @DisplayName("测试 SecurityError - 构建报告")
    void testSecurityError_buildReport() {
        List<String> violations = Arrays.asList("eval", "exec");
        SecurityError error = new SecurityError(
            SecurityErrorCode.DANGEROUS_FUNCTION_EVAL,
            "多个危险函数",
            violations,
            "eval('1+1')",
            5,
            SecurityError.Severity.HIGH
        );
        
        String report = error.buildReport();
        
        assertNotNull(report);
        assertTrue(report.contains("安全违规报告"));
        assertTrue(report.contains("E001"));
        assertTrue(report.contains("HIGH"));
        assertTrue(report.contains("eval"));
        assertTrue(report.contains("exec"));
    }
    
    @Test
    @DisplayName("测试 SecurityError - toString")
    void testSecurityError_toString() {
        SecurityError error = new SecurityError(
            SecurityErrorCode.DANGEROUS_FUNCTION_EVAL,
            "测试",
            Arrays.asList("eval", "exec")
        );
        
        String str = error.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("SecurityError"));
        assertTrue(str.contains("E001"));
    }
    
    @Test
    @DisplayName("测试 Severity 枚举")
    void testSeverity_enum() {
        assertEquals("高危", SecurityError.Severity.HIGH.getLabel());
        assertEquals("中危", SecurityError.Severity.MEDIUM.getLabel());
        assertEquals("低危", SecurityError.Severity.LOW.getLabel());
    }
    
    @Test
    @DisplayName("测试 SecurityErrorCode - 基本属性")
    void testSecurityErrorCode_basic() {
        SecurityErrorCode code = SecurityErrorCode.DANGEROUS_FUNCTION_EVAL;
        
        assertEquals("E001", code.getCode());
        assertEquals("危险函数调用", code.getCategory());
        assertNotNull(code.getDescription());
    }
    
    @Test
    @DisplayName("测试 SecurityErrorCode - fromCode")
    void testSecurityErrorCode_fromCode() {
        assertEquals(SecurityErrorCode.DANGEROUS_FUNCTION_EVAL, SecurityErrorCode.fromCode("E001"));
        assertEquals(SecurityErrorCode.UNAUTHORIZED_IMPORT_OS, SecurityErrorCode.fromCode("E101"));
        assertEquals(SecurityErrorCode.SECURITY_UNKNOWN, SecurityErrorCode.fromCode("E999"));
        assertEquals(SecurityErrorCode.SECURITY_UNKNOWN, SecurityErrorCode.fromCode("INVALID"));
    }
    
    @Test
    @DisplayName("测试 SecurityErrorCode - 分类判断")
    void testSecurityErrorCode_categories() {
        assertTrue(SecurityErrorCode.DANGEROUS_FUNCTION_EVAL.isDangerousFunction());
        assertFalse(SecurityErrorCode.DANGEROUS_FUNCTION_EVAL.isUnauthorizedImport());
        assertFalse(SecurityErrorCode.DANGEROUS_FUNCTION_EVAL.isDynamicExecution());
        assertTrue(SecurityErrorCode.DANGEROUS_FUNCTION_EVAL.isHighRisk());
        
        assertTrue(SecurityErrorCode.UNAUTHORIZED_IMPORT_OS.isUnauthorizedImport());
        assertTrue(SecurityErrorCode.DYNAMIC_EXECUTION_EVAL.isDynamicExecution());
    }
    
    @Test
    @DisplayName("测试 SecurityErrorCode - 所有错误码")
    void testSecurityErrorCode_allCodes() {
        // 危险函数调用 (E001-E009)
        assertTrue(SecurityErrorCode.DANGEROUS_FUNCTION_EVAL.getCode().startsWith("E0"));
        
        // 未授权导入 (E101-E108)
        assertTrue(SecurityErrorCode.UNAUTHORIZED_IMPORT_OS.getCode().startsWith("E1"));
        
        // 动态执行 (E201-E205)
        assertTrue(SecurityErrorCode.DYNAMIC_EXECUTION_EVAL.getCode().startsWith("E2"));
        
        // 资源滥用 (E301-E304)
        assertTrue(SecurityErrorCode.RESOURCE_TIMEOUT.getCode().startsWith("E3"));
        
        // 网络访问 (E401-E403)
        assertTrue(SecurityErrorCode.NETWORK_UNAUTHORIZED.getCode().startsWith("E4"));
        
        // 文件系统 (E501-E504)
        assertTrue(SecurityErrorCode.FILE_READ_FORBIDDEN.getCode().startsWith("E5"));
        
        // 其他 (E901-E999)
        assertTrue(SecurityErrorCode.SECURITY_ANALYSIS_FAILED.getCode().startsWith("E9"));
    }
    
    @Test
    @DisplayName("测试 SecurityError - 中危严重程度")
    void testSecurityError_mediumSeverity() {
        SecurityError error = new SecurityError(
            SecurityErrorCode.UNAUTHORIZED_IMPORT_OS,
            "未授权导入",
            Collections.singletonList("os"),
            null,
            -1,
            SecurityError.Severity.MEDIUM
        );
        
        assertTrue(error.isMediumSeverity());
        assertFalse(error.isHighSeverity());
    }
    
    @Test
    @DisplayName("测试 SecurityError - 低危严重程度")
    void testSecurityError_lowSeverity() {
        SecurityError error = new SecurityError(
            SecurityErrorCode.SECURITY_UNKNOWN,
            "未知错误",
            Collections.emptyList(),
            null,
            -1,
            SecurityError.Severity.LOW
        );
        
        assertTrue(error.isLowSeverity());
    }
    
    @Test
    @DisplayName("测试 SecurityError - null 违规列表处理")
    void testSecurityError_nullViolations() {
        SecurityError error = new SecurityError(
            SecurityErrorCode.DANGEROUS_FUNCTION_EVAL,
            "错误",
            (List<String>) null
        );
        
        assertNotNull(error.getViolations());
        assertTrue(error.getViolations().isEmpty());
        assertFalse(error.hasViolations());
    }
    
    @Test
    @DisplayName("测试 SecurityError - 违规列表不可变")
    void testSecurityError_violationsUnmodifiable() {
        List<String> violations = Arrays.asList("eval", "exec");
        SecurityError error = new SecurityError(
            SecurityErrorCode.DANGEROUS_FUNCTION_EVAL,
            "错误",
            violations
        );
        
        assertThrows(UnsupportedOperationException.class, () -> {
            error.getViolations().add("compile");
        });
    }
    
    @Test
    @DisplayName("测试 SecurityErrorCode - 所有高危错误码")
    void testSecurityErrorCode_allHighRisk() {
        // E0xx, E1xx, E2xx 都是高危
        assertTrue(SecurityErrorCode.DANGEROUS_FUNCTION_EVAL.isHighRisk());
        assertTrue(SecurityErrorCode.DANGEROUS_FUNCTION_EXEC.isHighRisk());
        assertTrue(SecurityErrorCode.UNAUTHORIZED_IMPORT_OS.isHighRisk());
        assertTrue(SecurityErrorCode.UNAUTHORIZED_IMPORT_SYS.isHighRisk());
        assertTrue(SecurityErrorCode.DYNAMIC_EXECUTION_EVAL.isHighRisk());
        assertTrue(SecurityErrorCode.DYNAMIC_EXECUTION_EXEC.isHighRisk());
        
        // E3xx+ 不是高危
        assertFalse(SecurityErrorCode.RESOURCE_TIMEOUT.isHighRisk());
        assertFalse(SecurityErrorCode.NETWORK_UNAUTHORIZED.isHighRisk());
        assertFalse(SecurityErrorCode.FILE_READ_FORBIDDEN.isHighRisk());
    }
}
