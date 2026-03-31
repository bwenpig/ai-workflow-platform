package com.ben.workflow.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowNode 实体测试
 */
public class WorkflowNodeTest {
    
    @Test
    @DisplayName("创建空 WorkflowNode 对象")
    public void testCreateEmptyNode() {
        WorkflowNode node = new WorkflowNode();
        
        assertNull(node.getNodeId());
        assertNull(node.getType());
        assertNull(node.getPosition());
        assertNull(node.getInputs());
        assertNull(node.getOutputs());
        assertNull(node.getConfig());
        assertNull(node.getModelProvider());
        assertNull(node.getStatus());
        assertNull(node.getResult());
    }
    
    @Test
    @DisplayName("设置和获取节点 ID")
    public void testSetNodeId() {
        WorkflowNode node = new WorkflowNode();
        
        node.setNodeId("node-123");
        
        assertEquals("node-123", node.getNodeId());
    }
    
    @Test
    @DisplayName("设置和获取节点类型")
    public void testSetType() {
        WorkflowNode node = new WorkflowNode();
        
        node.setType("IMAGE_GENERATION");
        
        assertEquals("IMAGE_GENERATION", node.getType());
    }
    
    @Test
    @DisplayName("设置和获取位置")
    public void testSetPosition() {
        WorkflowNode node = new WorkflowNode();
        WorkflowNode.Position position = new WorkflowNode.Position();
        position.setX(100.0);
        position.setY(200.0);
        
        node.setPosition(position);
        
        assertEquals(100.0, node.getPosition().getX());
        assertEquals(200.0, node.getPosition().getY());
    }
    
    @Test
    @DisplayName("Position 内部类测试")
    public void testPosition() {
        WorkflowNode.Position position = new WorkflowNode.Position();
        
        position.setX(50.5);
        position.setY(75.5);
        
        assertEquals(50.5, position.getX());
        assertEquals(75.5, position.getY());
    }
    
    @Test
    @DisplayName("Position 设置 null 值")
    public void testPositionNullValues() {
        WorkflowNode.Position position = new WorkflowNode.Position();
        
        position.setX(null);
        position.setY(null);
        
        assertNull(position.getX());
        assertNull(position.getY());
    }
    
    @Test
    @DisplayName("设置和获取输入端口列表")
    public void testSetInputs() {
        WorkflowNode node = new WorkflowNode();
        WorkflowNode.InputPort input = new WorkflowNode.InputPort();
        input.setId("input-1");
        input.setLabel("输入 1");
        
        node.setInputs(Arrays.asList(input));
        
        assertEquals(1, node.getInputs().size());
        assertEquals("input-1", node.getInputs().get(0).getId());
        assertEquals("输入 1", node.getInputs().get(0).getLabel());
    }
    
    @Test
    @DisplayName("设置空输入端口列表")
    public void testSetEmptyInputs() {
        WorkflowNode node = new WorkflowNode();
        
        node.setInputs(Arrays.asList());
        
        assertTrue(node.getInputs().isEmpty());
    }
    
    @Test
    @DisplayName("设置和获取输出端口列表")
    public void testSetOutputs() {
        WorkflowNode node = new WorkflowNode();
        WorkflowNode.OutputPort output = new WorkflowNode.OutputPort();
        output.setId("output-1");
        output.setLabel("输出 1");
        
        node.setOutputs(Arrays.asList(output));
        
        assertEquals(1, node.getOutputs().size());
        assertEquals("output-1", node.getOutputs().get(0).getId());
    }
    
    @Test
    @DisplayName("InputPort 内部类测试")
    public void testInputPort() {
        WorkflowNode.InputPort input = new WorkflowNode.InputPort();
        
        input.setId("input-1");
        input.setLabel("提示词");
        input.setType("TEXT");
        input.setDefaultValue("默认值");
        
        assertEquals("input-1", input.getId());
        assertEquals("提示词", input.getLabel());
        assertEquals("TEXT", input.getType());
        assertEquals("默认值", input.getDefaultValue());
    }
    
    @Test
    @DisplayName("InputPort 设置 null 默认值")
    public void testInputPortNullDefault() {
        WorkflowNode.InputPort input = new WorkflowNode.InputPort();
        
        input.setDefaultValue(null);
        
        assertNull(input.getDefaultValue());
    }
    
    @Test
    @DisplayName("OutputPort 内部类测试")
    public void testOutputPort() {
        WorkflowNode.OutputPort output = new WorkflowNode.OutputPort();
        
        output.setId("output-1");
        output.setLabel("生成结果");
        output.setType("IMAGE");
        
        assertEquals("output-1", output.getId());
        assertEquals("生成结果", output.getLabel());
        assertEquals("IMAGE", output.getType());
    }
    
    @Test
    @DisplayName("设置和获取配置")
    public void testSetConfig() {
        WorkflowNode node = new WorkflowNode();
        Map<String, Object> config = new HashMap<>();
        config.put("model", "stable-diffusion");
        config.put("steps", 50);
        
        node.setConfig(config);
        
        assertEquals(2, node.getConfig().size());
        assertEquals("stable-diffusion", node.getConfig().get("model"));
        assertEquals(50, node.getConfig().get("steps"));
    }
    
    @Test
    @DisplayName("设置空配置")
    public void testSetEmptyConfig() {
        WorkflowNode node = new WorkflowNode();
        
        node.setConfig(new HashMap<>());
        
        assertTrue(node.getConfig().isEmpty());
    }
    
    @Test
    @DisplayName("设置和获取模型提供商")
    public void testSetModelProvider() {
        WorkflowNode node = new WorkflowNode();
        
        node.setModelProvider("KLING");
        
        assertEquals("KLING", node.getModelProvider());
    }
    
    @Test
    @DisplayName("设置和获取节点状态")
    public void testSetNodeStatus() {
        WorkflowNode node = new WorkflowNode();
        
        node.setStatus(WorkflowNode.NodeStatus.PENDING);
        
        assertEquals(WorkflowNode.NodeStatus.PENDING, node.getStatus());
        
        node.setStatus(WorkflowNode.NodeStatus.RUNNING);
        
        assertEquals(WorkflowNode.NodeStatus.RUNNING, node.getStatus());
        
        node.setStatus(WorkflowNode.NodeStatus.SUCCESS);
        
        assertEquals(WorkflowNode.NodeStatus.SUCCESS, node.getStatus());
        
        node.setStatus(WorkflowNode.NodeStatus.FAILED);
        
        assertEquals(WorkflowNode.NodeStatus.FAILED, node.getStatus());
        
        node.setStatus(WorkflowNode.NodeStatus.SKIPPED);
        
        assertEquals(WorkflowNode.NodeStatus.SKIPPED, node.getStatus());
    }
    
    @Test
    @DisplayName("NodeStatus 枚举值测试")
    public void testNodeStatusEnumValues() {
        assertNotNull(WorkflowNode.NodeStatus.PENDING);
        assertNotNull(WorkflowNode.NodeStatus.RUNNING);
        assertNotNull(WorkflowNode.NodeStatus.SUCCESS);
        assertNotNull(WorkflowNode.NodeStatus.FAILED);
        assertNotNull(WorkflowNode.NodeStatus.SKIPPED);
    }
    
    @Test
    @DisplayName("设置和获取执行结果")
    public void testSetResult() {
        WorkflowNode node = new WorkflowNode();
        WorkflowNode.ExecutionResult result = new WorkflowNode.ExecutionResult();
        result.setOutputUrl("https://example.com/output.jpg");
        
        node.setResult(result);
        
        assertEquals("https://example.com/output.jpg", node.getResult().getOutputUrl());
    }
    
    @Test
    @DisplayName("ExecutionResult 内部类测试")
    public void testExecutionResult() {
        WorkflowNode.ExecutionResult result = new WorkflowNode.ExecutionResult();
        
        result.setOutputUrl("https://example.com/result.mp4");
        result.setMetadata(Map.of("duration", 30));
        result.setErrorMessage(null);
        
        assertEquals("https://example.com/result.mp4", result.getOutputUrl());
        assertEquals(30, result.getMetadata().get("duration"));
        assertNull(result.getErrorMessage());
    }
    
    @Test
    @DisplayName("ExecutionResult 设置错误信息")
    public void testExecutionResultWithError() {
        WorkflowNode.ExecutionResult result = new WorkflowNode.ExecutionResult();
        
        result.setErrorMessage("生成失败");
        
        assertEquals("生成失败", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("ExecutionResult 空元数据")
    public void testExecutionResultEmptyMetadata() {
        WorkflowNode.ExecutionResult result = new WorkflowNode.ExecutionResult();
        
        result.setMetadata(new HashMap<>());
        
        assertTrue(result.getMetadata().isEmpty());
    }
    
    @Test
    @DisplayName("完整节点对象测试")
    public void testFullNode() {
        WorkflowNode node = new WorkflowNode();
        
        node.setNodeId("node-1");
        node.setType("VIDEO_GENERATION");
        
        WorkflowNode.Position position = new WorkflowNode.Position();
        position.setX(100.0);
        position.setY(200.0);
        node.setPosition(position);
        
        WorkflowNode.InputPort input = new WorkflowNode.InputPort();
        input.setId("in-1");
        node.setInputs(Arrays.asList(input));
        
        WorkflowNode.OutputPort output = new WorkflowNode.OutputPort();
        output.setId("out-1");
        node.setOutputs(Arrays.asList(output));
        
        node.setConfig(Map.of("model", "wan"));
        node.setModelProvider("WAN");
        node.setStatus(WorkflowNode.NodeStatus.SUCCESS);
        
        WorkflowNode.ExecutionResult result = new WorkflowNode.ExecutionResult();
        result.setOutputUrl("result.mp4");
        node.setResult(result);
        
        assertEquals("node-1", node.getNodeId());
        assertEquals("VIDEO_GENERATION", node.getType());
        assertEquals(100.0, node.getPosition().getX());
        assertEquals(1, node.getInputs().size());
        assertEquals(1, node.getOutputs().size());
        assertEquals("wan", node.getConfig().get("model"));
        assertEquals("WAN", node.getModelProvider());
        assertEquals(WorkflowNode.NodeStatus.SUCCESS, node.getStatus());
        assertEquals("result.mp4", node.getResult().getOutputUrl());
    }
}
