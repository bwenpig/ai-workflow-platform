package com.ben.workflow.security;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入检查结果
 * 
 * @author 龙傲天
 * @version 1.0
 */
public class ImportCheckResult {
    
    private final List<ImportStatement> allImports;
    private final List<ImportStatement> allowedImports;
    private final List<ImportStatement> unauthorizedImports;
    private final List<String> warnings;
    
    /**
     * 构造函数
     * 
     * @param allImports 所有导入
     * @param allowedImports 允许的导入
     * @param unauthorizedImports 未授权的导入
     * @param warnings 警告列表
     */
    public ImportCheckResult(List<ImportStatement> allImports, 
                            List<ImportStatement> allowedImports,
                            List<ImportStatement> unauthorizedImports,
                            List<String> warnings) {
        this.allImports = allImports;
        this.allowedImports = allowedImports;
        this.unauthorizedImports = unauthorizedImports;
        this.warnings = warnings;
    }
    
    /**
     * 创建空结果
     * 
     * @return 空结果
     */
    public static ImportCheckResult empty() {
        return new ImportCheckResult(
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        );
    }
    
    /**
     * 是否所有导入都授权
     * 
     * @return true 表示所有导入都授权
     */
    public boolean isAllAllowed() {
        return unauthorizedImports.isEmpty();
    }
    
    /**
     * 获取所有导入
     * 
     * @return 所有导入
     */
    public List<ImportStatement> getAllImports() {
        return allImports;
    }
    
    /**
     * 获取允许的导入
     * 
     * @return 允许的导入
     */
    public List<ImportStatement> getAllowedImports() {
        return allowedImports;
    }
    
    /**
     * 获取未授权的导入
     * 
     * @return 未授权的导入
     */
    public List<ImportStatement> getUnauthorizedImports() {
        return unauthorizedImports;
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
     * 获取导入数量
     * 
     * @return 导入数量
     */
    public int getTotalCount() {
        return allImports.size();
    }
    
    /**
     * 获取允许导入数量
     * 
     * @return 允许导入数量
     */
    public int getAllowedCount() {
        return allowedImports.size();
    }
    
    /**
     * 获取未授权导入数量
     * 
     * @return 未授权导入数量
     */
    public int getUnauthorizedCount() {
        return unauthorizedImports.size();
    }
    
    @Override
    public String toString() {
        return String.format("ImportCheckResult{total=%d, allowed=%d, unauthorized=%d}",
            getTotalCount(), allowedImports.size(), getUnauthorizedCount());
    }
}
