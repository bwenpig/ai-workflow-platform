package com.ben.workflow.executor.extension;

import java.util.List;
import java.util.Map;

/**
 * Executor 元数据
 * 描述一个执行器的基本信息、参数 schema 和能力声明
 */
public class ExecutorMetadata {

    private String type;
    private String name;
    private String description;
    private String category;
    private String icon;
    private List<ParameterSchema> inputParams;
    private List<ParameterSchema> outputParams;
    private Map<String, Object> capabilities;
    private boolean experimental;
    private String version;

    public ExecutorMetadata() {}

    private ExecutorMetadata(Builder builder) {
        this.type = builder.type;
        this.name = builder.name;
        this.description = builder.description;
        this.category = builder.category;
        this.icon = builder.icon;
        this.inputParams = builder.inputParams;
        this.outputParams = builder.outputParams;
        this.capabilities = builder.capabilities;
        this.experimental = builder.experimental;
        this.version = builder.version;
    }

    public static Builder builder() { return new Builder(); }

    // Getters & Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public List<ParameterSchema> getInputParams() { return inputParams; }
    public void setInputParams(List<ParameterSchema> inputParams) { this.inputParams = inputParams; }
    public List<ParameterSchema> getOutputParams() { return outputParams; }
    public void setOutputParams(List<ParameterSchema> outputParams) { this.outputParams = outputParams; }
    public Map<String, Object> getCapabilities() { return capabilities; }
    public void setCapabilities(Map<String, Object> capabilities) { this.capabilities = capabilities; }
    public boolean isExperimental() { return experimental; }
    public void setExperimental(boolean experimental) { this.experimental = experimental; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public static class Builder {
        private String type;
        private String name;
        private String description;
        private String category;
        private String icon;
        private List<ParameterSchema> inputParams;
        private List<ParameterSchema> outputParams;
        private Map<String, Object> capabilities;
        private boolean experimental;
        private String version;

        public Builder type(String type) { this.type = type; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder icon(String icon) { this.icon = icon; return this; }
        public Builder inputParams(List<ParameterSchema> inputParams) { this.inputParams = inputParams; return this; }
        public Builder outputParams(List<ParameterSchema> outputParams) { this.outputParams = outputParams; return this; }
        public Builder capabilities(Map<String, Object> capabilities) { this.capabilities = capabilities; return this; }
        public Builder experimental(boolean experimental) { this.experimental = experimental; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public ExecutorMetadata build() { return new ExecutorMetadata(this); }
    }
}
