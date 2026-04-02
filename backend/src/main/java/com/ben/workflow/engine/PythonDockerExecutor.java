package com.ben.workflow.engine;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.model.PythonNodeConfig;
import com.ben.workflow.security.*;
import com.ben.workflow.spi.NodeComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Python Docker 沙箱执行器
 * 
 * 使用 Docker 容器隔离执行用户提供的 Python 脚本，提供：
 * - 容器级资源隔离（内存、CPU）
 * - 网络隔离（默认禁用）
 * - 文件系统隔离（只读 + 临时目录）
 * - 超时控制
 * - 日志收集
 * 
 * @author 龙傲天
 * @version 1.0
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
    
    /**
     * 安全配置（严格模式）
     */
    private final PythonSecurityConfig securityConfig;
    
    /**
     * 安全分析器
     */
    private final PythonSecurityAnalyzer securityAnalyzer;
    
    /**
     * 运行时拦截器
     */
    private final RuntimeInterceptor runtimeInterceptor;
    
    /**
     * 默认构造函数，使用本地 Docker 守护进程和严格安全配置
     */
    public PythonDockerExecutor() {
        this(DEFAULT_IMAGE);
    }
    
    /**
     * 构造函数，可指定 Docker 镜像
     * 
     * @param dockerImage Docker 镜像名称
     */
    public PythonDockerExecutor(String dockerImage) {
        this.dockerImage = dockerImage;
        
        // 初始化安全组件
        this.securityConfig = PythonSecurityConfig.createStrict();
        this.securityAnalyzer = new PythonSecurityAnalyzer(securityConfig);
        this.runtimeInterceptor = new RuntimeInterceptor(securityConfig);
        
        // 使用 docker-java 的默认配置（自动检测 Docker 环境）
        var dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        
        // 使用 OkHttp 传输（更好的 Unix socket 支持）
        var dockerHttpClient = new OkDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig.getDockerHost())
                .build();
        
        this.dockerClient = DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);
        
        System.out.println("[PythonDockerExecutor] 初始化完成，镜像：" + dockerImage + ", Docker Host: " + dockerClientConfig.getDockerHost());
        System.out.println("[PythonDockerExecutor] 安全模块已启用：AST 分析 + 运行时拦截");
    }
    
    /**
     * 解析 Docker Host 地址
     * 支持以下优先级：
     * 1. DOCKER_HOST 环境变量
     * 2. 自动检测常见 socket 路径
     * 3. 默认值
     * 
     * @return Docker Host URL
     */
    private String resolveDockerHost() {
        // 1. 优先使用 DOCKER_HOST 环境变量
        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost != null && !dockerHost.trim().isEmpty()) {
            System.out.println("[PythonDockerExecutor] 使用 DOCKER_HOST 环境变量：" + dockerHost);
            return dockerHost.trim();
        }
        
        // 2. 自动检测常见 socket 路径
        String osName = System.getProperty("os.name").toLowerCase();
        
        // Linux/macOS: /var/run/docker.sock
        if (!osName.contains("win")) {
            if (java.nio.file.Files.exists(java.nio.file.Paths.get("/var/run/docker.sock"))) {
                System.out.println("[PythonDockerExecutor] 检测到 Unix socket: /var/run/docker.sock");
                return "unix:///var/run/docker.sock";
            }
        }
        
        // Windows: npipe:////./pipe/docker_engine
        if (osName.contains("win")) {
            System.out.println("[PythonDockerExecutor] 使用 Windows named pipe");
            return "npipe:////./pipe/docker_engine";
        }
        
        // 3. 默认尝试本地 TCP 连接（远程 Docker 场景）
        System.out.println("[PythonDockerExecutor] 使用默认 Docker Host (tcp://localhost:2375)");
        return "tcp://localhost:2375";
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
     * 
     * @param script Python 脚本内容
     * @param inputs 输入数据
     * @param config 执行配置
     * @return 执行结果
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
            // 1. 创建临时目录用于挂载
            tempDir = Files.createTempDirectory("python_docker_");
            
            // 2. 准备输入文件
            String inputsJson = OBJECT_MAPPER.writeValueAsString(inputs);
            Path inputsFile = tempDir.resolve("inputs.json");
            Files.writeString(inputsFile, inputsJson);
            
            // 3. 包装用户脚本（使用安全包装后的代码）
            Path scriptFile = tempDir.resolve("script.py");
            Files.writeString(scriptFile, wrapScript(safeScript));
            
            // 4. 准备 requirements.txt（如果有）
            if (config != null && config.getRequirements() != null && !config.getRequirements().isEmpty()) {
                Path requirementsFile = tempDir.resolve("requirements.txt");
                Files.writeString(requirementsFile, String.join("\n", config.getRequirements()));
            }
            
            // 5. 创建并配置容器
            containerId = createContainer(tempDir, config);
            System.out.println("[PythonDockerExecutor] 容器创建成功：" + containerId);
            
            // 6. 启动容器并等待执行完成
            int timeout = config != null && config.getTimeout() != null ? config.getTimeout() : DEFAULT_TIMEOUT_SECONDS;
            String logs = executeContainer(containerId, timeout);
            
            // 7. 读取输出文件
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
            // 8. 清理资源
            if (containerId != null) {
                cleanupContainer(containerId);
            }
            if (tempDir != null) {
                cleanupTempDir(tempDir);
            }
        }
    }
    
    /**
     * 包装用户脚本，添加输入输出处理逻辑
     * 
     * @param script 用户脚本
     * @return 包装后的完整脚本
     */
    private String wrapScript(String script) {
        StringBuilder wrappedScript = new StringBuilder();
        wrappedScript.append("import json\n");
        wrappedScript.append("import sys\n");
        wrappedScript.append("import os\n");
        wrappedScript.append("\n");
        wrappedScript.append("# 读取输入\n");
        wrappedScript.append("inputs_file = sys.argv[1] if len(sys.argv) > 1 else 'inputs.json'\n");
        wrappedScript.append("try:\n");
        wrappedScript.append("    with open(inputs_file, 'r', encoding='utf-8') as f:\n");
        wrappedScript.append("        inputs = json.load(f)\n");
        wrappedScript.append("except:\n");
        wrappedScript.append("    inputs = {}\n");
        wrappedScript.append("\n");
        wrappedScript.append("# 执行用户脚本\n");
        wrappedScript.append("outputs = {}\n");
        wrappedScript.append("try:\n");
        
        // 缩进用户脚本
        for (String line : script.split("\n")) {
            wrappedScript.append("    ").append(line).append("\n");
        }
        
        wrappedScript.append("\n");
        wrappedScript.append("except Exception as e:\n");
        wrappedScript.append("    import traceback\n");
        wrappedScript.append("    error_info = {'error': str(e), 'traceback': traceback.format_exc()}\n");
        wrappedScript.append("    with open('outputs.json', 'w', encoding='utf-8') as f:\n");
        wrappedScript.append("        json.dump({'_error': error_info}, f, ensure_ascii=False, indent=2)\n");
        wrappedScript.append("    sys.exit(1)\n");
        wrappedScript.append("\n");
        wrappedScript.append("# 写入输出\n");
        wrappedScript.append("with open('outputs.json', 'w', encoding='utf-8') as f:\n");
        wrappedScript.append("    json.dump(outputs, f, ensure_ascii=False, indent=2)\n");
        wrappedScript.append("sys.exit(0)\n");
        
        return wrappedScript.toString();
    }
    
    /**
     * 创建 Docker 容器
     * 
     * @param workDir 工作目录（用于挂载）
     * @param config 执行配置
     * @return 容器 ID
     */
    private String createContainer(Path workDir, PythonNodeConfig config) throws IOException {
        int timeout = config != null && config.getTimeout() != null ? config.getTimeout() : DEFAULT_TIMEOUT_SECONDS;
        long memoryMb = config != null && config.getMemoryLimit() != null ? config.getMemoryLimit() : DEFAULT_MEMORY_MB;
        double cpuCores = config != null && config.getCpuLimit() != null ? config.getCpuLimit() : DEFAULT_CPU_QUOTA;
        
        // 构建主机配置
        HostConfig hostConfig = new HostConfig()
                .withMemory(memoryMb * 1024 * 1024)  // 内存限制 (字节)
                .withMemorySwap(memoryMb * 1024 * 1024)  // 禁止使用 swap
                .withCpuQuota((long) (cpuCores * 100000))  // CPU 限制 (微秒/100ms)
                .withNetworkMode("none")  // 禁用网络
                .withReadonlyRootfs(true)  // 只读文件系统
                .withTmpFs(Map.of("/tmp", "rw,noexec,nosuid,size=64m"));  // 临时目录
        
        // 创建工作目录卷
        Volume workVolume = new Volume("/sandbox");
        
        // 创建容器命令
        CreateContainerCmd createCmd = dockerClient.createContainerCmd(dockerImage)
                .withHostConfig(hostConfig)
                .withVolumes(workVolume)
                .withBinds(new Bind(workDir.toString(), workVolume))
                .withWorkingDir("/sandbox")
                .withCmd("python3", "script.py", "inputs.json")
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(false);
        
        // 添加环境变量
        if (config != null && config.getEnv() != null) {
            List<String> envVars = new ArrayList<>();
            for (Map.Entry<String, String> entry : config.getEnv().entrySet()) {
                envVars.add(entry.getKey() + "=" + entry.getValue());
            }
            createCmd.withEnv(envVars);
        }
        
        // 执行创建
        var createResponse = createCmd.exec();
        return createResponse.getId();
    }
    
    /**
     * 执行容器并收集日志
     * 
     * @param containerId 容器 ID
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行日志
     */
    private String executeContainer(String containerId, int timeoutSeconds) throws Exception {
        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
        System.out.println("[PythonDockerExecutor] 容器已启动");
        
        // 等待容器完成（带超时）
        long startTime = System.currentTimeMillis();
        while (true) {
            var inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
            if (!Boolean.TRUE.equals(inspectResponse.getState().getRunning())) {
                break;
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > timeoutSeconds * 1000L) {
                // 超时，强制停止
                System.out.println("[PythonDockerExecutor] 执行超时，强制停止");
                dockerClient.stopContainerCmd(containerId).withTimeout(0).exec();
                throw new TimeoutException("脚本执行超时 (" + timeoutSeconds + "秒)");
            }
            
            Thread.sleep(500);
        }
        
        // 获取退出码
        var inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
        Integer exitCode = inspectResponse.getState().getExitCode();
        
        // 收集容器 stdout/stderr 日志
        String logs = collectContainerLogs(containerId);
        
        // 检查退出码
        if (exitCode != null && exitCode != 0) {
            throw new RuntimeException("脚本执行失败 (退出码：" + exitCode + "): " + logs);
        }
        
        System.out.println("[PythonDockerExecutor] 执行完成，日志：" + logs.length() + " 字节");
        
        return logs;
    }
    
    /**
     * 收集容器的 stdout/stderr 日志
     * 
     * @param containerId 容器 ID
     * @return 日志内容
     */
    private String collectContainerLogs(String containerId) {
        StringBuilder logs = new StringBuilder();
        
        try {
            // 使用 logContainerCmd 收集 stdout 和 stderr
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
     * 
     * @param containerId 容器 ID
     */
    private void cleanupContainer(String containerId) {
        try {
            System.out.println("[PythonDockerExecutor] 清理容器：" + containerId);
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception e) {
            System.err.println("[PythonDockerExecutor] 清理容器失败：" + e.getMessage());
        }
    }
    
    /**
     * 清理临时目录
     * 
     * @param tempDir 临时目录
     */
    private void cleanupTempDir(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.out.println("[PythonDockerExecutor] 清理文件失败：" + path);
                        }
                    });
            System.out.println("[PythonDockerExecutor] 临时目录已清理");
        } catch (IOException e) {
            System.err.println("[PythonDockerExecutor] 清理临时目录失败：" + e.getMessage());
        }
    }
}
