package com.ben.workflow.api;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkflowController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class WorkflowControllerTest {

    private WorkflowController workflowController;

    @Mock
    private WorkflowService mockWorkflowService;

    @Mock
    private Workflow mockWorkflow;

    @BeforeEach
    void setUp() {
        workflowController = new WorkflowController(mockWorkflowService);
    }

    @Test
    @DisplayName("测试创建工作流 - 带用户 ID")
    void testCreateWorkflowWithUserId() {
        when(mockWorkflow.getId()).thenReturn("workflow-123");
        when(mockWorkflowService.createWorkflow(any(Workflow.class), eq("user-456")))
                .thenReturn(Mono.just(mockWorkflow));

        Mono<ResponseEntity<Workflow>> result = workflowController.createWorkflow(mockWorkflow, "user-456");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(mockWorkflow, response.getBody());
                })
                .verifyComplete();

        verify(mockWorkflowService).createWorkflow(mockWorkflow, "user-456");
    }

    @Test
    @DisplayName("测试创建工作流 - 匿名用户")
    void testCreateWorkflowAnonymous() {
        when(mockWorkflow.getId()).thenReturn("workflow-123");
        when(mockWorkflowService.createWorkflow(any(Workflow.class), eq("anonymous")))
                .thenReturn(Mono.just(mockWorkflow));

        Mono<ResponseEntity<Workflow>> result = workflowController.createWorkflow(mockWorkflow, null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(mockWorkflow, response.getBody());
                })
                .verifyComplete();

        verify(mockWorkflowService).createWorkflow(mockWorkflow, "anonymous");
    }

    @Test
    @DisplayName("测试列出工作流 - 无过滤")
    void testListWorkflowsNoFilter() {
        List<Workflow> workflows = Arrays.asList(mockWorkflow);
        when(mockWorkflowService.listWorkflows(null, false)).thenReturn(Mono.just(workflows));

        Mono<ResponseEntity<List<Workflow>>> result = workflowController.listWorkflows(null, null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(1, response.getBody().size());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试列出工作流 - 仅已发布")
    void testListWorkflowsPublishedOnly() {
        List<Workflow> workflows = Arrays.asList(mockWorkflow);
        when(mockWorkflowService.listWorkflows(null, true)).thenReturn(Mono.just(workflows));

        Mono<ResponseEntity<List<Workflow>>> result = workflowController.listWorkflows(true, null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(1, response.getBody().size());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试列出工作流 - 按用户过滤")
    void testListWorkflowsByUser() {
        List<Workflow> workflows = Arrays.asList(mockWorkflow);
        when(mockWorkflowService.listWorkflows("user-456", false)).thenReturn(Mono.just(workflows));

        Mono<ResponseEntity<List<Workflow>>> result = workflowController.listWorkflows(null, "user-456");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(1, response.getBody().size());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试获取工作流 - 成功")
    void testGetWorkflowSuccess() {
        when(mockWorkflowService.getWorkflow("workflow-123")).thenReturn(Mono.just(mockWorkflow));

        Mono<ResponseEntity<Workflow>> result = workflowController.getWorkflow("workflow-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(mockWorkflow, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试获取工作流 - 不存在")
    void testGetWorkflowNotFound() {
        when(mockWorkflowService.getWorkflow("workflow-123"))
                .thenReturn(Mono.error(new RuntimeException("Not found")));

        Mono<ResponseEntity<Workflow>> result = workflowController.getWorkflow("workflow-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(404, response.getStatusCodeValue());
                    assertNull(response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试更新工作流 - 成功")
    void testUpdateWorkflowSuccess() {
        when(mockWorkflowService.updateWorkflow(eq("workflow-123"), any(Workflow.class)))
                .thenReturn(Mono.just(mockWorkflow));

        Mono<ResponseEntity<Workflow>> result = workflowController.updateWorkflow("workflow-123", mockWorkflow);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(mockWorkflow, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试更新工作流 - 不存在")
    void testUpdateWorkflowNotFound() {
        when(mockWorkflowService.updateWorkflow(eq("workflow-123"), any(Workflow.class)))
                .thenReturn(Mono.error(new RuntimeException("Not found")));

        Mono<ResponseEntity<Workflow>> result = workflowController.updateWorkflow("workflow-123", mockWorkflow);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(404, response.getStatusCodeValue());
                    assertNull(response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试删除工作流 - 成功")
    void testDeleteWorkflowSuccess() {
        when(mockWorkflowService.deleteWorkflow("workflow-123")).thenReturn(Mono.empty());

        Mono<ResponseEntity<Void>> result = workflowController.deleteWorkflow("workflow-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(204, response.getStatusCodeValue());
                    assertNull(response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试删除工作流 - 不存在")
    void testDeleteWorkflowNotFound() {
        when(mockWorkflowService.deleteWorkflow("workflow-123"))
                .thenReturn(Mono.error(new RuntimeException("Not found")));

        Mono<ResponseEntity<Void>> result = workflowController.deleteWorkflow("workflow-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(404, response.getStatusCodeValue());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试切换发布状态 - 成功")
    void testTogglePublishedSuccess() {
        when(mockWorkflowService.togglePublished("workflow-123")).thenReturn(Mono.just(mockWorkflow));

        Mono<ResponseEntity<Workflow>> result = workflowController.togglePublished("workflow-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(mockWorkflow, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试执行工作流 - 成功")
    void testExecuteWorkflowSuccess() {
        Map<String, Object> inputs = Map.of("param1", "value1");
        com.ben.workflow.model.WorkflowExecution mockExecution = mock(com.ben.workflow.model.WorkflowExecution.class);
        when(mockExecution.getId()).thenReturn("exec-123");
        when(mockExecution.getStatus()).thenReturn("RUNNING");
        when(mockExecution.getWorkflowId()).thenReturn("workflow-123");
        
        when(mockWorkflowService.executeWorkflow(eq("workflow-123"), anyMap(), eq("user-456")))
                .thenReturn(Mono.just(mockExecution));

        Mono<ResponseEntity<Map<String, String>>> result = workflowController.executeWorkflow("workflow-123", inputs, "user-456");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(202, response.getStatusCodeValue());
                    assertNotNull(response.getBody());
                    assertEquals("exec-123", response.getBody().get("executionId"));
                    assertEquals("RUNNING", response.getBody().get("status"));
                    assertEquals("workflow-123", response.getBody().get("workflowId"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试执行工作流 - 空输入")
    void testExecuteWorkflowEmptyInputs() {
        com.ben.workflow.model.WorkflowExecution mockExecution = mock(com.ben.workflow.model.WorkflowExecution.class);
        when(mockExecution.getId()).thenReturn("exec-123");
        when(mockExecution.getStatus()).thenReturn("RUNNING");
        when(mockExecution.getWorkflowId()).thenReturn("workflow-123");
        
        when(mockWorkflowService.executeWorkflow(eq("workflow-123"), anyMap(), eq("anonymous")))
                .thenReturn(Mono.just(mockExecution));

        Mono<ResponseEntity<Map<String, String>>> result = workflowController.executeWorkflow("workflow-123", null, null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(202, response.getStatusCodeValue());
                    assertEquals("exec-123", response.getBody().get("executionId"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试执行工作流 - 失败")
    void testExecuteWorkflowFailure() {
        when(mockWorkflowService.executeWorkflow(eq("workflow-123"), anyMap(), eq("user-456")))
                .thenReturn(Mono.error(new RuntimeException("Execution failed")));

        Mono<ResponseEntity<Map<String, String>>> result = workflowController.executeWorkflow("workflow-123", Map.of(), "user-456");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                })
                .verifyComplete();
    }
}
