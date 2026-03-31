package com.ben.workflow.adapter;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 生成请求（统一格式）
 */


public class GenerationRequest {

    /** 提示词 */
    private String prompt;

    /** 反向提示词 */
    private String negativePrompt;

    /** 输入图片 URL 列表（图生图/视频参考） */
    private List<String> inputImages;

    /** 输入视频 URL 列表（视频生视频） */
    private List<String> inputVideos;

    /** 模型特定参数 */
    private Map<String, Object> params;

    /** 回调 URL（可选） */
    private String callbackUrl;

    /** 优先级（1-10，10 最高） */
    private Integer priority;
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getNegativePrompt() { return negativePrompt; }
    public void setNegativePrompt(String negativePrompt) { this.negativePrompt = negativePrompt; }
    public List<String> getInputImages() { return inputImages; }
    public void setInputImages(List<String> inputImages) { this.inputImages = inputImages; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public List<String> getInputVideos() { return inputVideos; }
    public void setInputVideos(List<String> inputVideos) { this.inputVideos = inputVideos; }
}
