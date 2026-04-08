# 子工作流 (Sub-workflow) 设计方案

## 概述

子工作流允许在父工作流中调用已存在的工作流，实现流程复用和模块化。

## 数据结构设计

### SubWorkflowReference

```java
@Data
@Document
public class SubWorkflowReference {
    private String id;
    private String workflowId;          // 引用的工作流ID
    private String name;                // 显示名称
    private Map<String, Object> inputMapping;   // 输入映射: 父节点输出 → 子流程输入
    private Map<String, String> outputMapping;  // 输出映射: 子流程输出 → 父节点输出
    
    private TimeoutConfig timeout;       // 超时配置
    private RetryConfig retryConfig;    // 重试配置
}

@Data
public class TimeoutConfig {
    private long timeoutMs = 300000;    // 默认5分钟
    private TimeoutAction action = TimeoutAction.FAIL;
    public enum TimeoutAction { FAIL, SKIP, CONTINUE }
}

@Data
public class RetryConfig {
    private int maxAttempts = 3;
    private long retryDelayMs = 1000;
    private List<Class<? extends Exception>> retryableExceptions;
}
```

### SubWorkflowExecution

```java
@Data
public class SubWorkflowExecution {
    private String executionId;
    private String parentExecutionId;
    private String subWorkflowId;
    private String subWorkflowName;
    private ExecutionStatus status;
    private Map<String, Object> inputs;
    private Map<String, Object> outputs;
    private long startTime;
    private long endTime;
    private String errorMessage;
    
    public enum ExecutionStatus {
        PENDING, RUNNING, COMPLETED, FAILED, TIMEOUT, CANCELLED
    }
}
```

## 核心类设计

### SubWorkflowExecutor

```java
@Component
public class SubWorkflowExecutor extends BaseExecutor {
    
    @Autowired
    private WorkflowService workflowService;
    
    @Autowired
    private ExecutionRepository executionRepository;
    
    @Autowired
    private ExecutorRegistry executorRegistry;
    
    @Override
    public NodeResult execute(NodeExecutionContext context) {
        SubWorkflowReference reference = context.getNode().getSubWorkflowReference();
        
        // 1. 构建子流程输入
        Map<String, Object> inputs = buildInputs(context, reference.getInputMapping());
        
        // 2. 创建子流程执行记录
        SubWorkflowExecution execution = createExecution(context, reference, inputs);
        
        try {
            // 3. 异步执行子流程
            CompletableFuture<NodeResult> future = CompletableFuture.supplyAsync(() -> {
                return executeSubWorkflow(reference.getWorkflowId(), inputs, context);
            });
            
            // 4. 等待结果或超时
            NodeResult result = waitForResult(future, reference.getTimeout());
            
            // 5. 处理输出映射
            handleOutputs(context, result, reference.getOutputMapping());
            
            execution.setStatus(SubWorkflowExecution.ExecutionStatus.COMPLETED);
            return result;
            
        } catch (TimeoutException e) {
            execution.setStatus(SubWorkflowExecution.ExecutionStatus.TIMEOUT);
            execution.setErrorMessage("Sub-workflow execution timeout");
            return NodeResult.failure("Sub-workflow timeout: " + e.getMessage());
        } catch (Exception e) {
            execution.setStatus(SubWorkflowExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            return NodeResult.failure("Sub-workflow failed: " + e.getMessage());
        } finally {
            executionRepository.save(execution);
        }
    }
    
    private Map<String, Object> buildInputs(NodeExecutionContext context, 
            Map<String, Object> inputMapping) {
        Map<String, Object> inputs = new HashMap<>();
        
        if (inputMapping != null) {
            for (Map.Entry<String, Object> entry : inputMapping.entrySet()) {
                String targetKey = entry.getKey();
                Object sourceValue = resolveExpression(entry.getValue().toString(), context);
                inputs.put(targetKey, sourceValue);
            }
        }
        
        return inputs;
    }
    
    private NodeResult executeSubWorkflow(String workflowId, 
            Map<String, Object> inputs, 
            NodeExecutionContext parentContext) {
        
        // 创建子流程执行实例
        WorkflowInstance instance = workflowService.createInstance(workflowId, inputs);
        
        // 使用 DagWorkflowEngine 执行
        DagWorkflowEngine engine = new DagWorkflowEngine(
            workflowService, 
            executorRegistry,
            executionRepository
        );
        
        return engine.execute(instance);
    }
    
    private NodeResult waitForResult(CompletableFuture<NodeResult> future, 
            TimeoutConfig timeout) {
        try {
            return future.get(timeout.getTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
```

### BaseExecutor (父类扩展)

```java
public abstract class BaseExecutor implements NodeExecutor {
    
    protected Object resolveExpression(String expression, NodeExecutionContext context) {
        // 解析 ${nodeId.output} 格式的表达式
        if (expression.startsWith("${") && expression.endsWith("}")) {
            String path = expression.substring(2, expression.length() - 1);
            return resolvePath(context.getExecutionContext(), path);
        }
        return expression;
    }
    
    protected Object resolvePath(Map<String, Object> context, String path) {
        String[] parts = path.split("\\.");
        Object current = context;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else if (current instanceof NodeResult) {
                current = ((NodeResult) current).getOutputs().get(part);
            }
        }
        
        return current;
    }
    
    protected void handleOutputs(NodeExecutionContext context, 
            NodeResult result, Map<String, String> outputMapping) {
        if (outputMapping != null && result != null) {
            for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
                String sourceKey = entry.getValue();
                String targetKey = entry.getKey();
                
                Object value = result.getOutputs().get(sourceKey);
                context.getExecutionContext().put(targetKey, value);
            }
        }
    }
}
```

## API 设计

### 节点配置 API

```java
// POST /api/nodes/sub-workflow
{
    "type": "sub_workflow",
    "name": "调用数据处理子流程",
    "config": {
        "workflowId": "wf_123456",
        "workflowName": "数据处理流程",
        "inputMapping": {
            "data": "${node_1.output.data}",
            "config": "${node_2.output.config}"
        },
        "outputMapping": {
            "result": "output.result",
            "metadata": "output.metadata"
        },
        "timeout": {
            "timeoutMs": 60000,
            "action": "FAIL"
        },
        "retry": {
            "maxAttempts": 3,
            "retryDelayMs": 2000
        }
    }
}
```

### 节点类型枚举扩展

```java
public enum NodeType {
    INPUT("input", InputExecutor.class),
    OUTPUT("output", OutputExecutor.class),
    MODEL("model", LLMNodeExecutor.class),
    CONDITIONAL("conditional", ConditionalExecutor.class),
    LOOP("loop", LoopExecutor.class),
    HTTP_REQUEST("http_request", HttpRequestExecutor.class),
    SUB_WORKFLOW("sub_workflow", SubWorkflowExecutor.class),  // 新增
    PARALLEL("parallel", ParallelExecutor.class),             // 新增
    JOIN("join", JoinExecutor.class);                          // 新增
}
```

## 执行流程图

```
父工作流
    │
    ▼
[SubWorkflow Node]
    │
    ├─→ 1. 构建输入映射
    │       ${node_1.output} → inputs.data
    │
    ├─→ 2. 创建执行记录
    │       SubWorkflowExecution
    │
    ├─→ 3. 异步执行子流程
    │       CompletableFuture.supplyAsync()
    │
    ├─→ 4. 等待结果/超时
    │       future.get(timeoutMs)
    │
    ├─→ 5. 处理输出映射
    │       output.result → ${context.result}
    │
    ▼
[继续执行]
```

## 测试用例建议

### 1. 基础功能测试

```java
@Test
void testSubWorkflowBasicExecution() {
    // 创建子工作流
    Workflow subWorkflow = createSubWorkflow();
    
    // 创建父工作流引用子工作流
    Workflow parentWorkflow = createParentWorkflow(subWorkflow.getId());
    
    // 执行
    NodeResult result = executor.execute(createContext(parentWorkflow));
    
    // 验证
    assertTrue(result.isSuccess());
    assertNotNull(result.getOutputs().get("result"));
}
```

### 2. 输入输出映射测试

```java
@Test
void testInputOutputMapping() {
    // 设置输入映射
    Map<String, Object> inputMapping = Map.of(
        "data", "${node_1.output.data",
        "config", "${node_2.output.config}"
    );
    
    // 设置输出映射
    Map<String, String> outputMapping = Map.of(
        "result", "output.result"
    );
    
    // 验证映射正确
}
```

### 3. 超时测试

```java
@Test
void testSubWorkflowTimeout() {
    // 创建长时间运行的子工作流
    Workflow longRunningWorkflow = createLongRunningWorkflow(120000);
    
    // 设置超时 5 秒
    TimeoutConfig timeout = new TimeoutConfig(5000, TimeoutAction.FAIL);
    
    // 执行并验证超时
    assertThrows(TimeoutException.class, () -> {
        executor.executeWithTimeout(timeout);
    });
}
```

### 4. 并发测试

```java
@Test
void testConcurrentSubWorkflowExecution() {
    // 同时调用多个子工作流
    List<CompletableFuture<NodeResult>> futures = new ArrayList<>();
    
    for (int i = 0; i < 10; i++) {
        CompletableFuture<NodeResult> future = CompletableFuture.supplyAsync(() -> {
            return executor.execute(createContext(workflows.get(i)));
        });
        futures.add(future);
    }
    
    // 等待所有完成
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    
    // 验证所有成功
    assertTrue(futures.stream().allMatch(NodeResult::isSuccess));
}
```

## 注意事项

1. **上下文隔离**: 子工作流应有独立的变量作用域，避免污染父流程
2. **循环引用检测**: 防止 A → B → A 死循环
3. **资源限制**: 限制并发子流程数量，防止资源耗尽
4. **错误传播**: 子流程失败时应有明确的错误信息
5. **日志追踪**: 记录完整的调用链，便于调试