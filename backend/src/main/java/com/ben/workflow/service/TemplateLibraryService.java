package com.ben.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 代码模板库服务
 * 
 * 提供预定义的代码模板，用户可以快速选择和使用
 * 
 * @author 龙傲天
 * @version 1.0
 */
@Service
public class TemplateLibraryService {
    
    private static final String TEMPLATE_PATH = "templates/";
    private final ObjectMapper objectMapper;
    private final Map<String, TemplateInfo> templates;
    
    /**
     * 模板信息
     */
    public static class TemplateInfo {
        private String id;
        private String name;
        private String description;
        private String category;
        private String script;
        private Map<String, Object> inputs;
        private List<String> requirements;
        private Integer timeout;
        private Boolean networkEnabled;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getScript() { return script; }
        public void setScript(String script) { this.script = script; }
        
        public Map<String, Object> getInputs() { return inputs; }
        public void setInputs(Map<String, Object> inputs) { this.inputs = inputs; }
        
        public List<String> getRequirements() { return requirements; }
        public void setRequirements(List<String> requirements) { this.requirements = requirements; }
        
        public Integer getTimeout() { return timeout; }
        public void setTimeout(Integer timeout) { this.timeout = timeout; }
        
        public Boolean getNetworkEnabled() { return networkEnabled; }
        public void setNetworkEnabled(Boolean networkEnabled) { this.networkEnabled = networkEnabled; }
    }
    
    public TemplateLibraryService() {
        this.objectMapper = new ObjectMapper();
        this.templates = new LinkedHashMap<>();
        loadTemplates();
    }
    
    /**
     * 加载所有模板
     */
    private void loadTemplates() {
        // 模板文件列表（按编号排序）
        String[] templateFiles = {
            "01_hello_world.json",
            "02_http_request.json",
            "03_data_processing.json",
            "04_image_processing.json",
            "05_json_parsing.json",
            "06_file_operations.json",
            "07_data_visualization.json",
            "08_text_processing.json",
            "09_datetime.json",
            "10_excel_processing.json"
        };
        
        for (String filename : templateFiles) {
            try {
                InputStream inputStream = getClass().getClassLoader()
                        .getResourceAsStream(TEMPLATE_PATH + filename);
                
                if (inputStream != null) {
                    String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    TemplateInfo template = objectMapper.readValue(content, TemplateInfo.class);
                    
                    // 使用文件名作为 ID（不含扩展名）
                    String id = filename.replace(".json", "");
                    template.setId(id);
                    
                    templates.put(id, template);
                    System.out.println("[TemplateLibrary] 加载模板：" + template.getName());
                }
            } catch (IOException e) {
                System.err.println("[TemplateLibrary] 加载模板失败：" + filename + " - " + e.getMessage());
            }
        }
        
        System.out.println("[TemplateLibrary] 模板库初始化完成，共 " + templates.size() + " 个模板");
    }
    
    /**
     * 获取所有模板列表（不含脚本内容）
     * 
     * @return 模板列表
     */
    public List<TemplateInfo> listTemplates() {
        List<TemplateInfo> list = new ArrayList<>(templates.values());
        // 返回精简版，隐藏脚本内容
        for (TemplateInfo info : list) {
            info.setScript(null);
            info.setInputs(null);
        }
        return list;
    }
    
    /**
     * 按分类获取模板列表
     * 
     * @param category 分类名称
     * @return 模板列表
     */
    public List<TemplateInfo> listTemplatesByCategory(String category) {
        List<TemplateInfo> list = new ArrayList<>();
        for (TemplateInfo template : templates.values()) {
            if (category.equals(template.getCategory())) {
                TemplateInfo info = new TemplateInfo();
                info.setId(template.getId());
                info.setName(template.getName());
                info.setDescription(template.getDescription());
                info.setCategory(template.getCategory());
                list.add(info);
            }
        }
        return list;
    }
    
    /**
     * 获取所有分类
     * 
     * @return 分类列表
     */
    public Set<String> getCategories() {
        Set<String> categories = new LinkedHashSet<>();
        for (TemplateInfo template : templates.values()) {
            categories.add(template.getCategory());
        }
        return categories;
    }
    
    /**
     * 获取单个模板详情
     * 
     * @param templateId 模板 ID
     * @return 模板信息，不存在返回 null
     */
    public TemplateInfo getTemplate(String templateId) {
        return templates.get(templateId);
    }
    
    /**
     * 搜索模板
     * 
     * @param keyword 关键词
     * @return 匹配的模板列表
     */
    public List<TemplateInfo> searchTemplates(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return listTemplates();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        List<TemplateInfo> results = new ArrayList<>();
        
        for (TemplateInfo template : templates.values()) {
            if (template.getName().toLowerCase().contains(lowerKeyword) ||
                template.getDescription().toLowerCase().contains(lowerKeyword) ||
                template.getCategory().toLowerCase().contains(lowerKeyword)) {
                
                TemplateInfo info = new TemplateInfo();
                info.setId(template.getId());
                info.setName(template.getName());
                info.setDescription(template.getDescription());
                info.setCategory(template.getCategory());
                results.add(info);
            }
        }
        
        return results;
    }
    
    /**
     * 获取模板数量
     * 
     * @return 模板总数
     */
    public int getTemplateCount() {
        return templates.size();
    }
}
