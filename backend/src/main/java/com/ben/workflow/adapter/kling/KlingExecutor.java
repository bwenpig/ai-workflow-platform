package com.ben.workflow.adapter.kling;

import com.ben.workflow.spi.ModelExecutor;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import com.ben.workflow.spi.NodeComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * 可灵 AI 文生视频模型执行器
 */
@NodeComponent(value = "kling", name = "可灵 AI", description = "可灵文生视频模型")
public class KlingExecutor implements ModelExecutor {

    @Override
    public String getType() { return "kling"; }

    @Override
    public ModelExecutionResult execute(ModelExecutionContext context) {
        try {
            Map<String, Object> config = context != null ? context.getConfig() : null;
            if (config == null) config = new HashMap<>();
            String prompt = (String) config.getOrDefault("prompt", "");
            Double duration = (Double) config.getOrDefault("duration", 5.0);
            Integer fps = (Integer) config.getOrDefault("fps", 24);
            
            Map<String, Object> result = new HashMap<>();
            result.put("type", "video");
            result.put("modelProvider", "kling");
            result.put("prompt", prompt);
            result.put("url", "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4");
            result.put("previewUrl", "https://via.placeholder.com/640x360.png?text=Kling+Video");
            result.put("duration", duration);
            result.put("fps", fps);
            result.put("width", 1280);
            result.put("height", 720);
            
            return ModelExecutionResult.success(result);
        } catch (Exception e) {
            return ModelExecutionResult.failure("Kling 执行失败：" + e.getMessage());
        }
    }
}
