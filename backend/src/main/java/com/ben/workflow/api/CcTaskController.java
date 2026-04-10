package com.ben.workflow.api;

import com.ben.workflow.model.CcTask;
import com.ben.workflow.service.CcTaskTracker;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * CC 任务追踪 API
 * 插件式端点，不耦合工作流执行逻辑
 *
 * @author 龙傲天
 * @since 2026-04-10
 */
@RestController
@RequestMapping("/api/v1/cc-tasks")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CcTaskController {

    private final CcTaskTracker taskTracker;

    public CcTaskController(CcTaskTracker taskTracker) {
        this.taskTracker = taskTracker;
    }

    /**
     * 创建任务
     * POST /api/v1/cc-tasks
     */
    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> createTask(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "CC Task");
        String description = body.getOrDefault("description", "");
        String agentId = body.getOrDefault("agentId", "cc");

        CcTask task = taskTracker.createTask(title, description, agentId);

        return Mono.just(ResponseEntity.ok(Map.of(
                "taskId", task.getId(),
                "status", task.getStatus().name(),
                "title", task.getTitle()
        )));
    }

    /**
     * 更新任务
     * PATCH /api/v1/cc-tasks/{id}
     */
    @PatchMapping("/{id}")
    public Mono<ResponseEntity<CcTask>> updateTask(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            CcTask.Status status = body.containsKey("status")
                    ? CcTask.Status.valueOf((String) body.get("status"))
                    : null;
            Integer progress = body.containsKey("progress")
                    ? ((Number) body.get("progress")).intValue()
                    : null;
            String currentStep = body.containsKey("currentStep")
                    ? (String) body.get("currentStep")
                    : null;
            String logMessage = body.containsKey("log")
                    ? (String) body.get("log")
                    : null;
            String errorMessage = body.containsKey("errorMessage")
                    ? (String) body.get("errorMessage")
                    : null;

            CcTask task = taskTracker.updateTask(id, status, progress, currentStep, logMessage, errorMessage);
            return Mono.just(ResponseEntity.ok(task));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.notFound().build());
        }
    }

    /**
     * 获取所有任务
     * GET /api/v1/cc-tasks
     */
    @GetMapping
    public Mono<ResponseEntity<List<CcTask>>> listTasks() {
        return Mono.just(ResponseEntity.ok(taskTracker.listTasks()));
    }

    /**
     * 获取单个任务
     * GET /api/v1/cc-tasks/{id}
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<CcTask>> getTask(@PathVariable String id) {
        try {
            return Mono.just(ResponseEntity.ok(taskTracker.getTask(id)));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.notFound().build());
        }
    }

    /**
     * SSE 实时流
     * GET /api/v1/cc-tasks/{id}/stream
     */
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamTask(@PathVariable String id) {
        Flux<ServerSentEvent<String>> taskFlux = taskTracker.subscribeTask(id)
                .map(task -> ServerSentEvent.<String>builder()
                        .id(task.getId())
                        .event("task-update")
                        .data(taskToJson(task))
                        .build());

        // 心跳
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(10))
                .map(tick -> ServerSentEvent.<String>builder()
                        .id("heartbeat-" + tick)
                        .event("heartbeat")
                        .data("{\"type\":\"heartbeat\"}")
                        .build());

        return taskFlux.mergeWith(heartbeat).take(Duration.ofMinutes(30));
    }

    /**
     * 简单的 JSON 序列化（避免引入额外依赖）
     */
    private String taskToJson(CcTask task) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(task.getId())).append("\",");
        sb.append("\"title\":\"").append(escapeJson(task.getTitle())).append("\",");
        sb.append("\"description\":\"").append(escapeJson(task.getDescription())).append("\",");
        sb.append("\"agentId\":\"").append(escapeJson(task.getAgentId())).append("\",");
        sb.append("\"status\":\"").append(task.getStatus().name()).append("\",");
        sb.append("\"progress\":").append(task.getProgress()).append(",");
        sb.append("\"currentStep\":\"").append(escapeJson(task.getCurrentStep())).append("\",");
        sb.append("\"createdAt\":\"").append(task.getCreatedAt()).append("\",");
        sb.append("\"updatedAt\":\"").append(task.getUpdatedAt()).append("\",");
        if (task.getCompletedAt() != null) {
            sb.append("\"completedAt\":\"").append(task.getCompletedAt()).append("\",");
        }
        if (task.getErrorMessage() != null) {
            sb.append("\"errorMessage\":\"").append(escapeJson(task.getErrorMessage())).append("\",");
        }

        // logs array
        sb.append("\"logs\":[");
        if (task.getLogs() != null) {
            for (int i = 0; i < task.getLogs().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(task.getLogs().get(i))).append("\"");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
