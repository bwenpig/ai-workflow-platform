package com.ben.workflow.api;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.service.WorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    public Mono<ResponseEntity<Workflow>> createWorkflow(@RequestBody Workflow workflow, @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String createdBy = userId != null ? userId : "anonymous";
        return workflowService.createWorkflow(workflow, createdBy).map(ResponseEntity::ok);
    }

    @GetMapping
    public Mono<ResponseEntity<List<Workflow>>> listWorkflows(@RequestParam(value = "published", required = false) Boolean publishedOnly, @RequestParam(value = "createdBy", required = false) String createdBy) {
        return workflowService.listWorkflows(createdBy, publishedOnly != null && publishedOnly).map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Workflow>> getWorkflow(@PathVariable String id) {
        return workflowService.getWorkflow(id).map(ResponseEntity::ok).onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Workflow>> updateWorkflow(@PathVariable String id, @RequestBody Workflow updates) {
        return workflowService.updateWorkflow(id, updates).map(ResponseEntity::ok).onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteWorkflow(@PathVariable String id) {
        return workflowService.deleteWorkflow(id)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .onErrorResume(e -> Mono.just(ResponseEntity.notFound().<Void>build()));
    }

    @PostMapping("/{id}/publish")
    public Mono<ResponseEntity<Workflow>> togglePublished(@PathVariable String id) {
        return workflowService.togglePublished(id).map(ResponseEntity::ok).onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping("/{id}/execute")
    public Mono<ResponseEntity<Map<String, String>>> executeWorkflow(@PathVariable String id, @RequestBody(required = false) Map<String, Object> inputs, @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String createdBy = userId != null ? userId : "anonymous";
        return workflowService.executeWorkflow(id, inputs != null ? inputs : Map.of(), createdBy)
                .map(execution -> {
                    Map<String, String> response = Map.of("executionId", execution.getId(), "status", execution.getStatus(), "workflowId", execution.getWorkflowId());
                    return ResponseEntity.accepted().body(response);
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }
}
