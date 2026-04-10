package com.ben.workflow.service;

import com.ben.workflow.model.CcTask;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * CC 任务追踪服务 - 插件式，不耦合工作流执行逻辑
 * 内存存储 + SSE 实时推送
 *
 * @author 龙傲天
 * @since 2026-04-10
 */
@Service
public class CcTaskTracker {

    /** 任务存储 */
    private final Map<String, CcTask> tasks = new ConcurrentHashMap<>();

    /** SSE 订阅者：taskId → FluxSink */
    private final Map<String, List<FluxSink<CcTask>>> subscribers = new ConcurrentHashMap<>();

    /**
     * 创建任务
     */
    public CcTask createTask(String title, String description, String agentId) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        CcTask task = CcTask.builder()
                .id(id)
                .title(title)
                .description(description)
                .agentId(agentId != null ? agentId : "cc")
                .status(CcTask.Status.PENDING)
                .progress(0)
                .logs(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();
        tasks.put(id, task);
        return task;
    }

    /**
     * 更新任务状态
     */
    public CcTask updateTask(String id, CcTask.Status status, Integer progress,
                              String currentStep, String logMessage, String errorMessage) {
        CcTask task = tasks.get(id);
        if (task == null) {
            throw new RuntimeException("任务不存在：" + id);
        }

        if (status != null) {
            task.setStatus(status);
        }
        if (progress != null) {
            task.setProgress(Math.max(0, Math.min(100, progress)));
        }
        if (currentStep != null) {
            task.setCurrentStep(currentStep);
        }
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }
        if (logMessage != null && !logMessage.isEmpty()) {
            task.addLog(logMessage);
        }

        task.setUpdatedAt(Instant.now());

        // 终态设置 completedAt
        if (task.getStatus() == CcTask.Status.COMPLETED
                || task.getStatus() == CcTask.Status.FAILED
                || task.getStatus() == CcTask.Status.CANCELLED) {
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(Instant.now());
            }
        }

        // 通知所有 SSE 订阅者
        notifySubscribers(id, task);
        return task;
    }

    /**
     * 追加日志（不改变状态）
     */
    public CcTask addLog(String id, String message) {
        return updateTask(id, null, null, null, message, null);
    }

    /**
     * 获取单个任务
     */
    public CcTask getTask(String id) {
        CcTask task = tasks.get(id);
        if (task == null) {
            throw new RuntimeException("任务不存在：" + id);
        }
        return task;
    }

    /**
     * 获取所有任务（按创建时间倒序）
     */
    public List<CcTask> listTasks() {
        return tasks.values().stream()
                .sorted(Comparator.comparing(CcTask::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 获取活跃任务（非终态）
     */
    public List<CcTask> listActiveTasks() {
        return tasks.values().stream()
                .filter(t -> t.getStatus() == CcTask.Status.PENDING
                        || t.getStatus() == CcTask.Status.RUNNING)
                .sorted(Comparator.comparing(CcTask::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * SSE 订阅任务实时更新
     */
    public Flux<CcTask> subscribeTask(String id) {
        return Flux.create(sink -> {
            CcTask task = tasks.get(id);
            if (task != null) {
                sink.next(task); // 先发送当前状态
            }

            subscribers.computeIfAbsent(id, k -> Collections.synchronizedList(new ArrayList<>())).add(sink);

            sink.onCancel(() -> {
                List<FluxSink<CcTask>> subs = subscribers.get(id);
                if (subs != null) {
                    subs.remove(sink);
                }
            });
        });
    }

    /**
     * 通知 SSE 订阅者
     */
    private void notifySubscribers(String taskId, CcTask task) {
        List<FluxSink<CcTask>> subs = subscribers.get(taskId);
        if (subs != null) {
            subs.removeIf(sink -> {
                if (sink.isCancelled()) return true;
                sink.next(task);
                return false;
            });
        }
    }

    /**
     * 删除任务（含订阅清理）
     */
    public void deleteTask(String id) {
        tasks.remove(id);
        List<FluxSink<CcTask>> subs = subscribers.remove(id);
        if (subs != null) {
            subs.forEach(FluxSink::complete);
        }
    }
}
