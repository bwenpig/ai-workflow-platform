package com.ben.workflow.service;

import com.ben.workflow.engine.WorkflowEngine;
import com.ben.workflow.model.Workflow;
import com.ben.workflow.model.WorkflowExecution;
import com.ben.workflow.model.WorkflowNode;
import com.ben.workflow.model.WorkflowEdge;
import com.ben.workflow.repository.ExecutionRepository;
import com.ben.workflow.repository.WorkflowRepository;
import com.ben.workflow.websocket.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkflowService 单元测试
 * 目标：覆盖率 ≥ 80%
 */
public class WorkflowServiceTest {

    private WorkflowRepository workflowRepository;
    private ExecutionRepository executionRepository;
    private WorkflowEngine workflowEngine;
    private NotificationService notificationService;
    private WorkflowService workflowService;

    @BeforeEach
    public void setUp() {
        workflowRepository = mock(WorkflowRepository.class);
        executionRepository = mock(ExecutionRepository.class);
        workflowEngine = mock(WorkflowEngine.class);
        notificationService = mock(NotificationService.class);
        workflowService = new WorkflowService(workflowRepository, executionRepository, workflowEngine, notificationService);
    }

    // ==================== 创建工作流测试 ====================

    @Test
    public void testCreateWorkflow_Success() {
        Workflow workflow = new Workflow();
        workflow.setName("测试工作流");
        workflow.setDescription("测试描述");

        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> {
            Workflow saved = invocation.getArgument(0);
            saved.setId("generated-id");
            return saved;
        });

        Mono<Workflow> result = workflowService.createWorkflow(workflow, "test-user");

        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertNotNull(saved.getId());
                    assertEquals("测试工作流", saved.getName());
                    assertEquals("test-user", saved.getCreatedBy());
                    assertEquals(1, saved.getVersion());
                    assertFalse(saved.getPublished());
                    assertNotNull(saved.getCreatedAt());
                    assertNotNull(saved.getUpdatedAt());
                })
                .verifyComplete();

        verify(workflowRepository).save(any(Workflow.class));
    }

    @Test
    public void testCreateWorkflow_WithNodesAndEdges() {
        Workflow workflow = new Workflow();
        workflow.setName("带节点的工作流");
        
        List<WorkflowNode> nodes = new ArrayList<>();
        WorkflowNode node = new WorkflowNode();
        node.setNodeId("node-1");
        node.setType("INPUT");
        nodes.add(node);
        workflow.setNodes(nodes);

        List<WorkflowEdge> edges = new ArrayList<>();
        WorkflowEdge edge = new WorkflowEdge();
        edge.setSource("node-1");
        edge.setTarget("node-2");
        edges.add(edge);
        workflow.setEdges(edges);

        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<Workflow> result = workflowService.createWorkflow(workflow, "builder-user");

        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertNotNull(saved.getNodes());
                    assertEquals(1, saved.getNodes().size());
                    assertNotNull(saved.getEdges());
                    assertEquals(1, saved.getEdges().size());
                })
                .verifyComplete();
    }

    // ==================== 获取工作流列表测试 ====================

    @Test
    public void testListWorkflows_All() {
        List<Workflow> workflows = Arrays.asList(
            createWorkflow("wf-1", "工作流 1", "user-1"),
            createWorkflow("wf-2", "工作流 2", "user-2")
        );

        when(workflowRepository.findAll()).thenReturn(workflows);

        Mono<List<Workflow>> result = workflowService.listWorkflows(null, false);

        StepVerifier.create(result)
                .assertNext(list -> {
                    assertEquals(2, list.size());
                    assertTrue(list.stream().anyMatch(w -> "工作流 1".equals(w.getName())));
                })
                .verifyComplete();

        verify(workflowRepository).findAll();
    }

    @Test
    public void testListWorkflows_ByUser() {
        List<Workflow> userWorkflows = Arrays.asList(
            createWorkflow("wf-1", "用户工作流", "specific-user")
        );

        when(workflowRepository.findByCreatedBy("specific-user")).thenReturn(userWorkflows);

        Mono<List<Workflow>> result = workflowService.listWorkflows("specific-user", false);

        StepVerifier.create(result)
                .assertNext(list -> {
                    assertEquals(1, list.size());
                    assertEquals("specific-user", list.get(0).getCreatedBy());
                })
                .verifyComplete();

        verify(workflowRepository).findByCreatedBy("specific-user");
    }

    @Test
    public void testListWorkflows_PublishedOnly() {
        List<Workflow> publishedWorkflows = Arrays.asList(
            createPublishedWorkflow("wf-1", "已发布工作流")
        );

        when(workflowRepository.findByPublishedTrue()).thenReturn(publishedWorkflows);

        Mono<List<Workflow>> result = workflowService.listWorkflows(null, true);

        StepVerifier.create(result)
                .assertNext(list -> {
                    assertEquals(1, list.size());
                    assertTrue(list.get(0).getPublished());
                })
                .verifyComplete();

        verify(workflowRepository).findByPublishedTrue();
    }

    // ==================== 获取工作流详情测试 ====================

    @Test
    public void testGetWorkflow_Success() {
        Workflow workflow = createWorkflow("wf-123", "详情工作流", "user-1");

        when(workflowRepository.findById("wf-123")).thenReturn(Optional.of(workflow));

        Mono<Workflow> result = workflowService.getWorkflow("wf-123");

        StepVerifier.create(result)
                .assertNext(found -> {
                    assertEquals("wf-123", found.getId());
                    assertEquals("详情工作流", found.getName());
                })
                .verifyComplete();
    }

    @Test
    public void testGetWorkflow_NotFound() {
        when(workflowRepository.findById("non-existent")).thenReturn(Optional.empty());

        Mono<Workflow> result = workflowService.getWorkflow("non-existent");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("工作流不存在"))
                .verify();
    }

    // ==================== 更新工作流测试 ====================

    @Test
    public void testUpdateWorkflow_Success() {
        Workflow existing = createWorkflow("wf-1", "原名称", "user-1");
        existing.setVersion(1);

        Workflow updates = new Workflow();
        updates.setName("新名称");
        updates.setDescription("新描述");

        when(workflowRepository.findById("wf-1")).thenReturn(Optional.of(existing));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<Workflow> result = workflowService.updateWorkflow("wf-1", updates);

        StepVerifier.create(result)
                .assertNext(updated -> {
                    assertEquals("新名称", updated.getName());
                    assertEquals("新描述", updated.getDescription());
                    assertEquals(2, updated.getVersion());
                    assertNotNull(updated.getUpdatedAt());
                })
                .verifyComplete();
    }

    @Test
    public void testUpdateWorkflow_VersionIncrement() {
        Workflow existing = createWorkflow("wf-2", "版本测试", "user-1");
        existing.setVersion(5);

        Workflow updates = new Workflow();
        updates.setName("仅改名");

        when(workflowRepository.findById("wf-2")).thenReturn(Optional.of(existing));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<Workflow> result = workflowService.updateWorkflow("wf-2", updates);

        StepVerifier.create(result)
                .assertNext(updated -> {
                    assertEquals(6, updated.getVersion());
                })
                .verifyComplete();
    }

    @Test
    public void testUpdateWorkflow_NotFound() {
        when(workflowRepository.findById("non-existent")).thenReturn(Optional.empty());

        Workflow updates = new Workflow();
        updates.setName("新名称");

        Mono<Workflow> result = workflowService.updateWorkflow("non-existent", updates);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("工作流不存在"))
                .verify();
    }

    @Test
    public void testUpdateWorkflow_PartialUpdate() {
        Workflow existing = createWorkflow("wf-3", "原名", "user-1");
        existing.setDescription("原描述");
        existing.setVersion(1);

        Workflow updates = new Workflow();
        updates.setDescription("仅更新描述");

        when(workflowRepository.findById("wf-3")).thenReturn(Optional.of(existing));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<Workflow> result = workflowService.updateWorkflow("wf-3", updates);

        StepVerifier.create(result)
                .assertNext(updated -> {
                    assertEquals("原名", updated.getName());
                    assertEquals("仅更新描述", updated.getDescription());
                    assertEquals(2, updated.getVersion());
                })
                .verifyComplete();
    }

    // ==================== 删除工作流测试 ====================

    @Test
    public void testDeleteWorkflow_Success() {
        when(workflowRepository.existsById("wf-1")).thenReturn(true);
        doNothing().when(workflowRepository).deleteById("wf-1");

        Mono<Void> result = workflowService.deleteWorkflow("wf-1");

        StepVerifier.create(result)
                .verifyComplete();

        verify(workflowRepository).deleteById("wf-1");
    }

    @Test
    public void testDeleteWorkflow_NotFound() {
        when(workflowRepository.existsById("non-existent")).thenReturn(false);

        Mono<Void> result = workflowService.deleteWorkflow("non-existent");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("工作流不存在"))
                .verify();

        verify(workflowRepository, never()).deleteById(any());
    }

    @Test
    public void testDeleteWorkflow_WithExecutionRecords() {
        when(workflowRepository.existsById("wf-with-executions")).thenReturn(true);
        doNothing().when(workflowRepository).deleteById("wf-with-executions");

        Mono<Void> result = workflowService.deleteWorkflow("wf-with-executions");

        StepVerifier.create(result)
                .verifyComplete();

        verify(workflowRepository).existsById("wf-with-executions");
        verify(workflowRepository).deleteById("wf-with-executions");
    }

    // ==================== 发布工作流测试 ====================

    @Test
    public void testTogglePublished_Publish() {
        Workflow workflow = createWorkflow("wf-1", "待发布", "user-1");
        workflow.setPublished(false);

        when(workflowRepository.findById("wf-1")).thenReturn(Optional.of(workflow));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<Workflow> result = workflowService.togglePublished("wf-1");

        StepVerifier.create(result)
                .assertNext(updated -> {
                    assertTrue(updated.getPublished());
                    assertNotNull(updated.getUpdatedAt());
                })
                .verifyComplete();
    }

    @Test
    public void testTogglePublished_Unpublish() {
        Workflow workflow = createWorkflow("wf-2", "已发布", "user-1");
        workflow.setPublished(true);

        when(workflowRepository.findById("wf-2")).thenReturn(Optional.of(workflow));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<Workflow> result = workflowService.togglePublished("wf-2");

        StepVerifier.create(result)
                .assertNext(updated -> {
                    assertFalse(updated.getPublished());
                })
                .verifyComplete();
    }

    @Test
    public void testTogglePublished_NotFound() {
        when(workflowRepository.findById("non-existent")).thenReturn(Optional.empty());

        Mono<Workflow> result = workflowService.togglePublished("non-existent");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("工作流不存在"))
                .verify();
    }

    // ==================== 执行工作流测试 ====================

    @Test
    public void testExecuteWorkflow_Success() {
        Workflow workflow = createWorkflow("wf-1", "执行工作流", "user-1");

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("param1", "value1");

        when(workflowRepository.findById("wf-1")).thenReturn(Optional.of(workflow));
        when(executionRepository.save(any(WorkflowExecution.class))).thenAnswer(invocation -> {
            WorkflowExecution execution = invocation.getArgument(0);
            execution.setId("exec-id");
            return execution;
        });
        when(workflowEngine.execute(any(Workflow.class), any(Map.class)))
                .thenReturn(Mono.just("instance-id"));

        Mono<WorkflowExecution> result = workflowService.executeWorkflow("wf-1", inputs, "executor-user");

        StepVerifier.create(result)
                .assertNext(execution -> {
                    assertNotNull(execution.getId());
                    assertEquals("wf-1", execution.getWorkflowId());
                    assertEquals("PENDING", execution.getStatus());
                    assertEquals("executor-user", execution.getCreatedBy());
                    assertNotNull(execution.getInputs());
                })
                .verifyComplete();

        verify(executionRepository).save(any(WorkflowExecution.class));
        verify(workflowEngine).execute(any(Workflow.class), any(Map.class));
    }

    @Test
    public void testExecuteWorkflow_CreatesExecutionRecord() {
        Workflow workflow = createWorkflow("wf-2", "记录测试", "user-1");

        when(workflowRepository.findById("wf-2")).thenReturn(Optional.of(workflow));
        when(executionRepository.save(any(WorkflowExecution.class))).thenAnswer(invocation -> {
            WorkflowExecution execution = invocation.getArgument(0);
            execution.setId("exec-id");
            return execution;
        });
        when(workflowEngine.execute(any(), any())).thenReturn(Mono.just("instance"));

        ArgumentCaptor<WorkflowExecution> captor = ArgumentCaptor.forClass(WorkflowExecution.class);

        Mono<WorkflowExecution> result = workflowService.executeWorkflow("wf-2", new HashMap<>(), "test-user");

        StepVerifier.create(result)
                .assertNext(execution -> {
                    assertNotNull(execution.getId());
                    assertEquals("wf-2", execution.getWorkflowId());
                    assertEquals("test-user", execution.getCreatedBy());
                })
                .verifyComplete();

        verify(executionRepository).save(captor.capture());
        WorkflowExecution saved = captor.getValue();
        assertEquals("wf-2", saved.getWorkflowId());
        assertEquals("PENDING", saved.getStatus());
        assertEquals("test-user", saved.getCreatedBy());
    }

    @Test
    public void testExecuteWorkflow_WorkflowNotFound() {
        when(workflowRepository.findById("non-existent")).thenReturn(Optional.empty());

        Mono<WorkflowExecution> result = workflowService.executeWorkflow("non-existent", new HashMap<>(), "user");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("工作流不存在"))
                .verify();

        verify(executionRepository, never()).save(any());
        verify(workflowEngine, never()).execute(any(), any());
    }

    // ==================== 获取执行实例测试 ====================

    @Test
    public void testGetExecutionStatus_Success() {
        WorkflowExecution execution = new WorkflowExecution();
        execution.setId("exec-1");
        execution.setWorkflowId("wf-1");
        execution.setStatus("RUNNING");
        execution.setCreatedBy("user-1");
        execution.setCreatedAt(Instant.now());

        when(executionRepository.findById("exec-1")).thenReturn(Optional.of(execution));

        Mono<WorkflowExecution> result = workflowService.getExecutionStatus("exec-1");

        StepVerifier.create(result)
                .assertNext(found -> {
                    assertEquals("exec-1", found.getId());
                    assertEquals("RUNNING", found.getStatus());
                })
                .verifyComplete();
    }

    @Test
    public void testGetExecutionStatus_NotFound() {
        when(executionRepository.findById("non-existent")).thenReturn(Optional.empty());

        Mono<WorkflowExecution> result = workflowService.getExecutionStatus("non-existent");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("执行实例不存在"))
                .verify();
    }

    @Test
    public void testGetExecutionHistory_Success() {
        List<WorkflowExecution> executions = Arrays.asList(
            createExecution("exec-1", "wf-1", "RUNNING", "user-1"),
            createExecution("exec-2", "wf-2", "SUCCESS", "user-1"),
            createExecution("exec-3", "wf-3", "FAILED", "user-1")
        );

        when(executionRepository.findByCreatedByOrderByCreatedAtDesc("user-1", 10)).thenReturn(executions);

        Mono<List<WorkflowExecution>> result = workflowService.getExecutionHistory("user-1", 10);

        StepVerifier.create(result)
                .assertNext(list -> {
                    assertEquals(3, list.size());
                })
                .verifyComplete();

        verify(executionRepository).findByCreatedByOrderByCreatedAtDesc("user-1", 10);
    }

    @Test
    public void testGetExecutionHistory_Empty() {
        when(executionRepository.findByCreatedByOrderByCreatedAtDesc("new-user", 10)).thenReturn(new ArrayList<>());

        Mono<List<WorkflowExecution>> result = workflowService.getExecutionHistory("new-user", 10);

        StepVerifier.create(result)
                .assertNext(list -> assertTrue(list.isEmpty()))
                .verifyComplete();
    }

    // ==================== 取消执行测试 ====================

    @Test
    public void testCancelExecution_Success() {
        WorkflowExecution execution = createExecution("exec-1", "wf-1", "RUNNING", "user-1");

        when(executionRepository.findById("exec-1")).thenReturn(Optional.of(execution));
        when(executionRepository.save(any(WorkflowExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowEngine.cancel("exec-1")).thenReturn(Mono.empty());

        Mono<WorkflowExecution> result = workflowService.cancelExecution("exec-1");

        StepVerifier.create(result)
                .assertNext(cancelled -> {
                    assertEquals("CANCELLED", cancelled.getStatus());
                    assertNotNull(cancelled.getEndedAt());
                })
                .verifyComplete();

        verify(workflowEngine).cancel("exec-1");
        verify(executionRepository).save(any(WorkflowExecution.class));
    }

    @Test
    public void testCancelExecution_StateChange() {
        WorkflowExecution execution = createExecution("exec-2", "wf-1", "PENDING", "user-1");

        when(executionRepository.findById("exec-2")).thenReturn(Optional.of(execution));
        when(executionRepository.save(any(WorkflowExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowEngine.cancel("exec-2")).thenReturn(Mono.empty());

        Mono<WorkflowExecution> result = workflowService.cancelExecution("exec-2");

        StepVerifier.create(result)
                .assertNext(cancelled -> {
                    assertEquals("CANCELLED", cancelled.getStatus());
                    assertNotNull(cancelled.getEndedAt());
                })
                .verifyComplete();
    }

    @Test
    public void testCancelExecution_AlreadyCompleted() {
        WorkflowExecution execution = createExecution("exec-3", "wf-1", "SUCCESS", "user-1");

        when(executionRepository.findById("exec-3")).thenReturn(Optional.of(execution));

        Mono<WorkflowExecution> result = workflowService.cancelExecution("exec-3");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("执行已完成，无法取消"))
                .verify();

        verify(workflowEngine, never()).cancel(any());
        verify(executionRepository, never()).save(any());
    }

    @Test
    public void testCancelExecution_FailedStatus() {
        WorkflowExecution execution = createExecution("exec-4", "wf-1", "FAILED", "user-1");

        when(executionRepository.findById("exec-4")).thenReturn(Optional.of(execution));

        Mono<WorkflowExecution> result = workflowService.cancelExecution("exec-4");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("执行已完成，无法取消"))
                .verify();
    }

    @Test
    public void testCancelExecution_NotFound() {
        when(executionRepository.findById("non-existent")).thenReturn(Optional.empty());

        Mono<WorkflowExecution> result = workflowService.cancelExecution("non-existent");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("执行实例不存在"))
                .verify();
    }

    // ==================== 版本管理测试 ====================

    @Test
    public void testVersionManagement_MultipleVersions() {
        Workflow v1 = createWorkflow("wf-1", "多版本工作流", "user-1");
        v1.setVersion(1);
        
        Workflow v2 = createWorkflow("wf-1", "多版本工作流", "user-1");
        v2.setVersion(2);
        v2.setDescription("版本 2 的描述");

        List<Workflow> allVersions = Arrays.asList(v1, v2);
        when(workflowRepository.findByNameAndVersion("多版本工作流", 1)).thenReturn(Optional.of(v1));
        when(workflowRepository.findByNameAndVersion("多版本工作流", 2)).thenReturn(Optional.of(v2));
        when(workflowRepository.findByName("多版本工作流")).thenReturn(allVersions);

        Optional<Workflow> foundV1 = workflowRepository.findByNameAndVersion("多版本工作流", 1);
        Optional<Workflow> foundV2 = workflowRepository.findByNameAndVersion("多版本工作流", 2);

        assertTrue(foundV1.isPresent());
        assertEquals(1, foundV1.get().getVersion());
        
        assertTrue(foundV2.isPresent());
        assertEquals(2, foundV2.get().getVersion());
        assertEquals("版本 2 的描述", foundV2.get().getDescription());
    }

    // ==================== 错误处理测试 ====================

    @Test
    public void testErrorHandling_WorkflowNotFound() {
        when(workflowRepository.findById("invalid-id")).thenReturn(Optional.empty());

        Mono<Workflow> getResult = workflowService.getWorkflow("invalid-id");
        Mono<Workflow> updateResult = workflowService.updateWorkflow("invalid-id", new Workflow());
        Mono<Void> deleteResult = workflowService.deleteWorkflow("invalid-id");
        Mono<Workflow> publishResult = workflowService.togglePublished("invalid-id");
        Mono<WorkflowExecution> execResult = workflowService.executeWorkflow("invalid-id", new HashMap<>(), "user");

        StepVerifier.create(getResult)
                .expectErrorMatches(t -> t.getMessage().contains("工作流不存在"))
                .verify();

        StepVerifier.create(updateResult)
                .expectErrorMatches(t -> t.getMessage().contains("工作流不存在"))
                .verify();

        StepVerifier.create(deleteResult)
                .expectErrorMatches(t -> t.getMessage().contains("工作流不存在"))
                .verify();

        StepVerifier.create(publishResult)
                .expectErrorMatches(t -> t.getMessage().contains("工作流不存在"))
                .verify();

        StepVerifier.create(execResult)
                .expectErrorMatches(t -> t.getMessage().contains("工作流不存在"))
                .verify();
    }

    @Test
    public void testErrorHandling_InvalidNodeConfiguration() {
        Workflow workflow = new Workflow();
        workflow.setName("无效节点工作流");
        
        List<WorkflowNode> nodes = new ArrayList<>();
        WorkflowNode invalidNode = new WorkflowNode();
        invalidNode.setNodeId(null);
        invalidNode.setType(null);
        nodes.add(invalidNode);
        workflow.setNodes(nodes);

        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> {
            Workflow saved = invocation.getArgument(0);
            saved.setId("wf-invalid");
            return saved;
        });

        Mono<Workflow> result = workflowService.createWorkflow(workflow, "user");

        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertNotNull(saved.getId());
                    assertNotNull(saved.getNodes());
                })
                .verifyComplete();
    }

    @Test
    public void testErrorHandling_NullInputs() {
        Workflow workflow = new Workflow();
        workflow.setName("测试工作流");

        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowRepository.findAll()).thenReturn(new ArrayList<>());

        Mono<Workflow> createResult = workflowService.createWorkflow(workflow, null);

        StepVerifier.create(createResult)
                .assertNext(saved -> assertNull(saved.getCreatedBy()))
                .verifyComplete();

        Mono<List<Workflow>> listResult = workflowService.listWorkflows(null, false);

        StepVerifier.create(listResult)
                .assertNext(list -> assertTrue(list.isEmpty()))
                .verifyComplete();
    }

    @Test
    public void testErrorHandling_EmptyInputs() {
        Workflow workflow = new Workflow();
        workflow.setName("空输入工作流");

        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowRepository.findById("wf-empty")).thenReturn(Optional.of(workflow));
        when(executionRepository.save(any(WorkflowExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workflowEngine.execute(any(), any())).thenReturn(Mono.just("instance"));

        Mono<WorkflowExecution> execResult = workflowService.executeWorkflow("wf-empty", new HashMap<>(), "user");

        StepVerifier.create(execResult)
                .assertNext(execution -> {
                    assertNotNull(execution.getInputs());
                    assertTrue(execution.getInputs().isEmpty());
                })
                .verifyComplete();
    }

    // ==================== 辅助方法 ====================

    private Workflow createWorkflow(String id, String name, String createdBy) {
        Workflow workflow = new Workflow();
        workflow.setId(id);
        workflow.setName(name);
        workflow.setCreatedBy(createdBy);
        workflow.setVersion(1);
        workflow.setPublished(false);
        workflow.setCreatedAt(Instant.now());
        workflow.setUpdatedAt(Instant.now());
        return workflow;
    }

    private Workflow createPublishedWorkflow(String id, String name) {
        Workflow workflow = createWorkflow(id, name, "user-1");
        workflow.setPublished(true);
        return workflow;
    }

    private WorkflowExecution createExecution(String id, String workflowId, String status, String createdBy) {
        WorkflowExecution execution = new WorkflowExecution();
        execution.setId(id);
        execution.setWorkflowId(workflowId);
        execution.setStatus(status);
        execution.setCreatedBy(createdBy);
        execution.setCreatedAt(Instant.now());
        return execution;
    }
}
