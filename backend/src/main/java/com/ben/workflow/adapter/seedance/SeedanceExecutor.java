package com.ben.workflow.adapter.seedance;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.spi.NodeComponent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Seedance 视频生成模型执行器
 */
@NodeComponent(value = "seedance", name = "Seedance", description = "Seedance 文生视频模型")
public class SeedanceExecutor implements NodeExecutor {

    @Override
    public String getType() { return "seedance"; }

    @Override
    public String getName() { return "Seedance"; }

    @Override
    public String getDescription() { return "Seedance 文生视频模型"; }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) throws Exception {
        try {
            LocalDateTime startTime = LocalDateTime.now();
            Map<String, Object> config = context != null ? context.getInputs() : null;
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
            
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.success(context != null ? context.getNodeId() : "unknown", result, startTime, endTime);
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.failed(context != null ? context.getNodeId() : "unknown", e, LocalDateTime.now(), endTime);
        }
    }
}
