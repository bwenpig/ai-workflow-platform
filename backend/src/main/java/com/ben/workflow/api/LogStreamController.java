package com.ben.workflow.api;

import com.ben.workflow.service.ExecutionLogService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class LogStreamController {
    
    private final ExecutionLogService logService;
    
    public LogStreamController(ExecutionLogService logService) {
        this.logService = logService;
    }
    
    /**
     * SSE 日志流端点 - 保持连接并发送日志
     */
    @GetMapping(value = "/executions/{executionId}/logs/stream", 
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamLogs(@PathVariable String executionId) {
        // 合并日志流和心跳保持连接活跃
        Flux<ServerSentEvent<String>> logFlux = logService.subscribeLogs(executionId)
            .map(logEntry -> ServerSentEvent.<String>builder()
                .id(logEntry.id)
                .event("log")
                .data(logEntry.toJson())
                .build());
        
        // 每5秒发送一个心跳保持连接
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(5))
            .take(60) // 最多5分钟
            .map(tick -> ServerSentEvent.<String>builder()
                .id("heartbeat-" + tick)
                .event("heartbeat")
                .data("{\"type\":\"heartbeat\",\"tick\":" + tick + "}")
                .build());
        
        return logFlux.mergeWith(heartbeat);
    }
    
    /**
     * 节点状态流
     */
    @GetMapping(value = "/executions/{executionId}/status/stream",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamStatus(@PathVariable String executionId) {
        return Flux.interval(Duration.ofSeconds(2))
            .map(tick -> ServerSentEvent.<String>builder()
                .event("status")
                .data("{\"executionId\":\"" + executionId + "\",\"tick\":" + tick + "}")
                .build())
            .take(30);
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "ok", "timestamp", Instant.now().toString()));
    }
}