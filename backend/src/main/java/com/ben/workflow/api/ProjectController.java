package com.ben.workflow.api;

import com.ben.workflow.model.Workflow;
import com.ben.workflow.repository.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<ByteArrayResource> exportProject(@PathVariable String id) {
        Optional<Workflow> opt = workflowRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Workflow workflow = opt.get();
        String json = "{\"schemaVersion\":1,\"project\":{\"id\":\"" + workflow.getId() + "\",\"name\":\"" + workflow.getName() + "\",\"version\":\"1.0.0\"}}";
        
        ByteArrayResource resource = new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
        String filename = workflow.getName() != null ? workflow.getName() : "project";
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + ".awf.json\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(resource);
    }
    
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importProject(@RequestParam("file") MultipartFile file) {
        try {
            Workflow workflow = new Workflow();
            workflow.setName("Imported Project");
            workflow.setCreatedAt(Instant.now());
            
            Workflow saved = workflowRepository.save(workflow);
            
            Map<String, Object> result = new HashMap<>();
            result.put("projectId", saved.getId());
            result.put("name", saved.getName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping
    public List<Workflow> listProjects() {
        return workflowRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getProject(@PathVariable String id) {
        return workflowRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Workflow> updateProject(@PathVariable String id, @RequestBody Workflow workflowData) {
        Optional<Workflow> opt = workflowRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Workflow existing = opt.get();
        existing.setName(workflowData.getName());
        existing.setDescription(workflowData.getDescription());
        existing.setNodes(workflowData.getNodes());
        existing.setEdges(workflowData.getEdges());
        existing.setUpdatedAt(Instant.now());
        
        Workflow saved = workflowRepository.save(existing);
        return ResponseEntity.ok(saved);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        if (workflowRepository.existsById(id)) {
            workflowRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}