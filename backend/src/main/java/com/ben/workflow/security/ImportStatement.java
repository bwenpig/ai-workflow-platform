package com.ben.workflow.security;

/**
 * 导入语句表示
 * 
 * @author 龙傲天
 * @version 1.0
 */
public class ImportStatement {
    
    private final String moduleName;
    private final int position;
    private final String importType;
    private final String alias;
    
    /**
     * 构造函数
     * 
     * @param moduleName 模块名称
     * @param position 在代码中的位置
     * @param importType 导入类型（"import" 或 "from"）
     */
    public ImportStatement(String moduleName, int position, String importType) {
        this(moduleName, position, importType, null);
    }
    
    /**
     * 构造函数
     * 
     * @param moduleName 模块名称
     * @param position 在代码中的位置
     * @param importType 导入类型（"import" 或 "from"）
     * @param alias 别名（如果有）
     */
    public ImportStatement(String moduleName, int position, String importType, String alias) {
        this.moduleName = moduleName;
        this.position = position;
        this.importType = importType;
        this.alias = alias;
    }
    
    /**
     * 获取模块名称
     * 
     * @return 模块名称
     */
    public String getModuleName() {
        return moduleName;
    }
    
    /**
     * 获取位置
     * 
     * @return 在代码中的位置
     */
    public int getPosition() {
        return position;
    }
    
    /**
     * 获取导入类型
     * 
     * @return 导入类型
     */
    public String getImportType() {
        return importType;
    }
    
    /**
     * 获取别名
     * 
     * @return 别名
     */
    public String getAlias() {
        return alias;
    }
    
    /**
     * 是否使用了别名
     * 
     * @return true 表示使用了别名
     */
    public boolean hasAlias() {
        return alias != null && !alias.isEmpty();
    }
    
    @Override
    public String toString() {
        if (hasAlias()) {
            return importType + " " + moduleName + " as " + alias;
        }
        return importType + " " + moduleName;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ImportStatement)) return false;
        ImportStatement other = (ImportStatement) obj;
        return moduleName.equals(other.moduleName);
    }
    
    @Override
    public int hashCode() {
        return moduleName.hashCode();
    }
}
