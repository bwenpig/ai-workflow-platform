package com.ben.workflow.adapter.kling;

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
 * Kling 可灵 AI 执行器
 * API 文档：https://api.klingai.com/v1
 */
@Component
public class KlingExecutor implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(KlingExecutor.class);

    private final WebClient webClient;
    private final String apiUrl;
    private final String apiKey;

    public KlingExecutor(
            WebClient.Builder webClientBuilder,
            @Value("${model.kling.api-url:https://api.klingai.com/v1}") String apiUrl,
            @Value("${model.kling.api-key:}") String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public ModelType getType() {
        return ModelType.VIDEO;
    }

    @Override
    public String getModelName() {
        return "kling-v1";
    }

    @Override
    public Mono<GenerationResult> generate(GenerationRequest request) {
        return Mono.fromCallable(() -> doGenerate(request))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("Kling API 调用失败：{}", e.getMessage(), e);
                    GenerationResult errorResult = new GenerationResult();
                    errorResult.setStatus(ModelProvider.TaskStatus.FAILED);
                    errorResult.setErrorMessage("Kling API 调用失败：" + e.getMessage());
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
                    .uri("/videos/generate")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(30));

            if (response == null) {
                throw new RuntimeException("Kling API 返回空响应");
            }

            String taskId = extractTaskId(response);
            if (taskId == null) {
                throw new RuntimeException("无法从响应中提取任务 ID: " + response);
            }

            result.setTaskId(taskId);
            result.setStatus(ModelProvider.TaskStatus.RUNNING);
            log.info("Kling 任务已提交：taskId={}", taskId);

            Map<String, Object> finalResult = pollTaskStatus(taskId);
            parseResult(finalResult, result);
            result.setStatus(ModelProvider.TaskStatus.SUCCESS);

        } catch (WebClientResponseException e) {
            log.error("Kling API HTTP 错误：{} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            result.setStatus(ModelProvider.TaskStatus.FAILED);
            result.setErrorMessage("Kling API 错误：" + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Kling 生成失败：{}", e.getMessage(), e);
            result.setStatus(ModelProvider.TaskStatus.FAILED);
            result.setErrorMessage("Kling 生成失败：" + e.getMessage());
        }

        result.setDurationMs(Duration.between(startTime, Instant.now()).toMillis());
        return result;
    }

    private Map<String, Object> buildRequestBody(GenerationRequest request) {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", request.getPrompt());
        
        if (request.getNegativePrompt() != null) {
            input.put("negative_prompt", request.getNegativePrompt());
        }

        Map<String, Object> params = request.getParams();
        if (params != null) {
            if (params.containsKey("duration")) input.put("duration", params.get("duration"));
            if (params.containsKey("fps")) input.put("fps", params.get("fps"));
            if (params.containsKey("resolution")) input.put("resolution", params.get("resolution"));
            if (params.containsKey("seed")) input.put("seed", params.get("seed"));
            if (params.containsKey("creativity")) input.put("creativity", params.get("creativity"));
        }

        if (!input.containsKey("duration")) input.put("duration", 5);
        if (!input.containsKey("fps")) input.put("fps", 24);
        if (!input.containsKey("resolution")) input.put("resolution", "720p");

        body.put("input", input);
        return body;
    }

    private String extractTaskId(Map<String, Object> response) {
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
                    .uri("/video/task/{task_id}", taskId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));

            if (statusResponse == null) continue;

            String status = extractStatus(statusResponse);
            log.debug("Kling 任务状态：taskId={}, status={}", taskId, status);

            if ("success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status) || "succeeded".equalsIgnoreCase(status)) {
                return statusResponse;
            }
            if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                throw new RuntimeException("Kling 任务失败：" + extractErrorMessage(statusResponse));
            }
        }

        throw new RuntimeException("Kling 任务超时：taskId=" + taskId);
    }

    private String extractStatus(Map<String, Object> response) {
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
        
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof Map) {
                Map<?, ?> dataMap = (Map<?, ?>) data;
                
                Object videoUrl = dataMap.get("video_url");
                if (videoUrl != null) outputUrls.add(videoUrl.toString());
                
                Object previewUrl = dataMap.get("cover_url");
                if (previewUrl != null) result.setPreviewUrl(previewUrl.toString());
                
                Object duration = dataMap.get("duration");
                if (duration != null) {
                    try { result.setDuration(Integer.parseInt(duration.toString())); } catch (NumberFormatException e) {}
                }
                
                Object fps = dataMap.get("fps");
                if (fps != null) {
                    try { result.setFps(Integer.parseInt(fps.toString())); } catch (NumberFormatException e) {}
                }
            }
        }

        result.setOutputUrls(outputUrls);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model", getModelName());
        metadata.put("provider", "kling");
        result.setMetadata(metadata);
    }

    @Override
    public Mono<TaskStatus> getStatus(String taskId) {
        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> response = webClient.get()
                        .uri("/video/task/{task_id}", taskId)
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
                log.error("获取 Kling 任务状态失败：{}", e.getMessage());
                return TaskStatus.FAILED;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> cancel(String taskId) {
        return Mono.fromRunnable(() -> {
            try {
                webClient.post()
                        .uri("/video/task/{task_id}/cancel", taskId)
                        .retrieve()
                        .bodyToMono(Object.class)
                        .block(Duration.ofSeconds(10));
                log.info("Kling 任务已取消：taskId={}", taskId);
            } catch (Exception e) {
                log.warn("取消 Kling 任务失败：{}", e.getMessage());
            }
        }).then().subscribeOn(Schedulers.boundedElastic());
    }
}
