# 并行执行 (Parallel Execution) 设计方案

## 概述

并行执行通过 Fork/Join 模式，允许工作流中的多个分支同时执行，提升执行效率。

## 核心概念

### Fork (分支)

多个节点同时启动执行，不等待其他节点完成。

### Join (汇合)

等待所有并行分支完成后，汇总结果继续执行。

## 数据结构设计

### ParallelNodeConfig

```java
@Data
public class ParallelNodeConfig {
    private String forkStrategy;           // 分支策略: "all" | "first" | "count"
    private Integer forkCount;             // 当 forkStrategy="count" 时指定数量
    private JoinStrategy joinStrategy;     // 汇合策略
    private Map<String, Object> options;   // 其他选项
    
    public enum JoinStrategy {
        WAIT_ALL,      // 等待所有分支完成
        WAIT_ANY,      // 任意一个完成即可
        WAIT_COUNT,    // 等待指定数量完成
        FAIL_FAST      // 任意失败则取消其他
    }
}
```

### BranchExecution

```java
@Data
public class BranchExecution {
    private String branchId;
    private String nodeId;
    private NodeResult result;
    private ExecutionStatus status;
    private long startTime;
    private long endTime;
    private String errorMessage;
    
    public enum ExecutionStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }
}
```

### ParallelExecutionContext

```java
@Data
public class ParallelExecutionContext {
    private String executionId;
    private String nodeId;
    private List<BranchExecution> branches;
    private JoinStrategy joinStrategy;
    private int requiredBranchCount;
    private CompletableFuture<Void> completionFuture;
    private volatile ExecutionStatus status;
    
    public enum ExecutionStatus {
        RUNNING, WAITING, COMPLETED, FAILED, CANCELLED
    }
}
```

## 核心类设计

### ParallelExecutor (Fork 节点)

```java
@Component
public class ParallelExecutor extends BaseExecutor {
    
    private final ExecutorService forkJoinPool;
    
    public ParallelExecutor() {
        this.forkJoinPool = new ForkJoinPool(
            Runtime.getRuntime().availableProcessors(),
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null,
            true  // 异步模式
        );
    }
    
    @Override
    public NodeResult execute(NodeExecutionContext context) {
        ParallelNodeConfig config = context.getNode().getParallelConfig();
        List<Node> branchNodes = context.getNode().getBranchNodes();
        
        // 创建并行执行上下文
        ParallelExecutionContext parallelCtx = createParallelContext(context, config, branchNodes);
        
        try {
            // 并行执行所有分支
            List<CompletableFuture<BranchExecution>> branchFutures = 
                branchNodes.stream()
                    .map(node -> executeBranchAsync(node, context, parallelCtx))
                    .collect(Collectors.toList());
            
            // 等待所有分支完成（根据 join 策略）
            List<BranchExecution> results = waitForBranches(
                branchFutures, 
                config.getJoinStrategy(),
                config.getRequiredBranchCount()
            );
            
            // 汇总结果
            return aggregateResults(results, context);
            
        } catch (Exception e) {
            // 取消所有进行中的分支
            cancelAllBranches(parallelCtx);
            return NodeResult.failure("Parallel execution failed: " + e.getMessage());
        }
    }
    
    private CompletableFuture<BranchExecution> executeBranchAsync(
            Node node, 
            NodeExecutionContext parentContext,
            ParallelExecutionContext parallelCtx) {
        
        return CompletableFuture.supplyAsync(() -> {
            BranchExecution branch = new BranchExecution();
            branch.setBranchId(UUID.randomUUID().toString());
            branch.setNodeId(node.getId());
            branch.setStatus(BranchExecution.ExecutionStatus.RUNNING);
            branch.setStartTime(System.currentTimeMillis());
            
            try {
                // 创建分支执行上下文（隔离）
                NodeExecutionContext branchContext = createBranchContext(
                    parentContext, 
                    node,
                    parallelCtx.getExecutionId()
                );
                
                // 获取对应 Executor 执行
                NodeExecutor executor = executorRegistry.get(node.getType());
                NodeResult result = executor.execute(branchContext);
                
                branch.setResult(result);
                branch.setStatus(result.isSuccess() 
                    ? BranchExecution.ExecutionStatus.COMPLETED 
                    : BranchExecution.ExecutionStatus.FAILED);
                    
            } catch (Exception e) {
                branch.setStatus(BranchExecution.ExecutionStatus.FAILED);
                branch.setErrorMessage(e.getMessage());
            } finally {
                branch.setEndTime(System.currentTimeMillis());
            }
            
            return branch;
            
        }, forkJoinPool);
    }
    
    private List<BranchExecution> waitForBranches(
            List<CompletableFuture<BranchExecution>> futures,
            JoinStrategy strategy,
            int requiredCount) {
        
        switch (strategy) {
            case WAIT_ALL:
                return futures.stream()
                    .map(f -> f.join())
                    .collect(Collectors.toList());
                    
            case WAIT_ANY:
                BranchExecution first = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(b -> b.getStatus() == BranchExecution.ExecutionStatus.COMPLETED)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No branch completed"));
                return Collections.singletonList(first);
                
            case WAIT_COUNT:
                return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(b -> b.getStatus() == BranchExecution.ExecutionStatus.COMPLETED)
                    .limit(requiredCount)
                    .collect(Collectors.toList());
                    
            case FAIL_FAST:
                for (CompletableFuture<BranchExecution> future : futures) {
                    BranchExecution result = future.join();
                    if (result.getStatus() == BranchExecution.ExecutionStatus.FAILED) {
                        throw new RuntimeException("Branch failed: " + result.getErrorMessage());
                    }
                }
                return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
                    
            default:
                throw new IllegalArgumentException("Unknown join strategy: " + strategy);
        }
    }
    
    private NodeResult aggregateResults(
            List<BranchExecution> branches, 
            NodeExecutionContext context) {
        
        NodeResult result = new NodeResult();
        Map<String, Object> outputs = new HashMap<>();
        
        // 汇总所有分支结果
        List<Map<String, Object>> branchResults = branches.stream()
            .map(b -> {
                Map<String, Object> r = new HashMap<>();
                r.put("branchId", b.getBranchId());
                r.put("nodeId", b.getNodeId());
                r.put("status", b.getStatus());
                r.put("outputs", b.getResult() != null ? b.getResult().getOutputs() : null);
                r.put("duration", b.getEndTime() - b.getStartTime());
                return r;
            })
            .collect(Collectors.toList());
        
        outputs.put("branches", branchResults);
        outputs.put("totalBranches", branches.size());
        outputs.put("successfulBranches", 
            (int) branches.stream().filter(b -> b.getStatus() == BranchExecution.ExecutionStatus.COMPLETED).count());
        
        // 合并所有分支的输出到主上下文
        for (BranchExecution branch : branches) {
            if (branch.getResult() != null && branch.getResult().getOutputs() != null) {
                context.getExecutionContext().putAll(branch.getResult().getOutputs());
            }
        }
        
        result.setSuccess(true);
        result.setOutputs(outputs);
        return result;
    }
    
    private NodeExecutionContext createBranchContext(
            NodeExecutionContext parent,
            Node node,
            String executionId) {
        
        // 创建隔离的分支上下文
        NodeExecutionContext branchContext = new NodeExecutionContext();
        branchContext.setWorkflowId(parent.getWorkflowId());
        branchContext.setInstanceId(parent.getInstanceId());
        branchContext.setNode(node);
        branchContext.setExecutionId(executionId + "_" + node.getId());
        
        // 复制变量副本，避免共享状态
        branchContext.setExecutionContext(new HashMap<>(parent.getExecutionContext()));
        
        return branchContext;
    }
}
```

### JoinExecutor (汇合节点)

```java
@Component
public class JoinExecutor extends BaseExecutor {
    
    @Override
    public NodeResult execute(NodeExecutionContext context) {
        JoinNodeConfig config = context.getNode().getJoinConfig();
        
        // Join 节点被动等待，由 Parallel 节点触发
        // 检查所有并行分支是否完成
        
        String parallelNodeId = config.getSourceParallelNodeId();
        ParallelExecutionContext parallelCtx = getParallelExecutionContext(parallelNodeId);
        
        if (parallelCtx == null) {
            return NodeResult.failure("Parallel execution context not found for: " + parallelNodeId);
        }
        
        // 等待并行执行完成
        parallelCtx.getCompletionFuture().join();
        
        // 聚合结果
        List<BranchExecution> branches = parallelCtx.getBranches();
        
        return aggregateJoinResults(branches, config);
    }
    
    private NodeResult aggregateJoinResults(List<BranchExecution> branches, JoinNodeConfig config) {
        NodeResult result = new NodeResult();
        Map<String, Object> outputs = new HashMap<>();
        
        switch (config.getAggregationType()) {
            case MERGE:
                // 合并所有输出
                Map<String, Object> merged = new HashMap<>();
                for (BranchExecution b : branches) {
                    if (b.getResult() != null) {
                        merged.putAll(b.getResult().getOutputs());
                    }
                }
                outputs.put("merged", merged);
                break;
                
            case COLLECT:
                // 收集为数组
                List<Object> collected = branches.stream()
                    .filter(b -> b.getResult() != null)
                    .map(b -> b.getResult().getOutputs())
                    .collect(Collectors.toList());
                outputs.put("collected", collected);
                break;
                
            case FIRST:
                // 取第一个成功的结果
                BranchExecution first = branches.stream()
                    .filter(b -> b.getStatus() == BranchExecution.ExecutionStatus.COMPLETED)
                    .findFirst()
                    .orElse(null);
                if (first != null && first.getResult() != null) {
                    outputs.put("first", first.getResult().getOutputs());
                }
                break;
        }
        
        result.setSuccess(true);
        result.setOutputs(outputs);
        return result;
    }
}
```

## API 设计

### Parallel 节点配置

```json
{
    "type": "parallel",
    "name": "并行处理",
    "config": {
        "forkStrategy": "all",
        "joinStrategy": "WAIT_ALL",
        "branches": [
            {
                "nodeId": "node_a",
                "name": "分支A"
            },
            {
                "nodeId": "node_b", 
                "name": "分支B"
            },
            {
                "nodeId": "node_c",
                "name": "分支C"
            }
        ],
        "options": {
            "cancelOnFail": true,
            "maxConcurrent": 10
        }
    }
}
```

### Join 节点配置

```json
{
    "type": "join",
    "name": "等待所有分支",
    "config": {
        "sourceParallelNodeId": "parallel_1",
        "aggregationType": "COLLECT",
        "timeout": 60000
    }
}
```

## 执行流程图

```
           [Fork]
          /  |  \
    [Branch A] [Branch B] [Branch C]
      (并行)    (并行)      (并行)
          \  |  /
         [Join]
           ↓
    [聚合结果]
```

## 线程安全设计

### 1. 使用 ThreadLocal 隔离上下文

```java
public class ExecutionContextHolder {
    private static final ThreadLocal<Map<String, Object>> contextHolder = 
        new ThreadLocal<>();
    
    public static void setContext(Map<String, Object> context) {
        contextHolder.set(new HashMap<>(context));
    }
    
    public static Map<String, Object> getContext() {
        return contextHolder.get();
    }
    
    public static void clear() {
        contextHolder.remove();
    }
}
```

### 2. 使用 AtomicReference 管理状态

```java
public class ParallelExecutionContext {
    private final AtomicReference<ExecutionStatus> status = 
        new AtomicReference<>(ExecutionStatus.RUNNING);
    
    private final ConcurrentHashMap<String, BranchExecution> branches = 
        new ConcurrentHashMap<>();
    
    public boolean markBranchComplete(String branchId, BranchExecution result) {
        branches.put(branchId, result);
        
        if (joinStrategy == JoinStrategy.FAIL_FAST && 
            result.getStatus() == BranchExecution.ExecutionStatus.FAILED) {
            status.set(ExecutionStatus.FAILED);
            return false;
        }
        
        return true;
    }
}
```

### 3. 使用 CountDownLatch 等待

```java
public class JoinExecutor {
    public void waitForCompletion(ParallelExecutionContext ctx, long timeoutMs) 
            throws TimeoutException {
        
        CountDownLatch latch = new CountDownLatch(ctx.getBranches().size());
        
        ctx.getBranches().forEach((id, branch) -> {
            branch.getCompletionFuture().whenComplete((result, ex) -> {
                latch.countDown();
            });
        });
        
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Join等待超时");
        }
    }
}
```

## 测试用例建议

### 1. 基本并行执行测试

```java
@Test
void testParallelExecution() {
    // 创建3个并行分支
    List<Node> branches = List.of(
        createNode("A", executorA),
        createNode("B", executorB),
        createNode("C", executorC)
    );
    
    ParallelNodeConfig config = new ParallelNodeConfig();
    config.setForkStrategy("all");
    config.setJoinStrategy(JoinStrategy.WAIT_ALL);
    
    NodeResult result = parallelExecutor.execute(createContext(branches, config));
    
    assertTrue(result.isSuccess());
    assertEquals(3, result.getOutputs().get("totalBranches"));
}
```

### 2. Join 策略测试

```java
@Test
void testJoinWaitAny() {
    config.setJoinStrategy(JoinStrategy.WAIT_ANY);
    
    NodeResult result = parallelExecutor.execute(createContext(branches, config));
    
    // 验证只返回第一个完成的结果
    assertEquals(1, ((List<?>) result.getOutputs().get("branches")).size());
}
```

### 3. 失败快速响应测试

```java
@Test
void testFailFast() {
    config.setJoinStrategy(JoinStrategy.FAIL_FAST);
    
    // 模拟一个分支失败
    when(executorB.execute(any())).thenThrow(new RuntimeException("Branch B failed"));
    
    assertThrows(RuntimeException.class, () -> {
        parallelExecutor.execute(createContext(branches, config));
    });
}
```

### 4. 性能测试

```java
@Test
void testParallelPerformance() {
    int branchCount = 100;
    List<Node> branches = createManyBranches(branchCount);
    
    long start = System.currentTimeMillis();
    NodeResult result = parallelExecutor.execute(createContext(branches, config));
    long duration = System.currentTimeMillis() - start;
    
    // 验证并行执行比串行快
    assertTrue(duration < branchCount * 100); // 假设单分支100ms
}
```

## 注意事项

1. **资源限制**: 限制最大并发数，避免资源耗尽
2. **上下文隔离**: 每个分支应有独立的变量作用域
3. **取消传播**: 父流程取消时，应取消所有进行中的分支
4. **超时处理**: 设置整体超时，防止无限等待
5. **错误收集**: 记录所有分支的错误信息，便于排查