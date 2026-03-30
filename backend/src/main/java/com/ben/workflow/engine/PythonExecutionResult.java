package com.ben.workflow.engine;

import java.util.Map;

/**
 * Python 脚本执行结果
 */
public class PythonExecutionResult {
    
    private boolean success;
    private Map<String, Object> outputs;
    private String logs;
    private String error;
    private Long duration;
    
    public PythonExecutionResult() {}
    
    public PythonExecutionResult(boolean success, Map<String, Object> outputs, String logs, String error, Long duration) {
        this.success = success;
        this.outputs = outputs;
        this.logs = logs;
        this.error = error;
        this.duration = duration;
    }
    
    public static PythonExecutionResult success(Map<String, Object> outputs, String logs) {
        return new PythonExecutionResult(true, outputs, logs, null, null);
    }
    
    public static PythonExecutionResult failure(String error) {
        return new PythonExecutionResult(false, null, null, error, null);
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Map<String, Object> getOutputs() { return outputs; }
    public void setOutputs(Map<String, Object> outputs) { this.outputs = outputs; }
    public String getLogs() { return logs; }
    public void setLogs(String logs) { this.logs = logs; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
}
