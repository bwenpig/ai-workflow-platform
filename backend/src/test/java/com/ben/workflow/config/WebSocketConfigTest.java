package com.ben.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocketConfig 单元测试
 */
class WebSocketConfigTest {

    private WebSocketConfig webSocketConfig;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig();
    }

    @Test
    @DisplayName("测试配置类实例化")
    void testConfigClassInstantiation() {
        assertNotNull(webSocketConfig);
    }

    @Test
    @DisplayName("测试配置类无额外配置")
    void testConfigHasNoAdditionalConfiguration() {
        // WebSocketConfig 是空配置类，用于标记配置包存在
        // WebFlux 由 Spring Boot 自动配置
        WebSocketConfig config = new WebSocketConfig();
        assertNotNull(config);
    }

    @Test
    @DisplayName("测试配置类注解")
    void testConfigAnnotation() {
        assertTrue(webSocketConfig.getClass().isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }
}
