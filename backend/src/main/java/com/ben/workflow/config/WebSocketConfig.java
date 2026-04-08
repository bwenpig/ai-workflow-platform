package com.ben.workflow.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

/**
 * WebSocket 配置 (WebFlux 版本)
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public HandlerMapping webSocketHandlerMapping(RealtimeHandler realtimeHandler) {
        Map<String, WebSocketHandler> handlerMap = new HashMap<>();
        handlerMap.put("/api/v1/ws/realtime", realtimeHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(handlerMap);
        mapping.setOrder(-1); // 优先级高于其他 HandlerMapping
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    public RealtimeHandler realtimeHandler() {
        return new RealtimeHandler();
    }
}
