package com.ben.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 单元测试
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    // ==================== RuntimeException 处理测试 ====================

    @Test
    @DisplayName("测试处理 RuntimeException")
    void testHandleRuntimeException() {
        RuntimeException exception = new RuntimeException("测试运行时异常");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertNotNull(response.getBody());
                    assertEquals("Bad Request", response.getBody().get("error"));
                    assertEquals("测试运行时异常", response.getBody().get("message"));
                    assertEquals(400, response.getBody().get("status"));
                    assertNotNull(response.getBody().get("timestamp"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试 RuntimeException 响应包含时间戳")
    void testRuntimeExceptionResponseContainsTimestamp() {
        RuntimeException exception = new RuntimeException("时间戳测试");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response.getBody());
                    String timestamp = (String) response.getBody().get("timestamp");
                    assertNotNull(timestamp);
                    assertTrue(timestamp.length() > 0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试处理空消息 RuntimeException")
    void testHandleRuntimeExceptionWithNullMessage() {
        RuntimeException exception = new RuntimeException();

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertNotNull(response.getBody());
                    assertEquals("Bad Request", response.getBody().get("error"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试处理空消息异常")
    void testHandleExceptionWithNullMessage() {
        Exception exception = new Exception();

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(500, response.getStatusCodeValue());
                    assertNotNull(response.getBody());
                    assertEquals("Internal Server Error", response.getBody().get("error"));
                })
                .verifyComplete();
    }

    // ==================== 普通 Exception 处理测试 ====================

    @Test
    @DisplayName("测试处理普通 Exception")
    void testHandleException() {
        Exception exception = new Exception("测试普通异常");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(500, response.getStatusCodeValue());
                    assertNotNull(response.getBody());
                    assertEquals("Internal Server Error", response.getBody().get("error"));
                    assertEquals("测试普通异常", response.getBody().get("message"));
                    assertEquals(500, response.getBody().get("status"));
                    assertNotNull(response.getBody().get("timestamp"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试 Exception 响应包含时间戳")
    void testExceptionResponseContainsTimestamp() {
        Exception exception = new Exception("时间戳测试");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response.getBody());
                    String timestamp = (String) response.getBody().get("timestamp");
                    assertNotNull(timestamp);
                    assertTrue(timestamp.length() > 0);
                })
                .verifyComplete();
    }

    // ==================== 嵌套异常测试 ====================

    @Test
    @DisplayName("测试处理带嵌套消息的异常")
    void testHandleExceptionWithNestedMessage() {
        Exception cause = new Exception("根本原因");
        Exception exception = new Exception("外层异常", cause);

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(500, response.getStatusCodeValue());
                    assertEquals("外层异常", response.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试处理带原因的 RuntimeException")
    void testHandleRuntimeExceptionWithCause() {
        Throwable cause = new Throwable("根本原因");
        RuntimeException exception = new RuntimeException("外层运行时异常", cause);

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertEquals("外层运行时异常", response.getBody().get("message"));
                })
                .verifyComplete();
    }

    // ==================== 非法参数异常测试 ====================

    @Test
    @DisplayName("测试处理非法参数异常")
    void testHandleIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("参数无效");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertEquals("参数无效", response.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试处理空参数非法异常")
    void testHandleIllegalArgumentExceptionEmpty() {
        IllegalArgumentException exception = new IllegalArgumentException();

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertNotNull(response.getBody());
                })
                .verifyComplete();
    }

    // ==================== 空指针异常测试 ====================

    @Test
    @DisplayName("测试处理空指针异常")
    void testHandleNullPointerException() {
        NullPointerException exception = new NullPointerException("空指针");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertEquals("空指针", response.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试处理空消息空指针异常")
    void testHandleNullPointerExceptionEmpty() {
        NullPointerException exception = new NullPointerException();

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertNotNull(response.getBody());
                })
                .verifyComplete();
    }

    // ==================== 状态异常测试 ====================

    @Test
    @DisplayName("测试处理状态异常")
    void testHandleIllegalStateException() {
        IllegalStateException exception = new IllegalStateException("状态错误");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertEquals("状态错误", response.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试处理空消息状态异常")
    void testHandleIllegalStateExceptionEmpty() {
        IllegalStateException exception = new IllegalStateException();

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertNotNull(response.getBody());
                })
                .verifyComplete();
    }

    // ==================== 自定义异常模拟测试 ====================

    @Test
    @DisplayName("测试模拟 WorkflowNotFoundException")
    void testSimulateWorkflowNotFoundException() {
        // 模拟工作流未找到异常
        RuntimeException workflowNotFound = new RuntimeException("工作流不存在：wf-123");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(workflowNotFound);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertEquals("工作流不存在：wf-123", response.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试模拟 InvalidWorkflowException")
    void testSimulateInvalidWorkflowException() {
        // 模拟无效工作流异常
        IllegalArgumentException invalidWorkflow = new IllegalArgumentException("工作流配置无效：缺少必要节点");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(invalidWorkflow);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertEquals("工作流配置无效：缺少必要节点", response.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试模拟 ExecutionAlreadyRunningException")
    void testSimulateExecutionAlreadyRunningException() {
        // 模拟执行已在运行异常
        IllegalStateException executionRunning = new IllegalStateException("执行实例已在运行中");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(executionRunning);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertEquals("执行实例已在运行中", response.getBody().get("message"));
                })
                .verifyComplete();
    }

    // ==================== 其他自定义异常测试 ====================

    @Test
    @DisplayName("测试模拟资源不存在异常")
    void testSimulateResourceNotFoundException() {
        RuntimeException resourceNotFound = new RuntimeException("资源不存在");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(resourceNotFound);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertEquals("资源不存在", response.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试模拟权限拒绝异常")
    void testSimulateAccessDeniedException() {
        RuntimeException accessDenied = new RuntimeException("访问被拒绝");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(accessDenied);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCodeValue());
                    assertEquals("访问被拒绝", response.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试模拟服务不可用异常")
    void testSimulateServiceUnavailableException() {
        Exception serviceUnavailable = new Exception("服务暂时不可用");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleException(serviceUnavailable);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(500, response.getStatusCodeValue());
                    assertEquals("服务暂时不可用", response.getBody().get("message"));
                    assertEquals("Internal Server Error", response.getBody().get("error"));
                })
                .verifyComplete();
    }

    // ==================== 边界和特殊场景测试 ====================

    @Test
    @DisplayName("测试处理超长消息异常")
    void testHandleExceptionWithLongMessage() {
        String longMessage = "异常消息".repeat(100);
        Exception exception = new Exception(longMessage);

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(500, response.getStatusCodeValue());
                    assertEquals(longMessage, response.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试处理特殊字符消息异常")
    void testHandleExceptionWithSpecialCharacters() {
        String specialMessage = "异常消息：\n换行\t制表符\"引号'单引号<>&";
        Exception exception = new Exception(specialMessage);

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(500, response.getStatusCodeValue());
                    assertEquals(specialMessage, response.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试处理中文消息异常")
    void testHandleExceptionWithChineseMessage() {
        Exception exception = new Exception("中文异常消息测试");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(500, response.getStatusCodeValue());
                    assertEquals("中文异常消息测试", response.getBody().get("message"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试响应体结构完整性")
    void testResponseBodyStructure() {
        Exception exception = new Exception("测试");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    Map<String, Object> body = response.getBody();
                    assertNotNull(body);
                    assertTrue(body.containsKey("timestamp"));
                    assertTrue(body.containsKey("status"));
                    assertTrue(body.containsKey("error"));
                    assertTrue(body.containsKey("message"));
                    assertEquals(4, body.size());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试 RuntimeException 响应体结构完整性")
    void testRuntimeExceptionResponseBodyStructure() {
        RuntimeException exception = new RuntimeException("测试");

        Mono<ResponseEntity<Map<String, Object>>> result = exceptionHandler.handleRuntimeException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    Map<String, Object> body = response.getBody();
                    assertNotNull(body);
                    assertTrue(body.containsKey("timestamp"));
                    assertTrue(body.containsKey("status"));
                    assertTrue(body.containsKey("error"));
                    assertTrue(body.containsKey("message"));
                    assertEquals(400, body.get("status"));
                    assertEquals("Bad Request", body.get("error"));
                })
                .verifyComplete();
    }
}
