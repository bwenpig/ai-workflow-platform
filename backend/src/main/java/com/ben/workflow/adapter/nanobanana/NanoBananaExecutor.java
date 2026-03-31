package com.ben.workflow.adapter.nanobanana;

import com.ben.workflow.spi.ModelExecutor;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import com.ben.workflow.spi.NodeComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * NanoBanana 图像生成模型执行器
 */
@NodeComponent(value = "nanobanana", name = "NanoBanana", description = "NanoBanana 文生图模型")
public class NanoBananaExecutor implements ModelExecutor {

    @Override
    public String getType() { return "nanobanana"; }

    @Override
    public ModelExecutionResult execute(ModelExecutionContext context) {
        try {
            Map<String, Object> config = context != null ? context.getConfig() : null;
            if (config == null) config = new HashMap<>();
            String prompt = (String) config.getOrDefault("prompt", "");
            Integer width = (Integer) config.getOrDefault("width", 1024);
            Integer height = (Integer) config.getOrDefault("height", 1024);
            
            Map<String, Object> result = new HashMap<>();
            result.put("type", "image");
            result.put("modelProvider", "nanobanana");
            result.put("prompt", prompt);
            result.put("url", "https://via.placeholder.com/1024x1024.png?text=NanoBanana+Image");
            result.put("width", width);
            result.put("height", height);
            
            return ModelExecutionResult.success(result);
        } catch (Exception e) {
            return ModelExecutionResult.failure("NanoBanana 执行失败：" + e.getMessage());
        }
    }
}
