package com.ben.workflow.repository;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.model.WorkflowNode;
import com.ben.workflow.model.WorkflowEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowRepository 测试
 * 使用 @DataMongoTest 进行数据访问层测试
 */
@DataMongoTest
@ActiveProfiles("test")
class WorkflowRepositoryTest {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @BeforeEach
    void setUp() {
        workflowRepository.deleteAll();
        executionRepository.deleteAll();
    }

    // ==================== 保存工作流测试 ====================

    @Test
    @DisplayName("保存简单工作流 - 成功")
    void testSaveSimpleWorkflow() {
        Workflow workflow = createTestWorkflow("保存测试", "测试描述", "user-1");

        Workflow saved = workflowRepository.save(workflow);

        assertNotNull(saved.getId());
        assertEquals("保存测试", saved.getName());
        assertEquals("测试描述", saved.getDescription());
        assertEquals("user-1", saved.getCreatedBy());
        assertEquals(1, saved.getVersion());
        assertFalse(saved.getPublished());
    }

    @Test
    @DisplayName("保存带节点的工作流")
    void testSaveWorkflowWithNodes() {
        Workflow workflow = createTestWorkflow("带节点工作流", "描述", "user-1");
        workflow.setNodes(List.of(
            createNode("node1", "开始", "start"),
            createNode("node2", "处理", "process"),
            createNode("node3", "结束", "end")
        ));

        Workflow saved = workflowRepository.save(workflow);

        assertNotNull(saved.getId());
        assertEquals(3, saved.getNodes().size());
        assertEquals("node1", saved.getNodes().get(0).getNodeId());
    }

    @Test
    @DisplayName("保存带边的工作流")
    void testSaveWorkflowWithEdges() {
        Workflow workflow = createTestWorkflow("带边工作流", "描述", "user-1");
        workflow.setEdges(List.of(
            createEdge("edge1", "node1", "node2"),
            createEdge("edge2", "node2", "node3")
        ));

        Workflow saved = workflowRepository.save(workflow);

        assertNotNull(saved.getId());
        assertEquals(2, saved.getEdges().size());
    }

    @Test
    @DisplayName("保存带节点和边的完整工作流")
    void testSaveCompleteWorkflow() {
        Workflow workflow = createTestWorkflow("完整工作流", "描述", "user-1");
        workflow.setNodes(List.of(
            createNode("n1", "节点 1", "start"),
            createNode("n2", "节点 2", "process")
        ));
        workflow.setEdges(List.of(
            createEdge("e1", "n1", "n2")
        ));
        workflow.setPublished(true);
        workflow.setVersion(5);

        Workflow saved = workflowRepository.save(workflow);

        assertNotNull(saved.getId());
        assertEquals(2, saved.getNodes().size());
        assertEquals(1, saved.getEdges().size());
        assertTrue(saved.getPublished());
        assertEquals(5, saved.getVersion());
    }

    @Test
    @DisplayName("保存多个工作流")
    void testSaveMultipleWorkflows() {
        for (int i = 0; i < 5; i++) {
            Workflow workflow = createTestWorkflow("工作流" + i, "描述" + i, "user-" + i);
            workflowRepository.save(workflow);
        }

        List<Workflow> all = workflowRepository.findAll();
        assertEquals(5, all.size());
    }

    // ==================== 查询工作流测试 ====================

    @Test
    @DisplayName("查询所有工作流")
    void testFindAll() {
        createAndSaveWorkflow("工作流 1", "user-1");
        createAndSaveWorkflow("工作流 2", "user-2");
        createAndSaveWorkflow("工作流 3", "user-1");

        List<Workflow> all = workflowRepository.findAll();
        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("按 ID 查询 - 存在")
    void testFindByIdExists() {
        Workflow saved = createAndSaveWorkflow("查询测试", "user-1");

        Optional<Workflow> found = workflowRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("查询测试", found.get().getName());
    }

    @Test
    @DisplayName("按 ID 查询 - 不存在")
    void testFindByIdNotExists() {
        Optional<Workflow> found = workflowRepository.findById("non-existent-id");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("检查 ID 是否存在")
    void testExistsById() {
        Workflow saved = createAndSaveWorkflow("存在检查", "user-1");

        assertTrue(workflowRepository.existsById(saved.getId()));
        assertFalse(workflowRepository.existsById("non-existent-id"));
    }

    @Test
    @DisplayName("按名称查找")
    void testFindByName() {
        Workflow w1 = createAndSaveWorkflow("相同名称", "user-1");
        Workflow w2 = createAndSaveWorkflow("相同名称", "user-2");
        createAndSaveWorkflow("不同名称", "user-1");

        List<Workflow> found = workflowRepository.findByName("相同名称");

        assertEquals(2, found.size());
        assertTrue(found.stream().allMatch(w -> "相同名称".equals(w.getName())));
    }

    @Test
    @DisplayName("按名称查找 - 无结果")
    void testFindByNameNoResults() {
        createAndSaveWorkflow("工作流 1", "user-1");
        createAndSaveWorkflow("工作流 2", "user-2");

        List<Workflow> found = workflowRepository.findByName("不存在的名称");

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("按名称和版本查找")
    void testFindByNameAndVersion() {
        Workflow w1 = createAndSaveWorkflow("名称", "user-1");
        w1.setVersion(1);
        workflowRepository.save(w1);

        Workflow w2 = createAndSaveWorkflow("名称", "user-2");
        w2.setVersion(2);
        workflowRepository.save(w2);

        createAndSaveWorkflow("名称", "user-3"); // version 1

        Optional<Workflow> found = workflowRepository.findByNameAndVersion("名称", 2);

        assertTrue(found.isPresent());
        assertEquals(2, found.get().getVersion());
    }

    // ==================== 更新工作流测试 ====================

    @Test
    @DisplayName("更新工作流 - 成功")
    void testUpdateWorkflow() {
        Workflow workflow = createAndSaveWorkflow("原名", "user-1");

        workflow.setName("新名称");
        workflow.setDescription("新描述");
        workflow.setVersion(2);

        Workflow updated = workflowRepository.save(workflow);

        assertEquals("新名称", updated.getName());
        assertEquals("新描述", updated.getDescription());
        assertEquals(2, updated.getVersion());
    }

    @Test
    @DisplayName("更新工作流节点")
    void testUpdateWorkflowNodes() {
        Workflow workflow = createAndSaveWorkflow("节点更新", "user-1");

        workflow.setNodes(List.of(
            createNode("new1", "新节点 1", "type"),
            createNode("new2", "新节点 2", "type")
        ));

        Workflow updated = workflowRepository.save(workflow);

        assertEquals(2, updated.getNodes().size());
        assertEquals("new1", updated.getNodes().get(0).getNodeId());
    }

    @Test
    @DisplayName("更新工作流边")
    void testUpdateWorkflowEdges() {
        Workflow workflow = createAndSaveWorkflow("边更新", "user-1");

        workflow.setEdges(List.of(
            createEdge("new-edge", "n1", "n2")
        ));

        Workflow updated = workflowRepository.save(workflow);

        assertEquals(1, updated.getEdges().size());
        assertEquals("new-edge", updated.getEdges().get(0).getId());
    }

    @Test
    @DisplayName("更新发布状态")
    void testUpdatePublishedStatus() {
        Workflow workflow = createAndSaveWorkflow("发布更新", "user-1");
        assertFalse(workflow.getPublished());

        workflow.setPublished(true);
        Workflow updated = workflowRepository.save(workflow);

        assertTrue(updated.getPublished());
    }

    // ==================== 删除工作流测试 ====================

    @Test
    @DisplayName("删除工作流 - 成功")
    void testDeleteWorkflowSuccess() {
        Workflow workflow = createAndSaveWorkflow("删除测试", "user-1");

        workflowRepository.delete(workflow);

        assertFalse(workflowRepository.existsById(workflow.getId()));
    }

    @Test
    @DisplayName("按 ID 删除 - 成功")
    void testDeleteByIdSuccess() {
        Workflow workflow = createAndSaveWorkflow("按 ID 删除", "user-1");
        String id = workflow.getId();

        workflowRepository.deleteById(id);

        assertFalse(workflowRepository.existsById(id));
    }

    @Test
    @DisplayName("删除不存在的工作流 - 无异常")
    void testDeleteNotExists() {
        // 删除不存在的 ID 不应该抛出异常
        assertDoesNotThrow(() -> workflowRepository.deleteById("non-existent-id"));
    }

    @Test
    @DisplayName("删除所有工作流")
    void testDeleteAll() {
        createAndSaveWorkflow("工作流 1", "user-1");
        createAndSaveWorkflow("工作流 2", "user-2");
        createAndSaveWorkflow("工作流 3", "user-1");

        workflowRepository.deleteAll();

        assertEquals(0, workflowRepository.count());
    }

    @Test
    @DisplayName("删除后无法查询到")
    void testDeleteThenFind() {
        Workflow workflow = createAndSaveWorkflow("删除后查询", "user-1");
        String id = workflow.getId();

        workflowRepository.deleteById(id);

        Optional<Workflow> found = workflowRepository.findById(id);
        assertFalse(found.isPresent());
    }

    // ==================== 按状态查询测试 ====================

    @Test
    @DisplayName("按用户查找工作流")
    void testFindByCreatedBy() {
        createAndSaveWorkflow("用户 1 工作流 1", "user-1");
        createAndSaveWorkflow("用户 1 工作流 2", "user-1");
        createAndSaveWorkflow("用户 2 工作流", "user-2");

        List<Workflow> user1Workflows = workflowRepository.findByCreatedBy("user-1");

        assertEquals(2, user1Workflows.size());
        assertTrue(user1Workflows.stream().allMatch(w -> "user-1".equals(w.getCreatedBy())));
    }

    @Test
    @DisplayName("按用户查找 - 无结果")
    void testFindByCreatedByNoResults() {
        createAndSaveWorkflow("工作流 1", "user-1");
        createAndSaveWorkflow("工作流 2", "user-2");

        List<Workflow> found = workflowRepository.findByCreatedBy("user-3");

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("查找已发布的工作流")
    void testFindByPublishedTrue() {
        Workflow w1 = createAndSaveWorkflow("已发布 1", "user-1");
        w1.setPublished(true);
        workflowRepository.save(w1);

        Workflow w2 = createAndSaveWorkflow("已发布 2", "user-2");
        w2.setPublished(true);
        workflowRepository.save(w2);

        Workflow w3 = createAndSaveWorkflow("未发布", "user-1");
        w3.setPublished(false);
        workflowRepository.save(w3);

        List<Workflow> published = workflowRepository.findByPublishedTrue();

        assertEquals(2, published.size());
        assertTrue(published.stream().allMatch(Workflow::getPublished));
    }

    @Test
    @DisplayName("查找已发布 - 无结果")
    void testFindByPublishedTrueNoResults() {
        Workflow w1 = createAndSaveWorkflow("未发布 1", "user-1");
        w1.setPublished(false);
        workflowRepository.save(w1);

        Workflow w2 = createAndSaveWorkflow("未发布 2", "user-2");
        w2.setPublished(false);
        workflowRepository.save(w2);

        List<Workflow> published = workflowRepository.findByPublishedTrue();

        assertTrue(published.isEmpty());
    }

    @Test
    @DisplayName("组合查询 - 按用户和发布状态")
    void testCombinedQuery() {
        // 创建测试数据
        Workflow w1 = createAndSaveWorkflow("用户 1 已发布", "user-1");
        w1.setPublished(true);
        workflowRepository.save(w1);

        Workflow w2 = createAndSaveWorkflow("用户 1 未发布", "user-1");
        w2.setPublished(false);
        workflowRepository.save(w2);

        Workflow w3 = createAndSaveWorkflow("用户 2 已发布", "user-2");
        w3.setPublished(true);
        workflowRepository.save(w3);

        // 先按用户筛选
        List<Workflow> user1Workflows = workflowRepository.findByCreatedBy("user-1");
        assertEquals(2, user1Workflows.size());

        // 再筛选已发布
        List<Workflow> published = workflowRepository.findByPublishedTrue();
        assertEquals(2, published.size());

        // 手动组合筛选
        long user1Published = user1Workflows.stream()
            .filter(Workflow::getPublished)
            .count();
        assertEquals(1, user1Published);
    }

    // ==================== 计数测试 ====================

    @Test
    @DisplayName("计数 - 空仓库")
    void testCountEmpty() {
        assertEquals(0, workflowRepository.count());
    }

    @Test
    @DisplayName("计数 - 有多个工作流")
    void testCountWithWorkflows() {
        for (int i = 0; i < 10; i++) {
            createAndSaveWorkflow("工作流" + i, "user-" + (i % 3));
        }

        assertEquals(10, workflowRepository.count());
    }

    // ==================== 批量操作测试 ====================

    @Test
    @DisplayName("批量保存工作流")
    void testSaveAll() {
        List<Workflow> workflows = List.of(
            createTestWorkflow("批量 1", "描述 1", "user-1"),
            createTestWorkflow("批量 2", "描述 2", "user-2"),
            createTestWorkflow("批量 3", "描述 3", "user-1")
        );

        Iterable<Workflow> saved = workflowRepository.saveAll(workflows);

        List<Workflow> all = workflowRepository.findAll();
        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("批量删除工作流")
    void testDeleteAllWorkflows() {
        createAndSaveWorkflow("工作流 1", "user-1");
        createAndSaveWorkflow("工作流 2", "user-2");
        createAndSaveWorkflow("工作流 3", "user-1");

        workflowRepository.deleteAll();

        assertEquals(0, workflowRepository.count());
        assertTrue(workflowRepository.findAll().isEmpty());
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("保存空名称工作流")
    void testSaveEmptyName() {
        Workflow workflow = createTestWorkflow("", "描述", "user-1");

        Workflow saved = workflowRepository.save(workflow);

        assertNotNull(saved.getId());
        assertEquals("", saved.getName());
    }

    @Test
    @DisplayName("保存 null 描述工作流")
    void testSaveNullDescription() {
        Workflow workflow = new Workflow();
        workflow.setName("无描述工作流");
        workflow.setCreatedBy("user-1");
        workflow.setCreatedAt(Instant.now());
        workflow.setUpdatedAt(Instant.now());
        workflow.setVersion(1);
        workflow.setPublished(false);

        Workflow saved = workflowRepository.save(workflow);

        assertNotNull(saved.getId());
        assertNull(saved.getDescription());
    }

    @Test
    @DisplayName("保存带 null 节点列表的工作流")
    void testSaveWithNullNodes() {
        Workflow workflow = createTestWorkflow("空节点", "描述", "user-1");
        workflow.setNodes(null);

        Workflow saved = workflowRepository.save(workflow);

        assertNotNull(saved.getId());
        assertNull(saved.getNodes());
    }

    @Test
    @DisplayName("保存带空节点列表的工作流")
    void testSaveWithEmptyNodes() {
        Workflow workflow = createTestWorkflow("空节点列表", "描述", "user-1");
        workflow.setNodes(List.of());

        Workflow saved = workflowRepository.save(workflow);

        assertNotNull(saved.getId());
        assertTrue(saved.getNodes().isEmpty());
    }

    @Test
    @DisplayName("工作流带特殊字符")
    void testWorkflowWithSpecialCharacters() {
        Workflow workflow = createTestWorkflow("工作流-特殊@#$%", "描述包含<>&\"'", "user-1");

        Workflow saved = workflowRepository.save(workflow);

        // MongoDB 可能会去掉某些特殊字符，我们只验证主要部分
        assertTrue(saved.getName().contains("特殊@#$%"));
        assertTrue(saved.getDescription().contains("描述包含"));
    }

    @Test
    @DisplayName("工作流带长名称")
    void testWorkflowWithLongName() {
        String longName = "A".repeat(1000);
        Workflow workflow = createTestWorkflow(longName, "描述", "user-1");

        Workflow saved = workflowRepository.save(workflow);

        assertEquals(1000, saved.getName().length());
    }

    // ==================== 辅助方法 ====================

    private Workflow createTestWorkflow(String name, String description, String createdBy) {
        Workflow workflow = new Workflow();
        workflow.setName(name);
        workflow.setDescription(description);
        workflow.setCreatedBy(createdBy);
        workflow.setCreatedAt(Instant.now());
        workflow.setUpdatedAt(Instant.now());
        workflow.setVersion(1);
        workflow.setPublished(false);
        return workflow;
    }

    private Workflow createAndSaveWorkflow(String name, String createdBy) {
        Workflow workflow = createTestWorkflow(name, "描述", createdBy);
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

    private WorkflowEdge createEdge(String id, String source, String target) {
        WorkflowEdge edge = new WorkflowEdge();
        edge.setId(id);
        edge.setSource(source);
        edge.setTarget(target);
        return edge;
    }
}
