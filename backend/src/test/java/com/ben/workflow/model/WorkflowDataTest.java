package com.ben.workflow.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工作流数据类型测试
 */
public class WorkflowDataTest {
    
    @Test
    @DisplayName("创建文本数据")
    public void testCreateTextData() {
        WorkflowData data = WorkflowData.text("Hello World");
        
        assertEquals(WorkflowData.DataType.TEXT, data.getType());
        assertEquals("Hello World", data.getContent());
    }
    
    @Test
    @DisplayName("创建图片数据")
    public void testCreateImageData() {
        WorkflowData data = WorkflowData.image("https://example.com/test.jpg", 1024, 768);
        
        assertEquals(WorkflowData.DataType.IMAGE, data.getType());
        assertEquals("https://example.com/test.jpg", data.getContent());
        assertEquals(1024, data.getMetadata().get("width"));
        assertEquals(768, data.getMetadata().get("height"));
    }
    
    @Test
    @DisplayName("创建视频数据")
    public void testCreateVideoData() {
        WorkflowData data = WorkflowData.video("https://example.com/test.mp4", 30, 24);
        
        assertEquals(WorkflowData.DataType.VIDEO, data.getType());
        assertEquals(30, data.getMetadata().get("duration"));
        assertEquals(24, data.getMetadata().get("fps"));
    }
    
    @Test
    @DisplayName("创建音频数据")
    public void testCreateAudioData() {
        WorkflowData data = WorkflowData.audio("https://example.com/test.mp3", 180, "mp3");
        
        assertEquals(WorkflowData.DataType.AUDIO, data.getType());
        assertEquals(180, data.getMetadata().get("duration"));
        assertEquals("mp3", data.getMetadata().get("format"));
    }
    
    @Test
    @DisplayName("创建 JSON 数据")
    public void testCreateJsonData() {
        Map<String, Object> testData = Map.of("key", "value", "count", 42);
        WorkflowData data = WorkflowData.json(testData);
        
        assertEquals(WorkflowData.DataType.JSON, data.getType());
        assertEquals(testData, data.getContent());
    }
    
    @Test
    @DisplayName("DataType 枚举转换")
    public void testDataTypeFromCode() {
        assertEquals(WorkflowData.DataType.TEXT, WorkflowData.DataType.fromCode("text"));
        assertEquals(WorkflowData.DataType.IMAGE, WorkflowData.DataType.fromCode("image"));
        assertEquals(WorkflowData.DataType.VIDEO, WorkflowData.DataType.fromCode("video"));
        assertEquals(WorkflowData.DataType.AUDIO, WorkflowData.DataType.fromCode("audio"));
        assertEquals(WorkflowData.DataType.JSON, WorkflowData.DataType.fromCode("json"));
    }
    
    @Test
    @DisplayName("未知类型默认返回 TEXT")
    public void testUnknownDataTypeDefaultsToText() {
        assertEquals(WorkflowData.DataType.TEXT, WorkflowData.DataType.fromCode("unknown"));
        assertEquals(WorkflowData.DataType.TEXT, WorkflowData.DataType.fromCode(null));
        assertEquals(WorkflowData.DataType.TEXT, WorkflowData.DataType.fromCode(""));
    }
    
    @Test
    @DisplayName("创建空数据对象")
    public void testCreateEmptyData() {
        WorkflowData data = new WorkflowData();
        
        assertNull(data.getType());
        assertNull(data.getContent());
        assertNull(data.getMetadata());
        assertNull(data.getSourceNode());
    }
    
    @Test
    @DisplayName("设置数据类型")
    public void testSetType() {
        WorkflowData data = new WorkflowData();
        
        data.setType(WorkflowData.DataType.IMAGE);
        
        assertEquals(WorkflowData.DataType.IMAGE, data.getType());
    }
    
    @Test
    @DisplayName("设置内容")
    public void testSetContent() {
        WorkflowData data = new WorkflowData();
        String content = "test content";
        
        data.setContent(content);
        
        assertEquals(content, data.getContent());
    }
    
    @Test
    @DisplayName("设置元数据")
    public void testSetMetadata() {
        WorkflowData data = new WorkflowData();
        Map<String, Object> metadata = Map.of("key", "value");
        
        data.setMetadata(metadata);
        
        assertEquals(metadata, data.getMetadata());
        assertEquals("value", data.getMetadata().get("key"));
    }
    
    @Test
    @DisplayName("设置源节点")
    public void testSetSourceNode() {
        WorkflowData data = new WorkflowData();
        
        data.setSourceNode("node-123");
        
        assertEquals("node-123", data.getSourceNode());
    }
    
    @Test
    @DisplayName("完整数据对象测试")
    public void testFullDataObject() {
        Map<String, Object> metadata = Map.of("width", 1920, "height", 1080);
        WorkflowData data = new WorkflowData(
            WorkflowData.DataType.VIDEO,
            "https://example.com/video.mp4",
            metadata,
            "encoder-node"
        );
        
        assertEquals(WorkflowData.DataType.VIDEO, data.getType());
        assertEquals("https://example.com/video.mp4", data.getContent());
        assertEquals(1920, data.getMetadata().get("width"));
        assertEquals(1080, data.getMetadata().get("height"));
        assertEquals("encoder-node", data.getSourceNode());
    }
    
    @Test
    @DisplayName("图片数据完整测试")
    public void testImageDataFull() {
        WorkflowData data = WorkflowData.image("https://example.com/img.png", 800, 600);
        
        assertEquals(WorkflowData.DataType.IMAGE, data.getType());
        assertEquals("https://example.com/img.png", data.getContent());
        assertEquals(800, data.getMetadata().get("width"));
        assertEquals(600, data.getMetadata().get("height"));
    }
    
    @Test
    @DisplayName("视频数据完整测试")
    public void testVideoDataFull() {
        WorkflowData data = WorkflowData.video("https://example.com/vid.mp4", 120, 30);
        
        assertEquals(WorkflowData.DataType.VIDEO, data.getType());
        assertEquals("https://example.com/vid.mp4", data.getContent());
        assertEquals(120, data.getMetadata().get("duration"));
        assertEquals(30, data.getMetadata().get("fps"));
    }
    
    @Test
    @DisplayName("音频数据完整测试")
    public void testAudioDataFull() {
        WorkflowData data = WorkflowData.audio("https://example.com/audio.wav", 240, "wav");
        
        assertEquals(WorkflowData.DataType.AUDIO, data.getType());
        assertEquals("https://example.com/audio.wav", data.getContent());
        assertEquals(240, data.getMetadata().get("duration"));
        assertEquals("wav", data.getMetadata().get("format"));
    }
    
    @Test
    @DisplayName("修改数据内容")
    public void testModifyContent() {
        WorkflowData data = WorkflowData.text("original");
        
        data.setContent("modified");
        
        assertEquals("modified", data.getContent());
    }
    
    @Test
    @DisplayName("修改元数据")
    public void testModifyMetadata() {
        WorkflowData data = WorkflowData.image("url", 100, 100);
        
        Map<String, Object> newMetadata = new HashMap<>();
        newMetadata.put("format", "png");
        data.setMetadata(newMetadata);
        
        assertEquals("png", data.getMetadata().get("format"));
    }
}
