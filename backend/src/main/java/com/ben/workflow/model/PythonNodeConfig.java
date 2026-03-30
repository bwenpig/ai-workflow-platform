package com.ben.workflow.model;

import java.util.List;
import java.util.Map;

/**
 * Python 脚本节点配置
 */
public class PythonNodeConfig {
    private String script;              // Python 代码
    private String scriptPath;          // 脚本文件路径 (可选)
    private Integer timeout = 30;       // 超时时间 (秒)
    private List<String> requirements;  // Python 依赖包
    private String pythonVersion = "3.9"; // Python 版本
    private Map<String, String> env;    // 环境变量
    private Boolean networkEnabled = false; // 是否允许网络访问
    
    public String getScript() { return script; }
    public void setScript(String script) { this.script = script; }
    
    public String getScriptPath() { return scriptPath; }
    public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }
    
    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }
    
    public List<String> getRequirements() { return requirements; }
    public void setRequirements(List<String> requirements) { this.requirements = requirements; }
    
    public String getPythonVersion() { return pythonVersion; }
    public void setPythonVersion(String pythonVersion) { this.pythonVersion = pythonVersion; }
    
    public Map<String, String> getEnv() { return env; }
    public void setEnv(Map<String, String> env) { this.env = env; }
    
    public Boolean getNetworkEnabled() { return networkEnabled; }
    public void setNetworkEnabled(Boolean networkEnabled) { this.networkEnabled = networkEnabled; }
}
