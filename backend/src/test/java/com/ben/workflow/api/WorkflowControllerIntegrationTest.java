package com.ben.workflow.api;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.model.WorkflowNode;
import com.ben.workflow.model.WorkflowEdge;
import com.ben.workflow.repository.WorkflowRepository;
import com.ben.workflow.repository.ExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowController 集成测试
 * 使用 @SpringBootTest 进行完整的 HTTP 请求测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class WorkflowControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        workflowRepository.deleteAll();
        executionRepository.deleteAll();
    }

    // ==================== 创建工作流测试 ====================

    @Test
    @DisplayName("创建简单工作流 - 成功")
    void testCreateSimpleWorkflow() {
        Workflow workflow = createTestWorkflow("测试工作流", "测试描述");

        webTestClient.post()
            .uri("/api/v1/workflows")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(workflow)
            .header("X-User-Id", "test-user")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Workflow.class)
            .value(w -> {
                assertNotNull(w.getId());
                assertEquals("测试工作流", w.getName());
                assertEquals("测试描述", w.getDescription());
                assertEquals("test-user", w.getCreatedBy());
                assertEquals(1, w.getVersion());
                assertFalse(w.getPublished());
                assertNotNull(w.getCreatedAt());
                assertNotNull(w.getUpdatedAt());
            });
    }

    @Test
    @DisplayName("创建工作流带节点和边 - 成功")
    void testCreateWorkflowWithNodesAndEdges() {
        Workflow workflow = createTestWorkflow("完整工作流", "带节点和边");
        workflow.setNodes(List.of(
            createNode("node1", "开始节点", "start"),
            createNode("node2", "处理节点", "process"),
            createNode("node3", "结束节点", "end")
        ));
        workflow.setEdges(List.of(
            createEdge("edge1", "node1", "node2"),
            createEdge("edge2", "node2", "node3")
        ));

        webTestClient.post()
            .uri("/api/v1/workflows")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(workflow)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Workflow.class)
            .value(w -> {
                assertNotNull(w.getId());
                assertEquals(3, w.getNodes().size());
                assertEquals(2, w.getEdges().size());
            });
    }

    @Test
    @DisplayName("创建工作流无用户 ID - 使用匿名")
    void testCreateWorkflowWithoutUserId() {
        Workflow workflow = createTestWorkflow("匿名工作流", "无用户 ID");

        webTestClient.post()
            .uri("/api/v1/workflows")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(workflow)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Workflow.class)
            .value(w -> {
                assertEquals("anonymous", w.getCreatedBy());
            });
    }

    @Test
    @DisplayName("创建空名称工作流 - 成功")
    void testCreateWorkflowWithEmptyName() {
        Workflow workflow = new Workflow();
        workflow.setName("");
        workflow.setDescription("空名称");

        webTestClient.post()
            .uri("/api/v1/workflows")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(workflow)
            .exchange()
            .expectStatus().isOk();
    }

    // ==================== 获取工作流列表测试 ====================

    @Test
    @DisplayName("获取所有工作流 - 空列表")
    void testListWorkflowsEmpty() {
        webTestClient.get()
            .uri("/api/v1/workflows")
            .exchange()
            .expectStatus().isOk()
            .expectBody(List.class)
            .value(list -> assertTrue(list.isEmpty()));
    }

    @Test
    @DisplayName("获取所有工作流 - 有多个工作流")
    void testListWorkflowsWithMultiple() {
        // 创建 3 个工作流
        for (int i = 1; i <= 3; i++) {
            Workflow workflow = createTestWorkflow("工作流" + i, "描述" + i);
            workflow.setCreatedBy("user-" + (i % 2)); // user-0, user-1, user-0
            if (i == 2) workflow.setPublished(true);
            workflowRepository.save(workflow);
        }

        webTestClient.get()
            .uri("/api/v1/workflows")
            .exchange()
            .expectStatus().isOk()
            .expectBody(List.class)
            .value(list -> assertEquals(3, list.size()));
    }

    @Test
    @DisplayName("按用户筛选工作流")
    void testListWorkflowsByUser() {
        Workflow w1 = createTestWorkflow("用户 1 工作流 1", "描述");
        w1.setCreatedBy("user-1");
        workflowRepository.save(w1);

        Workflow w2 = createTestWorkflow("用户 2 工作流", "描述");
        w2.setCreatedBy("user-2");
        workflowRepository.save(w2);

        webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("/api/v1/workflows")
                .queryParam("createdBy", "user-1")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody(List.class)
            .value(list -> assertEquals(1, list.size()));
    }

    @Test
    @DisplayName("只获取已发布的工作流")
    void testListWorkflowsPublishedOnly() {
        Workflow w1 = createTestWorkflow("已发布", "描述");
        w1.setPublished(true);
        workflowRepository.save(w1);

        Workflow w2 = createTestWorkflow("未发布", "描述");
        w2.setPublished(false);
        workflowRepository.save(w2);

        webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("/api/v1/workflows")
                .queryParam("published", true)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody(List.class)
            .value(list -> assertEquals(1, list.size()));
    }

    // ==================== 获取工作流详情测试 ====================

    @Test
    @DisplayName("获取工作流详情 - 成功")
    void testGetWorkflowSuccess() {
        Workflow workflow = createTestWorkflow("详情测试", "详情描述");
        workflow.setPublished(true);
        Workflow saved = workflowRepository.save(workflow);

        webTestClient.get()
            .uri("/api/v1/workflows/" + saved.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBody(Workflow.class)
            .value(w -> {
                assertEquals(saved.getId(), w.getId());
                assertEquals("详情测试", w.getName());
                assertTrue(w.getPublished());
            });
    }

    @Test
    @DisplayName("获取不存在的工作流 - 404")
    void testGetWorkflowNotFound() {
        webTestClient.get()
            .uri("/api/v1/workflows/non-existent-id")
            .exchange()
            .expectStatus().isNotFound();
    }

    // ==================== 更新工作流测试 ====================

    @Test
    @DisplayName("更新工作流名称 - 成功")
    void testUpdateWorkflowName() {
        Workflow workflow = workflowRepository.save(createTestWorkflow("原名", "原描述"));

        Workflow updates = new Workflow();
        updates.setName("新名称");

        webTestClient.put()
            .uri("/api/v1/workflows/" + workflow.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updates)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Workflow.class)
            .value(w -> {
                assertEquals("新名称", w.getName());
                assertEquals("原描述", w.getDescription());
                assertEquals(2, w.getVersion()); // 版本号应该增加
            });
    }

    @Test
    @DisplayName("更新工作流节点 - 成功")
    void testUpdateWorkflowNodes() {
        Workflow workflow = workflowRepository.save(createTestWorkflow("节点测试", "描述"));

        Workflow updates = new Workflow();
        updates.setNodes(List.of(createNode("new-node", "新节点", "type")));

        webTestClient.put()
            .uri("/api/v1/workflows/" + workflow.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updates)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Workflow.class)
            .value(w -> {
                assertEquals(1, w.getNodes().size());
                assertEquals("new-node", w.getNodes().get(0).getNodeId());
            });
    }

    @Test
    @DisplayName("更新不存在的工作流 - 404")
    void testUpdateWorkflowNotFound() {
        Workflow updates = new Workflow();
        updates.setName("新名称");

        webTestClient.put()
            .uri("/api/v1/workflows/non-existent-id")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updates)
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("多次更新工作流 - 版本号递增")
    void testUpdateWorkflowVersionIncrement() {
        Workflow workflow = workflowRepository.save(createTestWorkflow("版本测试", "描述"));
        int initialVersion = workflow.getVersion();

        for (int i = 0; i < 3; i++) {
            Workflow updates = new Workflow();
            updates.setName("名称" + i);

            webTestClient.put()
                .uri("/api/v1/workflows/" + workflow.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updates)
                .exchange()
                .expectStatus().isOk();
        }

        Workflow updated = workflowRepository.findById(workflow.getId()).orElseThrow();
        assertEquals(initialVersion + 3, updated.getVersion());
    }

    // ==================== 删除工作流测试 ====================

    @Test
    @DisplayName("删除工作流 - 成功")
    void testDeleteWorkflowSuccess() {
        Workflow workflow = workflowRepository.save(createTestWorkflow("删除测试", "描述"));

        webTestClient.delete()
            .uri("/api/v1/workflows/" + workflow.getId())
            .exchange()
            .expectStatus().isNoContent();

        // 验证已删除
        assertFalse(workflowRepository.existsById(workflow.getId()));
    }

    @Test
    @DisplayName("删除不存在的工作流 - 404")
    void testDeleteWorkflowNotFound() {
        webTestClient.delete()
            .uri("/api/v1/workflows/non-existent-id")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("删除后无法获取 - 404")
    void testDeleteWorkflowThenGet() {
        Workflow workflow = workflowRepository.save(createTestWorkflow("删除后获取", "描述"));

        webTestClient.delete()
            .uri("/api/v1/workflows/" + workflow.getId())
            .exchange()
            .expectStatus().isNoContent();

        webTestClient.get()
            .uri("/api/v1/workflows/" + workflow.getId())
            .exchange()
            .expectStatus().isNotFound();
    }

    // ==================== 发布/取消发布测试 ====================

    @Test
    @DisplayName("发布工作流 - 成功")
    void testTogglePublishToPublished() {
        Workflow workflow = workflowRepository.save(createTestWorkflow("发布测试", "描述"));
        assertFalse(workflow.getPublished());

        webTestClient.post()
            .uri("/api/v1/workflows/" + workflow.getId() + "/publish")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Workflow.class)
            .value(w -> assertTrue(w.getPublished()));
    }

    @Test
    @DisplayName("取消发布工作流 - 成功")
    void testTogglePublishToUnpublished() {
        Workflow workflow = createTestWorkflow("取消发布测试", "描述");
        workflow.setPublished(true);
        workflow = workflowRepository.save(workflow);

        webTestClient.post()
            .uri("/api/v1/workflows/" + workflow.getId() + "/publish")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Workflow.class)
            .value(w -> assertFalse(w.getPublished()));
    }

    @Test
    @DisplayName("发布不存在的工作流 - 404")
    void testTogglePublishNotFound() {
        webTestClient.post()
            .uri("/api/v1/workflows/non-existent-id/publish")
            .exchange()
            .expectStatus().isNotFound();
    }

    // ==================== 执行工作流测试 ====================

    @Test
    @DisplayName("执行工作流 - 成功")
    void testExecuteWorkflowSuccess() {
        Workflow workflow = workflowRepository.save(createTestWorkflow("执行测试", "描述"));

        Map<String, Object> inputs = Map.of("param1", "value1", "param2", 123);

        webTestClient.post()
            .uri("/api/v1/workflows/" + workflow.getId() + "/execute")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(inputs)
            .header("X-User-Id", "executor-user")
            .exchange()
            .expectStatus().isAccepted()
            .expectBody()
            .jsonPath("$.executionId").exists()
            .jsonPath("$.status").isEqualTo("PENDING")
            .jsonPath("$.workflowId").isEqualTo(workflow.getId());
    }

    @Test
    @DisplayName("执行工作流无输入 - 成功")
    void testExecuteWorkflowWithoutInputs() {
        Workflow workflow = workflowRepository.save(createTestWorkflow("无输入执行", "描述"));

        webTestClient.post()
            .uri("/api/v1/workflows/" + workflow.getId() + "/execute")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isAccepted();
    }

    @Test
    @DisplayName("执行不存在的工作流 - 400")
    void testExecuteWorkflowNotFound() {
        webTestClient.post()
            .uri("/api/v1/workflows/non-existent-id/execute")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isBadRequest();
    }

    // ==================== 错误处理测试 ====================

    @Test
    @DisplayName("请求格式错误 - 400")
    void testBadRequestInvalidJson() {
        webTestClient.post()
            .uri("/api/v1/workflows")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("invalid json")
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("路径变量格式错误 - 404")
    void testNotFoundInvalidPath() {
        webTestClient.get()
            .uri("/api/v1/workflows/invalid id with spaces")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("并发创建多个工作流")
    void testConcurrentCreateWorkflows() {
        for (int i = 0; i < 10; i++) {
            Workflow workflow = createTestWorkflow("并发工作流" + i, "描述" + i);
            webTestClient.post()
                .uri("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(workflow)
                .exchange()
                .expectStatus().isOk();
        }

        webTestClient.get()
            .uri("/api/v1/workflows")
            .exchange()
            .expectStatus().isOk()
            .expectBody(List.class)
            .value(list -> assertEquals(10, list.size()));
    }

    @Test
    @DisplayName("工作流完整 CRUD 流程")
    void testFullCrudFlow() {
        // Create
        Workflow workflow = createTestWorkflow("CRUD 测试", "完整流程");
        workflow.setNodes(List.of(createNode("n1", "节点 1", "type")));
        
        webTestClient.post()
            .uri("/api/v1/workflows")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(workflow)
            .header("X-User-Id", "crud-user")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Workflow.class)
            .value(w -> {
                assertNotNull(w.getId());
                assertEquals(1, w.getVersion());
            });

        Workflow created = workflowRepository.findAll().stream()
            .filter(w -> "CRUD 测试".equals(w.getName()))
            .findFirst()
            .orElseThrow();

        // Read
        webTestClient.get()
            .uri("/api/v1/workflows/" + created.getId())
            .exchange()
            .expectStatus().isOk();

        // Update
        Workflow updates = new Workflow();
        updates.setDescription("更新后的描述");
        webTestClient.put()
            .uri("/api/v1/workflows/" + created.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updates)
            .exchange()
            .expectStatus().isOk();

        // Publish
        webTestClient.post()
            .uri("/api/v1/workflows/" + created.getId() + "/publish")
            .exchange()
            .expectStatus().isOk();

        // Execute
        webTestClient.post()
            .uri("/api/v1/workflows/" + created.getId() + "/execute")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isAccepted();

        // Delete
        webTestClient.delete()
            .uri("/api/v1/workflows/" + created.getId())
            .exchange()
            .expectStatus().isNoContent();

        // Verify deleted
        webTestClient.get()
            .uri("/api/v1/workflows/" + created.getId())
            .exchange()
            .expectStatus().isNotFound();
    }

    // ==================== 辅助方法 ====================

    private Workflow createTestWorkflow(String name, String description) {
        Workflow workflow = new Workflow();
        workflow.setName(name);
        workflow.setDescription(description);
        return workflow;
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

    private WorkflowEdge createEdge(String id, String source, String target) {
        WorkflowEdge edge = new WorkflowEdge();
        edge.setId(id);
        edge.setSource(source);
        edge.setTarget(target);
        return edge;
    }
}
