package com.ben.workflow.security;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全违规异常
 * 
 * @author 龙傲天
 * @version 2.0
 */
public class SecurityViolationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final List<String> violations;
    
    /**
     * 构造函数
     * 
     * @param message 消息
     */
    public SecurityViolationException(String message) {
        super(message);
        this.violations = new ArrayList<>();
    }
    
    /**
     * 构造函数
     * 
     * @param message 消息
     * @param violations 违规列表
     */
    public SecurityViolationException(String message, List<String> violations) {
        super(message);
        this.violations = violations != null ? new ArrayList<>(violations) : new ArrayList<>();
    }
    
    /**
     * 获取违规列表
     * 
     * @return 违规列表（不可变）
     */
    public List<String> getViolations() {
        return new ArrayList<>(violations);
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
    
    @Override
    public String toString() {
        return String.format("SecurityViolationException{%s, violations=%d}", getMessage(), violations.size());
    }
}
