package com.ben.workflow.spi;

import java.util.Map;

/**
 * 模型执行结果
 */
public class ModelExecutionResult {
    private boolean success;
    private Map<String, Object> data;
    private String error;
    private String logs;

    public ModelExecutionResult() {}

    public static ModelExecutionResult success(Map<String, Object> data) {
        ModelExecutionResult result = new ModelExecutionResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static ModelExecutionResult failure(String error) {
        ModelExecutionResult result = new ModelExecutionResult();
        result.setSuccess(false);
        result.setError(error);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }
}
