package com.ben.workflow.adapter.wan;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.spi.NodeComponent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Wan AI 图像生成模型执行器
 */
@NodeComponent(value = "wan", name = "Wan AI", description = "Wan 文生图模型")
@Component
public class WanExecutor implements NodeExecutor {

    @Override
    public String getType() {
        return "wan";
    }

    @Override
    public String getName() {
        return "Wan AI";
    }

    @Override
    public String getDescription() {
        return "Wan 文生图模型";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) throws Exception {
        try {
            LocalDateTime startTime = LocalDateTime.now();
            Map<String, Object> config = context != null ? context.getInputs() : null;
            String prompt = config != null ? (String) config.get("prompt") : "";
            
            Map<String, Object> result = new HashMap<>();
            result.put("type", "image");
            result.put("modelProvider", "wan");
            result.put("prompt", prompt != null ? prompt : "");
            result.put("url", "https://via.placeholder.com/1024x1024.png?text=Wan+Image");
            result.put("width", 1024);
            result.put("height", 1024);
            result.put("steps", 30);
            
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.success(context != null ? context.getNodeId() : "unknown", result, startTime, endTime);
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.failed(context != null ? context.getNodeId() : "unknown", e, LocalDateTime.now(), endTime);
        }
    }
}
