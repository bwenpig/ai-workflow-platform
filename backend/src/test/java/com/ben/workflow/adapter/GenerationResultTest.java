package com.ben.workflow.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GenerationResult DTO 测试
 */
public class GenerationResultTest {
    
    @Test
    @DisplayName("创建空 GenerationResult 对象")
    public void testCreateEmptyResult() {
        GenerationResult result = new GenerationResult();
        
        assertNull(result.getTaskId());
        assertNull(result.getOutputUrls());
        assertNull(result.getMetadata());
        assertNull(result.getStatus());
        assertNull(result.getErrorMessage());
        assertNull(result.getDurationMs());
        assertNull(result.getPreviewUrl());
        assertNull(result.getDuration());
        assertNull(result.getFps());
    }
    
    @Test
    @DisplayName("设置和获取任务 ID")
    public void testSetTaskId() {
        GenerationResult result = new GenerationResult();
        
        result.setTaskId("task-123");
        
        assertEquals("task-123", result.getTaskId());
    }
    
    @Test
    @DisplayName("设置和获取输出 URL 列表")
    public void testSetOutputUrls() {
        GenerationResult result = new GenerationResult();
        List<String> urls = Arrays.asList("https://example.com/out1.jpg", "https://example.com/out2.jpg");
        
        result.setOutputUrls(urls);
        
        assertEquals(2, result.getOutputUrls().size());
        assertEquals("https://example.com/out1.jpg", result.getOutputUrls().get(0));
        assertEquals("https://example.com/out2.jpg", result.getOutputUrls().get(1));
    }
    
    @Test
    @DisplayName("设置空输出 URL 列表")
    public void testSetEmptyOutputUrls() {
        GenerationResult result = new GenerationResult();
        
        result.setOutputUrls(Arrays.asList());
        
        assertTrue(result.getOutputUrls().isEmpty());
    }
    
    @Test
    @DisplayName("设置和获取元数据")
    public void testSetMetadata() {
        GenerationResult result = new GenerationResult();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("width", 1920);
        metadata.put("height", 1080);
        
        result.setMetadata(metadata);
        
        assertEquals(2, result.getMetadata().size());
        assertEquals(1920, result.getMetadata().get("width"));
        assertEquals(1080, result.getMetadata().get("height"));
    }
    
    @Test
    @DisplayName("设置空元数据")
    public void testSetEmptyMetadata() {
        GenerationResult result = new GenerationResult();
        
        result.setMetadata(new HashMap<>());
        
        assertTrue(result.getMetadata().isEmpty());
    }
    
    @Test
    @DisplayName("设置和获取任务状态")
    public void testSetStatus() {
        GenerationResult result = new GenerationResult();
        
        result.setStatus(ModelProvider.TaskStatus.SUCCESS);
        
        assertEquals(ModelProvider.TaskStatus.SUCCESS, result.getStatus());
        
        result.setStatus(ModelProvider.TaskStatus.FAILED);
        
        assertEquals(ModelProvider.TaskStatus.FAILED, result.getStatus());
        
        result.setStatus(ModelProvider.TaskStatus.RUNNING);
        
        assertEquals(ModelProvider.TaskStatus.RUNNING, result.getStatus());
        
        result.setStatus(ModelProvider.TaskStatus.PENDING);
        
        assertEquals(ModelProvider.TaskStatus.PENDING, result.getStatus());
    }
    
    @Test
    @DisplayName("TaskStatus 枚举值测试")
    public void testTaskStatusEnumValues() {
        assertNotNull(ModelProvider.TaskStatus.PENDING);
        assertNotNull(ModelProvider.TaskStatus.RUNNING);
        assertNotNull(ModelProvider.TaskStatus.SUCCESS);
        assertNotNull(ModelProvider.TaskStatus.FAILED);
    }
    
    @Test
    @DisplayName("设置和获取错误信息")
    public void testSetErrorMessage() {
        GenerationResult result = new GenerationResult();
        
        result.setErrorMessage("生成失败：超时");
        
        assertEquals("生成失败：超时", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("设置 null 错误信息")
    public void testSetNullErrorMessage() {
        GenerationResult result = new GenerationResult();
        
        result.setErrorMessage(null);
        
        assertNull(result.getErrorMessage());
    }
    
    @Test
    @DisplayName("设置和获取耗时")
    public void testSetDurationMs() {
        GenerationResult result = new GenerationResult();
        
        result.setDurationMs(5000L);
        
        assertEquals(5000L, result.getDurationMs());
    }
    
    @Test
    @DisplayName("设置和获取预览图 URL")
    public void testSetPreviewUrl() {
        GenerationResult result = new GenerationResult();
        
        result.setPreviewUrl("https://example.com/preview.jpg");
        
        assertEquals("https://example.com/preview.jpg", result.getPreviewUrl());
    }
    
    @Test
    @DisplayName("设置和获取视频时长")
    public void testSetDuration() {
        GenerationResult result = new GenerationResult();
        
        result.setDuration(30);
        
        assertEquals(30, result.getDuration());
    }
    
    @Test
    @DisplayName("设置和获取帧率")
    public void testSetFps() {
        GenerationResult result = new GenerationResult();
        
        result.setFps(24);
        
        assertEquals(24, result.getFps());
        
        result.setFps(30);
        
        assertEquals(30, result.getFps());
    }
    
    @Test
    @DisplayName("成功结果测试")
    public void testSuccessResult() {
        GenerationResult result = new GenerationResult();
        
        result.setTaskId("task-1");
        result.setOutputUrls(Arrays.asList("result.jpg"));
        result.setStatus(ModelProvider.TaskStatus.SUCCESS);
        result.setDurationMs(3000L);
        
        assertEquals("task-1", result.getTaskId());
        assertEquals(1, result.getOutputUrls().size());
        assertEquals(ModelProvider.TaskStatus.SUCCESS, result.getStatus());
        assertEquals(3000L, result.getDurationMs());
        assertNull(result.getErrorMessage());
    }
    
    @Test
    @DisplayName("失败结果测试")
    public void testFailedResult() {
        GenerationResult result = new GenerationResult();
        
        result.setTaskId("task-2");
        result.setStatus(ModelProvider.TaskStatus.FAILED);
        result.setErrorMessage("内存不足");
        
        assertEquals("task-2", result.getTaskId());
        assertEquals(ModelProvider.TaskStatus.FAILED, result.getStatus());
        assertEquals("内存不足", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("视频生成结果测试")
    public void testVideoResult() {
        GenerationResult result = new GenerationResult();
        
        result.setTaskId("video-task");
        result.setOutputUrls(Arrays.asList("video.mp4"));
        result.setPreviewUrl("preview.jpg");
        result.setDuration(30);
        result.setFps(24);
        result.setStatus(ModelProvider.TaskStatus.SUCCESS);
        
        assertEquals("video-task", result.getTaskId());
        assertEquals("video.mp4", result.getOutputUrls().get(0));
        assertEquals("preview.jpg", result.getPreviewUrl());
        assertEquals(30, result.getDuration());
        assertEquals(24, result.getFps());
    }
    
    @Test
    @DisplayName("完整结果对象测试")
    public void testFullResult() {
        GenerationResult result = new GenerationResult();
        
        result.setTaskId("task-full");
        result.setOutputUrls(Arrays.asList("out1.jpg", "out2.jpg"));
        result.setMetadata(Map.of("model", "sd-xl"));
        result.setStatus(ModelProvider.TaskStatus.SUCCESS);
        result.setErrorMessage(null);
        result.setDurationMs(10000L);
        result.setPreviewUrl("preview.jpg");
        result.setDuration(60);
        result.setFps(30);
        
        assertEquals("task-full", result.getTaskId());
        assertEquals(2, result.getOutputUrls().size());
        assertEquals("sd-xl", result.getMetadata().get("model"));
        assertEquals(ModelProvider.TaskStatus.SUCCESS, result.getStatus());
        assertNull(result.getErrorMessage());
        assertEquals(10000L, result.getDurationMs());
        assertEquals("preview.jpg", result.getPreviewUrl());
        assertEquals(60, result.getDuration());
        assertEquals(30, result.getFps());
    }
}
