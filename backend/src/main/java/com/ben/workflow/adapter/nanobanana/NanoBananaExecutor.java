package com.ben.workflow.adapter.nanobanana;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.spi.NodeComponent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * NanoBanana 图像生成模型执行器
 */
@NodeComponent(value = "nanobanana", name = "NanoBanana", description = "NanoBanana 文生图模型")
public class NanoBananaExecutor implements NodeExecutor {

    @Override
    public String getType() { return "nanobanana"; }

    @Override
    public String getName() { return "NanoBanana"; }

    @Override
    public String getDescription() { return "NanoBanana 文生图模型"; }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) throws Exception {
        try {
            LocalDateTime startTime = LocalDateTime.now();
            Map<String, Object> config = context != null ? context.getInputs() : null;
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
            
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.success(context != null ? context.getNodeId() : "unknown", result, startTime, endTime);
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.failed(context != null ? context.getNodeId() : "unknown", e, LocalDateTime.now(), endTime);
        }
    }
}
