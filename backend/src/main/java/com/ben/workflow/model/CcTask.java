package com.ben.workflow.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * CC 任务追踪 Model
 *
 * @author 龙傲天
 * @since 2026-04-10
 */
public class CcTask {
    private String id;
    private String title;
    private String description;
    private String agentId;
    private Status status;
    private int progress;
    private String currentStep;
    private List<String> logs;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private String errorMessage;

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    public CcTask() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }

    public List<String> getLogs() { return logs; }
    public void setLogs(List<String> logs) { this.logs = logs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public void addLog(String message) {
        if (this.logs == null) this.logs = new ArrayList<>();
        this.logs.add("[" + Instant.now().toString() + "] " + message);
        this.updatedAt = Instant.now();
    }

    /** Builder pattern for convenient construction */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final CcTask task = new CcTask();

        public Builder id(String id) { task.setId(id); return this; }
        public Builder title(String title) { task.setTitle(title); return this; }
        public Builder description(String description) { task.setDescription(description); return this; }
        public Builder agentId(String agentId) { task.setAgentId(agentId); return this; }
        public Builder status(Status status) { task.setStatus(status); return this; }
        public Builder progress(int progress) { task.setProgress(progress); return this; }
        public Builder currentStep(String currentStep) { task.setCurrentStep(currentStep); return this; }
        public Builder logs(List<String> logs) { task.setLogs(logs); return this; }
        public Builder createdAt(Instant createdAt) { task.setCreatedAt(createdAt); return this; }
        public Builder updatedAt(Instant updatedAt) { task.setUpdatedAt(updatedAt); return this; }
        public Builder completedAt(Instant completedAt) { task.setCompletedAt(completedAt); return this; }
        public Builder errorMessage(String errorMessage) { task.setErrorMessage(errorMessage); return this; }

        public CcTask build() { return task; }
    }
}
