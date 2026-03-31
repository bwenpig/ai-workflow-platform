package com.ben.workflow.adapter.seedance;

import com.ben.workflow.spi.ModelExecutor;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import com.ben.workflow.spi.NodeComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Seedance 视频生成模型执行器
 */
@NodeComponent(value = "seedance", name = "Seedance", description = "Seedance 文生视频模型")
public class SeedanceExecutor implements ModelExecutor {

    @Override
    public String getType() { return "seedance"; }

    @Override
    public ModelExecutionResult execute(ModelExecutionContext context) {
        try {
            Map<String, Object> config = context != null ? context.getConfig() : null;
            if (config == null) config = new HashMap<>();
            String prompt = (String) config.getOrDefault("prompt", "");
            Double duration = (Double) config.getOrDefault("duration", 10.0);
            Integer fps = (Integer) config.getOrDefault("fps", 30);
            
            Map<String, Object> result = new HashMap<>();
            result.put("type", "video");
            result.put("modelProvider", "seedance");
            result.put("prompt", prompt);
            result.put("url", "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4");
            result.put("duration", duration);
            result.put("fps", fps);
            
            return ModelExecutionResult.success(result);
        } catch (Exception e) {
            return ModelExecutionResult.failure("Seedance 执行失败：" + e.getMessage());
        }
    }
}
