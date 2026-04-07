package com.ben.workflow.executor.extension;

import java.util.Map;

/**
 * 参数 Schema 定义
 * 描述执行器的输入/输出参数
 */
public class ParameterSchema {

    private String name;
    private String type;
    private String label;
    private String description;
    private boolean required;
    private Object defaultValue;
    private Map<String, Object> constraints;
    private Map<String, Object> options;

    public ParameterSchema() {}

    private ParameterSchema(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.label = builder.label;
        this.description = builder.description;
        this.required = builder.required;
        this.defaultValue = builder.defaultValue;
        this.constraints = builder.constraints;
        this.options = builder.options;
    }

    public static Builder builder() { return new Builder(); }

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public Object getDefaultValue() { return defaultValue; }
    public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
    public Map<String, Object> getConstraints() { return constraints; }
    public void setConstraints(Map<String, Object> constraints) { this.constraints = constraints; }
    public Map<String, Object> getOptions() { return options; }
    public void setOptions(Map<String, Object> options) { this.options = options; }

    public static class Builder {
        private String name;
        private String type;
        private String label;
        private String description;
        private boolean required;
        private Object defaultValue;
        private Map<String, Object> constraints;
        private Map<String, Object> options;

        public Builder name(String name) { this.name = name; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder required(boolean required) { this.required = required; return this; }
        public Builder defaultValue(Object defaultValue) { this.defaultValue = defaultValue; return this; }
        public Builder constraints(Map<String, Object> constraints) { this.constraints = constraints; return this; }
        public Builder options(Map<String, Object> options) { this.options = options; return this; }
        public ParameterSchema build() { return new ParameterSchema(this); }
    }
}
