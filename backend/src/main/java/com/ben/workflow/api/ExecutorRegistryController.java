package com.ben.workflow.api;

import com.ben.workflow.executor.extension.ConfigDrivenExecutorRegistry;
import com.ben.workflow.executor.extension.ExecutorMetadata;
import com.ben.workflow.executor.extension.ExecutorRegistration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Executor 管理 REST API
 * <p>
 * 提供 Executor 查询、详情和动态注册接口。
 */
@RestController
@RequestMapping("/api/v1/executors")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExecutorRegistryController {

    private final ConfigDrivenExecutorRegistry registry;

    public ExecutorRegistryController(ConfigDrivenExecutorRegistry registry) {
        this.registry = registry;
    }

    /**
     * GET /api/v1/executors
     * 获取所有已注册 Executor 列表
     *
     * @param category 可选，按分类过滤
     */
    @GetMapping
    public Mono<ResponseEntity<List<ExecutorMetadata>>> listExecutors(
            @RequestParam(value = "category", required = false) String category) {

        List<ExecutorMetadata> all = registry.getAllMetadata();

        if (category != null && !category.isBlank()) {
            all = all.stream()
                    .filter(m -> category.equalsIgnoreCase(m.getCategory()))
                    .toList();
        }

        return Mono.just(ResponseEntity.ok(all));
    }

    /**
     * GET /api/v1/executors/{type}
     * 获取指定 Executor 详情
     */
    @GetMapping("/{type}")
    public Mono<ResponseEntity<ExecutorMetadata>> getExecutor(@PathVariable String type) {
        ExecutorMetadata metadata = registry.getMetadata(type);
        if (metadata == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        return Mono.just(ResponseEntity.ok(metadata));
    }

    /**
     * POST /api/v1/executors
     * 动态注册新 Executor
     * <p>
     * 请求体示例:
     * <pre>
     * {
     *   "type": "slack",
     *   "className": "com.ben.workflow.executor.SlackExecutor",
     *   "enabled": true,
     *   "config": { "webhookUrl": "https://..." }
     * }
     * </pre>
     */
    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> registerExecutor(
            @RequestBody ExecutorRegistration registration) {

        // 校验
        if (registration.getType() == null || registration.getType().isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(
                    Map.of("error", "type is required")));
        }
        if (registration.getClassName() == null || registration.getClassName().isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(
                    Map.of("error", "className is required")));
        }

        // 检查是否已存在
        if (registry.hasExecutor(registration.getType())) {
            return Mono.just(ResponseEntity.badRequest().body(
                    Map.of("error", "Executor type already registered: " + registration.getType())));
        }

        try {
            registry.registerExecutor(registration);
            return Mono.just(ResponseEntity.ok(Map.of(
                    "type", registration.getType(),
                    "status", "registered"
            )));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to register: " + e.getMessage())));
        }
    }

    /**
     * DELETE /api/v1/executors/{type}
     * 注销 Executor
     */
    @DeleteMapping("/{type}")
    public Mono<ResponseEntity<Void>> unregisterExecutor(@PathVariable String type) {
        if (!registry.hasExecutor(type)) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        registry.unregister(type);
        return Mono.just(ResponseEntity.noContent().build());
    }
}
