package com.ben.workflow.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowEdge 实体测试
 */
public class WorkflowEdgeTest {
    
    @Test
    @DisplayName("创建空 WorkflowEdge 对象")
    public void testCreateEmptyEdge() {
        WorkflowEdge edge = new WorkflowEdge();
        
        assertNull(edge.getId());
        assertNull(edge.getSource());
        assertNull(edge.getTarget());
        assertNull(edge.getSourceHandle());
        assertNull(edge.getTargetHandle());
        assertNull(edge.getDataType());
    }
    
    @Test
    @DisplayName("设置和获取 ID")
    public void testSetId() {
        WorkflowEdge edge = new WorkflowEdge();
        
        edge.setId("edge-123");
        
        assertEquals("edge-123", edge.getId());
    }
    
    @Test
    @DisplayName("设置和获取源节点")
    public void testSetSource() {
        WorkflowEdge edge = new WorkflowEdge();
        
        edge.setSource("node-1");
        
        assertEquals("node-1", edge.getSource());
    }
    
    @Test
    @DisplayName("设置和获取目标节点")
    public void testSetTarget() {
        WorkflowEdge edge = new WorkflowEdge();
        
        edge.setTarget("node-2");
        
        assertEquals("node-2", edge.getTarget());
    }
    
    @Test
    @DisplayName("设置和获取源端口")
    public void testSetSourceHandle() {
        WorkflowEdge edge = new WorkflowEdge();
        
        edge.setSourceHandle("output-1");
        
        assertEquals("output-1", edge.getSourceHandle());
    }
    
    @Test
    @DisplayName("设置和获取目标端口")
    public void testSetTargetHandle() {
        WorkflowEdge edge = new WorkflowEdge();
        
        edge.setTargetHandle("input-1");
        
        assertEquals("input-1", edge.getTargetHandle());
    }
    
    @Test
    @DisplayName("设置和获取数据类型")
    public void testSetDataType() {
        WorkflowEdge edge = new WorkflowEdge();
        
        edge.setDataType("IMAGE");
        
        assertEquals("IMAGE", edge.getDataType());
    }
    
    @Test
    @DisplayName("设置 null 值")
    public void testSetNullValues() {
        WorkflowEdge edge = new WorkflowEdge();
        
        edge.setId(null);
        edge.setSource(null);
        edge.setTarget(null);
        edge.setSourceHandle(null);
        edge.setTargetHandle(null);
        edge.setDataType(null);
        
        assertNull(edge.getId());
        assertNull(edge.getSource());
        assertNull(edge.getTarget());
        assertNull(edge.getSourceHandle());
        assertNull(edge.getTargetHandle());
        assertNull(edge.getDataType());
    }
    
    @Test
    @DisplayName("设置空字符串")
    public void testSetEmptyStrings() {
        WorkflowEdge edge = new WorkflowEdge();
        
        edge.setId("");
        edge.setSource("");
        edge.setTarget("");
        
        assertEquals("", edge.getId());
        assertEquals("", edge.getSource());
        assertEquals("", edge.getTarget());
    }
    
    @Test
    @DisplayName("完整边对象测试")
    public void testFullEdge() {
        WorkflowEdge edge = new WorkflowEdge();
        
        edge.setId("edge-1");
        edge.setSource("node-1");
        edge.setTarget("node-2");
        edge.setSourceHandle("output");
        edge.setTargetHandle("input");
        edge.setDataType("VIDEO");
        
        assertEquals("edge-1", edge.getId());
        assertEquals("node-1", edge.getSource());
        assertEquals("node-2", edge.getTarget());
        assertEquals("output", edge.getSourceHandle());
        assertEquals("input", edge.getTargetHandle());
        assertEquals("VIDEO", edge.getDataType());
    }
    
    @Test
    @DisplayName("边连接同一节点")
    public void testEdgeSameNode() {
        WorkflowEdge edge = new WorkflowEdge();
        
        edge.setSource("node-1");
        edge.setTarget("node-1");
        
        assertEquals("node-1", edge.getSource());
        assertEquals("node-1", edge.getTarget());
    }
}
