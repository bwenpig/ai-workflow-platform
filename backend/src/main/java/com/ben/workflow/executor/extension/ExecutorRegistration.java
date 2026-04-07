package com.ben.workflow.executor.extension;

import java.util.Map;

/**
 * Executor 注册信息
 * 用于 YAML 配置加载和 REST API 注册
 */
public class ExecutorRegistration {

    private String type;
    private String className;
    private boolean enabled = true;
    private Map<String, Object> config;
    private ExecutorMetadata metadata;

    public ExecutorRegistration() {}

    private ExecutorRegistration(Builder builder) {
        this.type = builder.type;
        this.className = builder.className;
        this.enabled = builder.enabled;
        this.config = builder.config;
        this.metadata = builder.metadata;
    }

    public static Builder builder() { return new Builder(); }

    // Getters & Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public ExecutorMetadata getMetadata() { return metadata; }
    public void setMetadata(ExecutorMetadata metadata) { this.metadata = metadata; }

    public static class Builder {
        private String type;
        private String className;
        private boolean enabled = true;
        private Map<String, Object> config;
        private ExecutorMetadata metadata;

        public Builder type(String type) { this.type = type; return this; }
        public Builder className(String className) { this.className = className; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder config(Map<String, Object> config) { this.config = config; return this; }
        public Builder metadata(ExecutorMetadata metadata) { this.metadata = metadata; return this; }
        public ExecutorRegistration build() { return new ExecutorRegistration(this); }
    }
}
