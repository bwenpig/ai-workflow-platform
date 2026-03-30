package com.ben.workflow.api;

import com.ben.workflow.model.WorkflowExecution;
import com.ben.workflow.service.WorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/executions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExecutionController {

    private final WorkflowService workflowService;

    public ExecutionController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<WorkflowExecution>> getExecutionStatus(@PathVariable String id) {
        return workflowService.getExecutionStatus(id).map(ResponseEntity::ok).onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping("/{id}/cancel")
    public Mono<ResponseEntity<WorkflowExecution>> cancelExecution(@PathVariable String id) {
        return workflowService.cancelExecution(id).map(ResponseEntity::ok).onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/history")
    public Mono<ResponseEntity<List<WorkflowExecution>>> getExecutionHistory(@RequestParam(value = "userId", required = false) String userId, @RequestParam(value = "limit", defaultValue = "20") int limit) {
        if (userId == null) return Mono.just(ResponseEntity.badRequest().build());
        return workflowService.getExecutionHistory(userId, limit).map(ResponseEntity::ok);
    }
}
