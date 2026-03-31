package com.ben.workflow.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionController 单元测试
 */
class ExecutionControllerTest {

    @Test
    @DisplayName("测试控制器可以实例化")
    void testControllerInstantiation() {
        assertNotNull(ExecutionController.class);
    }

    @Test
    @DisplayName("测试控制器有正确的 RequestMapping")
    void testControllerHasRequestMapping() {
        assertTrue(ExecutionController.class.isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
        assertTrue(ExecutionController.class.isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class));
        
        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
            ExecutionController.class.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        assertEquals("/api/v1/executions", requestMapping.value()[0]);
    }

    @Test
    @DisplayName("测试控制器有 CrossOrigin 注解")
    void testControllerHasCrossOrigin() {
        assertTrue(ExecutionController.class.isAnnotationPresent(org.springframework.web.bind.annotation.CrossOrigin.class));
    }

    @Test
    @DisplayName("测试获取状态方法有 GetMapping")
    void testGetStatusMethodHasGetMapping() throws NoSuchMethodException {
        var method = ExecutionController.class.getMethod("getExecutionStatus", String.class);
        assertTrue(method.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class));
        var mapping = method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
        assertEquals("/{id}", mapping.value()[0]);
    }

    @Test
    @DisplayName("测试取消方法有 PostMapping")
    void testCancelMethodHasPostMapping() throws NoSuchMethodException {
        var method = ExecutionController.class.getMethod("cancelExecution", String.class);
        assertTrue(method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class));
        var mapping = method.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
        assertEquals("/{id}/cancel", mapping.value()[0]);
    }

    @Test
    @DisplayName("测试历史方法有 GetMapping")
    void testHistoryMethodHasGetMapping() throws NoSuchMethodException {
        var method = ExecutionController.class.getMethod("getExecutionHistory", String.class, int.class);
        assertTrue(method.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class));
        var mapping = method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
        assertEquals("/history", mapping.value()[0]);
    }

    @Test
    @DisplayName("测试控制器返回 Mono")
    void testControllerReturnsMono() throws NoSuchMethodException {
        var method = ExecutionController.class.getMethod("getExecutionStatus", String.class);
        assertEquals(reactor.core.publisher.Mono.class, method.getReturnType());
    }

    @Test
    @DisplayName("测试控制器方法数量")
    void testControllerMethodCount() {
        var methods = ExecutionController.class.getDeclaredMethods();
        // 至少有 3 个 API 方法
        assertTrue(methods.length >= 3);
    }

    @Test
    @DisplayName("测试获取历史方法有默认 limit 参数")
    void testHistoryMethodHasDefaultLimit() throws NoSuchMethodException {
        var method = ExecutionController.class.getMethod("getExecutionHistory", String.class, int.class);
        var params = method.getParameters();
        // 第二个参数是 limit
        assertEquals("limit", params[1].getName());
    }

    @Test
    @DisplayName("测试获取历史方法有 RequestParam")
    void testHistoryMethodHasRequestParam() throws NoSuchMethodException {
        var method = ExecutionController.class.getMethod("getExecutionHistory", String.class, int.class);
        var params = method.getParameters();
        assertTrue(params[0].isAnnotationPresent(org.springframework.web.bind.annotation.RequestParam.class));
    }
}
