package com.ben.workflow.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 执行日志服务 - 收集和推送执行日志
 */
@Service
public class ExecutionLogService {

    // 存储每个执行的日志
    private final Map<String, List<LogEntry>> executionLogs = new ConcurrentHashMap<>();
    
    // 存储每个执行的日志发布者
    private final Map<String, FluxSink<LogEntry>> logSinks = new ConcurrentHashMap<>();

    public static class LogEntry {
        public String id;
        public String timestamp;
        public String level;
        public String message;
        public String executionId;
        public String nodeId;

        public LogEntry() {}

        public LogEntry(String executionId, String nodeId, String level, String message) {
            this.id = executionId + "-" + System.currentTimeMillis();
            this.timestamp = Instant.now().toString();
            this.level = level;
            this.message = message;
            this.executionId = executionId;
            this.nodeId = nodeId;
        }

        public String toJson() {
            return String.format("{\"id\":\"%s\",\"timestamp\":\"%s\",\"level\":\"%s\",\"message\":\"%s\",\"nodeId\":\"%s\"}",
                id, timestamp, level, message != null ? message.replace("\"", "'") : "", nodeId != null ? nodeId : "system");
        }
    }

    /**
     * 添加日志
     */
    public void addLog(String executionId, String nodeId, String level, String message) {
        LogEntry entry = new LogEntry(executionId, nodeId, level, message);
        
        // 存储到列表
        executionLogs.computeIfAbsent(executionId, k -> new ArrayList<>()).add(entry);
        
        // 推送给订阅者
        FluxSink<LogEntry> sink = logSinks.get(executionId);
        if (sink != null && !sink.isCancelled()) {
            sink.next(entry);
        }
    }

    /**
     * 订阅日志流
     */
    public Flux<LogEntry> subscribeLogs(String executionId) {
        return Flux.create(sink -> {
            logSinks.put(executionId, sink);
            
            // 发送已有日志
            List<LogEntry> logs = executionLogs.get(executionId);
            if (logs != null) {
                logs.forEach(sink::next);
            }
            
            sink.onCancel(() -> logSinks.remove(executionId));
        });
    }

    /**
     * 清除执行日志
     */
    public void clearLogs(String executionId) {
        executionLogs.remove(executionId);
        FluxSink<LogEntry> sink = logSinks.remove(executionId);
        if (sink != null) {
            sink.complete();
        }
    }

    /**
     * 获取所有日志
     */
    public List<LogEntry> getLogs(String executionId) {
        return executionLogs.getOrDefault(executionId, new ArrayList<>());
    }
}