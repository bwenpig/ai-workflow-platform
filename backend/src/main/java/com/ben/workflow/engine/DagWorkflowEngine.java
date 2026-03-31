package com.ben.workflow.engine;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.model.WorkflowEdge;
import com.ben.workflow.model.WorkflowExecution;
import com.ben.workflow.model.WorkflowNode;
import com.ben.workflow.model.PythonNodeConfig;
import com.ben.workflow.engine.PythonExecutionResult;
import com.ben.workflow.spi.ModelExecutor;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import com.ben.workflow.spi.ExecutorRegistry;
import com.ben.workflow.repository.ExecutionRepository;
import com.ben.workflow.websocket.WebSocketNotificationService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DagWorkflowEngine implements WorkflowEngine {

    private final WebSocketNotificationService notificationService;
    private final ExecutionRepository executionRepository;
    private final ExecutorRegistry executorRegistry;
    private final Map<String, ExecutionState> executionStates = new ConcurrentHashMap<>();
    
    @Autowired
    public DagWorkflowEngine(WebSocketNotificationService notificationService, 
                            ExecutionRepository executionRepository,
                            ExecutorRegistry executorRegistry) {
        this.notificationService = notificationService;
        this.executionRepository = executionRepository;
        this.executorRegistry = executorRegistry;
    }

    @Override
    public Mono<String> execute(Workflow workflow, Map<String, Object> inputs) {
        return Mono.fromCallable(() -> {
            String instanceId = UUID.randomUUID().toString();
            System.out.println("开始执行工作流：workflowId=" + workflow.getId() + ", instanceId=" + instanceId);

            // 创建执行记录
            WorkflowExecution execution = new WorkflowExecution();
            execution.setId(instanceId);
            execution.setWorkflowId(workflow.getId());
            execution.setStatus("RUNNING");
            execution.setInputs(inputs);
            execution.setCreatedAt(Instant.now());
            execution.setStartedAt(Instant.now());
            executionRepository.save(execution);

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
            List<String> executionOrder = topologicalSort(workflow);
            System.out.println("DAG 拓扑排序完成：order=" + executionOrder);

            Map<String, Object> nodeOutputs = new HashMap<>();
            Map<String, WorkflowExecution.NodeExecutionState> nodeStates = new HashMap<>();
            
            for (String nodeId : executionOrder) {
                WorkflowNode node = findNode(workflow, nodeId);
                if (node == null) continue;

                Map<String, Object> nodeInputs = collectNodeInputs(workflow, node, nodeOutputs);
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
                
                notificationService.notifyNodeComplete(instanceId, nodeId, Map.of("output", result));
            }

            // 执行完成
            updateExecutionStatus(instanceId, "SUCCESS", nodeStates);
            System.out.println("工作流执行完成：instanceId=" + instanceId);

        } catch (Exception e) {
            System.err.println("工作流执行失败：instanceId=" + instanceId + ", error=" + e.getMessage());
            e.printStackTrace();
            updateExecutionStatus(instanceId, "FAILED", new HashMap<>());
        }
    }

    private void updateExecutionStatus(String instanceId, String status, Map<String, WorkflowExecution.NodeExecutionState> nodeStates) {
        try {
            WorkflowExecution execution = executionRepository.findById(instanceId).orElse(null);
            if (execution != null) {
                execution.setStatus(status);
                execution.setNodeStates(nodeStates);
                if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
                    execution.setEndedAt(Instant.now());
                    long duration = execution.getEndedAt().toEpochMilli() - execution.getStartedAt().toEpochMilli();
                    execution.setDurationMs(duration);
                }
                executionRepository.save(execution);
            }
        } catch (Exception e) {
            System.err.println("更新执行状态失败：" + e.getMessage());
        }
    }

    private Map<String, Object> collectNodeInputs(Workflow workflow, WorkflowNode node, Map<String, Object> outputs) {
        Map<String, Object> inputs = new HashMap<>();
        for (WorkflowEdge edge : workflow.getEdges()) {
            if (edge.getTarget().equals(node.getNodeId())) {
                Object sourceOutput = outputs.get(edge.getSource());
                if (sourceOutput != null) {
                    inputs.put(edge.getTargetHandle(), sourceOutput);
                }
            }
        }
        return inputs;
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
        
        ModelExecutor executor = executorRegistry.getExecutor(nodeType);
        if (executor == null) {
            // 尝试使用通用适配器
            executor = executorRegistry.getExecutor("adapter");
        }
        
        if (executor == null) {
            throw new RuntimeException("未找到模型执行器：type=" + nodeType);
        }
        
        ModelExecutionContext context = new ModelExecutionContext();
        context.setInstanceId(instanceId);
        context.setNodeId(node.getNodeId());
        context.setNodeType(nodeType);
        context.setInputs(inputs);
        context.setConfig(node.getConfig());
        
        ModelExecutionResult execResult = executor.execute(context);
        
        if (!execResult.isSuccess()) {
            throw new RuntimeException("模型执行失败：" + execResult.getError());
        }
        
        return execResult.getData();
    }

    /**
     * 使用 SPI 执行器执行
     */
    private Object executeWithSpi(String instanceId, WorkflowNode node, Map<String, Object> inputs) {
        String nodeType = node.getType();
        ModelExecutor executor = executorRegistry.getExecutor(nodeType);
        if (executor == null) {
            System.out.println("未找到 SPI 执行器：type=" + nodeType);
            return inputs;
        }
        
        ModelExecutionContext context = new ModelExecutionContext();
        context.setInstanceId(instanceId);
        context.setNodeId(node.getNodeId());
        context.setNodeType(nodeType);
        context.setInputs(inputs);
        context.setConfig(node.getConfig());
        
        ModelExecutionResult execResult = executor.execute(context);
        
        if (!execResult.isSuccess()) {
            throw new RuntimeException("执行器执行失败：" + execResult.getError());
        }
        
        return execResult.getData();
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
        return executeWithSpi(instanceId, node, inputs);
    }
}
