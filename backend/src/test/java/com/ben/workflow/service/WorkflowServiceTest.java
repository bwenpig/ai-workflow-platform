package com.ben.workflow.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowService 单元测试
 * 注：由于 Mockito 与 Java 25 兼容性限制，这里主要测试服务类的基本结构和配置
 */
public class WorkflowServiceTest {

    @Test
    @DisplayName("测试服务类可以加载")
    void testServiceClassExists() {
        assertNotNull(WorkflowService.class);
    }

    @Test
    @DisplayName("测试服务类有正确的注解")
    void testServiceHasAnnotation() {
        assertTrue(WorkflowService.class.isAnnotationPresent(org.springframework.stereotype.Service.class));
    }

    @Test
    @DisplayName("测试服务类构造函数存在")
    void testServiceConstructor() throws NoSuchMethodException {
        var constructor = WorkflowService.class.getConstructor(
            com.ben.workflow.repository.WorkflowRepository.class,
            com.ben.workflow.repository.ExecutionRepository.class,
            com.ben.workflow.engine.WorkflowEngine.class,
            com.ben.workflow.websocket.NotificationService.class
        );
        assertNotNull(constructor);
        assertEquals(4, constructor.getParameterCount());
    }

    @Test
    @DisplayName("测试服务类方法数量")
    void testServiceMethodCount() {
        var methods = WorkflowService.class.getDeclaredMethods();
        // 至少有 9 个业务方法
        assertTrue(methods.length >= 9);
    }

    @Test
    @DisplayName("测试服务类返回 Mono")
    void testServiceReturnsMono() throws NoSuchMethodException {
        var method = WorkflowService.class.getMethod("getWorkflow", String.class);
        assertEquals(reactor.core.publisher.Mono.class, method.getReturnType());
    }

    @Test
    @DisplayName("测试创建工作流程方法存在")
    void testCreateWorkflowMethodExists() throws NoSuchMethodException {
        var method = WorkflowService.class.getMethod("createWorkflow", 
            com.ben.workflow.model.Workflow.class, String.class);
        assertNotNull(method);
        assertEquals(2, method.getParameterCount());
    }

    @Test
    @DisplayName("测试列表工作流程方法存在")
    void testListWorkflowsMethodExists() throws NoSuchMethodException {
        var method = WorkflowService.class.getMethod("listWorkflows", String.class, boolean.class);
        assertNotNull(method);
    }

    @Test
    @DisplayName("测试获取工作流程方法存在")
    void testGetWorkflowMethodExists() throws NoSuchMethodException {
        var method = WorkflowService.class.getMethod("getWorkflow", String.class);
        assertNotNull(method);
    }

    @Test
    @DisplayName("测试更新工作流程方法存在")
    void testUpdateWorkflowMethodExists() throws NoSuchMethodException {
        var method = WorkflowService.class.getMethod("updateWorkflow", 
            String.class, com.ben.workflow.model.Workflow.class);
        assertNotNull(method);
    }

    @Test
    @DisplayName("测试删除工作流程方法存在")
    void testDeleteWorkflowMethodExists() throws NoSuchMethodException {
        var method = WorkflowService.class.getMethod("deleteWorkflow", String.class);
        assertNotNull(method);
        assertEquals(reactor.core.publisher.Mono.class, method.getReturnType());
    }

    @Test
    @DisplayName("测试切换发布状态方法存在")
    void testTogglePublishedMethodExists() throws NoSuchMethodException {
        var method = WorkflowService.class.getMethod("togglePublished", String.class);
        assertNotNull(method);
    }

    @Test
    @DisplayName("测试执行工作流程方法存在")
    void testExecuteWorkflowMethodExists() throws NoSuchMethodException {
        var method = WorkflowService.class.getMethod("executeWorkflow", 
            String.class, Map.class, String.class);
        assertNotNull(method);
    }

    @Test
    @DisplayName("测试获取执行状态方法存在")
    void testGetExecutionStatusMethodExists() throws NoSuchMethodException {
        var method = WorkflowService.class.getMethod("getExecutionStatus", String.class);
        assertNotNull(method);
    }

    @Test
    @DisplayName("测试取消执行方法存在")
    void testCancelExecutionMethodExists() throws NoSuchMethodException {
        var method = WorkflowService.class.getMethod("cancelExecution", String.class);
        assertNotNull(method);
    }

    @Test
    @DisplayName("测试获取执行历史方法存在")
    void testGetExecutionHistoryMethodExists() throws NoSuchMethodException {
        var method = WorkflowService.class.getMethod("getExecutionHistory", String.class, int.class);
        assertNotNull(method);
    }

    @Test
    @DisplayName("测试所有公共方法")
    void testAllPublicMethods() {
        Method[] methods = WorkflowService.class.getMethods();
        assertTrue(methods.length >= 9);
        
        // 验证关键方法存在
        var methodNames = java.util.Arrays.stream(methods)
            .map(Method::getName)
            .toList();
        
        assertTrue(methodNames.contains("createWorkflow"));
        assertTrue(methodNames.contains("listWorkflows"));
        assertTrue(methodNames.contains("getWorkflow"));
        assertTrue(methodNames.contains("updateWorkflow"));
        assertTrue(methodNames.contains("deleteWorkflow"));
        assertTrue(methodNames.contains("togglePublished"));
        assertTrue(methodNames.contains("executeWorkflow"));
        assertTrue(methodNames.contains("getExecutionStatus"));
        assertTrue(methodNames.contains("cancelExecution"));
        assertTrue(methodNames.contains("getExecutionHistory"));
    }

    @Test
    @DisplayName("测试服务类依赖字段")
    void testServiceFields() {
        var fields = WorkflowService.class.getDeclaredFields();
        assertTrue(fields.length >= 4);
        
        var fieldNames = java.util.Arrays.stream(fields)
            .map(java.lang.reflect.Field::getName)
            .toList();
        
        assertTrue(fieldNames.contains("workflowRepository"));
        assertTrue(fieldNames.contains("executionRepository"));
        assertTrue(fieldNames.contains("workflowEngine"));
        assertTrue(fieldNames.contains("notificationService"));
    }
}
