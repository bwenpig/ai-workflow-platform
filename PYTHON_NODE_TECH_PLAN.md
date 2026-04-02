# Python 节点技术实施方案

**评审人:** Builder-Backend (龙傲天)  
**评审日期:** 2026 年 4 月 1 日  
**版本:** 1.0  

---

## 一、当前实现状态评估

### 1.1 现有代码分析

已阅读两个 `PythonScriptExecutor.java` 实现:

#### ai-workflow 版本 (主实现)
**位置:** `ai-workflow/backend/src/main/java/com/ben/workflow/engine/PythonScriptExecutor.java`

**现有能力:**
- ✅ 基础 Python 脚本执行 (子进程方式)
- ✅ 输入数据传递 (JSON 文件)
- ✅ 输出数据返回 (JSON 文件)
- ✅ 超时控制 (默认 30 秒)
- ✅ 依赖安装支持 (requirements.txt + pip install -t)
- ✅ 临时目录隔离 + 自动清理
- ✅ 错误捕获和日志输出

**主要问题:**
- ❌ **无 Docker 沙箱** - 仅使用子进程隔离，安全性不足
- ❌ **无危险函数黑名单** - 可执行 `eval`, `exec`, `__import__` 等危险操作
- ❌ **无模块导入白名单** - 可导入任意模块 (包括 `os`, `subprocess`, `socket`)
- ❌ **无资源限制** - 无内存/CPU 限制，可能导致资源耗尽
- ❌ **无网络访问控制** - 可任意访问内外网
- ❌ **文件系统未隔离** - 可访问主机文件系统

#### dag-scheduler 版本 (简化版)
**位置:** `dag-scheduler/src/main/java/com/ben/dagscheduler/executor/PythonScriptExecutor.java`

**特点:**
- 更简单的实现，仅执行外部脚本文件
- 通过环境变量传递输入
- 功能更基础，不建议作为主要实现

### 1.2 竞品对比差距

| 功能维度 | 竞品标准 | 当前实现 | 差距等级 |
|----------|----------|----------|----------|
| 编辑器 | Monaco Editor | 待实现 | 🔴 高 |
| 沙箱隔离 | Docker 容器 | 子进程 | 🔴 高 |
| 安全机制 | 多层防护 | 无 | 🔴 高 |
| 资源限制 | 内存/CPU 限制 | 无 | 🔴 高 |
| 依赖管理 | 预装 + 缓存 | 动态安装 | 🟡 中 |
| 多输出 | 多分支支持 | 单输出 | 🟡 中 |
| 执行历史 | 保存记录 | 无 | 🟡 中 |

---

## 二、技术选型

### 2.1 Docker 沙箱方案

**推荐方案:** Docker SDK for Java + 预构建执行镜像

#### 方案对比

| 方案 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **Docker SDK (Java)** | - 原生 Java 集成<br>- 细粒度控制<br>- 社区成熟 | - 需要 Docker 守护进程<br>- 配置复杂度中等 | ⭐⭐⭐⭐⭐ |
| Testcontainers | - 测试友好<br>- 自动镜像管理 | - 运行时开销大<br>- 不适合生产 | ⭐⭐⭐ |
| Kubernetes Job | - 云原生<br>- 资源调度强 | - 架构复杂<br>- 过度设计 | ⭐⭐ |
| 子进程 + seccomp | - 无外部依赖<br>- 轻量 | - 安全性弱<br>- 实现复杂 | ⭐⭐ |

#### 技术选型决策

**选择:** Docker SDK for Java (`com.spotify:docker-client` 或 `com.github.docker-java:docker-java`)

**理由:**
1. 与现有 Java 技术栈无缝集成
2. 支持完整的 Docker API (容器创建、启动、日志、资源限制)
3. 社区活跃，文档完善
4. 可精确控制容器资源 (内存、CPU、网络)

**依赖:**
```xml
<dependency>
    <groupId>com.github.docker-java</groupId>
    <artifactId>docker-java</artifactId>
    <version>3.3.4</version>
</dependency>
<dependency>
    <groupId>com.github.docker-java</groupId>
    <artifactId>docker-java-transport-httpclient5</artifactId>
    <version>3.3.4</version>
</dependency>
```

### 2.2 Docker 镜像设计

**基础镜像:** `python:3.11-slim`

**预装库:**
```dockerfile
FROM python:3.11-slim

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# 预装 Python 库
RUN pip install --no-cache-dir \
    requests==2.31.0 \
    pandas==2.1.4 \
    numpy==1.26.2 \
    Pillow==10.1.0 \
    beautifulsoup4==4.12.2 \
    lxml==4.9.3 \
    python-dateutil==2.8.2

# 创建非 root 用户
RUN useradd -m -u 1000 sandbox && \
    mkdir /sandbox && chown sandbox:sandbox /sandbox

USER sandbox
WORKDIR /sandbox

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python3 -c "print('OK')" || exit 1
```

**镜像构建策略:**
- 本地开发：Dockerfile 直接构建
- 生产环境：预构建并推送到镜像仓库 (Docker Hub / 私有 Registry)
- 版本管理：语义化版本标签 (`python-sandbox:1.0.0`)

### 2.3 安全机制设计

#### 危险函数黑名单 (AST 级别拦截)

```java
public class SecurityAnalyzer {
    private static final Set<String> DANGEROUS_BUILTINS = Set.of(
        "eval", "exec", "compile", "__import__",
        "open", "file", "input", "raw_input",
        "globals", "locals", "vars", "dir",
        "getattr", "setattr", "delattr"
    );
    
    private static final Set<String> DANGEROUS_MODULES = Set.of(
        "os", "sys", "subprocess", "socket",
        "multiprocessing", "threading", "ctypes",
        "pickle", "marshal", "shelve"
    );
    
    public SecurityAnalysisResult analyze(String script) {
        // 使用 Python AST 模块分析代码
        // 检测危险函数调用和模块导入
    }
}
```

#### 模块导入白名单

```java
public class ImportWhitelist {
    private static final Set<String> ALLOWED_MODULES = Set.of(
        // 标准库
        "json", "datetime", "re", "math", "random",
        "string", "collections", "itertools", "functools",
        "typing", "dataclasses", "enum", "copy",
        "base64", "hashlib", "hmac", "secrets",
        "uuid", "decimal", "fractions", "statistics",
        
        // 预装第三方库
        "requests", "pandas", "numpy", "bs4", "lxml",
        "PIL", "dateutil"
    );
    
    public boolean isAllowed(String moduleName) {
        return ALLOWED_MODULES.contains(moduleName);
    }
}
```

#### 网络访问控制

**方案:** Docker 网络隔离 + Python 层拦截

```dockerfile
# Docker 级别：禁用外部网络
docker run --network=none python-sandbox ...
```

```python
# Python 级别：拦截 socket 调用
import socket
_original_socket = socket.socket

def restricted_socket(*args, **kwargs):
    raise PermissionError("Network access is disabled")

socket.socket = restricted_socket
```

### 2.4 资源限制配置

```java
public class ResourceLimits {
    // 内存限制
    private static final long DEFAULT_MEMORY_MB = 128;
    private static final long MAX_MEMORY_MB = 512;
    
    // CPU 限制
    private static final double DEFAULT_CPU_QUOTA = 0.5;
    private static final double MAX_CPU_QUOTA = 2.0;
    
    // 超时限制
    private static final int DEFAULT_TIMEOUT_SEC = 30;
    private static final int MAX_TIMEOUT_SEC = 300;
    
    // 磁盘限制
    private static final long DEFAULT_DISK_MB = 10;
    private static final long MAX_DISK_MB = 100;
}
```

**Docker 资源限制:**
```java
HostConfig hostConfig = HostConfig.newHostConfig()
    .withMemory(memoryLimit * 1024 * 1024)  // 内存
    .withCpuQuota((long) (cpuQuota * 100000))  // CPU
    .withNetworkMode("none")  // 禁用网络
    .withReadonlyRootfs(true)  // 只读文件系统
    .withTmpFs(Map.of("/tmp", "rw,noexec,nosuid,size=" + tmpfsSize));  // 临时目录
```

---

## 三、实现计划

### 3.1 阶段划分

#### 第一阶段 (2 周) - 基础沙箱执行
**目标:** 实现 Docker 沙箱执行，替换现有子进程方案

**任务:**
1. [ ] 集成 Docker SDK for Java
2. [ ] 构建 Python 沙箱镜像 (Dockerfile)
3. [ ] 实现 Docker 容器执行器
4. [ ] 实现容器生命周期管理 (创建、启动、销毁)
5. [ ] 实现输入输出数据传递 (Volume 挂载)
6. [ ] 实现基础错误处理和日志收集
7. [ ] 单元测试 + 集成测试

**交付物:**
- `DockerSandboxExecutor.java` - Docker 执行器
- `Dockerfile` - 沙箱镜像
- 测试用例覆盖核心场景

#### 第二阶段 (1.5 周) - 安全机制
**目标:** 实现多层安全防护

**任务:**
1. [ ] 实现 Python AST 代码分析器
2. [ ] 实现危险函数黑名单检测
3. [ ] 实现模块导入白名单验证
4. [ ] 实现 Docker 网络隔离
5. [ ] 实现文件系统只读限制
6. [ ] 实现资源限制 (内存/CPU/超时)
7. [ ] 安全测试 (渗透测试用例)

**交付物:**
- `SecurityAnalyzer.java` - 代码安全分析器
- `ImportWhitelist.java` - 导入白名单
- 安全测试报告

#### 第三阶段 (1.5 周) - 增强功能
**目标:** 完善用户体验和可观测性

**任务:**
1. [ ] 实现执行历史记录 (数据库存储)
2. [ ] 实现日志实时输出 (WebSocket)
3. [ ] 实现代码模板库
4. [ ] 实现多输出分支支持
5. [ ] 实现重试机制
6. [ ] 实现依赖缓存机制
7. [ ] 前端 Monaco Editor 集成 (配合前端团队)

**交付物:**
- `ExecutionHistoryService.java` - 执行历史服务
- `CodeTemplateRepository.java` - 代码模板库
- 前端编辑器组件

### 3.2 开发任务分解

#### Week 1-2: Docker 沙箱基础

| 任务 ID | 任务描述 | 预计工时 | 依赖 |
|---------|----------|----------|------|
| T1.1 | Docker SDK 集成和配置 | 4h | - |
| T1.2 | 编写 Dockerfile (基础镜像) | 4h | - |
| T1.3 | 实现 DockerSandboxExecutor 核心逻辑 | 8h | T1.1 |
| T1.4 | 实现容器资源限制配置 | 4h | T1.3 |
| T1.5 | 实现输入输出数据传递 | 4h | T1.3 |
| T1.6 | 实现容器日志收集 | 4h | T1.3 |
| T1.7 | 单元测试 (执行流程) | 4h | T1.3-T1.6 |
| T1.8 | 集成测试 (端到端) | 4h | T1.7 |

**小计:** 36 小时 ≈ 4.5 人天

#### Week 3: 安全机制

| 任务 ID | 任务描述 | 预计工时 | 依赖 |
|---------|----------|----------|------|
| T2.1 | Python AST 分析器实现 | 8h | - |
| T2.2 | 危险函数黑名单实现 | 4h | T2.1 |
| T2.3 | 模块导入白名单实现 | 4h | T2.1 |
| T2.4 | Docker 网络隔离配置 | 2h | T1.3 |
| T2.5 | 文件系统只读配置 | 2h | T1.3 |
| T2.6 | 安全测试用例编写 | 8h | T2.2-T2.5 |
| T2.7 | 渗透测试和修复 | 8h | T2.6 |

**小计:** 36 小时 ≈ 4.5 人天

#### Week 4: 增强功能

| 任务 ID | 任务描述 | 预计工时 | 依赖 |
|---------|----------|----------|------|
| T3.1 | 执行历史数据库设计 | 4h | - |
| T3.2 | ExecutionHistoryService 实现 | 8h | T3.1 |
| T3.3 | 日志实时输出 (WebSocket) | 8h | T1.6 |
| T3.4 | 代码模板库设计和实现 | 6h | - |
| T3.5 | 多输出分支支持 | 6h | T1.5 |
| T3.6 | 重试机制实现 | 4h | T3.2 |
| T3.7 | 依赖缓存机制 | 6h | T1.2 |
| T3.8 | 前端联调 (Monaco Editor) | 8h | 前端团队 |

**小计:** 50 小时 ≈ 6.25 人天

### 3.3 总工时估算

| 阶段 | 工时 (小时) | 工时 (人天) | 周期 |
|------|-------------|-------------|------|
| 第一阶段 | 36h | 4.5 天 | 2 周 |
| 第二阶段 | 36h | 4.5 天 | 1.5 周 |
| 第三阶段 | 50h | 6.25 天 | 1.5 周 |
| **总计** | **122h** | **15.25 天** | **5 周** |

**备注:**
- 按 1 名后端开发计算，约 5 周完成
- 如增加 1 名开发并行，可压缩至 3 周
- 前端 Monaco Editor 集成需前端团队配合 (未计入上述工时)

---

## 四、风险评估

### 4.1 技术风险

| 风险项 | 可能性 | 影响 | 缓解措施 | 备选方案 |
|--------|--------|------|----------|----------|
| **Docker 环境依赖** | 中 | 高 | - 提供 Docker 安装脚本<br>- 降级到子进程模式 | 子进程 + seccomp 沙箱 |
| **AST 分析准确性** | 中 | 中 | - 充分测试用例<br>- 允许白名单豁免 | 运行时拦截 (猴子补丁) |
| **镜像构建失败** | 低 | 中 | - 预构建镜像推送到 Registry<br>- 镜像版本锁定 | 使用官方 Python 镜像 |
| **资源限制不生效** | 低 | 高 | - 多层限制 (Docker + Python)<br>- 监控告警 | 外部监控进程 |
| **性能开销过大** | 中 | 中 | - 容器预热池<br>- 镜像优化 | 轻量级沙箱 (gVisor) |

### 4.2 安全风险

| 风险项 | 可能性 | 影响 | 缓解措施 |
|--------|--------|------|----------|
| **容器逃逸** | 低 | 极高 | - 使用非 root 用户<br>- 禁用特权模式<br>- 限制系统调用 (seccomp) |
| **代码注入** | 中 | 高 | - AST 静态分析<br>- 运行时拦截<br>- 输入验证 |
| **资源耗尽** | 中 | 高 | - Docker 资源限制<br>- 超时控制<br>- 并发限制 |
| **数据泄露** | 低 | 高 | - 临时数据加密<br>- 容器销毁后清理<br>- 审计日志 |

### 4.3 备选方案

#### 方案 A: gVisor 沙箱 (高安全场景)
**适用:** 多租户、不可信代码执行

**优点:**
- 更强的隔离性 (用户空间内核)
- 防止容器逃逸
- Google 开源，生产验证

**缺点:**
- 性能开销较大 (约 20-30%)
- 配置复杂
- 部分系统调用不支持

**实施:** 
```bash
docker run --runtime=runsc python-sandbox ...
```

#### 方案 B: Pyodide WebAssembly (浏览器端执行)
**适用:** 纯前端场景、低安全要求

**优点:**
- 无需后端沙箱
- 浏览器原生隔离
- 零服务器成本

**缺点:**
- 性能较低
- 库支持有限
- 不适合重型计算

#### 方案 C: 子进程 + seccomp (降级方案)
**适用:** Docker 不可用环境

**优点:**
- 无外部依赖
- 轻量级
- 快速部署

**缺点:**
- 安全性弱于 Docker
- 实现复杂度高
- 跨平台兼容性差

**实施:**
```java
// 使用 seccomp 限制系统调用
ProcessBuilder pb = new ProcessBuilder("python3", scriptPath);
pb.environment().put("PYTHON_SECCOMP_PROFILE", "restricted.json");
```

---

## 五、架构设计

### 5.1 组件架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Frontend (React)                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Monaco Editor Component                 │    │
│  │  - Syntax Highlighting                               │    │
│  │  - Auto Completion                                   │    │
│  │  - Error Checking                                    │    │
│  │  - Code Templates                                    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP / WebSocket
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Backend (Spring Boot)                      │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              PythonNodeController                    │    │
│  │  - Save Code                                         │    │
│  │  - Execute Script                                    │    │
│  │  - Get Execution History                             │    │
│  └─────────────────────────────────────────────────────┘    │
│                              │                                │
│                              ▼                                │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              PythonExecutionService                  │    │
│  │  - Security Analysis (AST)                           │    │
│  │  - Code Validation                                   │    │
│  │  - Execution Orchestration                           │    │
│  └─────────────────────────────────────────────────────┘    │
│                              │                                │
│                              ▼                                │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              DockerSandboxExecutor                   │    │
│  │  - Container Management                              │    │
│  │  - Resource Limits                                   │    │
│  │  - Log Collection                                    │    │
│  │  - Volume Mounting                                   │    │
│  └─────────────────────────────────────────────────────┘    │
│                              │                                │
│                              ▼                                │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Docker Daemon                           │    │
│  │  ┌─────────────────────────────────────────────┐    │    │
│  │  │         Python Sandbox Container             │    │    │
│  │  │  - Pre-installed Libraries                  │    │    │
│  │  │  - Restricted Permissions                   │    │    │
│  │  │  - Network Isolation                        │    │    │
│  │  └─────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Database (PostgreSQL)                   │
│  - Execution History                                         │
│  - Code Templates                                            │
│  - User Preferences                                          │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 核心类设计

```java
// 执行器接口
public interface PythonExecutor {
    PythonExecutionResult execute(String script, Map<String, Object> inputs, ExecutionConfig config);
}

// Docker 沙箱执行器
@Component
public class DockerSandboxExecutor implements PythonExecutor {
    private final DockerClient dockerClient;
    private final SecurityAnalyzer securityAnalyzer;
    private final ImageManager imageManager;
    
    @Override
    public PythonExecutionResult execute(String script, Map<String, Object> inputs, ExecutionConfig config) {
        // 1. 安全分析
        SecurityAnalysisResult analysis = securityAnalyzer.analyze(script);
        if (!analysis.isSafe()) {
            throw new SecurityException(analysis.getViolations());
        }
        
        // 2. 创建容器
        String containerId = createContainer(config);
        
        // 3. 挂载输入文件
        mountInputFile(containerId, inputs);
        
        // 4. 启动容器
        startContainer(containerId);
        
        // 5. 等待执行完成 (带超时)
        waitForCompletion(containerId, config.getTimeout());
        
        // 6. 收集日志和输出
        String logs = collectLogs(containerId);
        Map<String, Object> outputs = collectOutputs(containerId);
        
        // 7. 销毁容器
        destroyContainer(containerId);
        
        return PythonExecutionResult.success(outputs, logs);
    }
}

// 安全分析器
@Component
public class SecurityAnalyzer {
    public SecurityAnalysisResult analyze(String script) {
        // AST 分析
        // 检测危险函数
        // 检测危险模块导入
    }
}

// 执行配置
public class ExecutionConfig {
    private int timeoutSeconds = 30;
    private long memoryLimitMb = 128;
    private double cpuQuota = 0.5;
    private boolean networkEnabled = false;
    private List<String> allowedModules = new ArrayList<>();
    private Map<String, String> environment = new HashMap<>();
}
```

### 5.3 数据流

```
用户提交代码
    │
    ▼
安全分析 (AST)
    │
    ├─❌ 不安全 → 返回错误 (包含违规详情)
    │
    ▼ 安全
创建 Docker 容器 (资源限制 + 网络隔离)
    │
    ▼
挂载输入数据 (Volume)
    │
    ▼
启动容器执行
    │
    ├─⏱️ 超时 → 强制终止 → 返回超时错误
    │
    ├─❌ 错误 → 收集日志 → 返回错误信息
    │
    ▼ 成功
收集输出数据
    │
    ▼
销毁容器
    │
    ▼
保存执行历史
    │
    ▼
返回结果
```

---

## 六、测试策略

### 6.1 单元测试

**测试覆盖:**
- 安全分析器 (危险函数检测、模块导入检测)
- Docker 执行器 (容器创建、启动、销毁)
- 输入输出处理 (JSON 序列化/反序列化)
- 资源限制配置

**测试框架:** JUnit 5 + Mockito + Testcontainers

### 6.2 集成测试

**测试场景:**
1. 正常执行流程
2. 超时处理
3. 内存超限处理
4. 危险函数拦截
5. 网络访问拦截
6. 模块导入拦截
7. 并发执行

### 6.3 安全测试

**渗透测试用例:**
```python
# 测试用例 1: eval 注入
eval("__import__('os').system('rm -rf /')")

# 测试用例 2: 模块导入绕过
import builtins
builtins.__dict__['__import__']('os')

# 测试用例 3: 网络访问
import socket
s = socket.socket()
s.connect(('evil.com', 443))

# 测试用例 4: 文件系统访问
with open('/etc/passwd', 'r') as f:
    print(f.read())

# 测试用例 5: 环境变量泄露
import os
print(os.environ)

# 测试用例 6: 进程执行
import subprocess
subprocess.run(['ls', '-la'])

# 测试用例 7: 资源耗尽
x = [0] * (1024 * 1024 * 1024)  # 1GB 内存

# 测试用例 8: 无限循环
while True:
    pass
```

---

## 七、监控和告警

### 7.1 监控指标

| 指标 | 说明 | 告警阈值 |
|------|------|----------|
| `python_execution_total` | 执行次数 | - |
| `python_execution_duration` | 执行耗时 | p99 > 25s |
| `python_execution_errors` | 错误次数 | 错误率 > 5% |
| `python_container_active` | 活跃容器数 | > 100 |
| `python_memory_usage` | 内存使用 | > 80% |
| `python_security_violations` | 安全违规 | > 0 |

### 7.2 日志规范

```json
{
  "timestamp": "2026-04-01T12:00:00Z",
  "level": "INFO",
  "traceId": "abc123",
  "nodeId": "python_node_1",
  "userId": "user_456",
  "action": "PYTHON_EXECUTE",
  "duration": 1234,
  "status": "SUCCESS",
  "memoryUsed": 45678912,
  "securityCheck": "PASSED"
}
```

---

## 八、部署配置

### 8.1 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| Docker | 20.10+ | 需要 Docker Daemon |
| Java | 17+ | Spring Boot 3.x |
| Python | 3.11+ | 沙箱镜像内 |
| 内存 | 2GB+ | Docker 容器开销 |

### 8.2 配置文件

```yaml
# application.yml
python:
  executor:
    type: docker  # docker | subprocess
    docker:
      image: ben/python-sandbox:1.0.0
      registry: docker.io
      pullPolicy: IfNotPresent
    resources:
      memory:
        default: 128MB
        max: 512MB
      cpu:
        default: 0.5
        max: 2.0
      timeout:
        default: 30s
        max: 300s
    security:
      networkEnabled: false
      readOnlyFileSystem: true
      dangerousFunctions:
        - eval
        - exec
        - __import__
      allowedModules:
        - json
        - datetime
        - requests
        - pandas
        - numpy
```

### 8.3 Docker Compose (开发环境)

```yaml
version: '3.8'

services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - PYTHON_EXECUTOR_TYPE=docker
      - DOCKER_HOST=unix:///var/run/docker.sock
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    depends_on:
      - postgres
  
  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=workflow
      - POSTGRES_USER=workflow
      - POSTGRES_PASSWORD=workflow
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

---

## 九、后续优化方向

### 9.1 性能优化

1. **容器预热池**
   - 预创建 N 个空闲容器
   - 减少冷启动时间 (从 2s 降至 200ms)

2. **镜像优化**
   - 多阶段构建减少镜像大小
   - 层缓存优化

3. **依赖缓存**
   - 预装库镜像层缓存
   - 用户依赖 Volume 持久化

### 9.2 功能增强

1. **多语言支持**
   - JavaScript (Node.js 沙箱)
   - Go (编译执行)

2. **AI 辅助**
   - 代码自动生成
   - 代码审查建议
   - 错误修复建议

3. **调试增强**
   - 断点调试
   - 变量查看
   - 执行步骤追踪

### 9.3 安全增强

1. **审计日志**
   - 所有执行记录审计
   - 安全事件告警

2. **速率限制**
   - 用户级执行频率限制
   - 并发执行数限制

3. **代码扫描**
   - 集成 SAST 工具
   - 依赖漏洞扫描

---

## 十、总结

### 10.1 核心决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 沙箱方案 | Docker 容器 | 安全性、隔离性、资源控制 |
| 执行器 | Docker SDK for Java | Java 原生集成、社区成熟 |
| 基础镜像 | python:3.11-slim | 轻量、官方维护 |
| 安全机制 | AST 分析 + 运行时拦截 | 多层防护、准确性高 |
| 资源限制 | Docker cgroups | 内核级限制、可靠 |

### 10.2 P0 功能优先级

1. **Docker 沙箱执行** (最高优先级) - 安全基础
2. **危险函数黑名单** - 防止代码注入
3. **模块导入白名单** - 控制依赖范围
4. **资源限制** - 防止资源耗尽
5. **预装库列表** - 提升用户体验

### 10.3 关键里程碑

| 里程碑 | 时间 | 交付物 |
|--------|------|--------|
| M1: Docker 沙箱可用 | Week 2 | 可执行基础脚本 |
| M2: 安全机制完成 | Week 3 | 通过安全测试 |
| M3: 增强功能完成 | Week 5 | 完整 P0 功能 |
| M4: 生产就绪 | Week 6 | 性能测试 + 文档 |

---

**文档版本:** 1.0  
**最后更新:** 2026 年 4 月 1 日  
**维护人:** Builder-Backend (龙傲天)
