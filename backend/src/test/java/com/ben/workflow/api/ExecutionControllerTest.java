package com.ben.workflow.api;

import com.ben.workflow.model.WorkflowExecution;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExecutionController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ExecutionControllerTest {

    private ExecutionController executionController;

    @Mock
    private WorkflowService mockWorkflowService;

    @Mock
    private WorkflowExecution mockExecution;

    @BeforeEach
    void setUp() {
        executionController = new ExecutionController(mockWorkflowService);
    }

    @Test
    @DisplayName("测试获取执行状态 - 成功")
    void testGetExecutionStatusSuccess() {
        when(mockWorkflowService.getExecutionStatus("exec-123")).thenReturn(Mono.just(mockExecution));

        Mono<ResponseEntity<WorkflowExecution>> result = executionController.getExecutionStatus("exec-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(mockExecution, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试获取执行状态 - 不存在")
    void testGetExecutionStatusNotFound() {
        when(mockWorkflowService.getExecutionStatus("exec-123"))
                .thenReturn(Mono.error(new RuntimeException("Not found")));

        Mono<ResponseEntity<WorkflowExecution>> result = executionController.getExecutionStatus("exec-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(404, response.getStatusCodeValue());
                    assertNull(response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试取消执行 - 成功")
    void testCancelExecutionSuccess() {
        when(mockWorkflowService.cancelExecution("exec-123")).thenReturn(Mono.just(mockExecution));

        Mono<ResponseEntity<WorkflowExecution>> result = executionController.cancelExecution("exec-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(mockExecution, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试取消执行 - 失败")
    void testCancelExecutionFailure() {
        when(mockWorkflowService.cancelExecution("exec-123"))
                .thenReturn(Mono.error(new RuntimeException("Cannot cancel")));

        Mono<ResponseEntity<WorkflowExecution>> result = executionController.cancelExecution("exec-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertNull(response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试获取执行历史 - 成功")
    void testGetExecutionHistorySuccess() {
        List<WorkflowExecution> executions = Arrays.asList(mockExecution);
        when(mockWorkflowService.getExecutionHistory("user-456", 20)).thenReturn(Mono.just(executions));

        Mono<ResponseEntity<List<WorkflowExecution>>> result = executionController.getExecutionHistory("user-456", 20);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(1, response.getBody().size());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试获取执行历史 - 默认限制")
    void testGetExecutionHistoryDefaultLimit() {
        List<WorkflowExecution> executions = Arrays.asList(mockExecution);
        when(mockWorkflowService.getExecutionHistory("user-456", 20)).thenReturn(Mono.just(executions));

        Mono<ResponseEntity<List<WorkflowExecution>>> result = executionController.getExecutionHistory("user-456", 20);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertNotNull(response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试获取执行历史 - 缺少用户 ID")
    void testGetExecutionHistoryMissingUserId() {
        Mono<ResponseEntity<List<WorkflowExecution>>> result = executionController.getExecutionHistory(null, 20);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertNull(response.getBody());
                })
                .verifyComplete();

        verify(mockWorkflowService, never()).getExecutionHistory(any(), anyInt());
    }

    @Test
    @DisplayName("测试获取执行历史 - 不同限制值")
    void testGetExecutionHistoryDifferentLimits() {
        List<WorkflowExecution> executions = Arrays.asList(mockExecution);
        
        for (int limit : new int[]{10, 50, 100}) {
            when(mockWorkflowService.getExecutionHistory("user-456", limit)).thenReturn(Mono.just(executions));
            
            Mono<ResponseEntity<List<WorkflowExecution>>> result = executionController.getExecutionHistory("user-456", limit);
            
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertEquals(200, response.getStatusCodeValue());
                        assertEquals(1, response.getBody().size());
                    })
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("测试取消执行 - 执行中")
    void testCancelExecutionWhileRunning() {
        when(mockExecution.getStatus()).thenReturn("RUNNING");
        when(mockWorkflowService.cancelExecution("exec-123")).thenReturn(Mono.just(mockExecution));

        Mono<ResponseEntity<WorkflowExecution>> result = executionController.cancelExecution("exec-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertNotNull(response.getBody());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试获取执行状态 - 已完成")
    void testGetExecutionStatusCompleted() {
        when(mockExecution.getStatus()).thenReturn("COMPLETED");
        when(mockWorkflowService.getExecutionStatus("exec-123")).thenReturn(Mono.just(mockExecution));

        Mono<ResponseEntity<WorkflowExecution>> result = executionController.getExecutionStatus("exec-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals("COMPLETED", response.getBody().getStatus());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试获取执行状态 - 失败")
    void testGetExecutionStatusFailed() {
        when(mockExecution.getStatus()).thenReturn("FAILED");
        when(mockWorkflowService.getExecutionStatus("exec-123")).thenReturn(Mono.just(mockExecution));

        Mono<ResponseEntity<WorkflowExecution>> result = executionController.getExecutionStatus("exec-123");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals("FAILED", response.getBody().getStatus());
                })
                .verifyComplete();
    }
}
