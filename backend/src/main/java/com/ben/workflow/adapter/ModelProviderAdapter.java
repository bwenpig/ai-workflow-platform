package com.ben.workflow.adapter;

import com.ben.workflow.spi.ModelExecutor;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ModelProvider 到 ModelExecutor 的适配器
 * 将 adapter 层的 ModelProvider 适配到 spi 层的 ModelExecutor 接口
 */
@Component
public class ModelProviderAdapter implements ModelExecutor {

    private static final Logger log = LoggerFactory.getLogger(ModelProviderAdapter.class);

    private final ModelProviderRegistry registry;

    public ModelProviderAdapter(ModelProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getType() {
        return "adapter"; // 通用适配器，实际类型由 context 决定
    }

    /**
     * 执行模型调用
     */
    public ModelExecutionResult execute(ModelExecutionContext context) {
        ModelExecutionResult result = new ModelExecutionResult();
        
        String nodeType = context.getNodeType();
        if (nodeType == null) {
            result.setSuccess(false);
            result.setError("节点类型为空");
            return result;
        }

        // 获取对应的 ModelProvider
        ModelProvider provider = registry.getProvider(nodeType)
                .orElse(null);

        if (provider == null) {
            result.setSuccess(false);
            result.setError("未找到模型提供者：" + nodeType);
            return result;
        }

        try {
            // 构建 GenerationRequest
            GenerationRequest request = buildRequest(context);
            
            // 同步调用（block）
            GenerationResult generationResult = provider.generate(request)
                    .block(java.time.Duration.ofMinutes(5));

            if (generationResult == null) {
                result.setSuccess(false);
                result.setError("模型返回空结果");
                return result;
            }

            if (generationResult.getStatus() == ModelProvider.TaskStatus.FAILED) {
                result.setSuccess(false);
                result.setError(generationResult.getErrorMessage());
                return result;
            }

            // 转换为 Map 结果
            Map<String, Object> data = convertToData(generationResult, provider);
            result.setData(data);
            result.setSuccess(true);

        } catch (Exception e) {
            log.error("模型执行失败：nodeType={}, error={}", nodeType, e.getMessage(), e);
            result.setSuccess(false);
            result.setError("模型执行异常：" + e.getMessage());
        }

        return result;
    }

    private GenerationRequest buildRequest(ModelExecutionContext context) {
        GenerationRequest request = new GenerationRequest();
        
        // 从 inputs 或 config 获取 prompt
        Object promptObj = context.getInputs().get("prompt");
        if (promptObj == null && context.getConfig() != null) {
            promptObj = context.getConfig().get("prompt");
        }
        
        if (promptObj != null) {
            request.setPrompt(promptObj.toString());
        }

        // 反向提示词
        Object negativePromptObj = context.getInputs().get("negativePrompt");
        if (negativePromptObj == null && context.getConfig() != null) {
            negativePromptObj = context.getConfig().get("negativePrompt");
        }
        if (negativePromptObj != null) {
            request.setNegativePrompt(negativePromptObj.toString());
        }

        // 参数
        Map<String, Object> params = new HashMap<>();
        if (context.getConfig() != null) {
            params.putAll(context.getConfig());
        }
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
