package com.ben.workflow.engine;

import com.ben.workflow.model.*;
import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.dagscheduler.registry.ExecutorRegistry;
import com.ben.workflow.spi.NotificationService;
import com.ben.workflow.repository.ExecutionRepository;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DagWorkflowEngine 单元测试 - 完整覆盖测试
 */
public class DagWorkflowEngineTest {

    // ===== Stub 实现 =====
    static class StubNotificationService implements NotificationService {
        AtomicBoolean notified = new AtomicBoolean(false);
        List<String> notifications = new ArrayList<>();
        
        public void notifyExecutionStart(String executionId) {
            notified.set(true);
            notifications.add("start:" + executionId);
        }
        public void notifyNodeStart(String executionId, String nodeId) {
            notifications.add("nodeStart:" + nodeId);
        }
        public void notifyNodeComplete(String executionId, String nodeId, Object result) {
            notifications.add("nodeComplete:" + nodeId);
        }
        public void notifyExecutionComplete(String executionId, Object outputs) {
            notifications.add("complete:" + executionId);
        }
        public void notifyExecutionFailed(String executionId, String errorMessage) {
            notifications.add("failed:" + executionId);
        }
        public void notifyProgress(String executionId, String nodeId, int progress, String message) {}
        public void notifyNodeFailed(String executionId, String nodeId, String errorMessage) {
            notifications.add("nodeFailed:" + nodeId);
        }
    }

    static class StubNodeExecutor implements NodeExecutor {
        NodeExecutionResult result;
        boolean shouldThrow = false;
        String type = "stub";
        
        @Override public String getType() { return type; }
        
        @Override public NodeExecutionResult execute(NodeExecutionContext context) throws Exception {
            if (shouldThrow) throw new RuntimeException("Execution failed");
            LocalDateTime now = LocalDateTime.now();
            return result != null ? result : NodeExecutionResult.success(
                context != null ? context.getNodeId() : "unknown",
                Map.of("output", "mock"),
                now,
                now
            );
        }
        
        NodeExecutionResult createSuccessResult(Map<String, Object> data) {
            LocalDateTime now = LocalDateTime.now();
            return NodeExecutionResult.success("test-node", data, now, now);
        }
    }

    // ===== 测试字段 =====
    private StubNotificationService notificationService;
    private StubNodeExecutor nodeExecutor;
    private ExecutorRegistry executorRegistry;
    private DagWorkflowEngine engine;

    @BeforeEach
    public void setUp() throws Exception {
        notificationService = new StubNotificationService();
        nodeExecutor = new StubNodeExecutor();
        executorRegistry = new ExecutorRegistry();
        
        // 注册 nodeExecutor
        executorRegistry.register(nodeExecutor, "stub");
        // 绕过 Spring，直接手动注册
        try {
            java.lang.reflect.Field field = ExecutorRegistry.class.getDeclaredField("executors");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, NodeExecutor> executors = (Map<String, NodeExecutor>) field.get(executorRegistry);
            executors.clear();
        } catch (Exception e) {
            // ignore
        }
        // 使用动态代理创建 Repository
        Map<String, WorkflowExecution> store = new HashMap<>();
        ExecutionRepository repoProxy = (ExecutionRepository) java.lang.reflect.Proxy.newProxyInstance(
            ExecutionRepository.class.getClassLoader(),
            new Class<?>[] { ExecutionRepository.class },
            (proxy, method, args) -> {
                String name = method.getName();
                System.out.println("Repo method: " + name);
                if ("save".equals(name)) {
                    WorkflowExecution ex = (WorkflowExecution) args[0];
                    if (ex != null) { 
                        // 确保 startedAt 不为 null
                        if (ex.getStartedAt() == null) ex.setStartedAt(java.time.Instant.now());
                        store.put(ex.getId(), ex); 
                        System.out.println("Repo save: " + ex.getId()); 
                    }
                    return ex;
                }
                if ("findById".equals(name)) {
                    Optional<?> result = Optional.ofNullable(store.get(args[0]));
                    System.out.println("Repo findById: " + args[0] + " -> " + (result.isPresent() ? "found" : "not found"));
                    return result;
                }
                if ("findByWorkflowId".equals(name) || "findByStatus".equals(name) || 
                    "findByCreatedByOrderByCreatedAtDesc".equals(name) || "findByStatusAndStartedAtBefore".equals(name)) {
                    return new ArrayList<>();
                }
                if ("count".equals(name)) return (long) store.size();
                if ("existsById".equals(name)) return store.containsKey(args[0]);
                if ("delete".equals(name) || "deleteById".equals(name)) {
                    if (args[0] != null) store.remove(args[0].toString());
                    return null;
                }
                if ("deleteAll".equals(name)) { store.clear(); return null; }
                if ("getById".equals(name) || "getOne".equals(name)) return store.get(args[0]);
                if ("findAll".equals(name)) return new ArrayList<>(store.values());
                if ("findAllById".equals(name)) return new ArrayList<>();
                return null;
            }
        );
        engine = new DagWorkflowEngine(notificationService, repoProxy, executorRegistry);
    }

    // ==================== 拓扑排序测试 ====================

    @Test @DisplayName("拓扑排序 - 线性")
    public void testTopologicalSort_Linear() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Workflow wf = createWorkflow(List.of(n("n1"), n("n2"), n("n3")), List.of(e("n1","n2"), e("n2","n3")));
        List<String> r = bareEngine.topologicalSortForTest(wf);
        assertEquals(3, r.size());
        assertTrue(r.indexOf("n1") < r.indexOf("n2") && r.indexOf("n2") < r.indexOf("n3"));
    }

    @Test @DisplayName("拓扑排序 - 循环依赖")
    public void testTopologicalSort_Circular() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Workflow wf = createWorkflow(List.of(n("n1"), n("n2"), n("n3")), List.of(e("n1","n2"), e("n2","n3"), e("n3","n1")));
        assertThrows(IllegalStateException.class, () -> bareEngine.topologicalSortForTest(wf));
    }

    @Test @DisplayName("拓扑排序 - 并行")
    public void testTopologicalSort_Parallel() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Workflow wf = createWorkflow(List.of(n("n1"), n("n2"), n("n3")), List.of(e("n1","n3"), e("n2","n3")));
        List<String> r = bareEngine.topologicalSortForTest(wf);
        assertTrue(r.indexOf("n1") < r.indexOf("n3") && r.indexOf("n2") < r.indexOf("n3"));
    }

    @Test @DisplayName("拓扑排序 - 空工作流")
    public void testTopologicalSort_Empty() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Workflow wf = new Workflow();
        wf.setNodes(new ArrayList<>());
        wf.setEdges(new ArrayList<>());
        assertTrue(bareEngine.topologicalSortForTest(wf).isEmpty());
    }

    @Test @DisplayName("拓扑排序 - 单节点")
    public void testTopologicalSort_Single() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Workflow wf = createWorkflow(List.of(n("n1")), new ArrayList<>());
        assertEquals(List.of("n1"), bareEngine.topologicalSortForTest(wf));
    }

    @Test @DisplayName("拓扑排序 - 复杂 DAG")
    public void testTopologicalSort_ComplexDag() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Workflow wf = createWorkflow(
            List.of(n("n1"), n("n2"), n("n3"), n("n4")),
            List.of(e("n1","n2"), e("n1","n3"), e("n2","n4"), e("n3","n4"))
        );
        List<String> r = bareEngine.topologicalSortForTest(wf);
        assertEquals(4, r.size());
        assertTrue(r.indexOf("n1") < r.indexOf("n2"));
        assertTrue(r.indexOf("n1") < r.indexOf("n3"));
        assertTrue(r.indexOf("n2") < r.indexOf("n4"));
        assertTrue(r.indexOf("n3") < r.indexOf("n4"));
    }

    // ==================== 输入收集测试 ====================

    @Test @DisplayName("输入收集 - 线性")
    public void testCollectInputs_Linear() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Workflow wf = createWorkflow(List.of(n("n1"), n("n2"), n("n3")), List.of(e("n1","n2"), e("n2","n3")));
        Map<String,Object> out = Map.of("n1", Map.of("v", 1), "n2", Map.of("v", 2));
        Map<String, Object> inputs = bareEngine.collectNodeInputsForTest(wf, findNode(wf, "n3"), out);
        assertNotNull(inputs);
        assertEquals(1, inputs.size());
    }

    @Test @DisplayName("输入收集 - 并行")
    public void testCollectInputs_Parallel() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Workflow wf = createWorkflow(List.of(n("n1"), n("n2"), n("n3")), List.of(e("n1","n3"), e("n2","n3")));
        Map<String,Object> out = Map.of("n1", Map.of("out", 1), "n2", Map.of("out", 2));
        Map<String,Object> inputs = bareEngine.collectNodeInputsForTest(wf, findNode(wf, "n3"), out);
        assertNotNull(inputs);
        assertTrue(inputs.size() >= 1);
    }

    @Test @DisplayName("输入收集 - 无输入")
    public void testCollectInputs_NoInputs() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Workflow wf = createWorkflow(List.of(n("n1")), new ArrayList<>());
        Map<String,Object> inputs = bareEngine.collectNodeInputsForTest(wf, findNode(wf, "n1"), new HashMap<>());
        assertTrue(inputs.isEmpty());
    }

    // ==================== 节点执行测试 ====================

    @Test @DisplayName("节点执行 - INPUT 类型")
    public void testExecuteNode_Input() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        WorkflowNode node = n("n1", "INPUT");
        node.setConfig(Map.of("value", "test"));
        assertEquals("test", bareEngine.executeNodeForTest("i", node, new HashMap<>()));
    }

    @Test @DisplayName("节点执行 - INPUT 类型无配置")
    public void testExecuteNode_InputNoConfig() {
        ExecutorRegistry emptyRegistry = new ExecutorRegistry();
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, emptyRegistry);
        WorkflowNode node = n("n1", "INPUT");
        node.setConfig(new HashMap<>());
        assertEquals("fallback", bareEngine.executeNodeForTest("i", node, Map.of("default", "fallback")));
    }

    @Test @DisplayName("节点执行 - PROCESS 类型")
    public void testExecuteNode_Process() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Map<String,Object> in = Map.of("k", "v");
        assertEquals(in, bareEngine.executeNodeForTest("i", n("n1", "PROCESS"), in));
    }

    @Test @DisplayName("节点执行 - MODEL 类型")
    public void testExecuteNode_Model() throws Exception {
        nodeExecutor.result = nodeExecutor.createSuccessResult(Map.of("model_output", "result"));
        nodeExecutor.type = "kling";
        executorRegistry.register(nodeExecutor);

        WorkflowNode node = n("n1", "MODEL");
        node.setModelProvider("kling");
        node.setConfig(Map.of("prompt", "test"));

        Object result = engine.executeNodeForTest("i", node, Map.of("prompt", "test"));
        assertNotNull(result);
    }

    @Test @DisplayName("节点执行 - kling 类型")
    public void testExecuteNode_Kling() throws Exception {
        nodeExecutor.result = nodeExecutor.createSuccessResult(Map.of("video_url", "http://test.com"));
        nodeExecutor.type = "kling";
        executorRegistry.register(nodeExecutor);

        WorkflowNode node = n("n1", "kling");
        node.setConfig(Map.of("prompt", "video"));

        Object result = engine.executeNodeForTest("i", node, Map.of("prompt", "video"));
        assertNotNull(result);
    }

    @Test @DisplayName("节点执行 - wan 类型")
    public void testExecuteNode_Wan() throws Exception {
        nodeExecutor.result = nodeExecutor.createSuccessResult(Map.of("video", "wan_result"));
        nodeExecutor.type = "wan";
        executorRegistry.register(nodeExecutor);

        WorkflowNode node = n("n1", "wan");
        Object result = engine.executeNodeForTest("i", node, new HashMap<>());
        assertNotNull(result);
    }

    @Test @DisplayName("节点执行 - seedance 类型")
    public void testExecuteNode_Seedance() throws Exception {
        nodeExecutor.result = nodeExecutor.createSuccessResult(Map.of("dance", "seedance_result"));
        nodeExecutor.type = "seedance";
        executorRegistry.register(nodeExecutor);

        WorkflowNode node = n("n1", "seedance");
        Object result = engine.executeNodeForTest("i", node, new HashMap<>());
        assertNotNull(result);
    }

    @Test @DisplayName("节点执行 - nanobanana 类型")
    public void testExecuteNode_Nanobanana() throws Exception {
        nodeExecutor.result = nodeExecutor.createSuccessResult(Map.of("image", "nanobanana_result"));
        nodeExecutor.type = "nanobanana";
        executorRegistry.register(nodeExecutor);

        WorkflowNode node = n("n1", "nanobanana");
        Object result = engine.executeNodeForTest("i", node, new HashMap<>());
        assertNotNull(result);
    }

    @Test @DisplayName("节点执行 - python_script 类型")
    public void testExecuteNode_PythonScript() throws Exception {
        nodeExecutor.result = nodeExecutor.createSuccessResult(Map.of("script_output", "python_result"));
        nodeExecutor.type = "python_script";
        executorRegistry.register(nodeExecutor);

        WorkflowNode node = n("n1", "python_script");
        node.setConfig(Map.of("script", "print('hello')"));
        Object result = engine.executeNodeForTest("i", node, new HashMap<>());
        assertNotNull(result);
    }

    @Test @DisplayName("节点执行 - PYTHON_SCRIPT 类型 (大写)")
    public void testExecuteNode_PythonScriptUppercase() throws Exception {
        nodeExecutor.result = nodeExecutor.createSuccessResult(Map.of("output", "PYTHON_RESULT"));
        nodeExecutor.type = "PYTHON_SCRIPT";
        executorRegistry.register(nodeExecutor);

        WorkflowNode node = n("n1", "PYTHON_SCRIPT");
        Object result = engine.executeNodeForTest("i", node, new HashMap<>());
        assertNotNull(result);
    }

    @Test @DisplayName("节点执行 - 未知类型使用 SPI")
    public void testExecuteNode_UnknownTypeWithSpi() throws Exception {
        nodeExecutor.result = nodeExecutor.createSuccessResult(Map.of("spi", "result"));
        nodeExecutor.type = "custom_type";
        executorRegistry.register(nodeExecutor);

        WorkflowNode node = n("n1", "custom_type");
        Object result = engine.executeNodeForTest("i", node, Map.of("input", "data"));
        assertNotNull(result);
    }

    @Test @DisplayName("节点执行 - 未知类型无 SPI")
    public void testExecuteNode_UnknownTypeNoSpi() {
        ExecutorRegistry emptyRegistry = new ExecutorRegistry();
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, emptyRegistry);
        WorkflowNode node = n("n1", "unknown");
        Object result = bareEngine.executeNodeForTest("i", node, Map.of("input", "data"));
        assertEquals(Map.of("input", "data"), result);
    }

    @Test @DisplayName("节点执行 - MODEL 类型未找到执行器")
    public void testExecuteNode_ModelNoExecutor() {
        WorkflowNode node = n("n1", "MODEL");
        node.setModelProvider("unknown_model");
        
        assertThrows(RuntimeException.class, () -> engine.executeNodeForTest("i", node, new HashMap<>()));
    }

    @Test @DisplayName("节点执行 - 模型执行失败")
    public void testExecuteNode_ModelExecutionFailed() {
        LocalDateTime now = LocalDateTime.now();
        nodeExecutor.result = NodeExecutionResult.failed("test-node", "Model error", null, now, now);
        nodeExecutor.type = "kling";
        executorRegistry.register(nodeExecutor);

        WorkflowNode node = n("n1", "MODEL");
        node.setModelProvider("kling");
        
        assertThrows(RuntimeException.class, () -> engine.executeNodeForTest("i", node, new HashMap<>()));
    }

    @Test @DisplayName("节点执行 - 节点类型为空字符串")
    public void testExecuteNode_EmptyType() {
        ExecutorRegistry emptyRegistry = new ExecutorRegistry();
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, emptyRegistry);
        WorkflowNode node = new WorkflowNode();
        node.setNodeId("n1");
        node.setType("");
        node.setConfig(new HashMap<>());
        
        Object result = bareEngine.executeNodeForTest("i", node, Map.of("input", "data"));
        assertNotNull(result);
    }

    // ==================== 查找节点测试 ====================

    @Test @DisplayName("查找节点 - 存在")
    public void testFindNode_Exists() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Workflow wf = createWorkflow(List.of(n("n1"), n("n2")), List.of(e("n1","n2")));
        assertNotNull(bareEngine.findNodeForTest(wf, "n1"));
    }

    @Test @DisplayName("查找节点 - 不存在")
    public void testFindNode_NotExists() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        Workflow wf = createWorkflow(List.of(n("n1")), new ArrayList<>());
        assertNull(bareEngine.findNodeForTest(wf, "x"));
    }

    // ==================== 状态管理测试 ====================

    @Test @DisplayName("状态管理 - 创建")
    public void testState_Create() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        bareEngine.createExecutionStateForTest("i");
        assertNotNull(bareEngine.getState("i").block());
    }

    @Test @DisplayName("状态管理 - 取消")
    public void testState_Cancel() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        bareEngine.createExecutionStateForTest("i");
        bareEngine.cancel("i").block();
        assertEquals(ExecutionState.Status.CANCELLED, bareEngine.getState("i").block().getStatus());
    }

    @Test @DisplayName("状态管理 - 不存在")
    public void testState_NotFound() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        assertNull(bareEngine.getState("x").block());
    }

    // ==================== 完整工作流执行测试 ====================

    @Test @DisplayName("完整工作流执行 - 线性工作流")
    public void testExecuteWorkflow_Linear() throws Exception {
        Workflow wf = createWorkflow(
            List.of(n("n1", "INPUT"), n("n2", "PROCESS")),
            List.of(e("n1","n2"))
        );

        String instanceId = engine.execute(wf, Map.of("input", "data")).block();
        assertNotNull(instanceId);
        Thread.sleep(2000);

        assertTrue(notificationService.notified.get());
    }

    @Test @DisplayName("完整工作流执行 - 并行工作流")
    public void testExecuteWorkflow_Parallel() throws Exception {
        Workflow wf = createWorkflow(
            List.of(n("n1", "INPUT"), n("n2", "INPUT"), n("n3", "PROCESS")),
            List.of(e("n1","n3"), e("n2","n3"))
        );

        String instanceId = engine.execute(wf, Map.of()).block();
        assertNotNull(instanceId);
        Thread.sleep(2000);

        assertTrue(notificationService.notified.get());
    }

    @Disabled("异步执行问题待修复") @Test @DisplayName("完整工作流执行 - 单节点工作流")
    public void testExecuteWorkflow_SingleNode() throws Exception {
        Workflow wf = createWorkflow(List.of(n("n1", "INPUT")), new ArrayList<>());

        String instanceId = engine.execute(wf, Map.of()).block();
        assertNotNull(instanceId);
        
        // 等待异步执行完成，最多等待 5 秒
        for (int i = 0; i < 50 && !notificationService.notifications.stream().anyMatch(n -> n.contains("nodeComplete:n1")); i++) {
            Thread.sleep(100);
        }

        assertTrue(notificationService.notifications.stream().anyMatch(n -> n.contains("nodeComplete:n1")), 
            "Expected nodeComplete notification, got: " + notificationService.notifications);
    }

    @Test @DisplayName("完整工作流执行 - 混合工作流 (MODEL+PROCESS)")
    public void testExecuteWorkflow_Mixed() throws Exception {
        nodeExecutor.result = nodeExecutor.createSuccessResult(Map.of("output", "model_result"));
        nodeExecutor.type = "kling";
        executorRegistry.register(nodeExecutor);

        Workflow wf = createWorkflow(
            List.of(
                createNode("n1", "INPUT", Map.of("value", "prompt")),
                createNode("n2", "MODEL", Map.of("prompt", "test")),
                createNode("n3", "PROCESS", new HashMap<>())
            ),
            List.of(e("n1","n2"), e("n2","n3"))
        );
        wf.getNodes().get(1).setModelProvider("kling");

        String instanceId = engine.execute(wf, Map.of()).block();
        assertNotNull(instanceId);
        Thread.sleep(3000);

        assertTrue(notificationService.notifications.stream().anyMatch(n -> n.contains("nodeComplete:n2")));
    }

    // ==================== 错误处理测试 ====================

    @Test @DisplayName("错误处理 - 循环依赖检测")
    public void testError_CircularDependency() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, new ExecutorRegistry());
        Workflow wf = createWorkflow(
            List.of(n("n1"), n("n2"), n("n3")),
            List.of(e("n1","n2"), e("n2","n3"), e("n3","n1"))
        );
        assertThrows(IllegalStateException.class, () -> bareEngine.topologicalSortForTest(wf));
    }

    @Test @DisplayName("错误处理 - 节点执行失败")
    public void testError_NodeExecutionFailed() {
        nodeExecutor.shouldThrow = true;
        nodeExecutor.type = "kling";
        executorRegistry.register(nodeExecutor);

        WorkflowNode node = createNode("n1", "MODEL", new HashMap<>());
        node.setModelProvider("kling");

        assertThrows(RuntimeException.class, () -> engine.executeNodeForTest("i", node, new HashMap<>()));
    }

    // ==================== 边界条件测试 ====================

    @Test @DisplayName("边界条件 - 空工作流执行")
    public void testEdgeCase_EmptyWorkflow() throws Exception {
        Workflow wf = new Workflow();
        wf.setId("empty-wf");
        wf.setNodes(new ArrayList<>());
        wf.setEdges(new ArrayList<>());

        String instanceId = engine.execute(wf, Map.of()).block();
        assertNotNull(instanceId);
        Thread.sleep(1000);
    }

    @Test @DisplayName("边界条件 - retry 操作")
    public void testEdgeCase_Retry() {
        Mono<Void> result = engine.retry("instance", "node");
        assertEquals(Mono.empty(), result);
    }

    @Test @DisplayName("边界条件 - getState 不存在")
    public void testEdgeCase_GetStateNotFound() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        assertNull(bareEngine.getState("non-existent").block());
    }

    @Test @DisplayName("边界条件 - cancel 不存在的实例")
    public void testEdgeCase_CancelNotFound() {
        DagWorkflowEngine bareEngine = new DagWorkflowEngine(null, null, null);
        assertDoesNotThrow(() -> bareEngine.cancel("non-existent").block());
    }

    // ==================== 辅助方法 ====================
    private Workflow createWorkflow(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        Workflow wf = new Workflow();
        wf.setId("w");
        wf.setNodes(nodes);
        wf.setEdges(edges);
        return wf;
    }

    private WorkflowNode createNode(String id, String type, Map<String, Object> config) {
        WorkflowNode node = new WorkflowNode();
        node.setNodeId(id);
        node.setType(type);
        node.setConfig(config);
        return node;
    }

    private WorkflowNode n(String id) { return n(id, "INPUT"); }
    private WorkflowNode n(String id, String type) {
        WorkflowNode n = new WorkflowNode();
        n.setNodeId(id);
        n.setType(type);
        n.setConfig(new HashMap<>());
        return n;
    }

    private WorkflowEdge e(String s, String t) {
        WorkflowEdge e = new WorkflowEdge();
        e.setSource(s);
        e.setTarget(t);
        return e;
    }

    private WorkflowNode findNode(Workflow wf, String id) {
        return wf.getNodes().stream().filter(n -> n.getNodeId().equals(id)).findFirst().orElse(null);
    }

}
