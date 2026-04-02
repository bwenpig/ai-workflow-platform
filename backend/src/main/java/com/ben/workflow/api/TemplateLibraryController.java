package com.ben.workflow.api;

import com.ben.workflow.service.TemplateLibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 代码模板库 API 控制器
 * 
 * @author 龙傲天
 * @version 1.0
 */
@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "*")
public class TemplateLibraryController {
    
    @Autowired
    private TemplateLibraryService templateLibraryService;
    
    /**
     * 获取所有模板列表
     * 
     * @return 模板列表
     */
    @GetMapping
    public ResponseEntity<List<TemplateLibraryService.TemplateInfo>> listTemplates() {
        return ResponseEntity.ok(templateLibraryService.listTemplates());
    }
    
    /**
     * 按分类获取模板列表
     * 
     * @param category 分类名称
     * @return 模板列表
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<TemplateLibraryService.TemplateInfo>> listTemplatesByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(templateLibraryService.listTemplatesByCategory(category));
    }
    
    /**
     * 获取所有分类
     * 
     * @return 分类列表
     */
    @GetMapping("/categories")
    public ResponseEntity<Set<String>> getCategories() {
        return ResponseEntity.ok(templateLibraryService.getCategories());
    }
    
    /**
     * 获取单个模板详情
     * 
     * @param templateId 模板 ID
     * @return 模板详情
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateLibraryService.TemplateInfo> getTemplate(
            @PathVariable String templateId) {
        TemplateLibraryService.TemplateInfo template = templateLibraryService.getTemplate(templateId);
        
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(template);
    }
    
    /**
     * 搜索模板
     * 
     * @param keyword 关键词
     * @return 匹配的模板列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<TemplateLibraryService.TemplateInfo>> searchTemplates(
            @RequestParam String keyword) {
        return ResponseEntity.ok(templateLibraryService.searchTemplates(keyword));
    }
    
    /**
     * 获取模板统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<TemplateStats> getStats() {
        TemplateStats stats = new TemplateStats();
        stats.setTotalCount(templateLibraryService.getTemplateCount());
        stats.setCategories(templateLibraryService.getCategories());
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 模板统计信息
     */
    public static class TemplateStats {
        private int totalCount;
        private Set<String> categories;
        
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        
        public Set<String> getCategories() { return categories; }
        public void setCategories(Set<String> categories) { this.categories = categories; }
    }
}
