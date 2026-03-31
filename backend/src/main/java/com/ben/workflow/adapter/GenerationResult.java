package com.ben.workflow.adapter;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 生成结果（统一格式）
 */


public class GenerationResult {

    /** 任务 ID */
    private String taskId;

    /** 输出 URL 列表 */
    private List<String> outputUrls;

    /** 元数据 */
    private Map<String, Object> metadata;

    /** 状态 */
    private ModelProvider.TaskStatus status;

    /** 错误信息 */
    private String errorMessage;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** 预览图 URL（视频模型需要） */
    private String previewUrl;

    /** 时长（秒，视频模型需要） */
    private Integer duration;

    /** 帧率（视频模型需要） */
    private Integer fps;
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public List<String> getOutputUrls() { return outputUrls; }
    public void setOutputUrls(List<String> outputUrls) { this.outputUrls = outputUrls; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
    public Integer getFps() { return fps; }
    public void setFps(Integer fps) { this.fps = fps; }
    public ModelProvider.TaskStatus getStatus() { return status; }
    public void setStatus(ModelProvider.TaskStatus status) { this.status = status; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
}
