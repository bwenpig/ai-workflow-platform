package com.ben.workflow.engine;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.model.WorkflowEdge;
import com.ben.workflow.model.WorkflowExecution;
import com.ben.workflow.model.WorkflowNode;
import com.ben.workflow.model.PythonNodeConfig;
import com.ben.workflow.engine.PythonExecutionResult;
import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.dagscheduler.registry.ExecutorRegistry;
import com.ben.workflow.repository.ExecutionRepository;
import com.ben.workflow.spi.NotificationService;
import com.ben.workflow.service.ExecutionLogService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DagWorkflowEngine implements WorkflowEngine {

    private final NotificationService notificationService;
    protected ExecutionRepository executionRepository;  // protected 用于测试
    private final ExecutorRegistry executorRegistry;
    private final ExecutionLogService logService;
    private final Map<String, ExecutionState> executionStates = new ConcurrentHashMap<>();
    
    @Autowired
    public DagWorkflowEngine(NotificationService notificationService, 
                            ExecutionRepository executionRepository,
                            ExecutionLogService logService,
                            @Autowired(required = false) ExecutorRegistry executorRegistry) {
        this.notificationService = notificationService;
        this.executionRepository = executionRepository;
        this.logService = logService;
        this.executorRegistry = executorRegistry;
    }
    
    // 测试用构造函数
    DagWorkflowEngine(NotificationService notificationService,
                     Object executionRepository,
                     ExecutionLogService logService,
                     ExecutorRegistry executorRegistry) {
        this.notificationService = notificationService;
        if (executionRepository instanceof ExecutionRepository) {
            this.executionRepository = (ExecutionRepository) executionRepository;
        }
        this.logService = logService;
        this.executorRegistry = executorRegistry;
    }

    @Override
    public Mono<String> execute(Workflow workflow, Map<String, Object> inputs) {
        return execute(workflow, inputs, null);
    }
    
    public Mono<String> execute(Workflow workflow, Map<String, Object> inputs, String existingExecutionId) {
        return Mono.fromCallable(() -> {
            // 如果没有提供 executionId，则生成新的
            String instanceId = existingExecutionId != null ? existingExecutionId : UUID.randomUUID().toString();
            System.out.println("开始执行工作流：workflowId=" + workflow.getId() + ", instanceId=" + instanceId);

            // 检查执行记录是否已存在（如果是从 WorkflowService 预先创建的）
            WorkflowExecution execution = executionRepository.findById(instanceId).orElse(null);
            if (execution == null) {
                // 创建执行记录
                execution = new WorkflowExecution();
                execution.setId(instanceId);
                execution.setWorkflowId(workflow.getId());
                execution.setStatus("RUNNING");
                execution.setInputs(inputs);
                execution.setCreatedAt(Instant.now());
                execution.setStartedAt(Instant.now());
                executionRepository.save(execution);
            } else {
                // 更新已存在的记录
                execution.setStatus("RUNNING");
                execution.setStartedAt(Instant.now());
                execution.setInputs(inputs);
                executionRepository.save(execution);
            }

            ExecutionState initialState = ExecutionState.builder()
                    .instanceId(instanceId)
                    .workflowId(workflow.getId())
                    .status(ExecutionState.Status.RUNNING)
                    .startedAt(Instant.now())
                    .nodeStates(new HashMap<>())
                    .build();
            
            executionStates.put(instanceId, initialState);
            notificationService.notifyExecutionStart(instanceId);
            executeAsync(instanceId, workflow, inputs);
            return instanceId;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private void executeAsync(String instanceId, Workflow workflow, Map<String, Object> inputs) {
        try {
            if (logService != null) logService.addLog(instanceId, null, "info", "工作流开始执行: " + workflow.getName());
            
            List<String> executionOrder = topologicalSort(workflow);
            if (logService != null) logService.addLog(instanceId, null, "info", "DAG拓扑排序完成，共 " + executionOrder.size() + " 个节点");

            Map<String, Object> nodeOutputs = new HashMap<>();
            Map<String, WorkflowExecution.NodeExecutionState> nodeStates = new HashMap<>();
            
            for (String nodeId : executionOrder) {
                WorkflowNode node = findNode(workflow, nodeId);
                if (node == null) continue;

                Map<String, Object> nodeInputs = collectNodeInputs(workflow, node, nodeOutputs);
                if (logService != null) logService.addLog(instanceId, nodeId, "info", "节点 [" + nodeId + "] 开始执行");
                notificationService.notifyNodeStart(instanceId, nodeId);
                
                // 创建节点状态
                WorkflowExecution.NodeExecutionState nodeState = new WorkflowExecution.NodeExecutionState();
                nodeState.setNodeId(nodeId);
                nodeState.setStatus("RUNNING");
                nodeState.setStartedAt(Instant.now());
                nodeStates.put(nodeId, nodeState);
                updateExecutionStatus(instanceId, "RUNNING", nodeStates);
                
                Object result = executeNode(instanceId, node, nodeInputs);
                nodeOutputs.put(nodeId, result);
                
                // 更新节点状态为成功
                nodeState.setStatus("SUCCESS");
                nodeState.setEndedAt(Instant.now());
                nodeState.setResult(result);
                nodeStates.put(nodeId, nodeState);
                updateExecutionStatus(instanceId, "RUNNING", nodeStates);
                
                if (logService != null) logService.addLog(instanceId, nodeId, "success", "节点 [" + nodeId + "] 执行成功");
                notificationService.notifyNodeComplete(instanceId, nodeId, Map.of("output", result));
            }

            // 执行完成
            if (logService != null) logService.addLog(instanceId, null, "success", "工作流执行完成!");
            updateExecutionStatus(instanceId, "SUCCESS", nodeStates);
            System.out.println("工作流执行完成：instanceId=" + instanceId);

        } catch (Exception e) {
            if (logService != null) logService.addLog(instanceId, null, "error", "工作流执行失败: " + e.getMessage());
            System.err.println("工作流执行失败：instanceId=" + instanceId + ", error=" + e.getMessage());
            e.printStackTrace();
            updateExecutionStatus(instanceId, "FAILED", new HashMap<>());
        }
    }

    private void updateExecutionStatus(String instanceId, String status, Map<String, WorkflowExecution.NodeExecutionState> nodeStates) {
        try {
            WorkflowExecution execution = executionRepository.findById(instanceId).orElse(null);
            if (execution == null) {
                System.out.println("警告：未找到执行记录 instanceId=" + instanceId);
                return;
            }
            
            execution.setStatus(status);
            
            // 过滤掉 null key
            if (nodeStates != null) {
                Map<String, WorkflowExecution.NodeExecutionState> filteredStates = new HashMap<>();
                for (Map.Entry<String, WorkflowExecution.NodeExecutionState> entry : nodeStates.entrySet()) {
                    if (entry.getKey() != null) {
                        filteredStates.put(entry.getKey(), entry.getValue());
                    }
                }
                execution.setNodeStates(filteredStates);
            }
            
            if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
                execution.setEndedAt(Instant.now());
                if (execution.getStartedAt() != null) {
                    long duration = execution.getEndedAt().toEpochMilli() - execution.getStartedAt().toEpochMilli();
                    execution.setDurationMs(duration);
                }
            }
            
            executionRepository.save(execution);
            System.out.println("更新执行状态：instanceId=" + instanceId + ", status=" + status);
        } catch (Exception e) {
            System.err.println("更新执行状态失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private Map<String, Object> collectNodeInputs(Workflow workflow, WorkflowNode node, Map<String, Object> outputs) {
        Map<String, Object> inputs = new HashMap<>();
        
        for (WorkflowEdge edge : workflow.getEdges()) {
            if (edge.getTarget().equals(node.getNodeId())) {
                String sourceNodeId = edge.getSource();
                Object sourceOutput = outputs.get(sourceNodeId);
                
                if (sourceOutput == null) continue;
                
                if (sourceOutput instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sourceMap = (Map<String, Object>) sourceOutput;
                    
                    // 1. 以 sourceNodeId 为前缀存储，支持 {{nodeId.field}} 语法
                    for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
                        inputs.put(sourceNodeId + "." + entry.getKey(), entry.getValue());
                    }
                    
                    // 2. 同时扁平化合并（后来的覆盖前面的，保持向后兼容 {{field}} 语法）
                    inputs.putAll(sourceMap);
                    
                    // 3. 如果有 result 键，展开 result 里面的内容
                    if (sourceMap.containsKey("result") && sourceMap.get("result") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resultMap = (Map<String, Object>) sourceMap.get("result");
                        // 同样以 nodeId 前缀存储
                        for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                            inputs.put(sourceNodeId + "." + entry.getKey(), entry.getValue());
                        }
                        inputs.putAll(resultMap);
                    }
                    
                    System.out.println("[DAG] 收集上游输出: " + sourceNodeId + " -> " + node.getNodeId() 
                        + ", keys=" + sourceMap.keySet());
                } else {
                    // 非 Map 类型输出，以 sourceNodeId 为 key 存储
                    inputs.put(sourceNodeId, sourceOutput);
                    System.out.println("[DAG] 收集上游输出(非Map): " + sourceNodeId + " -> " + node.getNodeId()
                        + ", type=" + sourceOutput.getClass().getSimpleName());
                }
            }
        }
        
        // ===== 变量替换：将 config 中的 {{field}} 和 {{nodeId.field}} 替换为实际值 =====
        if (node.getConfig() != null) {
            for (Map.Entry<String, Object> entry : node.getConfig().entrySet()) {
                Object replacedValue = replaceVariables(entry.getValue(), inputs);
                inputs.put(entry.getKey(), replacedValue);
            }
        }
        
        System.out.println("[DAG] 节点 " + node.getNodeId() + " 最终 inputs keys: " + inputs.keySet());
        return inputs;
    }
    
    /**
     * 变量替换：支持 {{field}} 和 {{nodeId.field}} 语法
     */
    private Object replaceVariables(Object value, Map<String, Object> variables) {
        if (value == null) return null;
        if (!(value instanceof String)) return value;
        
        String str = (String) value;
        if (!str.contains("{{") || !str.contains("}}")) {
            return str;
        }
        
        String result = str;
        // 使用正则匹配 {{xxx}} 或 {{xxx.yyy}} 模式
        Pattern pattern = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*\\}\\}");
        Matcher matcher = pattern.matcher(result);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object val = variables.get(varName);
            if (val != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(val.toString()));
            } else {
                // 未找到变量，保留原始占位符
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private List<String> topologicalSort(Workflow workflow) {
        Map<String, WorkflowNode> nodeMap = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (WorkflowNode node : workflow.getNodes()) {
            nodeMap.put(node.getNodeId(), node);
            inDegree.put(node.getNodeId(), 0);
            adjacency.put(node.getNodeId(), new ArrayList<>());
        }

        for (WorkflowEdge edge : workflow.getEdges()) {
            inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
            adjacency.get(edge.getSource()).add(edge.getTarget());
        }

        List<String> result = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();

        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);
            for (String neighbor : adjacency.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        if (result.size() != workflow.getNodes().size()) {
            throw new IllegalStateException("工作流存在循环依赖");
        }
        return result;
    }

    private Object executeNode(String instanceId, WorkflowNode node, Map<String, Object> inputs) {
        System.out.println("执行节点：nodeId=" + node.getNodeId() + ", type=" + node.getType());

        try {
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            Object result;
            String nodeType = node.getType();
            if (nodeType == null) nodeType = "";
            
            switch (nodeType) {
                case "INPUT":
                    result = inputs.getOrDefault("default", node.getConfig().get("value"));
                    break;
                case "MODEL":
                case "kling":
                case "wan":
                case "seedance":
                case "nanobanana":
                    result = executeModel(instanceId, node, inputs);
                    break;
                case "PROCESS":
                    result = processInput(inputs);
                    break;
                case "python_script":
                case "PYTHON_SCRIPT":
                    result = executePythonScript(instanceId, node, inputs);
                    break;
                default:
                    // 尝试使用 SPI 执行器
                    result = executeWithSpi(instanceId, node, inputs);
            }
            return result;
        } catch (Exception e) {
            System.err.println("节点执行失败：nodeId=" + node.getNodeId());
            throw e;
        }
    }

    /**
     * 使用 SPI 执行器执行模型
     */
    private Object executeModel(String instanceId, WorkflowNode node, Map<String, Object> inputs) {
        String nodeType = node.getType();
        if ("MODEL".equals(nodeType)) {
            nodeType = node.getModelProvider();
        }
        
        if (executorRegistry == null) {
            System.out.println("ExecutorRegistry not configured, returning inputs");
            return inputs;
        }
        
        NodeExecutor executor = executorRegistry.getExecutor(nodeType);
        if (executor == null) {
            // 尝试使用通用适配器
            executor = executorRegistry.getExecutor("adapter");
        }
        
        if (executor == null) {
            throw new RuntimeException("未找到模型执行器：type=" + nodeType);
        }
        
        NodeExecutionContext context = new NodeExecutionContext(
            node.getNodeId(),
            node.getNodeId(),
            nodeType,
            inputs,
            0
        );
        
        try {
            NodeExecutionResult execResult = executor.execute(context);
            
            if (!execResult.isSuccess()) {
                throw new RuntimeException("模型执行失败：" + execResult.getErrorMessage());
            }
            
            return execResult.getOutputs();
        } catch (Exception e) {
            throw new RuntimeException("模型执行异常：" + e.getMessage(), e);
        }
    }

    /**
     * 使用 SPI 执行器执行
     */
    private Object executeWithSpi(String instanceId, WorkflowNode node, Map<String, Object> inputs) {
        String nodeType = node.getType();
        if (executorRegistry == null) {
            System.out.println("ExecutorRegistry not configured for SPI");
            return inputs;
        }
        NodeExecutor executor = executorRegistry.getExecutor(nodeType);
        if (executor == null) {
            System.out.println("未找到 SPI 执行器：type=" + nodeType);
            return inputs;
        }
        
        // 直接使用 inputs（已经包含变量替换后的值）
        // 不再重复合并 node.getConfig()，避免覆盖已替换的变量
        NodeExecutionContext context = new NodeExecutionContext(
            node.getNodeId(),
            node.getNodeId(),
            nodeType,
            inputs,
            0
        );
        
        try {
            NodeExecutionResult execResult = executor.execute(context);
            
            if (!execResult.isSuccess()) {
                throw new RuntimeException("执行器执行失败：" + execResult.getErrorMessage());
            }
            
            return execResult.getOutputs();
        } catch (Exception e) {
            throw new RuntimeException("执行器执行异常：" + e.getMessage(), e);
        }
    }

    private Object processInput(Map<String, Object> inputs) {
        return inputs;
    }

    private WorkflowNode findNode(Workflow workflow, String nodeId) {
        return workflow.getNodes().stream()
                .filter(n -> n.getNodeId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Mono<ExecutionState> getState(String instanceId) {
        return Mono.justOrEmpty(executionStates.get(instanceId));
    }

    @Override
    public Mono<Void> cancel(String instanceId) {
        return Mono.fromRunnable(() -> {
            ExecutionState state = executionStates.get(instanceId);
            if (state != null) {
                state.setStatus(ExecutionState.Status.CANCELLED);
                state.setEndedAt(Instant.now());
            }
        });
    }

    @Override
    public Mono<Void> retry(String instanceId, String nodeId) {
        return Mono.empty();
    }

    // ==================== 测试辅助方法 ====================
    
    /**
     * 拓扑排序（公共，用于测试）
     */
    public List<String> topologicalSortForTest(Workflow workflow) {
        return topologicalSort(workflow);
    }

    /**
     * 收集节点输入（公共，用于测试）
     */
    public Map<String, Object> collectNodeInputsForTest(Workflow workflow, WorkflowNode node, Map<String, Object> outputs) {
        return collectNodeInputs(workflow, node, outputs);
    }

    /**
     * 执行节点（公共，用于测试）
     */
    public Object executeNodeForTest(String instanceId, WorkflowNode node, Map<String, Object> inputs) {
        return executeNode(instanceId, node, inputs);
    }

    /**
     * 查找节点（公共，用于测试）
     */
    public WorkflowNode findNodeForTest(Workflow workflow, String nodeId) {
        return findNode(workflow, nodeId);
    }

    /**
     * 更新执行状态（公共，用于测试）
     */
    public void updateExecutionStatusForTest(String instanceId, String status, Map<String, WorkflowExecution.NodeExecutionState> nodeStates) {
        updateExecutionStatus(instanceId, status, nodeStates);
    }

    /**
     * 创建执行状态（公共，用于测试）
     */
    public void createExecutionStateForTest(String instanceId) {
        ExecutionState state = ExecutionState.builder()
                .instanceId(instanceId)
                .workflowId("test-workflow")
                .status(ExecutionState.Status.RUNNING)
                .nodeStates(new HashMap<>())
                .build();
        executionStates.put(instanceId, state);
    }

    /**
     * 执行 Python 脚本节点（使用 SPI）
     */
    private Object executePythonScript(String instanceId, WorkflowNode node, Map<String, Object> inputs) {
        System.out.println("DEBUG executePythonScript: nodeId=" + node.getNodeId() + ", inputs keys=" + (inputs != null ? inputs.keySet() : "null"));
        try {
            Map<String, Object> config = node.getConfig();
            String script = (config != null && config.containsKey("script")) ? (String) config.get("script") : "";
            if (script == null || script.isBlank()) script = "pass";
            Integer timeout = (config != null && config.containsKey("timeout")) ? (Integer) config.get("timeout") : 30;
            
            PythonNodeConfig nodeConfig = new PythonNodeConfig();
            nodeConfig.setScript(script);
            nodeConfig.setTimeout(timeout);
            
            // 使用 PythonScriptExecutor 执行脚本
            PythonScriptExecutor executor = new PythonScriptExecutor();
            PythonExecutionResult result = executor.execute(script, inputs, nodeConfig);
            
            if (!result.isSuccess()) {
                throw new RuntimeException("Python 脚本执行失败: " + result.getError());
            }
            
            return result.getOutputs();
        } catch (Exception e) {
            throw new RuntimeException("Python 脚本执行异常: " + e.getMessage(), e);
        }
    }
}
