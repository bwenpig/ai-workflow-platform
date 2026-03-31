package com.ben.workflow.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowExecution 实体测试
 */
public class WorkflowExecutionTest {
    
    @Test
    @DisplayName("创建空 WorkflowExecution 对象")
    public void testCreateEmptyExecution() {
        WorkflowExecution execution = new WorkflowExecution();
        
        assertNull(execution.getId());
        assertNull(execution.getWorkflowId());
        assertNull(execution.getStatus());
        assertNull(execution.getInputs());
        assertNull(execution.getOutputs());
        assertNull(execution.getNodeStates());
        assertNull(execution.getErrorMessage());
        assertNull(execution.getStartedAt());
        assertNull(execution.getEndedAt());
        assertNull(execution.getDurationMs());
        assertNull(execution.getCreatedBy());
        assertNull(execution.getCreatedAt());
    }
    
    @Test
    @DisplayName("设置和获取 ID")
    public void testSetId() {
        WorkflowExecution execution = new WorkflowExecution();
        
        execution.setId("exec-123");
        
        assertEquals("exec-123", execution.getId());
    }
    
    @Test
    @DisplayName("设置和获取工作流 ID")
    public void testSetWorkflowId() {
        WorkflowExecution execution = new WorkflowExecution();
        
        execution.setWorkflowId("wf-456");
        
        assertEquals("wf-456", execution.getWorkflowId());
    }
    
    @Test
    @DisplayName("设置和获取状态")
    public void testSetStatus() {
        WorkflowExecution execution = new WorkflowExecution();
        
        execution.setStatus("RUNNING");
        
        assertEquals("RUNNING", execution.getStatus());
        
        execution.setStatus("SUCCESS");
        
        assertEquals("SUCCESS", execution.getStatus());
    }
    
    @Test
    @DisplayName("设置和获取输入参数")
    public void testSetInputs() {
        WorkflowExecution execution = new WorkflowExecution();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("prompt", "test prompt");
        inputs.put("count", 5);
        
        execution.setInputs(inputs);
        
        assertEquals(2, execution.getInputs().size());
        assertEquals("test prompt", execution.getInputs().get("prompt"));
        assertEquals(5, execution.getInputs().get("count"));
    }
    
    @Test
    @DisplayName("设置空输入参数")
    public void testSetEmptyInputs() {
        WorkflowExecution execution = new WorkflowExecution();
        
        execution.setInputs(new HashMap<>());
        
        assertTrue(execution.getInputs().isEmpty());
    }
    
    @Test
    @DisplayName("设置和获取输出结果")
    public void testSetOutputs() {
        WorkflowExecution execution = new WorkflowExecution();
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("result_url", "https://example.com/result.mp4");
        
        execution.setOutputs(outputs);
        
        assertEquals(1, execution.getOutputs().size());
        assertEquals("https://example.com/result.mp4", execution.getOutputs().get("result_url"));
    }
    
    @Test
    @DisplayName("设置和获取节点状态")
    public void testSetNodeStates() {
        WorkflowExecution execution = new WorkflowExecution();
        Map<String, WorkflowExecution.NodeExecutionState> nodeStates = new HashMap<>();
        
        WorkflowExecution.NodeExecutionState state = new WorkflowExecution.NodeExecutionState();
        state.setNodeId("node-1");
        state.setStatus("SUCCESS");
        nodeStates.put("node-1", state);
        
        execution.setNodeStates(nodeStates);
        
        assertEquals(1, execution.getNodeStates().size());
        assertEquals("node-1", execution.getNodeStates().get("node-1").getNodeId());
        assertEquals("SUCCESS", execution.getNodeStates().get("node-1").getStatus());
    }
    
    @Test
    @DisplayName("设置和获取错误信息")
    public void testSetErrorMessage() {
        WorkflowExecution execution = new WorkflowExecution();
        
        execution.setErrorMessage("执行失败：超时");
        
        assertEquals("执行失败：超时", execution.getErrorMessage());
    }
    
    @Test
    @DisplayName("设置 null 错误信息")
    public void testSetNullErrorMessage() {
        WorkflowExecution execution = new WorkflowExecution();
        
        execution.setErrorMessage(null);
        
        assertNull(execution.getErrorMessage());
    }
    
    @Test
    @DisplayName("设置和获取开始时间")
    public void testSetStartedAt() {
        WorkflowExecution execution = new WorkflowExecution();
        Instant now = Instant.now();
        
        execution.setStartedAt(now);
        
        assertEquals(now, execution.getStartedAt());
    }
    
    @Test
    @DisplayName("设置和获取结束时间")
    public void testSetEndedAt() {
        WorkflowExecution execution = new WorkflowExecution();
        Instant now = Instant.now();
        
        execution.setEndedAt(now);
        
        assertEquals(now, execution.getEndedAt());
    }
    
    @Test
    @DisplayName("设置和获取耗时")
    public void testSetDurationMs() {
        WorkflowExecution execution = new WorkflowExecution();
        
        execution.setDurationMs(5000L);
        
        assertEquals(5000L, execution.getDurationMs());
    }
    
    @Test
    @DisplayName("设置和获取创建者")
    public void testSetCreatedBy() {
        WorkflowExecution execution = new WorkflowExecution();
        
        execution.setCreatedBy("user-789");
        
        assertEquals("user-789", execution.getCreatedBy());
    }
    
    @Test
    @DisplayName("设置和获取创建时间")
    public void testSetCreatedAt() {
        WorkflowExecution execution = new WorkflowExecution();
        Instant now = Instant.now();
        
        execution.setCreatedAt(now);
        
        assertEquals(now, execution.getCreatedAt());
    }
    
    @Test
    @DisplayName("NodeExecutionState 内部类测试")
    public void testNodeExecutionState() {
        WorkflowExecution.NodeExecutionState state = new WorkflowExecution.NodeExecutionState();
        Instant now = Instant.now();
        
        state.setNodeId("node-1");
        state.setStatus("SUCCESS");
        state.setResult(Map.of("output", "result"));
        state.setErrorMessage(null);
        state.setStartedAt(now);
        state.setEndedAt(now);
        state.setDurationMs(1000L);
        
        assertEquals("node-1", state.getNodeId());
        assertEquals("SUCCESS", state.getStatus());
        assertEquals("result", ((Map)state.getResult()).get("output"));
        assertNull(state.getErrorMessage());
        assertEquals(now, state.getStartedAt());
        assertEquals(now, state.getEndedAt());
        assertEquals(1000L, state.getDurationMs());
    }
    
    @Test
    @DisplayName("NodeExecutionState 设置错误信息")
    public void testNodeExecutionStateWithError() {
        WorkflowExecution.NodeExecutionState state = new WorkflowExecution.NodeExecutionState();
        
        state.setStatus("FAILED");
        state.setErrorMessage("节点执行失败");
        
        assertEquals("FAILED", state.getStatus());
        assertEquals("节点执行失败", state.getErrorMessage());
    }
    
    @Test
    @DisplayName("NodeExecutionState 空结果")
    public void testNodeExecutionStateNullResult() {
        WorkflowExecution.NodeExecutionState state = new WorkflowExecution.NodeExecutionState();
        
        state.setResult(null);
        
        assertNull(state.getResult());
    }
    
    @Test
    @DisplayName("完整执行对象测试")
    public void testFullExecution() {
        WorkflowExecution execution = new WorkflowExecution();
        Instant now = Instant.now();
        
        execution.setId("exec-123");
        execution.setWorkflowId("wf-456");
        execution.setStatus("SUCCESS");
        execution.setInputs(Map.of("prompt", "test"));
        execution.setOutputs(Map.of("url", "result.mp4"));
        execution.setErrorMessage(null);
        execution.setStartedAt(now);
        execution.setEndedAt(now);
        execution.setDurationMs(5000L);
        execution.setCreatedBy("ben");
        execution.setCreatedAt(now);
        
        assertEquals("exec-123", execution.getId());
        assertEquals("wf-456", execution.getWorkflowId());
        assertEquals("SUCCESS", execution.getStatus());
        assertEquals("test", execution.getInputs().get("prompt"));
        assertEquals("result.mp4", execution.getOutputs().get("url"));
        assertNull(execution.getErrorMessage());
        assertEquals(5000L, execution.getDurationMs());
        assertEquals("ben", execution.getCreatedBy());
    }
}
