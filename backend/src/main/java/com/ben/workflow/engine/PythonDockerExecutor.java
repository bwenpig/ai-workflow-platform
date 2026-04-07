package com.ben.workflow.engine;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.model.PythonNodeConfig;
import com.ben.workflow.security.*;
import com.ben.workflow.spi.NodeComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Python Docker 沙箱执行器
 * <p>
 * 使用 Docker 容器隔离执行用户提供的 Python 脚本，提供：
 * - 容器级资源隔离（内存、CPU）
 * - 网络隔离（默认禁用）
 * - 文件系统隔离（只读 + 临时目录）
 * - 超时控制
 * - 日志收集
 * <p>
 * 脚本包装和临时目录清理委托给 {@link PythonScriptUtils}。
 *
 * @author 龙傲天
 * @version 1.1
 */
@NodeComponent(value = "python_docker", name = "Python Docker 沙箱", description = "使用 Docker 沙箱执行 Python 脚本，提供安全隔离的执行环境")
public class PythonDockerExecutor implements NodeExecutor {
    
    private static final String DEFAULT_IMAGE = "python:3.11-slim";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final long DEFAULT_MEMORY_MB = 128;
    private static final double DEFAULT_CPU_QUOTA = 0.5;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final DockerClient dockerClient;
    private final String dockerImage;
    
    /** 安全配置（严格模式） */
    private final PythonSecurityConfig securityConfig;
    /** 安全分析器 */
    private final PythonSecurityAnalyzer securityAnalyzer;
    /** 运行时拦截器 */
    private final RuntimeInterceptor runtimeInterceptor;
    
    /** 默认构造函数，使用本地 Docker 守护进程和严格安全配置 */
    public PythonDockerExecutor() {
        this(DEFAULT_IMAGE);
    }
    
    /**
     * 构造函数，可指定 Docker 镜像
     * @param dockerImage Docker 镜像名称
     */
    public PythonDockerExecutor(String dockerImage) {
        this.dockerImage = dockerImage;
        
        this.securityConfig = PythonSecurityConfig.createStrict();
        this.securityAnalyzer = new PythonSecurityAnalyzer(securityConfig);
        this.runtimeInterceptor = new RuntimeInterceptor(securityConfig);
        
        var dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        var dockerHttpClient = new OkDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig.getDockerHost())
                .build();
        
        this.dockerClient = DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
        
        System.out.println("[PythonDockerExecutor] 初始化完成，镜像：" + dockerImage + ", Docker Host: " + dockerClientConfig.getDockerHost());
        System.out.println("[PythonDockerExecutor] 安全模块已启用：AST 分析 + 运行时拦截");
    }
    
    @Override
    public String getType() {
        return "python_docker";
    }
    
    @Override
    public String getName() {
        return "Python Docker 沙箱";
    }
    
    @Override
    public String getDescription() {
        return "使用 Docker 沙箱执行 Python 脚本，提供安全隔离的执行环境";
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
    
    /**
     * 执行 Python 脚本
     */
    public PythonExecutionResult execute(String script, Map<String, Object> inputs, PythonNodeConfig config) {
        if (inputs == null) {
            inputs = new HashMap<>();
        }
        
        System.out.println("[PythonDockerExecutor] 开始执行 Python 脚本，inputs=" + inputs.keySet());
        
        // ===== 安全层 1: 执行前 AST 分析 =====
        System.out.println("[PythonDockerExecutor] 执行安全分析...");
        SecurityAnalysisResult analysisResult = securityAnalyzer.analyze(script);
        if (!analysisResult.isSafe()) {
            System.err.println("[PythonDockerExecutor] 安全检查失败：" + analysisResult.getViolations());
            return PythonExecutionResult.failure("安全校验失败：" + String.join(", ", analysisResult.getViolations()));
        }
        System.out.println("[PythonDockerExecutor] 安全检查通过，发现 " + analysisResult.getImports().size() + " 个导入");
        
        // ===== 安全层 2: 运行时拦截包装 =====
        String safeScript;
        try {
            safeScript = runtimeInterceptor.wrapUserCode(script);
            System.out.println("[PythonDockerExecutor] 运行时拦截层已注入");
        } catch (SecurityViolationException e) {
            System.err.println("[PythonDockerExecutor] 安全包装失败：" + e.getMessage());
            return PythonExecutionResult.failure("安全包装失败：" + e.getMessage());
        }
        
        String containerId = null;
        Path tempDir = null;
        
        try {
            tempDir = Files.createTempDirectory("python_docker_");
            
            String inputsJson = OBJECT_MAPPER.writeValueAsString(inputs);
            Path inputsFile = tempDir.resolve("inputs.json");
            Files.writeString(inputsFile, inputsJson);
            
            // 使用 PythonScriptUtils.wrapScript 统一包装脚本
            Path scriptFile = tempDir.resolve("script.py");
            Files.writeString(scriptFile, PythonScriptUtils.wrapScript(safeScript));
            
            if (config != null && config.getRequirements() != null && !config.getRequirements().isEmpty()) {
                Path requirementsFile = tempDir.resolve("requirements.txt");
                Files.writeString(requirementsFile, String.join("\n", config.getRequirements()));
            }
            
            containerId = createContainer(tempDir, config);
            System.out.println("[PythonDockerExecutor] 容器创建成功：" + containerId);
            
            int timeout = config != null && config.getTimeout() != null ? config.getTimeout() : DEFAULT_TIMEOUT_SECONDS;
            String logs = executeContainer(containerId, timeout);
            
            Path outputFile = tempDir.resolve("outputs.json");
            Map<String, Object> outputs = new HashMap<>();
            if (Files.exists(outputFile)) {
                String outputsJson = Files.readString(outputFile);
                outputs = OBJECT_MAPPER.readValue(outputsJson, Map.class);
            }
            
            System.out.println("[PythonDockerExecutor] 执行成功，输出：" + outputs.keySet());
            return PythonExecutionResult.success(outputs, logs);
            
        } catch (Exception e) {
            System.err.println("[PythonDockerExecutor] 执行异常：" + e.getMessage());
            e.printStackTrace();
            return PythonExecutionResult.failure("执行异常：" + e.getMessage());
        } finally {
            if (containerId != null) {
                cleanupContainer(containerId);
            }
            // 使用 PythonScriptUtils.cleanupTempDir 统一清理
            PythonScriptUtils.cleanupTempDir(tempDir);
        }
    }
    
    /**
     * 创建 Docker 容器
     */
    private String createContainer(Path workDir, PythonNodeConfig config) throws IOException {
        int timeout = config != null && config.getTimeout() != null ? config.getTimeout() : DEFAULT_TIMEOUT_SECONDS;
        long memoryMb = config != null && config.getMemoryLimit() != null ? config.getMemoryLimit() : DEFAULT_MEMORY_MB;
        double cpuCores = config != null && config.getCpuLimit() != null ? config.getCpuLimit() : DEFAULT_CPU_QUOTA;
        
        HostConfig hostConfig = new HostConfig()
                .withMemory(memoryMb * 1024 * 1024)
                .withMemorySwap(memoryMb * 1024 * 1024)
                .withCpuQuota((long) (cpuCores * 100000))
                .withNetworkMode("none")
                .withReadonlyRootfs(true)
                .withTmpFs(Map.of("/tmp", "rw,noexec,nosuid,size=64m"));
        
        Volume workVolume = new Volume("/sandbox");
        
        CreateContainerCmd createCmd = dockerClient.createContainerCmd(dockerImage)
                .withHostConfig(hostConfig)
                .withVolumes(workVolume)
                .withBinds(new Bind(workDir.toString(), workVolume))
                .withWorkingDir("/sandbox")
                .withCmd("python3", "script.py", "inputs.json")
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(false);
        
        if (config != null && config.getEnv() != null) {
            List<String> envVars = new ArrayList<>();
            for (Map.Entry<String, String> entry : config.getEnv().entrySet()) {
                envVars.add(entry.getKey() + "=" + entry.getValue());
            }
            createCmd.withEnv(envVars);
        }
        
        var createResponse = createCmd.exec();
        return createResponse.getId();
    }
    
    /**
     * 执行容器并收集日志
     */
    private String executeContainer(String containerId, int timeoutSeconds) throws Exception {
        dockerClient.startContainerCmd(containerId).exec();
        System.out.println("[PythonDockerExecutor] 容器已启动");
        
        long startTime = System.currentTimeMillis();
        while (true) {
            var inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
            if (!Boolean.TRUE.equals(inspectResponse.getState().getRunning())) {
                break;
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > timeoutSeconds * 1000L) {
                System.out.println("[PythonDockerExecutor] 执行超时，强制停止");
                dockerClient.stopContainerCmd(containerId).withTimeout(0).exec();
                throw new TimeoutException("脚本执行超时 (" + timeoutSeconds + "秒)");
            }
            
            Thread.sleep(500);
        }
        
        var inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
        Integer exitCode = inspectResponse.getState().getExitCode();
        
        String logs = collectContainerLogs(containerId);
        
        if (exitCode != null && exitCode != 0) {
            throw new RuntimeException("脚本执行失败 (退出码：" + exitCode + "): " + logs);
        }
        
        System.out.println("[PythonDockerExecutor] 执行完成，日志：" + logs.length() + " 字节");
        return logs;
    }
    
    /**
     * 收集容器的 stdout/stderr 日志
     */
    private String collectContainerLogs(String containerId) {
        StringBuilder logs = new StringBuilder();
        
        try {
            var logCmd = dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTailAll();
            
            logCmd.exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.Frame>() {
                @Override
                public void onNext(com.github.dockerjava.api.model.Frame frame) {
                    if (frame != null && frame.getPayload() != null) {
                        logs.append(new String(frame.getPayload(), java.nio.charset.StandardCharsets.UTF_8));
                    }
                    super.onNext(frame);
                }
            }).awaitCompletion();
            
        } catch (Exception e) {
            System.err.println("[PythonDockerExecutor] 收集日志失败：" + e.getMessage());
            logs.append("Failed to collect logs: ").append(e.getMessage());
        }
        
        return logs.toString();
    }
    
    /**
     * 清理容器
     */
    private void cleanupContainer(String containerId) {
        try {
            System.out.println("[PythonDockerExecutor] 清理容器：" + containerId);
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception e) {
            System.err.println("[PythonDockerExecutor] 清理容器失败：" + e.getMessage());
        }
    }
}
