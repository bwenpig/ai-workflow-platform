package com.ben.workflow.adapter;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ModelProvider 到 NodeExecutor 的适配器
 * 将 adapter 层的 ModelProvider 适配到 dag-scheduler 的 NodeExecutor 接口
 */
@Component
public class ModelProviderAdapter implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ModelProviderAdapter.class);

    private final ModelProviderRegistry registry;

    public ModelProviderAdapter(ModelProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getType() {
        return "adapter"; // 通用适配器，实际类型由 context 决定
    }

    @Override
    public String getName() {
        return "Model Provider Adapter";
    }

    @Override
    public String getDescription() {
        return "通用模型提供者适配器";
    }

    /**
     * 执行模型调用
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) throws Exception {
        LocalDateTime startTime = LocalDateTime.now();
        String nodeId = context != null ? context.getNodeId() : "unknown";
        
        String nodeType = context != null ? context.getNodeType() : null;
        if (nodeType == null) {
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.failed(nodeId, "节点类型为空", null, startTime, endTime);
        }

        // 获取对应的 ModelProvider
        ModelProvider provider = registry.getProvider(nodeType)
                .orElse(null);

        if (provider == null) {
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.failed(nodeId, "未找到模型提供者：" + nodeType, null, startTime, endTime);
        }

        try {
            // 构建 GenerationRequest
            GenerationRequest request = buildRequest(context);
            
            // 同步调用（block）
            GenerationResult generationResult = provider.generate(request)
                    .block(java.time.Duration.ofMinutes(5));

            if (generationResult == null) {
                LocalDateTime endTime = LocalDateTime.now();
                return NodeExecutionResult.failed(nodeId, "模型返回空结果", null, startTime, endTime);
            }

            if (generationResult.getStatus() == ModelProvider.TaskStatus.FAILED) {
                LocalDateTime endTime = LocalDateTime.now();
                return NodeExecutionResult.failed(nodeId, generationResult.getErrorMessage(), null, startTime, endTime);
            }

            // 转换为 Map 结果
            Map<String, Object> data = convertToData(generationResult, provider);
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.success(nodeId, data, startTime, endTime);

        } catch (Exception e) {
            log.error("模型执行失败：nodeType={}, error={}", nodeType, e.getMessage(), e);
            LocalDateTime endTime = LocalDateTime.now();
            return NodeExecutionResult.failed(nodeId, e, startTime, endTime);
        }
    }

    private GenerationRequest buildRequest(NodeExecutionContext context) {
        GenerationRequest request = new GenerationRequest();
        
        // 从 inputs 获取 prompt
        Object promptObj = context.getInputs().get("prompt");
        if (promptObj != null) {
            request.setPrompt(promptObj.toString());
        }

        // 反向提示词
        Object negativePromptObj = context.getInputs().get("negativePrompt");
        if (negativePromptObj != null) {
            request.setNegativePrompt(negativePromptObj.toString());
        }

        // 参数
        Map<String, Object> params = new HashMap<>(context.getInputs());
        request.setParams(params);

        return request;
    }

    private Map<String, Object> convertToData(GenerationResult generationResult, ModelProvider provider) {
        Map<String, Object> data = new HashMap<>();
        
        // 基本信息
        data.put("taskId", generationResult.getTaskId());
        data.put("status", generationResult.getStatus().name());
        
        // 输出 URLs
        List<String> outputUrls = generationResult.getOutputUrls();
        if (outputUrls != null && !outputUrls.isEmpty()) {
            data.put("url", outputUrls.get(0));
            data.put("outputUrls", outputUrls);
        }
        
        // 预览图
        if (generationResult.getPreviewUrl() != null) {
            data.put("previewUrl", generationResult.getPreviewUrl());
        }
        
        // 视频特定字段
        if (generationResult.getDuration() != null) {
            data.put("duration", generationResult.getDuration());
        }
        if (generationResult.getFps() != null) {
            data.put("fps", generationResult.getFps());
        }
        
        // 图片特定字段
        if (generationResult.getMetadata() != null) {
            Object width = generationResult.getMetadata().get("width");
            Object height = generationResult.getMetadata().get("height");
            if (width != null) data.put("width", width);
            if (height != null) data.put("height", height);
        }
        
        // 模型信息
        data.put("modelProvider", provider.getClass().getSimpleName().replace("Executor", "").toLowerCase());
        data.put("modelName", provider.getModelName());
        
        // 耗时
        if (generationResult.getDurationMs() != null) {
            data.put("durationMs", generationResult.getDurationMs());
        }

        return data;
    }
}
