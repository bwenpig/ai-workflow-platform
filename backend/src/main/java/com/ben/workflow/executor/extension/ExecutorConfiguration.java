package com.ben.workflow.executor.extension;

import java.util.Map;

/**
 * Executor 配置
 * 传递给 Executor 的初始化配置参数
 */
public class ExecutorConfiguration {

    private Map<String, Object> params;

    public ExecutorConfiguration() {}

    private ExecutorConfiguration(Builder builder) {
        this.params = builder.params;
    }

    public static Builder builder() { return new Builder(); }

    // Getters & Setters
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    /**
     * 获取字符串参数
     */
    public String getString(String key, String defaultValue) {
        if (params == null) return defaultValue;
        Object v = params.get(key);
        return v != null ? v.toString() : defaultValue;
    }

    /**
     * 获取整数参数
     */
    public int getInt(String key, int defaultValue) {
        if (params == null) return defaultValue;
        Object v = params.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取布尔参数
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        if (params == null) return defaultValue;
        Object v = params.get(key);
        if (v == null) return defaultValue;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    public static class Builder {
        private Map<String, Object> params;

        public Builder params(Map<String, Object> params) { this.params = params; return this; }
        public ExecutorConfiguration build() { return new ExecutorConfiguration(this); }
    }
}
