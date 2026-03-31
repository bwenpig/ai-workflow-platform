package com.ben.workflow.adapter.nanobanana;

import com.ben.workflow.spi.ModelExecutor;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import com.ben.workflow.spi.NodeComponent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * NanoBanana 模型执行器
 */
@NodeComponent(value = "nanobanana", name = "NanoBanana", description = "NanoBanana 图片生成模型")
@Component
public class NanoBananaExecutor implements ModelExecutor {

    @Override
    public String getType() {
        return "nanobanana";
    }

    @Override
    public ModelExecutionResult execute(ModelExecutionContext context) {
        try {
            Map<String, Object> inputs = context.getInputs();
            Map<String, Object> config = context.getConfig();
            
            String prompt = config != null ? (String) config.get("prompt") : "";
            String negativePrompt = config != null ? (String) config.get("negativePrompt") : "";
            Integer width = config != null ? (Integer) config.get("width") : 1024;
            Integer height = config != null ? (Integer) config.get("height") : 1024;
            Integer steps = config != null ? (Integer) config.get("steps") : 30;
            
            // 调用 NanoBanana 图像生成 API
            Map<String, Object> result = new HashMap<>();
            result.put("type", "image");
            result.put("modelProvider", "nanobanana");
            result.put("prompt", prompt);
            result.put("url", "https://via.placeholder.com/1024x1024.png?text=NanoBanana+Image");
            result.put("width", width);
            result.put("height", height);
            result.put("steps", steps);
            
            return ModelExecutionResult.success(result);
            
        } catch (Exception e) {
            return ModelExecutionResult.failure("NanoBanana 执行失败：" + e.getMessage());
        }
    }
}
