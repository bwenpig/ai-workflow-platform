package com.ben.workflow.scheduler;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * 定时调度任务实体
 */
@Document(collection = "scheduler_jobs")
public class SchedulerJob {

    @Id
    private String id;

    /** 关联的工作流 ID */
    private String workflowId;

    /** 工作流名称（冗余，方便展示） */
    private String workflowName;

    /** Cron 表达式 */
    private String cronExpression;

    /** 任务状态：RUNNING / PAUSED / COMPLETED */
    private String status;

    /** 下次执行时间 */
    private Instant nextFireTime;

    /** 上次执行时间 */
    private Instant lastFireTime;

    /** 上次执行ID */
    private String lastExecutionId;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;

    /** 描述/备注 */
    private String description;

    // ==================== Getters & Setters ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getNextFireTime() { return nextFireTime; }
    public void setNextFireTime(Instant nextFireTime) { this.nextFireTime = nextFireTime; }

    public Instant getLastFireTime() { return lastFireTime; }
    public void setLastFireTime(Instant lastFireTime) { this.lastFireTime = lastFireTime; }

    public String getLastExecutionId() { return lastExecutionId; }
    public void setLastExecutionId(String lastExecutionId) { this.lastExecutionId = lastExecutionId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
