package com.ben.workflow.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GenerationRequest DTO 测试
 */
public class GenerationRequestTest {
    
    @Test
    @DisplayName("创建空 GenerationRequest 对象")
    public void testCreateEmptyRequest() {
        GenerationRequest request = new GenerationRequest();
        
        assertNull(request.getPrompt());
        assertNull(request.getNegativePrompt());
        assertNull(request.getInputImages());
        assertNull(request.getInputVideos());
        assertNull(request.getParams());
        assertNull(request.getCallbackUrl());
        assertNull(request.getPriority());
    }
    
    @Test
    @DisplayName("设置和获取提示词")
    public void testSetPrompt() {
        GenerationRequest request = new GenerationRequest();
        
        request.setPrompt("一个美丽的日落");
        
        assertEquals("一个美丽的日落", request.getPrompt());
    }
    
    @Test
    @DisplayName("设置空提示词")
    public void testSetEmptyPrompt() {
        GenerationRequest request = new GenerationRequest();
        
        request.setPrompt("");
        
        assertEquals("", request.getPrompt());
    }
    
    @Test
    @DisplayName("设置 null 提示词")
    public void testSetNullPrompt() {
        GenerationRequest request = new GenerationRequest();
        
        request.setPrompt(null);
        
        assertNull(request.getPrompt());
    }
    
    @Test
    @DisplayName("设置和获取反向提示词")
    public void testSetNegativePrompt() {
        GenerationRequest request = new GenerationRequest();
        
        request.setNegativePrompt("模糊，低质量");
        
        assertEquals("模糊，低质量", request.getNegativePrompt());
    }
    
    @Test
    @DisplayName("设置和获取输入图片列表")
    public void testSetInputImages() {
        GenerationRequest request = new GenerationRequest();
        List<String> images = Arrays.asList("https://example.com/img1.jpg", "https://example.com/img2.jpg");
        
        request.setInputImages(images);
        
        assertEquals(2, request.getInputImages().size());
        assertEquals("https://example.com/img1.jpg", request.getInputImages().get(0));
        assertEquals("https://example.com/img2.jpg", request.getInputImages().get(1));
    }
    
    @Test
    @DisplayName("设置空图片列表")
    public void testSetEmptyInputImages() {
        GenerationRequest request = new GenerationRequest();
        
        request.setInputImages(Arrays.asList());
        
        assertTrue(request.getInputImages().isEmpty());
    }
    
    @Test
    @DisplayName("设置和获取输入视频列表")
    public void testSetInputVideos() {
        GenerationRequest request = new GenerationRequest();
        List<String> videos = Arrays.asList("https://example.com/vid1.mp4");
        
        request.setInputVideos(videos);
        
        assertEquals(1, request.getInputVideos().size());
        assertEquals("https://example.com/vid1.mp4", request.getInputVideos().get(0));
    }
    
    @Test
    @DisplayName("设置和获取参数")
    public void testSetParams() {
        GenerationRequest request = new GenerationRequest();
        Map<String, Object> params = new HashMap<>();
        params.put("steps", 50);
        params.put("guidance_scale", 7.5);
        
        request.setParams(params);
        
        assertEquals(2, request.getParams().size());
        assertEquals(50, request.getParams().get("steps"));
        assertEquals(7.5, request.getParams().get("guidance_scale"));
    }
    
    @Test
    @DisplayName("设置空参数")
    public void testSetEmptyParams() {
        GenerationRequest request = new GenerationRequest();
        
        request.setParams(new HashMap<>());
        
        assertTrue(request.getParams().isEmpty());
    }
    
    @Test
    @DisplayName("设置和获取回调 URL")
    public void testSetCallbackUrl() {
        GenerationRequest request = new GenerationRequest();
        
        request.setCallbackUrl("https://example.com/callback");
        
        assertEquals("https://example.com/callback", request.getCallbackUrl());
    }
    
    @Test
    @DisplayName("设置和获取优先级")
    public void testSetPriority() {
        GenerationRequest request = new GenerationRequest();
        
        request.setPriority(10);
        
        assertEquals(10, request.getPriority());
        
        request.setPriority(1);
        
        assertEquals(1, request.getPriority());
    }
    
    @Test
    @DisplayName("设置 null 优先级")
    public void testSetNullPriority() {
        GenerationRequest request = new GenerationRequest();
        
        request.setPriority(null);
        
        assertNull(request.getPriority());
    }
    
    @Test
    @DisplayName("完整请求对象测试")
    public void testFullRequest() {
        GenerationRequest request = new GenerationRequest();
        
        request.setPrompt("生成一个视频");
        request.setNegativePrompt("低质量");
        request.setInputImages(Arrays.asList("img.jpg"));
        request.setInputVideos(Arrays.asList("vid.mp4"));
        request.setParams(Map.of("steps", 30));
        request.setCallbackUrl("https://example.com/callback");
        request.setPriority(5);
        
        assertEquals("生成一个视频", request.getPrompt());
        assertEquals("低质量", request.getNegativePrompt());
        assertEquals(1, request.getInputImages().size());
        assertEquals(1, request.getInputVideos().size());
        assertEquals(30, request.getParams().get("steps"));
        assertEquals("https://example.com/callback", request.getCallbackUrl());
        assertEquals(5, request.getPriority());
    }
    
    @Test
    @DisplayName("仅设置提示词")
    public void testPromptOnly() {
        GenerationRequest request = new GenerationRequest();
        
        request.setPrompt("简单提示词");
        
        assertEquals("简单提示词", request.getPrompt());
        assertNull(request.getNegativePrompt());
        assertNull(request.getInputImages());
        assertNull(request.getParams());
    }
}
