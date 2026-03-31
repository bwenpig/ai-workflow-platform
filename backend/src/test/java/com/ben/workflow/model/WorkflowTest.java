package com.ben.workflow.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Workflow 实体测试
 */
public class WorkflowTest {
    
    @Test
    @DisplayName("创建空 Workflow 对象")
    public void testCreateEmptyWorkflow() {
        Workflow workflow = new Workflow();
        
        assertNull(workflow.getId());
        assertNull(workflow.getName());
        assertNull(workflow.getDescription());
        assertNull(workflow.getNodes());
        assertNull(workflow.getEdges());
        assertNull(workflow.getCreatedAt());
        assertNull(workflow.getUpdatedAt());
        assertNull(workflow.getCreatedBy());
        assertNull(workflow.getVersion());
        assertNull(workflow.getPublished());
    }
    
    @Test
    @DisplayName("设置和获取 ID")
    public void testSetId() {
        Workflow workflow = new Workflow();
        String id = "workflow-123";
        
        workflow.setId(id);
        
        assertEquals(id, workflow.getId());
    }
    
    @Test
    @DisplayName("设置和获取名称")
    public void testSetName() {
        Workflow workflow = new Workflow();
        String name = "视频生成工作流";
        
        workflow.setName(name);
        
        assertEquals(name, workflow.getName());
    }
    
    @Test
    @DisplayName("设置和获取描述")
    public void testSetDescription() {
        Workflow workflow = new Workflow();
        String description = "用于 AI 视频生成的工作流";
        
        workflow.setDescription(description);
        
        assertEquals(description, workflow.getDescription());
    }
    
    @Test
    @DisplayName("设置和获取节点列表")
    public void testSetNodes() {
        Workflow workflow = new Workflow();
        WorkflowNode node1 = new WorkflowNode();
        node1.setNodeId("node-1");
        WorkflowNode node2 = new WorkflowNode();
        node2.setNodeId("node-2");
        List<WorkflowNode> nodes = Arrays.asList(node1, node2);
        
        workflow.setNodes(nodes);
        
        assertEquals(2, workflow.getNodes().size());
        assertEquals("node-1", workflow.getNodes().get(0).getNodeId());
        assertEquals("node-2", workflow.getNodes().get(1).getNodeId());
    }
    
    @Test
    @DisplayName("设置空节点列表")
    public void testSetEmptyNodes() {
        Workflow workflow = new Workflow();
        
        workflow.setNodes(Arrays.asList());
        
        assertTrue(workflow.getNodes().isEmpty());
    }
    
    @Test
    @DisplayName("设置 null 节点列表")
    public void testSetNullNodes() {
        Workflow workflow = new Workflow();
        
        workflow.setNodes(null);
        
        assertNull(workflow.getNodes());
    }
    
    @Test
    @DisplayName("设置和获取边列表")
    public void testSetEdges() {
        Workflow workflow = new Workflow();
        WorkflowEdge edge1 = new WorkflowEdge();
        edge1.setId("edge-1");
        WorkflowEdge edge2 = new WorkflowEdge();
        edge2.setId("edge-2");
        List<WorkflowEdge> edges = Arrays.asList(edge1, edge2);
        
        workflow.setEdges(edges);
        
        assertEquals(2, workflow.getEdges().size());
        assertEquals("edge-1", workflow.getEdges().get(0).getId());
        assertEquals("edge-2", workflow.getEdges().get(1).getId());
    }
    
    @Test
    @DisplayName("设置和获取创建时间")
    public void testSetCreatedAt() {
        Workflow workflow = new Workflow();
        Instant now = Instant.now();
        
        workflow.setCreatedAt(now);
        
        assertEquals(now, workflow.getCreatedAt());
    }
    
    @Test
    @DisplayName("设置和获取更新时间")
    public void testSetUpdatedAt() {
        Workflow workflow = new Workflow();
        Instant now = Instant.now();
        
        workflow.setUpdatedAt(now);
        
        assertEquals(now, workflow.getUpdatedAt());
    }
    
    @Test
    @DisplayName("设置和获取创建者")
    public void testSetCreatedBy() {
        Workflow workflow = new Workflow();
        String createdBy = "user-456";
        
        workflow.setCreatedBy(createdBy);
        
        assertEquals(createdBy, workflow.getCreatedBy());
    }
    
    @Test
    @DisplayName("设置和获取版本号")
    public void testSetVersion() {
        Workflow workflow = new Workflow();
        
        workflow.setVersion(1);
        
        assertEquals(1, workflow.getVersion());
        
        workflow.setVersion(2);
        
        assertEquals(2, workflow.getVersion());
    }
    
    @Test
    @DisplayName("设置和获取发布状态")
    public void testSetPublished() {
        Workflow workflow = new Workflow();
        
        workflow.setPublished(true);
        
        assertTrue(workflow.getPublished());
        
        workflow.setPublished(false);
        
        assertFalse(workflow.getPublished());
    }
    
    @Test
    @DisplayName("设置 null 名称")
    public void testSetNullName() {
        Workflow workflow = new Workflow();
        
        workflow.setName(null);
        
        assertNull(workflow.getName());
    }
    
    @Test
    @DisplayName("设置空字符串名称")
    public void testSetEmptyName() {
        Workflow workflow = new Workflow();
        
        workflow.setName("");
        
        assertEquals("", workflow.getName());
    }
    
    @Test
    @DisplayName("完整 Workflow 对象测试")
    public void testFullWorkflow() {
        Workflow workflow = new Workflow();
        Instant now = Instant.now();
        
        workflow.setId("wf-123");
        workflow.setName("测试工作流");
        workflow.setDescription("测试描述");
        workflow.setNodes(Arrays.asList(new WorkflowNode()));
        workflow.setEdges(Arrays.asList(new WorkflowEdge()));
        workflow.setCreatedAt(now);
        workflow.setUpdatedAt(now);
        workflow.setCreatedBy("ben");
        workflow.setVersion(1);
        workflow.setPublished(true);
        
        assertEquals("wf-123", workflow.getId());
        assertEquals("测试工作流", workflow.getName());
        assertEquals("测试描述", workflow.getDescription());
        assertEquals(1, workflow.getNodes().size());
        assertEquals(1, workflow.getEdges().size());
        assertEquals(now, workflow.getCreatedAt());
        assertEquals(now, workflow.getUpdatedAt());
        assertEquals("ben", workflow.getCreatedBy());
        assertEquals(1, workflow.getVersion());
        assertTrue(workflow.getPublished());
    }
}
