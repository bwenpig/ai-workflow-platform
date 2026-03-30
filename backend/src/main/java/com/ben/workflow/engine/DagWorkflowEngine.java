package com.ben.workflow.engine;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.model.WorkflowEdge;
import com.ben.workflow.model.WorkflowExecution;
import com.ben.workflow.model.WorkflowNode;
import com.ben.workflow.model.PythonNodeConfig;
import com.ben.workflow.engine.PythonExecutionResult;
import com.ben.workflow.repository.ExecutionRepository;
import com.ben.workflow.websocket.WebSocketNotificationService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DagWorkflowEngine implements WorkflowEngine {

    private final WebSocketNotificationService notificationService;
    private final ExecutionRepository executionRepository;
    private final PythonScriptExecutor pythonExecutor;
    private final Map<String, ExecutionState> executionStates = new ConcurrentHashMap<>();
    
    public DagWorkflowEngine(WebSocketNotificationService notificationService, 
                            ExecutionRepository executionRepository,
                            PythonScriptExecutor pythonExecutor) {
        this.notificationService = notificationService;
        this.executionRepository = executionRepository;
        this.pythonExecutor = pythonExecutor;
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
                    result = mockModelResult(node);
                    break;
                case "PROCESS":
                    result = processInput(inputs);
                    break;
                case "PYTHON_SCRIPT":
                    result = executePythonScript(node, inputs);
                    break;
                default:
                    result = inputs;
            }
            return result;
        } catch (Exception e) {
            System.err.println("节点执行失败：nodeId=" + node.getNodeId());
            throw e;
        }
    }

    private Object mockModelResult(WorkflowNode node) {
        String modelProvider = node.getModelProvider();
        Map<String, Object> result = new HashMap<>();
        result.put("modelProvider", modelProvider);
        
        switch (modelProvider != null ? modelProvider : "") {
            case "kling":
                result.put("type", "video");
                result.put("url", "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4");
                result.put("previewUrl", "https://via.placeholder.com/640x360.png?text=Kling+Video");
                result.put("duration", 5);
                result.put("fps", 24);
                break;
            case "wan":
                result.put("type", "image");
                result.put("url", "https://via.placeholder.com/1024x1024.png?text=Wan+Image");
                result.put("width", 1024);
                result.put("height", 1024);
                break;
            case "seedance":
                result.put("type", "video");
                result.put("url", "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4");
                result.put("previewUrl", "https://via.placeholder.com/640x360.png?text=Seedance+Video");
                result.put("duration", 10);
                result.put("fps", 30);
                break;
            case "nanobanana":
                result.put("type", "image");
                result.put("url", "https://via.placeholder.com/1024x1024.png?text=NanoBanana+Image");
                result.put("width", 1024);
                result.put("height", 1024);
                break;
            default:
                result.put("type", "image");
                result.put("url", "https://via.placeholder.com/512x512.png?text=Mock+Output");
        }
        return result;
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

    /**
     * 执行 Python 脚本节点
     */
    private Object executePythonScript(WorkflowNode node, Map<String, Object> inputs) {
        System.out.println("执行 Python 脚本节点：nodeId=" + node.getNodeId());
        
        try {
            PythonNodeConfig config = new PythonNodeConfig();
            if (node.getConfig() != null) {
                config.setScript((String) node.getConfig().get("script"));
                config.setTimeout((Integer) node.getConfig().get("timeout"));
                config.setRequirements((List<String>) node.getConfig().get("requirements"));
            }
            
            PythonExecutionResult execResult = pythonExecutor.execute(
                config.getScript(), 
                inputs, 
                config
            );
            
            if (!execResult.isSuccess()) {
                throw new RuntimeException("Python 脚本执行失败：" + execResult.getError());
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("type", "python_script");
            result.put("outputs", execResult.getOutputs());
            result.put("logs", execResult.getLogs());
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Python 脚本节点执行失败：" + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
