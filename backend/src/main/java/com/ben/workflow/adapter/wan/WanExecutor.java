package com.ben.workflow.adapter.wan;

import com.ben.workflow.spi.ModelExecutor;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import com.ben.workflow.spi.NodeComponent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Wan AI 图像生成模型执行器
 */
@NodeComponent(value = "wan", name = "Wan AI", description = "Wan 文生图模型")
@Component
public class WanExecutor implements ModelExecutor {

    @Override
    public String getType() {
        return "wan";
    }

    @Override
    public ModelExecutionResult execute(ModelExecutionContext context) {
        try {
            Map<String, Object> config = context.getConfig();
            String prompt = config != null ? (String) config.get("prompt") : "";
            
            Map<String, Object> result = new HashMap<>();
            result.put("type", "image");
            result.put("modelProvider", "wan");
            result.put("prompt", prompt != null ? prompt : "");
            result.put("url", "https://via.placeholder.com/1024x1024.png?text=Wan+Image");
            result.put("width", 1024);
            result.put("height", 1024);
            result.put("steps", 30);
            
            return ModelExecutionResult.success(result);
        } catch (Exception e) {
            return ModelExecutionResult.failure("Wan 执行失败：" + e.getMessage());
        }
    }
}
