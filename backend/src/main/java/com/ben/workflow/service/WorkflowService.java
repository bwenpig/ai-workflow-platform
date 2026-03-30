package com.ben.workflow.service;

import com.ben.workflow.engine.DagWorkflowEngine;
import com.ben.workflow.model.Workflow;
import com.ben.workflow.model.WorkflowExecution;
import com.ben.workflow.repository.ExecutionRepository;
import com.ben.workflow.repository.WorkflowRepository;
import com.ben.workflow.websocket.WebSocketNotificationService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final ExecutionRepository executionRepository;
    private final DagWorkflowEngine workflowEngine;
    private final WebSocketNotificationService notificationService;

    public WorkflowService(WorkflowRepository workflowRepository, ExecutionRepository executionRepository, DagWorkflowEngine workflowEngine, WebSocketNotificationService notificationService) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.workflowEngine = workflowEngine;
        this.notificationService = notificationService;
    }

    public Mono<Workflow> createWorkflow(Workflow workflow, String createdBy) {
        return Mono.fromCallable(() -> {
            workflow.setId(UUID.randomUUID().toString());
            workflow.setCreatedBy(createdBy);
            workflow.setCreatedAt(Instant.now());
            workflow.setUpdatedAt(Instant.now());
            workflow.setVersion(1);
            workflow.setPublished(false);
            return workflowRepository.save(workflow);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<Workflow>> listWorkflows(String createdBy, boolean publishedOnly) {
        return Mono.fromCallable(() -> {
            if (publishedOnly) {
                return workflowRepository.findByPublishedTrue();
            } else if (createdBy != null) {
                return workflowRepository.findByCreatedBy(createdBy);
            } else {
                return workflowRepository.findAll();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Workflow> getWorkflow(String id) {
        return Mono.fromCallable(() -> 
            workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工作流不存在：" + id))
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Workflow> updateWorkflow(String id, Workflow updates) {
        return Mono.fromCallable(() -> {
            Workflow existing = workflowRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("工作流不存在：" + id));
            
            if (updates.getName() != null) existing.setName(updates.getName());
            if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
            if (updates.getNodes() != null) existing.setNodes(updates.getNodes());
            if (updates.getEdges() != null) existing.setEdges(updates.getEdges());
            
            existing.setUpdatedAt(Instant.now());
            existing.setVersion(existing.getVersion() + 1);
            
            return workflowRepository.save(existing);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteWorkflow(String id) {
        return Mono.<Void>fromRunnable(() -> {
            if (!workflowRepository.existsById(id)) {
                throw new RuntimeException("工作流不存在：" + id);
            }
            workflowRepository.deleteById(id);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Workflow> togglePublished(String id) {
        return Mono.fromCallable(() -> {
            Workflow workflow = workflowRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("工作流不存在：" + id));
            
            workflow.setPublished(!workflow.getPublished());
            workflow.setUpdatedAt(Instant.now());
            
            return workflowRepository.save(workflow);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<WorkflowExecution> executeWorkflow(String workflowId, Map<String, Object> inputs, String createdBy) {
        return Mono.fromCallable(() -> {
            Workflow workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new RuntimeException("工作流不存在：" + workflowId));
            
            WorkflowExecution execution = new WorkflowExecution();
            execution.setId(UUID.randomUUID().toString());
            execution.setWorkflowId(workflowId);
            execution.setStatus("PENDING");
            execution.setInputs(inputs);
            execution.setCreatedBy(createdBy);
            execution.setCreatedAt(Instant.now());
            
            execution = executionRepository.save(execution);
            
            workflowEngine.execute(workflow, inputs).subscribe(instanceId -> {
                System.out.println("工作流执行已启动：instanceId=" + instanceId);
            });
            
            return execution;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<WorkflowExecution> getExecutionStatus(String executionId) {
        return Mono.fromCallable(() -> 
            executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("执行实例不存在：" + executionId))
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<WorkflowExecution> cancelExecution(String executionId) {
        return Mono.fromCallable(() -> {
            WorkflowExecution execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new RuntimeException("执行实例不存在：" + executionId));
            
            if ("SUCCESS".equals(execution.getStatus()) || "FAILED".equals(execution.getStatus())) {
                throw new RuntimeException("执行已完成，无法取消");
            }
            
            execution.setStatus("CANCELLED");
            execution.setEndedAt(Instant.now());
            
            workflowEngine.cancel(executionId).subscribe();
            
            return executionRepository.save(execution);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<WorkflowExecution>> getExecutionHistory(String createdBy, int limit) {
        return Mono.fromCallable(() -> 
            executionRepository.findByCreatedByOrderByCreatedAtDesc(createdBy, limit)
        ).subscribeOn(Schedulers.boundedElastic());
    }
}
