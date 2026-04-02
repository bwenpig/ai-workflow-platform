package com.ben.workflow.api;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class LogStreamController {
    
    /**
     * SSE 日志流端点 - 简化版
     */
    @GetMapping(value = "/executions/{executionId}/logs/stream", 
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamLogs(@PathVariable String executionId) {
        return Flux.fromArray(new ServerSentEvent[] {
            ServerSentEvent.builder()
                .id("1").event("log")
                .data("{\"id\":\"log1\",\"level\":\"INFO\",\"message\":\"Log stream started\"}")
                .build(),
            ServerSentEvent.builder()
                .id("2").event("log")
                .data("{\"id\":\"log2\",\"level\":\"INFO\",\"message\":\"Processing\"}")
                .build(),
            ServerSentEvent.builder()
                .id("3").event("log")
                .data("{\"id\":\"log3\",\"level\":\"INFO\",\"message\":\"Completed\"}")
                .build()
        });
    }
    
    /**
     * 节点状态流 - 简化版
     */
    @GetMapping(value = "/executions/{executionId}/status/stream",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamStatus(@PathVariable String executionId) {
        return Flux.fromArray(new ServerSentEvent[] {
            ServerSentEvent.builder()
                .event("status")
                .data("{\"nodeId\":\"node_1\",\"state\":\"running\",\"progress\":0}")
                .build(),
            ServerSentEvent.builder()
                .event("status")
                .data("{\"nodeId\":\"node_1\",\"state\":\"running\",\"progress\":50}")
                .build(),
            ServerSentEvent.builder()
                .event("status")
                .data("{\"nodeId\":\"node_1\",\"state\":\"success\",\"progress\":100}")
                .build()
        });
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "ok", "timestamp", Instant.now().toString()));
    }
}