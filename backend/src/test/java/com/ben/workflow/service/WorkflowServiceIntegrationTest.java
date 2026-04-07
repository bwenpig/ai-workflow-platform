package com.ben.workflow.service;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.model.WorkflowExecution;
import com.ben.workflow.model.WorkflowNode;
import com.ben.workflow.model.WorkflowEdge;
import com.ben.workflow.repository.WorkflowRepository;
import com.ben.workflow.repository.ExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowService 集成测试
 * 测试 Service 层真实业务逻辑
 */
@SpringBootTest
@ActiveProfiles("test")
class WorkflowServiceIntegrationTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @BeforeEach
    void setUp() {
        workflowRepository.deleteAll();
        executionRepository.deleteAll();
    }

    // ==================== 工作流 CRUD 测试 ====================

    @Test
    @DisplayName("创建工作流 - 完整流程")
    void testCreateWorkflow() {
        Workflow workflow = createTestWorkflow("创建工作流测试", "测试描述");

        Mono<Workflow> result = workflowService.createWorkflow(workflow, "test-user");

        StepVerifier.create(result)
            .assertNext(w -> {
                assertNotNull(w.getId());
                assertEquals("创建工作流测试", w.getName());
                assertEquals("测试描述", w.getDescription());
                assertEquals("test-user", w.getCreatedBy());
                assertEquals(1, w.getVersion());
                assertFalse(w.getPublished());
                assertNotNull(w.getCreatedAt());
                assertNotNull(w.getUpdatedAt());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("创建多个工作流")
    void testCreateMultipleWorkflows() {
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            Workflow workflow = createTestWorkflow("工作流" + i, "描述" + i);
            Mono<Workflow> result = workflowService.createWorkflow(workflow, "user-" + i);
            
            StepVerifier.create(result)
                .assertNext(w -> assertEquals("工作流" + finalI, w.getName()))
                .verifyComplete();
        }

        List<Workflow> all = workflowRepository.findAll();
        assertEquals(5, all.size());
    }

    @Test
    @DisplayName("获取工作流列表 - 全部")
    void testListWorkflowsAll() {
        createAndSaveWorkflow("工作流 1", "user-1");
        createAndSaveWorkflow("工作流 2", "user-2");
        createAndSaveWorkflow("工作流 3", "user-1");

        Mono<List<Workflow>> result = workflowService.listWorkflows(null, false);

        StepVerifier.create(result)
            .assertNext(list -> assertEquals(3, list.size()))
            .verifyComplete();
    }

    @Test
    @DisplayName("获取工作流列表 - 按用户筛选")
    void testListWorkflowsByUser() {
        createAndSaveWorkflow("用户 1 工作流 1", "user-1");
        createAndSaveWorkflow("用户 1 工作流 2", "user-1");
        createAndSaveWorkflow("用户 2 工作流", "user-2");

        Mono<List<Workflow>> result = workflowService.listWorkflows("user-1", false);

        StepVerifier.create(result)
            .assertNext(list -> assertEquals(2, list.size()))
            .verifyComplete();
    }

    @Test
    @DisplayName("获取工作流列表 - 只获取已发布")
    void testListWorkflowsPublishedOnly() {
        Workflow w1 = createAndSaveWorkflow("已发布 1", "user-1");
        w1.setPublished(true);
        workflowRepository.save(w1);

        Workflow w2 = createAndSaveWorkflow("已发布 2", "user-1");
        w2.setPublished(true);
        workflowRepository.save(w2);

        Workflow w3 = createAndSaveWorkflow("未发布", "user-1");
        w3.setPublished(false);
        workflowRepository.save(w3);

        Mono<List<Workflow>> result = workflowService.listWorkflows(null, true);

        StepVerifier.create(result)
            .assertNext(list -> assertEquals(2, list.size()))
            .verifyComplete();
    }

    @Test
    @DisplayName("获取工作流详情 - 成功")
    void testGetWorkflowSuccess() {
        Workflow saved = createAndSaveWorkflow("详情测试", "user-1");

        Mono<Workflow> result = workflowService.getWorkflow(saved.getId());

        StepVerifier.create(result)
            .assertNext(w -> {
                assertEquals(saved.getId(), w.getId());
                assertEquals("详情测试", w.getName());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("获取不存在的工作流 - 抛出异常")
    void testGetWorkflowNotFound() {
        Mono<Workflow> result = workflowService.getWorkflow("non-existent-id");

        StepVerifier.create(result)
            .expectErrorMatches(e -> e.getMessage().contains("工作流不存在"))
            .verify();
    }

    @Test
    @DisplayName("更新工作流 - 成功")
    void testUpdateWorkflowSuccess() {
        Workflow saved = createAndSaveWorkflow("原名", "user-1");

        Workflow updates = new Workflow();
        updates.setName("新名称");
        updates.setDescription("新描述");

        Mono<Workflow> result = workflowService.updateWorkflow(saved.getId(), updates);

        StepVerifier.create(result)
            .assertNext(w -> {
                assertEquals("新名称", w.getName());
                assertEquals("新描述", w.getDescription());
                assertEquals(2, w.getVersion());
                assertTrue(w.getUpdatedAt().isAfter(w.getCreatedAt()));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("更新工作流 - 部分字段")
    void testUpdateWorkflowPartial() {
        Workflow saved = createAndSaveWorkflow("原名", "user-1");
        Instant originalUpdatedAt = saved.getUpdatedAt();

        Workflow updates = new Workflow();
        updates.setName("仅更新名称");

        Mono<Workflow> result = workflowService.updateWorkflow(saved.getId(), updates);

        StepVerifier.create(result)
            .assertNext(w -> {
                assertEquals("仅更新名称", w.getName());
                assertEquals("描述", w.getDescription()); // 描述不变
                assertTrue(w.getUpdatedAt().isAfter(originalUpdatedAt));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("更新不存在的工作流 - 抛出异常")
    void testUpdateWorkflowNotFound() {
        Workflow updates = new Workflow();
        updates.setName("新名称");

        Mono<Workflow> result = workflowService.updateWorkflow("non-existent-id", updates);

        StepVerifier.create(result)
            .expectErrorMatches(e -> e.getMessage().contains("工作流不存在"))
            .verify();
    }

    @Test
    @DisplayName("删除工作流 - 成功")
    void testDeleteWorkflowSuccess() {
        Workflow saved = createAndSaveWorkflow("删除测试", "user-1");

        Mono<Void> result = workflowService.deleteWorkflow(saved.getId());

        StepVerifier.create(result)
            .verifyComplete();

        assertFalse(workflowRepository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("删除不存在的工作流 - 抛出异常")
    void testDeleteWorkflowNotFound() {
        Mono<Void> result = workflowService.deleteWorkflow("non-existent-id");

        StepVerifier.create(result)
            .expectErrorMatches(e -> e.getMessage().contains("工作流不存在"))
            .verify();
    }

    // ==================== 版本管理测试 ====================

    @Test
    @DisplayName("版本管理 - 每次更新版本号递增")
    void testVersionManagement() {
        Workflow saved = createAndSaveWorkflow("版本测试", "user-1");
        assertEquals(1, saved.getVersion());

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            Workflow updates = new Workflow();
            updates.setName("版本" + i);
            
            Mono<Workflow> result = workflowService.updateWorkflow(saved.getId(), updates);
            StepVerifier.create(result)
                .assertNext(w -> assertEquals(2 + finalI, w.getVersion()))
                .verifyComplete();
        }
    }

    @Test
    @DisplayName("版本管理 - 创建时版本为 1")
    void testVersionOnInit() {
        Workflow workflow = createTestWorkflow("初始版本", "描述");

        Mono<Workflow> result = workflowService.createWorkflow(workflow, "user-1");

        StepVerifier.create(result)
            .assertNext(w -> assertEquals(1, w.getVersion()))
            .verifyComplete();
    }

    // ==================== 执行实例管理测试 ====================

    @Test
    @DisplayName("执行工作流 - 创建执行实例")
    void testExecuteWorkflow() {
        Workflow saved = createAndSaveWorkflow("执行测试", "user-1");

        Map<String, Object> inputs = Map.of("param1", "value1", "param2", 123);

        Mono<WorkflowExecution> result = workflowService.executeWorkflow(saved.getId(), inputs, "executor");

        StepVerifier.create(result)
            .assertNext(execution -> {
                assertNotNull(execution.getId());
                assertEquals(saved.getId(), execution.getWorkflowId());
                assertEquals("PENDING", execution.getStatus());
                assertEquals("executor", execution.getCreatedBy());
                assertNotNull(execution.getCreatedAt());
                assertEquals(2, execution.getInputs().size());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("执行工作流 - 空输入")
    void testExecuteWorkflowEmptyInputs() {
        Workflow saved = createAndSaveWorkflow("执行测试", "user-1");

        Mono<WorkflowExecution> result = workflowService.executeWorkflow(saved.getId(), Map.of(), "executor");

        StepVerifier.create(result)
            .assertNext(execution -> {
                assertNotNull(execution.getId());
                assertEquals("PENDING", execution.getStatus());
                assertTrue(execution.getInputs().isEmpty());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("执行不存在的工作流 - 抛出异常")
    void testExecuteWorkflowNotFound() {
        Mono<WorkflowExecution> result = workflowService.executeWorkflow("non-existent-id", Map.of(), "executor");

        StepVerifier.create(result)
            .expectErrorMatches(e -> e.getMessage().contains("工作流不存在"))
            .verify();
    }

    @Test
    @DisplayName("获取执行状态 - 成功")
    void testGetExecutionStatus() {
        Workflow workflow = createAndSaveWorkflow("执行状态测试", "user-1");
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflow.getId());
        execution.setStatus("RUNNING");
        execution.setCreatedBy("user-1");
        execution.setCreatedAt(Instant.now());
        execution = executionRepository.save(execution);
        final String executionId = execution.getId();

        Mono<WorkflowExecution> result = workflowService.getExecutionStatus(executionId);

        StepVerifier.create(result)
            .assertNext(e -> {
                assertEquals(executionId, e.getId());
                assertEquals("RUNNING", e.getStatus());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("获取不存在的执行状态 - 抛出异常")
    void testGetExecutionStatusNotFound() {
        Mono<WorkflowExecution> result = workflowService.getExecutionStatus("non-existent-id");

        StepVerifier.create(result)
            .expectErrorMatches(e -> e.getMessage().contains("执行实例不存在"))
            .verify();
    }

    @Test
    @DisplayName("获取执行历史 - 按用户")
    void testGetExecutionHistory() {
        Workflow workflow = createAndSaveWorkflow("历史测试", "user-1");

        for (int i = 0; i < 5; i++) {
            WorkflowExecution execution = new WorkflowExecution();
            execution.setWorkflowId(workflow.getId());
            execution.setStatus("SUCCESS");
            execution.setCreatedBy("user-1");
            execution.setCreatedAt(Instant.now().minusSeconds(i));
            executionRepository.save(execution);
        }

        Mono<List<WorkflowExecution>> result = workflowService.getExecutionHistory("user-1", 10);

        StepVerifier.create(result)
            .assertNext(list -> assertEquals(5, list.size()))
            .verifyComplete();
    }

    @Test
    @DisplayName("获取执行历史 - 限制数量")
    void testGetExecutionHistoryWithLimit() {
        Workflow workflow = createAndSaveWorkflow("历史限制测试", "user-1");

        for (int i = 0; i < 10; i++) {
            WorkflowExecution execution = new WorkflowExecution();
            execution.setWorkflowId(workflow.getId());
            execution.setStatus("SUCCESS");
            execution.setCreatedBy("user-1");
            execution.setCreatedAt(Instant.now().minusSeconds(i));
            executionRepository.save(execution);
        }

        Mono<List<WorkflowExecution>> result = workflowService.getExecutionHistory("user-1", 5);

        StepVerifier.create(result)
            .assertNext(list -> assertEquals(5, list.size()))
            .verifyComplete();
    }

    // ==================== 取消/重试执行测试 ====================

    @Test
    @DisplayName("取消执行 - 成功")
    void testCancelExecutionSuccess() {
        Workflow workflow = createAndSaveWorkflow("取消测试", "user-1");
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflow.getId());
        execution.setStatus("RUNNING");
        execution.setCreatedBy("user-1");
        execution.setCreatedAt(Instant.now());
        execution = executionRepository.save(execution);

        Mono<WorkflowExecution> result = workflowService.cancelExecution(execution.getId());

        StepVerifier.create(result)
            .assertNext(e -> {
                assertEquals("CANCELLED", e.getStatus());
                assertNotNull(e.getEndedAt());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("取消已完成的执行 - 抛出异常")
    void testCancelExecutionAlreadyCompleted() {
        Workflow workflow = createAndSaveWorkflow("取消测试", "user-1");
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflow.getId());
        execution.setStatus("SUCCESS");
        execution.setCreatedBy("user-1");
        execution.setCreatedAt(Instant.now());
        execution = executionRepository.save(execution);

        Mono<WorkflowExecution> result = workflowService.cancelExecution(execution.getId());

        StepVerifier.create(result)
            .expectErrorMatches(e -> e.getMessage().contains("执行已完成"))
            .verify();
    }

    @Test
    @DisplayName("取消失败的执行 - 抛出异常")
    void testCancelExecutionFailed() {
        Workflow workflow = createAndSaveWorkflow("取消测试", "user-1");
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflow.getId());
        execution.setStatus("FAILED");
        execution.setCreatedBy("user-1");
        execution.setCreatedAt(Instant.now());
        execution = executionRepository.save(execution);

        Mono<WorkflowExecution> result = workflowService.cancelExecution(execution.getId());

        StepVerifier.create(result)
            .expectErrorMatches(e -> e.getMessage().contains("执行已完成"))
            .verify();
    }

    @Test
    @DisplayName("取消不存在的执行 - 抛出异常")
    void testCancelExecutionNotFound() {
        Mono<WorkflowExecution> result = workflowService.cancelExecution("non-existent-id");

        StepVerifier.create(result)
            .expectErrorMatches(e -> e.getMessage().contains("执行实例不存在"))
            .verify();
    }

    // ==================== 发布/取消发布测试 ====================

    @Test
    @DisplayName("切换发布状态 - 未发布到已发布")
    void testTogglePublishedToPublished() {
        Workflow saved = createAndSaveWorkflow("发布测试", "user-1");
        assertFalse(saved.getPublished());

        Mono<Workflow> result = workflowService.togglePublished(saved.getId());

        StepVerifier.create(result)
            .assertNext(w -> {
                assertTrue(w.getPublished());
                assertNotNull(w.getUpdatedAt());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("切换发布状态 - 已发布到未发布")
    void testTogglePublishedToUnpublished() {
        Workflow saved = createAndSaveWorkflow("发布测试", "user-1");
        saved.setPublished(true);
        workflowRepository.save(saved);

        Mono<Workflow> result = workflowService.togglePublished(saved.getId());

        StepVerifier.create(result)
            .assertNext(w -> assertFalse(w.getPublished()))
            .verifyComplete();
    }

    @Test
    @DisplayName("切换不存在的发布状态 - 抛出异常")
    void testTogglePublishedNotFound() {
        Mono<Workflow> result = workflowService.togglePublished("non-existent-id");

        StepVerifier.create(result)
            .expectErrorMatches(e -> e.getMessage().contains("工作流不存在"))
            .verify();
    }

    // ==================== 完整流程测试 ====================

    @Test
    @DisplayName("完整工作流生命周期")
    void testFullWorkflowLifecycle() {
        // 1. 创建 - 先保存到仓库获取 ID
        Workflow workflow = createTestWorkflow("生命周期测试", "完整流程");
        workflow.setNodes(List.of(createNode("n1", "节点 1", "start")));
        
        Mono<Workflow> createResult = workflowService.createWorkflow(workflow, "lifecycle-user");
        StepVerifier.create(createResult)
            .assertNext(w -> {
                assertNotNull(w.getId());
                assertEquals(1, w.getVersion());
            })
            .verifyComplete();
        
        // 从仓库获取创建的工作流
        Workflow created = workflowRepository.findByName("生命周期测试").get(0);
        assertNotNull(created.getId());

        // 2. 获取
        Mono<Workflow> getResult = workflowService.getWorkflow(created.getId());
        StepVerifier.create(getResult)
            .assertNext(w -> assertEquals("生命周期测试", w.getName()))
            .verifyComplete();

        // 3. 更新
        Workflow updates = new Workflow();
        updates.setDescription("更新后的描述");
        Mono<Workflow> updateResult = workflowService.updateWorkflow(created.getId(), updates);
        StepVerifier.create(updateResult)
            .assertNext(w -> {
                assertEquals("更新后的描述", w.getDescription());
                assertEquals(2, w.getVersion());
            })
            .verifyComplete();

        // 4. 发布
        Mono<Workflow> publishResult = workflowService.togglePublished(created.getId());
        StepVerifier.create(publishResult)
            .assertNext(w -> assertTrue(w.getPublished()))
            .verifyComplete();

        // 5. 执行
        Mono<WorkflowExecution> execResult = workflowService.executeWorkflow(created.getId(), Map.of("key", "value"), "executor");
        StepVerifier.create(execResult)
            .assertNext(e -> assertEquals("PENDING", e.getStatus()))
            .verifyComplete();

        // 获取执行实例
        WorkflowExecution execution = executionRepository.findAll().get(0);
        assertTrue(List.of("PENDING", "RUNNING").contains(execution.getStatus()));

        // 6. 获取执行状态
        Mono<WorkflowExecution> statusResult = workflowService.getExecutionStatus(execution.getId());
        StepVerifier.create(statusResult)
            .assertNext(e -> assertTrue(List.of("PENDING", "RUNNING").contains(e.getStatus())))
            .verifyComplete();

        // 7. 取消执行
        Mono<WorkflowExecution> cancelResult = workflowService.cancelExecution(execution.getId());
        StepVerifier.create(cancelResult)
            .assertNext(e -> assertEquals("CANCELLED", e.getStatus()))
            .verifyComplete();

        // 8. 删除
        Mono<Void> deleteResult = workflowService.deleteWorkflow(created.getId());
        StepVerifier.create(deleteResult).verifyComplete();

        // 9. 验证删除
        assertFalse(workflowRepository.existsById(created.getId()));
    }

    @Test
    @DisplayName("多用户并发操作")
    void testMultiUserConcurrentOperations() {
        // 多个用户同时创建工作流
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            Workflow workflow = createTestWorkflow("并发工作流" + i, "描述");
            Mono<Workflow> result = workflowService.createWorkflow(workflow, "user-" + (i % 3));
            StepVerifier.create(result)
                .assertNext(w -> assertEquals("并发工作流" + finalI, w.getName()))
                .verifyComplete();
        }

        // 验证所有工作流已创建
        List<Workflow> all = workflowRepository.findAll();
        assertEquals(10, all.size());

        // 按用户筛选
        Mono<List<Workflow>> user0Result = workflowService.listWorkflows("user-0", false);
        StepVerifier.create(user0Result)
            .assertNext(list -> assertEquals(4, list.size())) // 0, 3, 6, 9
            .verifyComplete();
    }

    // ==================== 辅助方法 ====================

    private Workflow createTestWorkflow(String name, String description) {
        Workflow workflow = new Workflow();
        workflow.setName(name);
        workflow.setDescription(description);
        return workflow;
    }

    private Workflow createAndSaveWorkflow(String name, String createdBy) {
        Workflow workflow = createTestWorkflow(name, "描述");
        workflow.setCreatedBy(createdBy);
        workflow.setCreatedAt(Instant.now());
        workflow.setUpdatedAt(Instant.now());
        workflow.setVersion(1);
        workflow.setPublished(false);
        return workflowRepository.save(workflow);
    }

    private WorkflowNode createNode(String nodeId, String name, String type) {
        WorkflowNode node = new WorkflowNode();
        node.setNodeId(nodeId);
        node.setType(type);
        WorkflowNode.Position position = new WorkflowNode.Position();
        position.setX(100.0);
        position.setY(100.0);
        node.setPosition(position);
        return node;
    }
}
