package com.ben.workflow.adapter.wan;

import com.ben.workflow.adapter.GenerationRequest;
import com.ben.workflow.adapter.GenerationResult;
import com.ben.workflow.adapter.ModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Wan AI 执行器 - 阿里云通义万相
 * API 文档：https://dashscope.aliyuncs.com/api/v1
 */
@Component
public class WanExecutor implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(WanExecutor.class);

    private final WebClient webClient;
    private final String apiUrl;
    private final String apiKey;
    private final String modelVersion;

    public WanExecutor(
            WebClient.Builder webClientBuilder,
            @Value("${model.wan.api-url:https://dashscope.aliyuncs.com/api/v1}") String apiUrl,
            @Value("${model.wan.api-key:}") String apiKey,
            @Value("${model.wan.model-version:wanx-v1}") String modelVersion) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.modelVersion = modelVersion;
        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public ModelType getType() {
        return ModelType.IMAGE;
    }

    @Override
    public String getModelName() {
        return modelVersion;
    }

    @Override
    public Mono<GenerationResult> generate(GenerationRequest request) {
        return Mono.fromCallable(() -> doGenerate(request))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("Wan API 调用失败：{}", e.getMessage(), e);
                    GenerationResult errorResult = new GenerationResult();
                    errorResult.setStatus(ModelProvider.TaskStatus.FAILED);
                    errorResult.setErrorMessage("Wan API 调用失败：" + e.getMessage());
                    errorResult.setDurationMs(Duration.ofSeconds(1).toMillis());
                    return Mono.just(errorResult);
                });
    }

    private GenerationResult doGenerate(GenerationRequest request) {
        Instant startTime = Instant.now();
        GenerationResult result = new GenerationResult();
        result.setStatus(ModelProvider.TaskStatus.PENDING);

        try {
            Map<String, Object> requestBody = buildRequestBody(request);
            
            Map<String, Object> response = webClient.post()
                    .uri("/services/aigc/text-generation/generation")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(30));

            if (response == null) {
                throw new RuntimeException("Wan API 返回空响应");
            }

            String taskId = extractTaskId(response);
            if (taskId == null) {
                throw new RuntimeException("无法从响应中提取任务 ID: " + response);
            }

            result.setTaskId(taskId);
            result.setStatus(ModelProvider.TaskStatus.RUNNING);
            log.info("Wan 任务已提交：taskId={}", taskId);

            Map<String, Object> finalResult = pollTaskStatus(taskId);
            parseResult(finalResult, result);
            result.setStatus(ModelProvider.TaskStatus.SUCCESS);

        } catch (WebClientResponseException e) {
            log.error("Wan API HTTP 错误：{} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            result.setStatus(ModelProvider.TaskStatus.FAILED);
            result.setErrorMessage("Wan API 错误：" + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Wan 生成失败：{}", e.getMessage(), e);
            result.setStatus(ModelProvider.TaskStatus.FAILED);
            result.setErrorMessage("Wan 生成失败：" + e.getMessage());
        }

        result.setDurationMs(Duration.between(startTime, Instant.now()).toMillis());
        return result;
    }

    private Map<String, Object> buildRequestBody(GenerationRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelVersion);
        
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", request.getPrompt());
        
        if (request.getNegativePrompt() != null) {
            input.put("negative_prompt", request.getNegativePrompt());
        }

        Map<String, Object> params = request.getParams();
        if (params != null) {
            if (params.containsKey("size")) input.put("size", params.get("size"));
            if (params.containsKey("width")) input.put("width", params.get("width"));
            if (params.containsKey("height")) input.put("height", params.get("height"));
            if (params.containsKey("style")) input.put("style", params.get("style"));
            if (params.containsKey("seed")) input.put("seed", params.get("seed"));
            if (params.containsKey("steps")) input.put("steps", params.get("steps"));
        }

        if (!input.containsKey("size") && !input.containsKey("width")) {
            input.put("width", 1024);
            input.put("height", 1024);
        }
        if (!input.containsKey("style")) input.put("style", "<auto>");

        body.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        if (params != null) {
            if (params.containsKey("steps")) parameters.put("steps", params.get("steps"));
            if (params.containsKey("guidance_scale")) parameters.put("guidance_scale", params.get("guidance_scale"));
        }
        if (!parameters.isEmpty()) {
            body.put("parameters", parameters);
        }

        return body;
    }

    private String extractTaskId(Map<String, Object> response) {
        if (response.containsKey("output")) {
            Object output = response.get("output");
            if (output instanceof Map) {
                Object taskId = ((Map<?, ?>) output).get("task_id");
                if (taskId != null) return taskId.toString();
            }
        }
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof Map) {
                Object taskId = ((Map<?, ?>) data).get("id");
                if (taskId != null) return taskId.toString();
            }
        }
        if (response.containsKey("id")) {
            return response.get("id").toString();
        }
        return null;
    }

    private Map<String, Object> pollTaskStatus(String taskId) throws InterruptedException {
        int maxRetries = 60;
        int retryDelay = 2000;

        for (int i = 0; i < maxRetries; i++) {
            Thread.sleep(retryDelay);

            Map<String, Object> statusResponse = webClient.get()
                    .uri("/tasks/{task_id}", taskId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));

            if (statusResponse == null) continue;

            String status = extractStatus(statusResponse);
            log.debug("Wan 任务状态：taskId={}, status={}", taskId, status);

            if ("success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status) || "succeeded".equalsIgnoreCase(status)) {
                return statusResponse;
            }
            if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                throw new RuntimeException("Wan 任务失败：" + extractErrorMessage(statusResponse));
            }
        }

        throw new RuntimeException("Wan 任务超时：taskId=" + taskId);
    }

    private String extractStatus(Map<String, Object> response) {
        if (response.containsKey("output")) {
            Object output = response.get("output");
            if (output instanceof Map) {
                Object status = ((Map<?, ?>) output).get("task_status");
                if (status != null) return status.toString();
            }
        }
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof Map) {
                Object status = ((Map<?, ?>) data).get("status");
                if (status != null) return status.toString();
            }
        }
        if (response.containsKey("status")) {
            return response.get("status").toString();
        }
        return "unknown";
    }

    private String extractErrorMessage(Map<String, Object> response) {
        if (response.containsKey("output")) {
            Object output = response.get("output");
            if (output instanceof Map) {
                Object error = ((Map<?, ?>) output).get("message");
                if (error != null) return error.toString();
            }
        }
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof Map) {
                Object error = ((Map<?, ?>) data).get("error");
                if (error != null) return error.toString();
            }
        }
        if (response.containsKey("error")) return response.get("error").toString();
        if (response.containsKey("message")) return response.get("message").toString();
        return "未知错误";
    }

    private void parseResult(Map<String, Object> response, GenerationResult result) {
        List<String> outputUrls = new ArrayList<>();
        
        if (response.containsKey("output")) {
            Object output = response.get("output");
            if (output instanceof Map) {
                Map<?, ?> outputMap = (Map<?, ?>) output;
                
                Object results = outputMap.get("results");
                if (results instanceof List) {
                    List<?> resultsList = (List<?>) results;
                    for (Object item : resultsList) {
                        if (item instanceof Map) {
                            Object url = ((Map<?, ?>) item).get("url");
                            if (url != null) outputUrls.add(url.toString());
                        }
                    }
                }
                
                if (outputUrls.isEmpty()) {
                    Object imageUrl = outputMap.get("image_url");
                    if (imageUrl != null) outputUrls.add(imageUrl.toString());
                }
                
                Object width = outputMap.get("width");
                Object height = outputMap.get("height");
                if (width != null && height != null) {
                    Map<String, Object> metadata = result.getMetadata();
                    if (metadata == null) metadata = new HashMap<>();
                    try {
                        metadata.put("width", Integer.parseInt(width.toString()));
                        metadata.put("height", Integer.parseInt(height.toString()));
                    } catch (NumberFormatException e) {}
                }
            }
        }

        result.setOutputUrls(outputUrls);
        
        Map<String, Object> metadata = result.getMetadata();
        if (metadata == null) metadata = new HashMap<>();
        metadata.put("model", getModelName());
        metadata.put("provider", "wan");
        result.setMetadata(metadata);
    }

    @Override
    public Mono<TaskStatus> getStatus(String taskId) {
        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> response = webClient.get()
                        .uri("/tasks/{task_id}", taskId)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block(Duration.ofSeconds(10));

                if (response == null) return TaskStatus.FAILED;

                String status = extractStatus(response);
                if ("success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status) || "succeeded".equalsIgnoreCase(status)) {
                    return TaskStatus.SUCCESS;
                }
                if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                    return TaskStatus.FAILED;
                }
                if ("cancelled".equalsIgnoreCase(status)) {
                    return TaskStatus.CANCELLED;
                }
                return TaskStatus.RUNNING;

            } catch (Exception e) {
                log.error("获取 Wan 任务状态失败：{}", e.getMessage());
                return TaskStatus.FAILED;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> cancel(String taskId) {
        return Mono.fromRunnable(() -> {
            try {
                webClient.post()
                        .uri("/tasks/{task_id}/cancel", taskId)
                        .retrieve()
                        .bodyToMono(Object.class)
                        .block(Duration.ofSeconds(10));
                log.info("Wan 任务已取消：taskId={}", taskId);
            } catch (Exception e) {
                log.warn("取消 Wan 任务失败：{}", e.getMessage());
            }
        }).then().subscribeOn(Schedulers.boundedElastic());
    }
}
