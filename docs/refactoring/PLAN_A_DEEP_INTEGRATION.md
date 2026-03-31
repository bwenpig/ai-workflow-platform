# 重构方案 A：深度集成 dag-scheduler

## 背景

**问题：** ai-workflow 使用自研引擎，未使用 dag-scheduler SPI。

**立场：** 深度集成 dag-scheduler，将 PythonScriptExecutor 和模型适配器迁移到 SPI。

---

## 架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ai-workflow (业务层)                             │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                     DagWorkflowEngine                              │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐    │  │
│  │  │  DAG 解析器      │  │  拓扑排序器      │  │  调度执行器      │    │  │
│  │  │  • 图验证        │  │  • 依赖分析      │  │  • 并行执行      │    │  │
│  │  │  • 循环检测      │  │  • 执行顺序      │  │  • 状态追踪      │    │  │
│  │  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘    │  │
│  │           └─────────────────────┼─────────────────────┘            │  │
│  │                                 ↓ 调用                              │  │
│  │  ┌─────────────────────────────────────────────────────────────┐   │  │
│  │  │              ExecutorRegistry (SPI 注册中心)                 │   │  │
│  │  │  • getExecutor("python_script")                              │   │  │
│  │  │  • getExecutor("kling")                                      │   │  │
│  │  │  • getExecutor("wan")                                        │   │  │
│  │  │  • getExecutor("seedance")                                   │   │  │
│  │  │  • getExecutor("nanobanana")                                 │   │  │
│  │  └─────────────────────────────────────────────────────────────┘   │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                   ↑ 依赖                                 │
│                                   │                                      │
│  ┌────────────────────────────────┼──────────────────────────────────┐  │
│  │              dag-scheduler (SPI 框架层)                            │  │
│  │  ┌────────────────────────────────────────────────────────────┐    │  │
│  │  │                   NodeExecutor 接口                         │    │  │
│  │  │  • getType() / getName() / getDescription()                │    │  │
│  │  │  • execute(NodeExecutionContext) → NodeExecutionResult     │    │  │
│  │  │  • init() / destroy() / isAvailable() / validateInputs()   │    │  │
│  │  └────────────────────────────────────────────────────────────┘    │  │
│  │  ┌────────────────────────────────────────────────────────────┐    │  │
│  │  │                   执行器实现                                 │    │  │
│  │  │  ┌────────────────┐  ┌──────────────┐  ┌──────────────┐    │    │  │
│  │  │  │PythonScript    │  │ KlingExecutor│  │ WanExecutor  │    │    │  │
│  │  │  │Executor        │  │              │  │              │    │    │  │
│  │  │  └────────────────┘  └──────────────┘  └──────────────┘    │    │  │
│  │  │  ┌────────────────┐  ┌──────────────┐                      │    │  │
│  │  │  │SeedanceExecutor│  │NanoBanana    │                      │    │  │
│  │  │  │                │  │Executor      │                      │    │  │
│  │  │  └────────────────┘  └──────────────┘                      │    │  │
│  │  └────────────────────────────────────────────────────────────┘    │  │
│  │  ┌────────────────────────────────────────────────────────────┐    │  │
│  │  │  @NodeComponent 自动发现  │  ExecutorRegistry  │  REST API  │    │  │
│  │  └────────────────────────────────────────────────────────────┘    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 实施步骤

### 步骤 1：将 PythonScriptExecutor 迁移到 dag-scheduler

**现状：**
- ai-workflow 有自己的 `PythonScriptExecutor`（内联脚本执行）
- dag-scheduler 已有 `PythonScriptExecutor`（文件路径执行）

**迁移方案：**

1. **增强 dag-scheduler 的 PythonScriptExecutor**，支持内联脚本：
```java
@Component
@NodeComponent(
    types = {"python_script", "python"}, 
    name = "Python Script Executor",
    description = "执行 Python 脚本（支持内联和文件）"
)
public class PythonScriptExecutor implements NodeExecutor {
    
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        // 优先使用内联脚本，其次使用文件路径
        String script = context.getInput("script", String.class);
        String scriptPath = context.getInput("scriptPath", String.class);
        
        if (script != null && !script.isEmpty()) {
            return executeInlineScript(context, script);
        } else if (scriptPath != null) {
            return executeFileScript(context, scriptPath);
        } else {
            return NodeExecutionResult.failed(
                context.getNodeId(),
                "Either 'script' or 'scriptPath' parameter is required",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
            );
        }
    }
    
    private NodeExecutionResult executeInlineScript(NodeExecutionContext context, String script) {
        // 复用 ai-workflow 的内联脚本执行逻辑
        // 创建临时文件 → 执行 → 清理
    }
    
    private NodeExecutionResult executeFileScript(NodeExecutionContext context, String scriptPath) {
        // 现有实现
    }
}
```

2. **删除 ai-workflow 的 PythonScriptExecutor**

3. **修改 DagWorkflowEngine**，从 ExecutorRegistry 获取执行器：
```java
@Component
public class DagWorkflowEngine implements WorkflowEngine {
    
    private final ExecutorRegistry executorRegistry;
    
    @Autowired
    public DagWorkflowEngine(ExecutorRegistry executorRegistry, ...) {
        this.executorRegistry = executorRegistry;
    }
    
    private Object executeNode(String instanceId, WorkflowNode node, Map<String, Object> inputs) {
        String nodeType = node.getType();
        NodeExecutor executor = executorRegistry.getExecutor(nodeType);
        
        if (executor == null) {
            throw new IllegalStateException("No executor found for type: " + nodeType);
        }
        
        // 构建执行上下文
        NodeExecutionContext context = new NodeExecutionContext(
            node.getNodeId(),
            node.getNodeId(), // 可使用节点名称
            nodeType,
            inputs,
            60000 // 超时配置
        );
        
        // 执行
        NodeExecutionResult result = executor.execute(context);
        
        if (!result.isSuccess()) {
            throw new RuntimeException("Node execution failed: " + result.getErrorMessage());
        }
        
        return result.getOutputs();
    }
}
```

**工作量：** 2-3 天
- 增强 dag-scheduler PythonScriptExecutor：1 天
- 修改 DagWorkflowEngine：0.5 天
- 测试与调试：1-1.5 天

---

### 步骤 2：创建 Kling/Wan/Seedance/NanoBanana 执行器

**设计：** 每个模型提供者实现为独立的 NodeExecutor

#### 2.1 KlingExecutor（可灵视频）

```java
@Component
@NodeComponent(
    value = "kling", 
    name = "Kling Video Generator",
    description = "可灵 AI 视频生成模型"
)
public class KlingExecutor implements NodeExecutor {
    
    private final WebClient webClient;
    
    @Override
    public String getType() {
        return "kling";
    }
    
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            String prompt = context.getInput("prompt", String.class);
            String negativePrompt = context.getInput("negativePrompt", String.class);
            List<String> inputImages = context.getInput("inputImages", List.class);
            
            // 调用可灵 API
            KlingResponse response = callKlingAPI(prompt, negativePrompt, inputImages);
            
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("type", "video");
            outputs.put("url", response.getVideoUrl());
            outputs.put("previewUrl", response.getPreviewUrl());
            outputs.put("duration", response.getDuration());
            outputs.put("fps", response.getFps());
            outputs.put("taskId", response.getTaskId());
            
            return NodeExecutionResult.success(
                context.getNodeId(),
                outputs,
                startTime,
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            return NodeExecutionResult.failed(
                context.getNodeId(),
                e.getMessage(),
                NodeExecutionResult.getStackTrace(e),
                startTime,
                LocalDateTime.now()
            );
        }
    }
}
```

#### 2.2 WanExecutor（万相生图）

```java
@Component
@NodeComponent(
    value = "wan", 
    name = "Wan Image Generator",
    description = "通义万相 AI 图像生成模型"
)
public class WanExecutor implements NodeExecutor {
    
    @Override
    public String getType() {
        return "wan";
    }
    
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        // 类似 KlingExecutor，调用万相 API
    }
}
```

#### 2.3 SeedanceExecutor（Seedance 视频）

```java
@Component
@NodeComponent(
    value = "seedance", 
    name = "Seedance Video Generator",
    description = "Seedance AI 视频生成模型"
)
public class SeedanceExecutor implements NodeExecutor {
    
    @Override
    public String getType() {
        return "seedance";
    }
    
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        // 类似 KlingExecutor，调用 Seedance API
    }
}
```

#### 2.4 NanoBananaExecutor（NanoBanana 生图）

```java
@Component
@NodeComponent(
    value = "nanobanana", 
    name = "NanoBanana Image Generator",
    description = "NanoBanana AI 图像生成模型"
)
public class NanoBananaExecutor implements NodeExecutor {
    
    @Override
    public String getType() {
        return "nanobanana";
    }
    
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        // 类似 WanExecutor，调用 NanoBanana API
    }
}
```

**工作量：** 4-6 天
- 每个执行器：1-1.5 天（含 API 对接、错误处理、测试）
- 共 4 个执行器：4-6 天

---

### 步骤 3：修改 DagWorkflowEngine 使用 SPI 执行

**核心改动：**

```java
@Component
public class DagWorkflowEngine implements WorkflowEngine {

    private final ExecutorRegistry executorRegistry;
    private final WebSocketNotificationService notificationService;
    private final ExecutionRepository executionRepository;
    
    // 移除原有的 PythonScriptExecutor 依赖
    // private final PythonScriptExecutor pythonExecutor;
    
    @Autowired
    public DagWorkflowEngine(
            ExecutorRegistry executorRegistry,
            WebSocketNotificationService notificationService, 
            ExecutionRepository executionRepository) {
        this.executorRegistry = executorRegistry;
        this.notificationService = notificationService;
        this.executionRepository = executionRepository;
    }

    @Override
    public Mono<String> execute(Workflow workflow, Map<String, Object> inputs) {
        return Mono.fromCallable(() -> {
            String instanceId = UUID.randomUUID().toString();
            
            // 创建执行记录
            WorkflowExecution execution = createExecution(instanceId, workflow);
            executionRepository.save(execution);
            
            ExecutionState initialState = createInitialState(instanceId, workflow);
            executionStates.put(instanceId, initialState);
            notificationService.notifyExecutionStart(instanceId);
            
            executeAsync(instanceId, workflow, inputs);
            return instanceId;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private void executeAsync(String instanceId, Workflow workflow, Map<String, Object> inputs) {
        try {
            // DAG 拓扑排序
            List<String> executionOrder = topologicalSort(workflow);
            
            Map<String, Object> nodeOutputs = new HashMap<>();
            Map<String, WorkflowExecution.NodeExecutionState> nodeStates = new HashMap<>();
            
            for (String nodeId : executionOrder) {
                WorkflowNode node = findNode(workflow, nodeId);
                if (node == null) continue;

                // 收集节点输入
                Map<String, Object> nodeInputs = collectNodeInputs(workflow, node, nodeOutputs);
                
                // 通知节点开始
                notificationService.notifyNodeStart(instanceId, nodeId);
                
                // 使用 SPI 执行器执行节点
                Object result = executeNodeWithSPI(instanceId, node, nodeInputs);
                nodeOutputs.put(nodeId, result);
                
                // 更新节点状态
                updateNodeState(nodeStates, nodeId, "SUCCESS", result);
                notificationService.notifyNodeComplete(instanceId, nodeId, Map.of("output", result));
            }

            // 执行完成
            updateExecutionStatus(instanceId, "SUCCESS", nodeStates);

        } catch (Exception e) {
            updateExecutionStatus(instanceId, "FAILED", new HashMap<>());
            throw e;
        }
    }

    /**
     * 使用 SPI 执行器执行节点
     */
    private Object executeNodeWithSPI(String instanceId, WorkflowNode node, Map<String, Object> inputs) {
        String nodeType = node.getType();
        
        // 从注册中心获取执行器
        NodeExecutor executor = executorRegistry.getExecutor(nodeType);
        
        if (executor == null) {
            throw new IllegalStateException(
                "No executor registered for node type: " + nodeType + 
                ". Available types: " + executorRegistry.getRegisteredTypes()
            );
        }
        
        // 检查执行器可用性
        if (!executor.isAvailable()) {
            throw new IllegalStateException("Executor is not available: " + nodeType);
        }
        
        // 构建执行上下文
        NodeExecutionContext context = new NodeExecutionContext(
            node.getNodeId(),
            node.getNodeId(),
            nodeType,
            inputs,
            60000 // 可从配置读取
        );
        
        // 验证输入
        if (!executor.validateInputs(context)) {
            throw new IllegalArgumentException("Invalid inputs for node: " + node.getNodeId());
        }
        
        // 执行
        NodeExecutionResult result = executor.execute(context);
        
        if (!result.isSuccess()) {
            throw new RuntimeException(
                "Node execution failed: " + result.getErrorMessage()
            );
        }
        
        return result.getOutputs();
    }
}
```

**工作量：** 1-2 天
- 修改 DagWorkflowEngine：1 天
- 集成测试：0.5-1 天

---

### 步骤 4：补充单元测试

#### 4.1 执行器单元测试

```java
@SpringBootTest
class KlingExecutorTest {
    
    @Autowired
    private KlingExecutor klingExecutor;
    
    @Test
    void testExecute_Success() {
        Map<String, Object> inputs = Map.of(
            "prompt", "一只可爱的小狗在草地上奔跑",
            "negativePrompt", "模糊，低质量"
        );
        
        NodeExecutionContext context = new NodeExecutionContext(
            "test-node-1",
            "Test Kling Node",
            "kling",
            inputs
        );
        
        NodeExecutionResult result = klingExecutor.execute(context);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutputs());
        assertEquals("video", result.getOutputs().get("type"));
    }
    
    @Test
    void testExecute_InvalidPrompt() {
        Map<String, Object> inputs = Map.of(); // 缺少 prompt
        
        NodeExecutionContext context = new NodeExecutionContext(
            "test-node-2",
            "Test Kling Node",
            "kling",
            inputs
        );
        
        NodeExecutionResult result = klingExecutor.execute(context);
        
        assertTrue(result.isFailed());
        assertNotNull(result.getErrorMessage());
    }
}
```

#### 4.2 集成测试

```java
@SpringBootTest
class DagWorkflowEngineIntegrationTest {
    
    @Autowired
    private DagWorkflowEngine engine;
    
    @Autowired
    private ExecutorRegistry executorRegistry;
    
    @Test
    void testWorkflowExecution_WithPythonScript() {
        // 创建工作流
        Workflow workflow = createTestWorkflow();
        
        // 执行
        Mono<String> instanceIdMono = engine.execute(workflow, Map.of());
        
        // 验证
        StepVerifier.create(instanceIdMono)
            .assertNext(id -> assertNotNull(id))
            .verifyComplete();
    }
    
    @Test
    void testWorkflowExecution_WithKlingModel() {
        // 测试 Kling 模型节点执行
    }
}
```

**工作量：** 2-3 天
- 执行器单元测试：1-1.5 天
- 集成测试：1-1.5 天

---

## 工作量估算

| 任务 | 工作量 | 风险 |
|------|--------|------|
| 1. PythonScriptExecutor 迁移 | 2-3 天 | 低 |
| 2. 创建 4 个模型执行器 | 4-6 天 | 中（API 对接） |
| 3. 修改 DagWorkflowEngine | 1-2 天 | 低 |
| 4. 补充单元测试 | 2-3 天 | 低 |
| **总计** | **9-14 天** | |

**风险因素：**
- API 对接复杂度（取决于各模型 API 文档完整性）
- 异步任务处理（某些模型 API 是异步的，需要轮询状态）
- 错误处理边界情况

---

## 优势/劣势

### ✅ 优势

1. **避免重复造轮子**
   - dag-scheduler 已有成熟的 SPI 框架
   - 无需维护两套执行器逻辑

2. **统一 SPI 标准**
   - 所有执行器实现 `NodeExecutor` 接口
   - 代码风格一致，易于理解和维护

3. **扩展性强**
   - 新增模型只需实现 SPI，无需修改引擎
   - 支持运行时动态注册/注销执行器

4. **测试复用**
   - dag-scheduler 已有 95.7% 测试覆盖率
   - 可复用现有测试框架和工具

5. **REST API 管理**
   - 自动获得执行器管理接口
   - 支持启用/禁用/查询执行器

6. **配置外部化**
   - 使用 `DagExecutorProperties` 统一管理配置
   - 支持环境变量和配置文件

### ❌ 劣势

1. **迁移成本**
   - 需要修改现有代码
   - 短期工作量增加

2. **学习曲线**
   - 团队需要熟悉 dag-scheduler SPI
   - 需要理解 NodeExecutor 接口设计

3. **依赖增加**
   - ai-workflow 依赖 dag-scheduler 模块
   - 需要管理模块版本

4. **灵活性降低**
   - 必须遵循 SPI 接口规范
   - 某些特殊场景可能需要额外适配

---

## 迁移检查清单

- [ ] dag-scheduler PythonScriptExecutor 支持内联脚本
- [ ] KlingExecutor 实现并测试
- [ ] WanExecutor 实现并测试
- [ ] SeedanceExecutor 实现并测试
- [ ] NanoBananaExecutor 实现并测试
- [ ] DagWorkflowEngine 改用 ExecutorRegistry
- [ ] 删除 ai-workflow 原有的 PythonScriptExecutor
- [ ] 删除 ai-workflow 原有的 mockModelResult 逻辑
- [ ] 编写所有执行器单元测试
- [ ] 编写集成测试
- [ ] 更新文档
- [ ] 代码审查
- [ ] 性能测试

---

## 后续优化方向

1. **异步执行支持** - 对于耗时模型调用，支持异步任务轮询
2. **执行器缓存** - 缓存模型执行结果，减少 API 调用
3. **限流降级** - 实现 API 限流和失败降级策略
4. **监控告警** - 集成 Prometheus/Grafana 监控执行器性能
5. **执行器热加载** - 支持运行时动态加载新执行器

---

## 结论

**推荐采用方案 A**。虽然短期工作量较大（9-14 天），但长期收益显著：

- 代码质量提升（统一 SPI 标准）
- 维护成本降低（避免重复代码）
- 扩展性增强（新增模型更简单）
- 测试覆盖率高（复用 dag-scheduler 测试）

**适合场景：** 长期维护的项目，需要频繁新增模型提供者

---

*准备与龙傲天 B 辩论！*
