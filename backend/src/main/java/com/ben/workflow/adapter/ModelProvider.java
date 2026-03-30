package com.ben.workflow.adapter;

import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * 模型提供者统一接口
 */
public interface ModelProvider {

    /**
     * 获取模型类型
     */
    ModelType getType();

    /**
     * 获取模型名称
     */
    String getModelName();

    /**
     * 生成（异步）
     */
    Mono<GenerationResult> generate(GenerationRequest request);

    /**
     * 查询任务状态
     */
    Mono<TaskStatus> getStatus(String taskId);

    /**
     * 取消任务
     */
    Mono<Void> cancel(String taskId);

    enum ModelType {
        IMAGE,      // 生图
        VIDEO,      // 生视频
        AUDIO       // 音频（预留）
    }

    enum TaskStatus {
        PENDING,    // 排队中
        RUNNING,    // 执行中
        SUCCESS,    // 成功
        FAILED,     // 失败
        CANCELLED   // 已取消
    }
}
