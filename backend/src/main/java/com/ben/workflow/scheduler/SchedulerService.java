package com.ben.workflow.scheduler;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.model.WorkflowExecution;
import com.ben.workflow.repository.WorkflowRepository;
import com.ben.workflow.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时调度服务
 * 使用 Spring TaskScheduler 动态注册/注销 cron 任务
 */
@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final SchedulerJobRepository jobRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowService workflowService;
    private final TaskScheduler taskScheduler;

    /** 内存中维护的活跃任务句柄 */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    public SchedulerService(SchedulerJobRepository jobRepository,
                            WorkflowRepository workflowRepository,
                            WorkflowService workflowService,
                            TaskScheduler taskScheduler) {
        this.jobRepository = jobRepository;
        this.workflowRepository = workflowRepository;
        this.workflowService = workflowService;
        this.taskScheduler = taskScheduler;
    }

    /**
     * 应用启动时恢复所有 RUNNING 状态的任务
     */
    @PostConstruct
    public void recoverRunningJobs() {
        List<SchedulerJob> runningJobs = jobRepository.findByStatus("RUNNING");
        log.info("恢复 {} 个运行中的定时任务", runningJobs.size());
        for (SchedulerJob job : runningJobs) {
            try {
                scheduleTask(job);
                log.info("已恢复定时任务: jobId={}, workflowId={}, cron={}",
                        job.getId(), job.getWorkflowId(), job.getCronExpression());
            } catch (Exception e) {
                log.error("恢复定时任务失败: jobId={}, error={}", job.getId(), e.getMessage());
                job.setStatus("PAUSED");
                job.setUpdatedAt(Instant.now());
                jobRepository.save(job);
            }
        }
    }

    /**
     * 创建定时任务
     */
    public Mono<SchedulerJob> createJob(String workflowId, String cronExpression, String description, String createdBy) {
        return Mono.fromCallable(() -> {
            // 验证工作流存在
            Workflow workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new RuntimeException("工作流不存在：" + workflowId));

            // 验证 cron 表达式
            validateCron(cronExpression);

            SchedulerJob job = new SchedulerJob();
            job.setId(UUID.randomUUID().toString());
            job.setWorkflowId(workflowId);
            job.setWorkflowName(workflow.getName());
            job.setCronExpression(cronExpression);
            job.setStatus("RUNNING");
            job.setDescription(description);
            job.setCreatedBy(createdBy);
            job.setCreatedAt(Instant.now());
            job.setUpdatedAt(Instant.now());

            // 计算下次执行时间
            job.setNextFireTime(computeNextFireTime(cronExpression));

            job = jobRepository.save(job);

            // 注册到 TaskScheduler
            scheduleTask(job);

            log.info("创建定时任务: jobId={}, workflowId={}, cron={}", job.getId(), workflowId, cronExpression);
            return job;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 查询所有定时任务
     */
    public Mono<List<SchedulerJob>> listJobs() {
        return Mono.fromCallable(() -> jobRepository.findAll())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 查询单个定时任务
     */
    public Mono<SchedulerJob> getJob(String jobId) {
        return Mono.fromCallable(() ->
                jobRepository.findById(jobId)
                        .orElseThrow(() -> new RuntimeException("定时任务不存在：" + jobId))
        ).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 暂停定时任务
     */
    public Mono<SchedulerJob> pauseJob(String jobId) {
        return Mono.fromCallable(() -> {
            SchedulerJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("定时任务不存在：" + jobId));

            if (!"RUNNING".equals(job.getStatus())) {
                throw new RuntimeException("只能暂停运行中的任务，当前状态：" + job.getStatus());
            }

            // 取消调度
            cancelScheduledTask(jobId);

            job.setStatus("PAUSED");
            job.setUpdatedAt(Instant.now());
            job = jobRepository.save(job);

            log.info("暂停定时任务: jobId={}", jobId);
            return job;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 恢复定时任务
     */
    public Mono<SchedulerJob> resumeJob(String jobId) {
        return Mono.fromCallable(() -> {
            SchedulerJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("定时任务不存在：" + jobId));

            if (!"PAUSED".equals(job.getStatus())) {
                throw new RuntimeException("只能恢复暂停中的任务，当前状态：" + job.getStatus());
            }

            // 重新注册调度
            scheduleTask(job);

            job.setStatus("RUNNING");
            job.setNextFireTime(computeNextFireTime(job.getCronExpression()));
            job.setUpdatedAt(Instant.now());
            job = jobRepository.save(job);

            log.info("恢复定时任务: jobId={}", jobId);
            return job;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除定时任务
     */
    public Mono<Void> deleteJob(String jobId) {
        return Mono.<Void>fromRunnable(() -> {
            if (!jobRepository.existsById(jobId)) {
                throw new RuntimeException("定时任务不存在：" + jobId);
            }

            // 取消调度
            cancelScheduledTask(jobId);

            jobRepository.deleteById(jobId);
            log.info("删除定时任务: jobId={}", jobId);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 更新 cron 表达式
     */
    public Mono<SchedulerJob> updateCron(String jobId, String newCronExpression) {
        return Mono.fromCallable(() -> {
            SchedulerJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("定时任务不存在：" + jobId));

            validateCron(newCronExpression);

            // 如果正在运行，先取消再重新调度
            if ("RUNNING".equals(job.getStatus())) {
                cancelScheduledTask(jobId);
            }

            job.setCronExpression(newCronExpression);
            job.setNextFireTime(computeNextFireTime(newCronExpression));
            job.setUpdatedAt(Instant.now());
            job = jobRepository.save(job);

            // 如果之前是运行状态，重新调度
            if ("RUNNING".equals(job.getStatus())) {
                scheduleTask(job);
            }

            log.info("更新定时任务 cron: jobId={}, newCron={}", jobId, newCronExpression);
            return job;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ==================== 内部方法 ====================

    /**
     * 将任务注册到 Spring TaskScheduler
     */
    private void scheduleTask(SchedulerJob job) {
        CronTrigger trigger = new CronTrigger(job.getCronExpression(), TimeZone.getTimeZone(ZoneId.of("Asia/Shanghai")));

        ScheduledFuture<?> future = taskScheduler.schedule(() -> {
            try {
                log.info("定时触发工作流执行: jobId={}, workflowId={}", job.getId(), job.getWorkflowId());
                executeWorkflowForJob(job);
            } catch (Exception e) {
                log.error("定时执行工作流失败: jobId={}, error={}", job.getId(), e.getMessage(), e);
            }
        }, trigger);

        activeTasks.put(job.getId(), future);
    }

    /**
     * 取消已调度的任务
     */
    private void cancelScheduledTask(String jobId) {
        ScheduledFuture<?> future = activeTasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 执行工作流并更新任务状态
     */
    private void executeWorkflowForJob(SchedulerJob job) {
        workflowService.executeWorkflow(job.getWorkflowId(), Map.of(), job.getCreatedBy())
                .subscribe(
                        execution -> {
                            // 更新最后执行信息
                            job.setLastFireTime(Instant.now());
                            job.setLastExecutionId(execution.getId());
                            job.setNextFireTime(computeNextFireTime(job.getCronExpression()));
                            job.setUpdatedAt(Instant.now());
                            jobRepository.save(job);
                            log.info("定时执行成功: jobId={}, executionId={}", job.getId(), execution.getId());
                        },
                        error -> {
                            log.error("定时执行失败: jobId={}, error={}", job.getId(), error.getMessage());
                            job.setLastFireTime(Instant.now());
                            job.setUpdatedAt(Instant.now());
                            jobRepository.save(job);
                        }
                );
    }

    /**
     * 验证 cron 表达式合法性
     */
    private void validateCron(String cronExpression) {
        try {
            new CronTrigger(cronExpression);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的 Cron 表达式：" + cronExpression + "，错误：" + e.getMessage());
        }
    }

    /**
     * 计算下次执行时间
     */
    private Instant computeNextFireTime(String cronExpression) {
        try {
            CronTrigger trigger = new CronTrigger(cronExpression, TimeZone.getTimeZone(ZoneId.of("Asia/Shanghai")));
            // Spring 6 CronTrigger 不直接暴露 next，使用 org.springframework.scheduling.support.CronExpression
            org.springframework.scheduling.support.CronExpression cron =
                    org.springframework.scheduling.support.CronExpression.parse(cronExpression);
            java.time.LocalDateTime next = cron.next(java.time.LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            if (next != null) {
                return next.atZone(ZoneId.of("Asia/Shanghai")).toInstant();
            }
        } catch (Exception e) {
            log.warn("计算下次执行时间失败: cron={}, error={}", cronExpression, e.getMessage());
        }
        return null;
    }

    /**
     * 获取当前活跃任务数量
     */
    public int getActiveTaskCount() {
        return activeTasks.size();
    }
}
