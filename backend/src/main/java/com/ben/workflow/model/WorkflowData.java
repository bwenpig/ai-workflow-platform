package com.ben.workflow.model;

import java.util.Map;

/**
 * 工作流数据类型定义
 */
public class WorkflowData {
    
    public enum DataType {
        TEXT("text", "文本"),
        IMAGE("image", "图片"),
        VIDEO("video", "视频"),
        AUDIO("audio", "音频"),
        JSON("json", "结构化数据");
        
        private final String code;
        private final String description;
        
        DataType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public static DataType fromCode(String code) {
            for (DataType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return TEXT;
        }
    }
    
    private DataType type;
    private Object content;
    private Map<String, Object> metadata;
    private String sourceNode;
    
    public WorkflowData() {}
    
    public WorkflowData(DataType type, Object content, Map<String, Object> metadata, String sourceNode) {
        this.type = type;
        this.content = content;
        this.metadata = metadata;
        this.sourceNode = sourceNode;
    }
    
    public static WorkflowData text(String content) {
        return new WorkflowData(DataType.TEXT, content, null, null);
    }
    
    public static WorkflowData image(String url, Integer width, Integer height) {
        return new WorkflowData(DataType.IMAGE, url, Map.of("width", width, "height", height), null);
    }
    
    public static WorkflowData video(String url, Integer duration, Integer fps) {
        return new WorkflowData(DataType.VIDEO, url, Map.of("duration", duration, "fps", fps), null);
    }
    
    public static WorkflowData audio(String url, Integer duration, String format) {
        return new WorkflowData(DataType.AUDIO, url, Map.of("duration", duration, "format", format), null);
    }
    
    public static WorkflowData json(Object data) {
        return new WorkflowData(DataType.JSON, data, null, null);
    }
    
    public DataType getType() { return type; }
    public void setType(DataType type) { this.type = type; }
    public Object getContent() { return content; }
    public void setContent(Object content) { this.content = content; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public String getSourceNode() { return sourceNode; }
    public void setSourceNode(String sourceNode) { this.sourceNode = sourceNode; }
}
