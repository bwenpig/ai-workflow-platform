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
}
