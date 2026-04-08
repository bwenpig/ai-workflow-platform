package com.ben.workflow.api;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.repository.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    
    @Autowired
    private WorkflowRepository workflowRepository;
    
    @GetMapping("/{id}/export")
    public Mono<ResponseEntity<ByteArrayResource>> exportProject(@PathVariable String id) {
        return Mono.fromCallable(() -> workflowRepository.findById(id))
            .subscribeOn(Schedulers.boundedElastic())
            .map(opt -> {
                if (opt.isEmpty()) {
                    return ResponseEntity.notFound().<ByteArrayResource>build();
                }
                Workflow workflow = opt.get();
                String json = "{\"schemaVersion\":1,\"project\":{\"id\":\"" + workflow.getId() + "\",\"name\":\"" + workflow.getName() + "\",\"version\":\"1.0.0\"}}";
                ByteArrayResource resource = new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
                String filename = workflow.getName() != null ? workflow.getName() : "project";
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + ".awf.json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resource);
            });
    }
    
    @PostMapping("/import")
    public Mono<ResponseEntity<Map<String, Object>>> importProject(@RequestPart("file") FilePart file) {
        return Mono.fromCallable(() -> {
                Workflow workflow = new Workflow();
                workflow.setName("Imported Project");
                workflow.setCreatedAt(Instant.now());
                return workflowRepository.save(workflow);
            })
            .subscribeOn(Schedulers.boundedElastic())
            .map(saved -> {
                Map<String, Object> result = new HashMap<>();
                result.put("projectId", saved.getId());
                result.put("name", saved.getName());
                return ResponseEntity.ok(result);
            })
            .onErrorResume(e -> {
                Map<String, Object> error = Map.of("error", e.getMessage());
                return Mono.just(ResponseEntity.badRequest().body(error));
            });
    }
    
    @GetMapping
    public Mono<ResponseEntity<List<Workflow>>> listProjects() {
        return Mono.fromCallable(() -> workflowRepository.findAll())
            .subscribeOn(Schedulers.boundedElastic())
            .map(ResponseEntity::ok);
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Workflow>> getProject(@PathVariable String id) {
        return Mono.fromCallable(() -> workflowRepository.findById(id))
            .subscribeOn(Schedulers.boundedElastic())
            .map(opt -> opt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()));
    }
    
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Workflow>> updateProject(@PathVariable String id, @RequestBody Workflow workflowData) {
        return Mono.fromCallable(() -> {
                Optional<Workflow> opt = workflowRepository.findById(id);
                if (opt.isEmpty()) {
                    return null;
                }
                Workflow existing = opt.get();
                existing.setName(workflowData.getName());
                existing.setDescription(workflowData.getDescription());
                existing.setNodes(workflowData.getNodes());
                existing.setEdges(workflowData.getEdges());
                existing.setUpdatedAt(Instant.now());
                return workflowRepository.save(existing);
            })
            .subscribeOn(Schedulers.boundedElastic())
            .map(saved -> {
                if (saved == null) {
                    return ResponseEntity.notFound().<Workflow>build();
                }
                return ResponseEntity.ok(saved);
            });
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteProject(@PathVariable String id) {
        return Mono.fromCallable(() -> {
                if (workflowRepository.existsById(id)) {
                    workflowRepository.deleteById(id);
                    return true;
                }
                return false;
            })
            .subscribeOn(Schedulers.boundedElastic())
            .map(deleted -> {
                if (deleted) {
                    return ResponseEntity.ok().<Void>build();
                }
                return ResponseEntity.notFound().<Void>build();
            });
    }
}
