package com.ben.workflow.scheduler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 定时调度 REST API
 */
@RestController
@RequestMapping("/api/v1/scheduler")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    /**
     * 创建定时任务
     * POST /api/v1/scheduler/jobs
     * Body: { workflowId, cronExpression, description? }
     */
    @PostMapping("/jobs")
    public Mono<ResponseEntity<SchedulerJob>> createJob(
            @RequestBody CreateJobRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String createdBy = userId != null ? userId : "anonymous";
        return schedulerService.createJob(
                request.workflowId(),
                request.cronExpression(),
                request.description(),
                createdBy
        ).map(ResponseEntity::ok)
         .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    /**
     * 查询所有定时任务
     * GET /api/v1/scheduler/jobs
     */
    @GetMapping("/jobs")
    public Mono<ResponseEntity<List<SchedulerJob>>> listJobs() {
        return schedulerService.listJobs().map(ResponseEntity::ok);
    }

    /**
     * 查询单个定时任务
     * GET /api/v1/scheduler/jobs/{id}
     */
    @GetMapping("/jobs/{id}")
    public Mono<ResponseEntity<SchedulerJob>> getJob(@PathVariable String id) {
        return schedulerService.getJob(id)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    /**
     * 暂停定时任务
     * POST /api/v1/scheduler/jobs/{id}/pause
     */
    @PostMapping("/jobs/{id}/pause")
    public Mono<ResponseEntity<SchedulerJob>> pauseJob(@PathVariable String id) {
        return schedulerService.pauseJob(id)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    /**
     * 恢复定时任务
     * POST /api/v1/scheduler/jobs/{id}/resume
     */
    @PostMapping("/jobs/{id}/resume")
    public Mono<ResponseEntity<SchedulerJob>> resumeJob(@PathVariable String id) {
        return schedulerService.resumeJob(id)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    /**
     * 删除定时任务
     * DELETE /api/v1/scheduler/jobs/{id}
     */
    @DeleteMapping("/jobs/{id}")
    public Mono<ResponseEntity<Void>> deleteJob(@PathVariable String id) {
        return schedulerService.deleteJob(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().<Void>build()));
    }

    /**
     * 更新 cron 表达式
     * PUT /api/v1/scheduler/jobs/{id}/cron
     * Body: { cronExpression }
     */
    @PutMapping("/jobs/{id}/cron")
    public Mono<ResponseEntity<SchedulerJob>> updateCron(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String cronExpression = body.get("cronExpression");
        if (cronExpression == null || cronExpression.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return schedulerService.updateCron(id, cronExpression)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    // ==================== Request DTOs ====================

    public record CreateJobRequest(
            String workflowId,
            String cronExpression,
            String description
    ) {}
}
