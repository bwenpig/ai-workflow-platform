package com.ben.workflow.engine;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.model.PythonNodeConfig;
import com.ben.workflow.spi.NodeComponent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Python 脚本执行器
 */
@NodeComponent(value = "python_script", name = "Python 脚本", description = "执行 Python 脚本节点")
public class PythonScriptExecutor implements NodeExecutor {
    
    private static final String PYTHON_EXECUTABLE = "python3";
    private static final int DEFAULT_TIMEOUT = 30;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Override
    public String getType() {
        return "python_script";
    }
    
    @Override
    public String getName() {
        return "Python 脚本";
    }
    
    @Override
    public String getDescription() {
        return "执行 Python 脚本节点";
    }
    
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) throws Exception {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            Map<String, Object> config = context != null ? context.getInputs() : null;
            String script = config != null ? (String) config.get("script") : "";
            Integer timeout = config != null ? (Integer) config.get("timeout") : null;
            List<String> requirements = config != null ? (List<String>) config.get("requirements") : null;
            Map<String, Object> env = config != null ? (Map<String, Object>) config.get("env") : null;
            
            PythonNodeConfig nodeConfig = new PythonNodeConfig();
            nodeConfig.setScript(script);
            nodeConfig.setTimeout(timeout);
            nodeConfig.setRequirements(requirements);
            if (env != null) {
                Map<String, String> envMap = new HashMap<>();
                for (Map.Entry<String, Object> entry : env.entrySet()) {
                    envMap.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
                }
                nodeConfig.setEnv(envMap);
            }
            
            PythonExecutionResult result = execute(script, context != null ? context.getInputs() : new HashMap<>(), nodeConfig);
            
            LocalDateTime endTime = LocalDateTime.now();
            if (!result.isSuccess()) {
                return NodeExecutionResult.failed(context != null ? context.getNodeId() : "unknown", result.getError(), null, startTime, endTime);
            }
            
            return NodeExecutionResult.success(context != null ? context.getNodeId() : "unknown", result.getOutputs(), startTime, endTime);
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.failed(context != null ? context.getNodeId() : "unknown", e, startTime, endTime);
        }
    }
    
    public PythonExecutionResult execute(String script, Map<String, Object> inputs, PythonNodeConfig config) {
        if (inputs == null) {
            inputs = new HashMap<>();
        }
        System.out.println("开始执行 Python 脚本，inputs=" + inputs.keySet());
        
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("python_script_");
            
            String inputsJson = toJson(inputs);
            Path inputsFile = tempDir.resolve("inputs.json");
            Files.writeString(inputsFile, inputsJson);
            
            Path scriptFile = tempDir.resolve("script.py");
            Files.writeString(scriptFile, wrapScript(script));
            
            if (config != null && config.getRequirements() != null && !config.getRequirements().isEmpty()) {
                installRequirements(tempDir, config.getRequirements());
            }
            
            int timeout = config != null && config.getTimeout() != null ? config.getTimeout() : DEFAULT_TIMEOUT;
            
            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXECUTABLE, scriptFile.toString(), inputsFile.toString());
            pb.directory(tempDir.toFile());
            pb.redirectErrorStream(true);
            
            if (config != null && config.getEnv() != null) {
                pb.environment().putAll(config.getEnv());
            }
            
            Process process = pb.start();
            boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
            
            String output = readStream(process.getInputStream());
            
            if (!completed) {
                process.destroyForcibly();
                return PythonExecutionResult.failure("脚本执行超时 (" + timeout + "秒)");
            }
            
            if (process.exitValue() != 0) {
                return PythonExecutionResult.failure("脚本执行失败：" + output);
            }
            
            Path outputFile = tempDir.resolve("outputs.json");
            String outputsJson = Files.exists(outputFile) ? Files.readString(outputFile) : "{}";
            Map<String, Object> outputs = toMap(outputsJson);
            
            return PythonExecutionResult.success(outputs, output);
            
        } catch (Exception e) {
            System.err.println("Python 脚本执行异常：" + e.getMessage());
            e.printStackTrace();
            return PythonExecutionResult.failure("执行异常：" + e.getMessage());
        } finally {
            if (tempDir != null) {
                cleanup(tempDir);
            }
        }
    }
    
    private String wrapScript(String script) {
        return """
import json
import sys
import os

# 读取输入
inputs_file = sys.argv[1] if len(sys.argv) > 1 else 'inputs.json'
try:
    with open(inputs_file, 'r', encoding='utf-8') as f:
        inputs = json.load(f)
except:
    inputs = {}

# 执行用户脚本
outputs = {}
try:
%s

except Exception as e:
    import traceback
    error_info = {'error': str(e), 'traceback': traceback.format_exc()}
    with open('outputs.json', 'w', encoding='utf-8') as f:
        json.dump({'_error': error_info}, f, ensure_ascii=False, indent=2)
    sys.exit(1)

# 写入输出
with open('outputs.json', 'w', encoding='utf-8') as f:
    json.dump(outputs, f, ensure_ascii=False, indent=2)
sys.exit(0)
""".formatted(script.indent(4));
    }
    
    private void installRequirements(Path tempDir, List<String> requirements) throws Exception {
        System.out.println("安装 Python 依赖：" + requirements);
        
        Path requirementsFile = tempDir.resolve("requirements.txt");
        Files.writeString(requirementsFile, String.join("\n", requirements));
        
        ProcessBuilder pb = new ProcessBuilder(PYTHON_EXECUTABLE, "-m", "pip", "install", "-r", requirementsFile.toString(), "-t", tempDir.resolve("venv").toString(), "--quiet");
        pb.directory(tempDir.toFile());
        
        Process process = pb.start();
        boolean completed = process.waitFor(60, TimeUnit.SECONDS);
        
        if (!completed) {
            process.destroyForcibly();
            System.out.println("依赖安装超时");
        }
        
        if (process.exitValue() != 0) {
            String error = readStream(process.getInputStream());
            System.out.println("依赖安装失败：" + error);
        }
    }
    
    private String readStream(InputStream stream) throws IOException {
        return new String(stream.readAllBytes());
    }
    
    private String toJson(Object obj) {
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        }
        return obj.toString();
    }
    
    private String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Map) {
                sb.append(mapToJson((Map<?, ?>) value));
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return new HashMap<>();
            }
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            System.out.println("JSON 解析失败：" + e.getMessage());
            return new HashMap<>();
        }
    }
    
    private void cleanup(Path tempDir) {
        try {
            Files.walk(tempDir).sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    System.out.println("清理临时文件失败：" + path);
                }
            });
        } catch (IOException e) {
            System.out.println("清理临时目录失败：" + e.getMessage());
        }
    }
}
