package com.ben.workflow.security;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全分析结果
 * 
 * @author 龙傲天
 * @version 2.0
 */
public class SecurityAnalysisResult {
    
    private final boolean safe;
    private final List<String> violations;
    private final List<String> warnings;
    private final List<String> imports;
    private final String message;
    private final int detectedFunctionsCount;
    
    /**
     * 构造函数
     * 
     * @param safe 是否安全
     * @param violations 违规列表
     * @param warnings 警告列表
     * @param imports 导入列表
     * @param detectedFunctionsCount 检测到的函数数量
     */
    public SecurityAnalysisResult(boolean safe, List<String> violations, List<String> warnings, 
                                   List<String> imports, int detectedFunctionsCount) {
        this.safe = safe;
        this.violations = violations;
        this.warnings = warnings;
        this.imports = imports;
        this.detectedFunctionsCount = detectedFunctionsCount;
        this.message = safe ? "代码安全检查通过" : "代码安全检查失败";
    }
    
    /**
     * 创建安全结果
     * 
     * @param message 消息
     * @return 安全结果
     */
    public static SecurityAnalysisResult safe(String message) {
        return new SecurityAnalysisResult(true, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0);
    }
    
    /**
     * 是否安全
     * 
     * @return true 表示安全
     */
    public boolean isSafe() {
        return safe;
    }
    
    /**
     * 获取违规列表
     * 
     * @return 违规列表
     */
    public List<String> getViolations() {
        return violations;
    }
    
    /**
     * 获取警告列表
     * 
     * @return 警告列表
     */
    public List<String> getWarnings() {
        return warnings;
    }
    
    /**
     * 获取导入列表
     * 
     * @return 导入列表
     */
    public List<String> getImports() {
        return imports;
    }
    
    /**
     * 获取消息
     * 
     * @return 消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取检测到的函数数量
     * 
     * @return 函数数量
     */
    public int getDetectedFunctionsCount() {
        return detectedFunctionsCount;
    }
    
    @Override
    public String toString() {
        return String.format("SecurityAnalysisResult{safe=%s, violations=%d, warnings=%d, imports=%d, functions=%d}",
            safe, violations.size(), warnings.size(), imports.size(), detectedFunctionsCount);
    }
}
