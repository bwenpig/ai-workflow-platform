package com.ben.workflow.model;

import com.ben.workflow.adapter.GenerationRequest;
import com.ben.workflow.adapter.GenerationResult;
import com.ben.workflow.adapter.ModelProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Model 层边缘代码路径测试
 * 测试边界条件：null 输入、空集合、异常值
 */
public class ModelEdgeCasesTest {
    
    @Nested
    @DisplayName("Workflow 边缘条件测试")
    class WorkflowEdgeCases {
        
        @Test
        @DisplayName("Workflow 所有字段为 null")
        public void testAllFieldsNull() {
            Workflow workflow = new Workflow();
            
            // 所有 getter 应该返回 null 而不抛异常
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
        @DisplayName("Workflow 设置 null 列表")
        public void testSetNullLists() {
            Workflow workflow = new Workflow();
            
            workflow.setNodes(null);
            workflow.setEdges(null);
            
            assertNull(workflow.getNodes());
            assertNull(workflow.getEdges());
        }
        
        @Test
        @DisplayName("Workflow 设置空列表")
        public void testSetEmptyLists() {
            Workflow workflow = new Workflow();
            
            workflow.setNodes(new ArrayList<>());
            workflow.setEdges(new ArrayList<>());
            
            assertTrue(workflow.getNodes().isEmpty());
            assertTrue(workflow.getEdges().isEmpty());
        }
        
        @Test
        @DisplayName("Workflow 版本号边界值")
        public void testVersionBoundaryValues() {
            Workflow workflow = new Workflow();
            
            workflow.setVersion(0);
            assertEquals(0, workflow.getVersion());
            
            workflow.setVersion(Integer.MAX_VALUE);
            assertEquals(Integer.MAX_VALUE, workflow.getVersion());
            
            workflow.setVersion(-1);
            assertEquals(-1, workflow.getVersion());
        }
    }
    
    @Nested
    @DisplayName("WorkflowExecution 边缘条件测试")
    class WorkflowExecutionEdgeCases {
        
        @Test
        @DisplayName("WorkflowExecution 所有字段为 null")
        public void testAllFieldsNull() {
            WorkflowExecution execution = new WorkflowExecution();
            
            assertNull(execution.getId());
            assertNull(execution.getWorkflowId());
            assertNull(execution.getStatus());
            assertNull(execution.getInputs());
            assertNull(execution.getOutputs());
            assertNull(execution.getNodeStates());
            assertNull(execution.getErrorMessage());
            assertNull(execution.getDurationMs());
        }
        
        @Test
        @DisplayName("WorkflowExecution 设置 null Map")
        public void testSetNullMaps() {
            WorkflowExecution execution = new WorkflowExecution();
            
            execution.setInputs(null);
            execution.setOutputs(null);
            execution.setNodeStates(null);
            
            assertNull(execution.getInputs());
            assertNull(execution.getOutputs());
            assertNull(execution.getNodeStates());
        }
        
        @Test
        @DisplayName("WorkflowExecution 设置空 Map")
        public void testSetEmptyMaps() {
            WorkflowExecution execution = new WorkflowExecution();
            
            execution.setInputs(new HashMap<>());
            execution.setOutputs(new HashMap<>());
            
            assertTrue(execution.getInputs().isEmpty());
            assertTrue(execution.getOutputs().isEmpty());
        }
        
        @Test
        @DisplayName("WorkflowExecution 耗时边界值")
        public void testDurationBoundaryValues() {
            WorkflowExecution execution = new WorkflowExecution();
            
            execution.setDurationMs(0L);
            assertEquals(0L, execution.getDurationMs());
            
            execution.setDurationMs(Long.MAX_VALUE);
            assertEquals(Long.MAX_VALUE, execution.getDurationMs());
        }
        
        @Test
        @DisplayName("NodeExecutionState 所有字段为 null")
        public void testNodeExecutionStateAllNull() {
            WorkflowExecution.NodeExecutionState state = new WorkflowExecution.NodeExecutionState();
            
            assertNull(state.getNodeId());
            assertNull(state.getStatus());
            assertNull(state.getResult());
            assertNull(state.getErrorMessage());
            assertNull(state.getDurationMs());
        }
    }
    
    @Nested
    @DisplayName("WorkflowNode 边缘条件测试")
    class WorkflowNodeEdgeCases {
        
        @Test
        @DisplayName("WorkflowNode 所有字段为 null")
        public void testAllFieldsNull() {
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
        @DisplayName("WorkflowNode 设置 null 列表")
        public void testSetNullLists() {
            WorkflowNode node = new WorkflowNode();
            
            node.setInputs(null);
            node.setOutputs(null);
            
            assertNull(node.getInputs());
            assertNull(node.getOutputs());
        }
        
        @Test
        @DisplayName("WorkflowNode 设置空列表")
        public void testSetEmptyLists() {
            WorkflowNode node = new WorkflowNode();
            
            node.setInputs(new ArrayList<>());
            node.setOutputs(new ArrayList<>());
            
            assertTrue(node.getInputs().isEmpty());
            assertTrue(node.getOutputs().isEmpty());
        }
        
        @Test
        @DisplayName("Position 坐标边界值")
        public void testPositionBoundaryValues() {
            WorkflowNode.Position position = new WorkflowNode.Position();
            
            position.setX(0.0);
            assertEquals(0.0, position.getX());
            
            position.setX(Double.MAX_VALUE);
            assertEquals(Double.MAX_VALUE, position.getX());
            
            position.setX(Double.NEGATIVE_INFINITY);
            assertEquals(Double.NEGATIVE_INFINITY, position.getX());
        }
        
        @Test
        @DisplayName("InputPort 所有字段为 null")
        public void testInputPortAllNull() {
            WorkflowNode.InputPort input = new WorkflowNode.InputPort();
            
            assertNull(input.getId());
            assertNull(input.getLabel());
            assertNull(input.getType());
            assertNull(input.getDefaultValue());
        }
        
        @Test
        @DisplayName("OutputPort 所有字段为 null")
        public void testOutputPortAllNull() {
            WorkflowNode.OutputPort output = new WorkflowNode.OutputPort();
            
            assertNull(output.getId());
            assertNull(output.getLabel());
            assertNull(output.getType());
        }
        
        @Test
        @DisplayName("ExecutionResult 所有字段为 null")
        public void testExecutionResultAllNull() {
            WorkflowNode.ExecutionResult result = new WorkflowNode.ExecutionResult();
            
            assertNull(result.getOutputUrl());
            assertNull(result.getMetadata());
            assertNull(result.getErrorMessage());
        }
    }
    
    @Nested
    @DisplayName("WorkflowEdge 边缘条件测试")
    class WorkflowEdgeEdgeCases {
        
        @Test
        @DisplayName("WorkflowEdge 所有字段为 null")
        public void testAllFieldsNull() {
            WorkflowEdge edge = new WorkflowEdge();
            
            assertNull(edge.getId());
            assertNull(edge.getSource());
            assertNull(edge.getTarget());
            assertNull(edge.getSourceHandle());
            assertNull(edge.getTargetHandle());
            assertNull(edge.getDataType());
        }
        
        @Test
        @DisplayName("WorkflowEdge 设置空字符串")
        public void testSetEmptyStrings() {
            WorkflowEdge edge = new WorkflowEdge();
            
            edge.setId("");
            edge.setSource("");
            edge.setTarget("");
            edge.setSourceHandle("");
            edge.setTargetHandle("");
            edge.setDataType("");
            
            assertEquals("", edge.getId());
            assertEquals("", edge.getSource());
            assertEquals("", edge.getTarget());
            assertEquals("", edge.getSourceHandle());
            assertEquals("", edge.getTargetHandle());
            assertEquals("", edge.getDataType());
        }
    }
    
    @Nested
    @DisplayName("WorkflowData 边缘条件测试")
    class WorkflowDataEdgeCases {
        
        @Test
        @DisplayName("WorkflowData 所有字段为 null")
        public void testAllFieldsNull() {
            WorkflowData data = new WorkflowData();
            
            assertNull(data.getType());
            assertNull(data.getContent());
            assertNull(data.getMetadata());
            assertNull(data.getSourceNode());
        }
        
        @Test
        @DisplayName("WorkflowData 设置 null 内容")
        public void testSetNullContent() {
            WorkflowData data = new WorkflowData();
            
            data.setContent(null);
            
            assertNull(data.getContent());
        }
        
        @Test
        @DisplayName("WorkflowData 设置 null 元数据")
        public void testSetNullMetadata() {
            WorkflowData data = new WorkflowData();
            
            data.setMetadata(null);
            
            assertNull(data.getMetadata());
        }
        
        @Test
        @DisplayName("WorkflowData fromCode null 输入")
        public void testFromCodeNullInput() {
            assertEquals(WorkflowData.DataType.TEXT, WorkflowData.DataType.fromCode(null));
        }
        
        @Test
        @DisplayName("WorkflowData fromCode 空字符串")
        public void testFromCodeEmptyString() {
            assertEquals(WorkflowData.DataType.TEXT, WorkflowData.DataType.fromCode(""));
        }
        
        @Test
        @DisplayName("WorkflowData fromCode 未知类型")
        public void testFromCodeUnknownType() {
            assertEquals(WorkflowData.DataType.TEXT, WorkflowData.DataType.fromCode("UNKNOWN_TYPE"));
        }
    }
    
    @Nested
    @DisplayName("PythonNodeConfig 边缘条件测试")
    class PythonNodeConfigEdgeCases {
        
        @Test
        @DisplayName("PythonNodeConfig 所有字段为 null")
        public void testAllFieldsNull() {
            PythonNodeConfig config = new PythonNodeConfig();
            
            assertNull(config.getScript());
            assertNull(config.getScriptPath());
            assertNull(config.getRequirements());
            assertNull(config.getEnv());
            
            // 默认值
            assertEquals(30, config.getTimeout());
            assertEquals("3.9", config.getPythonVersion());
            assertFalse(config.getNetworkEnabled());
        }
        
        @Test
        @DisplayName("PythonNodeConfig 设置 null 列表")
        public void testSetNullLists() {
            PythonNodeConfig config = new PythonNodeConfig();
            
            config.setRequirements(null);
            
            assertNull(config.getRequirements());
        }
        
        @Test
        @DisplayName("PythonNodeConfig 设置空列表")
        public void testSetEmptyLists() {
            PythonNodeConfig config = new PythonNodeConfig();
            
            config.setRequirements(new ArrayList<>());
            
            assertTrue(config.getRequirements().isEmpty());
        }
        
        @Test
        @DisplayName("PythonNodeConfig 超时边界值")
        public void testTimeoutBoundaryValues() {
            PythonNodeConfig config = new PythonNodeConfig();
            
            config.setTimeout(0);
            assertEquals(0, config.getTimeout());
            
            config.setTimeout(Integer.MAX_VALUE);
            assertEquals(Integer.MAX_VALUE, config.getTimeout());
            
            config.setTimeout(-1);
            assertEquals(-1, config.getTimeout());
        }
    }
    
    @Nested
    @DisplayName("GenerationRequest 边缘条件测试")
    class GenerationRequestEdgeCases {
        
        @Test
        @DisplayName("GenerationRequest 所有字段为 null")
        public void testAllFieldsNull() {
            GenerationRequest request = new GenerationRequest();
            
            assertNull(request.getPrompt());
            assertNull(request.getNegativePrompt());
            assertNull(request.getInputImages());
            assertNull(request.getInputVideos());
            assertNull(request.getParams());
            assertNull(request.getCallbackUrl());
            assertNull(request.getPriority());
        }
        
        @Test
        @DisplayName("GenerationRequest 设置 null 列表")
        public void testSetNullLists() {
            GenerationRequest request = new GenerationRequest();
            
            request.setInputImages(null);
            request.setInputVideos(null);
            
            assertNull(request.getInputImages());
            assertNull(request.getInputVideos());
        }
        
        @Test
        @DisplayName("GenerationRequest 设置空列表")
        public void testSetEmptyLists() {
            GenerationRequest request = new GenerationRequest();
            
            request.setInputImages(new ArrayList<>());
            request.setInputVideos(new ArrayList<>());
            
            assertTrue(request.getInputImages().isEmpty());
            assertTrue(request.getInputVideos().isEmpty());
        }
        
        @Test
        @DisplayName("GenerationRequest 优先级边界值")
        public void testPriorityBoundaryValues() {
            GenerationRequest request = new GenerationRequest();
            
            request.setPriority(0);
            assertEquals(0, request.getPriority());
            
            request.setPriority(10);
            assertEquals(10, request.getPriority());
            
            request.setPriority(100);
            assertEquals(100, request.getPriority());
        }
    }
    
    @Nested
    @DisplayName("GenerationResult 边缘条件测试")
    class GenerationResultEdgeCases {
        
        @Test
        @DisplayName("GenerationResult 所有字段为 null")
        public void testAllFieldsNull() {
            GenerationResult result = new GenerationResult();
            
            assertNull(result.getTaskId());
            assertNull(result.getOutputUrls());
            assertNull(result.getMetadata());
            assertNull(result.getStatus());
            assertNull(result.getErrorMessage());
            assertNull(result.getDurationMs());
            assertNull(result.getPreviewUrl());
            assertNull(result.getDuration());
            assertNull(result.getFps());
        }
        
        @Test
        @DisplayName("GenerationResult 设置 null 列表")
        public void testSetNullLists() {
            GenerationResult result = new GenerationResult();
            
            result.setOutputUrls(null);
            
            assertNull(result.getOutputUrls());
        }
        
        @Test
        @DisplayName("GenerationResult 设置空列表")
        public void testSetEmptyLists() {
            GenerationResult result = new GenerationResult();
            
            result.setOutputUrls(new ArrayList<>());
            
            assertTrue(result.getOutputUrls().isEmpty());
        }
        
        @Test
        @DisplayName("GenerationResult 耗时边界值")
        public void testDurationBoundaryValues() {
            GenerationResult result = new GenerationResult();
            
            result.setDurationMs(0L);
            assertEquals(0L, result.getDurationMs());
            
            result.setDurationMs(Long.MAX_VALUE);
            assertEquals(Long.MAX_VALUE, result.getDurationMs());
        }
    }
}
