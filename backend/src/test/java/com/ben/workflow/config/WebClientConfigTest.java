package com.ben.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebClientConfig 单元测试
 */
class WebClientConfigTest {

    private WebClientConfig webClientConfig;

    @BeforeEach
    void setUp() {
        webClientConfig = new WebClientConfig();
    }

    @Test
    @DisplayName("测试 WebClient.Builder Bean 创建")
    void testWebClientBuilderBean() {
        WebClient.Builder builder = webClientConfig.webClientBuilder();

        assertNotNull(builder);
        assertNotNull(builder.build());
    }

    @Test
    @DisplayName("测试 WebClient 可以创建实例")
    void testWebClientCanBeCreated() {
        WebClient.Builder builder = webClientConfig.webClientBuilder();
        WebClient client = builder.build();

        assertNotNull(client);
    }

    @Test
    @DisplayName("测试多次创建返回不同实例")
    void testMultipleCreationsReturnDifferentInstances() {
        WebClient.Builder builder1 = webClientConfig.webClientBuilder();
        WebClient.Builder builder2 = webClientConfig.webClientBuilder();

        assertNotNull(builder1);
        assertNotNull(builder2);
        assertNotSame(builder1, builder2);
    }

    @Test
    @DisplayName("测试 WebClient 可以配置基础 URL")
    void testWebClientCanBeConfigured() {
        WebClient.Builder builder = webClientConfig.webClientBuilder();
        WebClient client = builder.baseUrl("https://api.example.com").build();

        assertNotNull(client);
    }

    @Test
    @DisplayName("测试配置类实例化")
    void testConfigClassInstantiation() {
        WebClientConfig config = new WebClientConfig();
        assertNotNull(config);
    }
}
