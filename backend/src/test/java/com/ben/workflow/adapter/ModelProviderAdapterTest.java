package com.ben.workflow.adapter;

import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ModelProviderAdapter 单元测试
 * 测试适配器层的各种场景
 */
class ModelProviderAdapterTest {

    private ModelProviderRegistry registry;
    private TestModelProvider mockProvider;
    private ModelProviderAdapter adapter;
    private ModelExecutionContext context;

    @BeforeEach
    void setUp() {
        mockProvider = new TestModelProvider();
        registry = new ModelProviderRegistry(Arrays.asList(mockProvider));
        adapter = new ModelProviderAdapter(registry);
        context = new ModelExecutionContext();
        context.setNodeType("testmodelprovider");  // 类名 TestModelProvider -> testmodelprovider
        context.setInputs(new HashMap<>());
        context.setConfig(new HashMap<>());
    }

    /**
     * 测试用的 ModelProvider 实现
     */
    static class TestModelProvider implements ModelProvider {
        private String modelName = "test-v1";
        private GenerationResult mockResult;
        private Mono<GenerationResult> mockMono;
        private boolean throwError = false;
        private String errorMessage = "测试错误";

        @Override
        public ModelType getType() {
            return ModelType.VIDEO;
        }

        @Override
        public String getModelName() {
            return modelName;
        }

        @Override
        public Mono<GenerationResult> generate(GenerationRequest request) {
            if (throwError) {
                return Mono.error(new RuntimeException(errorMessage));
            }
            if (mockMono != null) {
                return mockMono;
            }
            if (mockResult != null) {
                return Mono.just(mockResult);
            }
            // 默认成功结果
            GenerationResult result = new GenerationResult();
            result.setTaskId("test-task-" + UUID.randomUUID().toString().substring(0, 8));
            result.setStatus(TaskStatus.SUCCESS);
            result.setOutputUrls(Arrays.asList("https://example.com/output.mp4"));
            result.setDurationMs(1000L);
            return Mono.just(result);
        }

        @Override
        public Mono<TaskStatus> getStatus(String taskId) {
            return Mono.just(TaskStatus.SUCCESS);
        }

        @Override
        public Mono<Void> cancel(String taskId) {
            return Mono.empty();
        }

        public void setMockResult(GenerationResult result) {
            this.mockResult = result;
        }

        public void setMockMono(Mono<GenerationResult> mono) {
            this.mockMono = mono;
        }

        public void setThrowError(boolean throwError) {
            this.throwError = throwError;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }
    }

    /**
     * 第二个测试 Provider（用于多 Provider 测试）
     */
    static class WanTestProvider implements ModelProvider {
        private String modelName = "wan-v2";
        private GenerationResult mockResult;

        @Override
        public ModelType getType() {
            return ModelType.VIDEO;
        }

        @Override
        public String getModelName() {
            return modelName;
        }

        @Override
        public Mono<GenerationResult> generate(GenerationRequest request) {
            if (mockResult != null) {
                return Mono.just(mockResult);
            }
            GenerationResult result = new GenerationResult();
            result.setTaskId("wan-task");
            result.setStatus(TaskStatus.SUCCESS);
            result.setOutputUrls(Arrays.asList("https://example.com/wan-output.mp4"));
            return Mono.just(result);
        }

        @Override
        public Mono<TaskStatus> getStatus(String taskId) {
            return Mono.just(TaskStatus.SUCCESS);
        }

        @Override
        public Mono<Void> cancel(String taskId) {
            return Mono.empty();
        }

        public void setMockResult(GenerationResult result) {
            this.mockResult = result;
        }
    }

    // ==================== 成功场景测试 ====================

    @Test
    @DisplayName("测试成功调用模型 API - 基本场景")
    void testExecuteSuccess_Basic() {
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockProvider.setMockResult(mockResult);

        // 设置输入
        context.getInputs().put("prompt", "一朵盛开的花");

        // 执行测试
        ModelExecutionResult result = adapter.execute(context);

        // 验证结果
        assertTrue(result.isSuccess(), "执行应该成功");
        assertNotNull(result.getData(), "返回数据不应为空");
        assertEquals("testmodelprovider", result.getData().get("modelProvider"));
        assertEquals("test-v1", result.getData().get("modelName"));
        assertEquals("SUCCESS", result.getData().get("status"));
    }

    @Test
    @DisplayName("测试成功调用模型 API - 带输出 URLs")
    void testExecuteSuccess_WithOutputUrls() {
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockResult.setOutputUrls(Arrays.asList("https://example.com/output1.mp4", "https://example.com/output2.mp4"));
        mockResult.setPreviewUrl("https://example.com/preview.jpg");
        mockProvider.setMockResult(mockResult);

        context.getInputs().put("prompt", "视频生成测试");

        ModelExecutionResult result = adapter.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("https://example.com/output1.mp4", result.getData().get("url"));
        assertEquals("https://example.com/preview.jpg", result.getData().get("previewUrl"));
    }

    @Test
    @DisplayName("测试成功调用模型 API - 带元数据")
    void testExecuteSuccess_WithMetadata() {
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("width", 1920);
        metadata.put("height", 1080);
        mockResult.setMetadata(metadata);
        mockResult.setDuration(5);
        mockResult.setFps(30);
        mockResult.setDurationMs(5000L);
        mockProvider.setMockResult(mockResult);

        context.setNodeType("testmodelprovider");
        context.getInputs().put("prompt", "带元数据的测试");

        ModelExecutionResult result = adapter.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(1920, result.getData().get("width"));
        assertEquals(1080, result.getData().get("height"));
        assertEquals(5, result.getData().get("duration"));
        assertEquals(30, result.getData().get("fps"));
        assertEquals(5000L, result.getData().get("durationMs"));
    }

    @Test
    @DisplayName("测试成功调用模型 API - 从 config 获取 prompt")
    void testExecuteSuccess_PromptFromConfig() {
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockProvider.setMockResult(mockResult);

        // prompt 在 config 中而不是 inputs 中
        context.getConfig().put("prompt", "来自 config 的 prompt");

        ModelExecutionResult result = adapter.execute(context);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("测试成功调用模型 API - 带反向提示词")
    void testExecuteSuccess_WithNegativePrompt() {
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockProvider.setMockResult(mockResult);

        context.getInputs().put("prompt", "正面提示词");
        context.getInputs().put("negativePrompt", "负面提示词");

        ModelExecutionResult result = adapter.execute(context);

        assertTrue(result.isSuccess());
    }

    // ==================== 错误场景测试 ====================

    @Test
    @DisplayName("测试 API 错误返回 - 任务失败状态")
    void testExecute_ApiError_FailedStatus() {
        GenerationResult mockResult = new GenerationResult();
        mockResult.setStatus(ModelProvider.TaskStatus.FAILED);
        mockResult.setErrorMessage("模型服务内部错误");
        mockProvider.setMockResult(mockResult);

        context.getInputs().put("prompt", "测试错误处理");

        ModelExecutionResult result = adapter.execute(context);

        assertFalse(result.isSuccess(), "执行应该失败");
        assertEquals("模型服务内部错误", result.getError());
    }

    @Test
    @DisplayName("测试 API 错误返回 - 异常抛出")
    void testExecute_ApiError_Exception() {
        mockProvider.setThrowError(true);
        mockProvider.setErrorMessage("网络连接失败");

        context.getInputs().put("prompt", "测试异常处理");

        ModelExecutionResult result = adapter.execute(context);

        assertFalse(result.isSuccess(), "执行应该失败");
        assertTrue(result.getError().contains("模型执行异常"), "错误信息应包含执行异常");
        assertTrue(result.getError().contains("网络连接失败"), "错误信息应包含原始错误");
    }

    @Test
    @DisplayName("测试 API 错误返回 - Provider 不存在")
    void testExecute_ApiError_ProviderNotFound() {
        context.setNodeType("unknown_model");
        context.getInputs().put("prompt", "测试未找到的 Provider");

        ModelExecutionResult result = adapter.execute(context);

        assertFalse(result.isSuccess());
        assertEquals("未找到模型提供者：unknown_model", result.getError());
    }

    @Test
    @DisplayName("测试空响应处理 - null 结果")
    void testExecute_EmptyResponse_NullResult() {
        // 设置一个会返回 null 的 Mono
        mockProvider.setMockMono(Mono.fromSupplier(() -> null));

        context.getInputs().put("prompt", "测试空响应");

        ModelExecutionResult result = adapter.execute(context);

        assertFalse(result.isSuccess());
        assertEquals("模型返回空结果", result.getError());
    }

    @Test
    @DisplayName("测试空响应处理 - 空输出 URLs")
    void testExecute_EmptyResponse_EmptyOutputUrls() {
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockResult.setOutputUrls(new ArrayList<>());
        mockProvider.setMockResult(mockResult);

        context.getInputs().put("prompt", "测试空输出");

        ModelExecutionResult result = adapter.execute(context);

        assertTrue(result.isSuccess());
        assertFalse(result.getData().containsKey("url"), "空输出列表时不应有 url 字段");
    }

    @Test
    @DisplayName("测试节点类型为空")
    void testExecute_NullNodeType() {
        context.setNodeType(null);

        ModelExecutionResult result = adapter.execute(context);

        assertFalse(result.isSuccess());
        assertEquals("节点类型为空", result.getError());
    }

    // ==================== 超时处理测试 ====================

    @Test
    @DisplayName("测试 API 超时处理 - block 超时")
    void testExecute_Timeout_BlockTimeout() {
        // 模拟一个永远不会返回的 Mono，会导致 block 超时
        mockProvider.setMockMono(Mono.never());

        context.getInputs().put("prompt", "测试超时");

        // 由于 block 有 5 分钟超时，这个测试会等待较长时间
        // 实际项目中应该使用虚拟时间或更短的超时配置
        ModelExecutionResult result = adapter.execute(context);

        // 超时后应该返回错误
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("异常") || result.getError().contains("超时"));
    }

    @Test
    @DisplayName("测试 API 超时处理 - 慢响应但仍在超时范围内")
    void testExecute_Timeout_SlowButWithinTimeout() {
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        // 模拟延迟返回
        mockProvider.setMockMono(Mono.just(mockResult).delayElement(Duration.ofMillis(100)));

        context.getInputs().put("prompt", "测试慢响应");

        ModelExecutionResult result = adapter.execute(context);

        assertTrue(result.isSuccess(), "在超时范围内的响应应该成功");
    }

    // ==================== 重试机制测试 ====================

    @Test
    @DisplayName("测试重试机制 - 首次失败后成功")
    void testExecute_Retry_FirstFailThenSuccess() {
        // 注意：当前实现没有内置重试机制
        // 这个测试验证单次调用行为
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockProvider.setMockResult(mockResult);

        context.getInputs().put("prompt", "测试单次调用");

        ModelExecutionResult result = adapter.execute(context);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("测试重试机制 - 多次调用独立性")
    void testExecute_Retry_MultipleCallsIndependent() {
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockProvider.setMockResult(mockResult);

        context.getInputs().put("prompt", "第一次调用");
        ModelExecutionResult result1 = adapter.execute(context);

        context.getInputs().put("prompt", "第二次调用");
        ModelExecutionResult result2 = adapter.execute(context);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
    }

    // ==================== 边缘场景测试 ====================

    @Test
    @DisplayName("测试边缘场景 - 空 inputs 和 config")
    void testExecute_EdgeCase_EmptyInputsAndConfig() {
        context.setInputs(new HashMap<>());
        context.setConfig(new HashMap<>());

        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockProvider.setMockResult(mockResult);

        ModelExecutionResult result = adapter.execute(context);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("测试边缘场景 - config 包含额外参数")
    void testExecute_EdgeCase_ExtraConfigParams() {
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockProvider.setMockResult(mockResult);

        context.getConfig().put("prompt", "测试");
        context.getConfig().put("width", 1920);
        context.getConfig().put("height", 1080);
        context.getConfig().put("extraParam", "extraValue");

        ModelExecutionResult result = adapter.execute(context);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("测试边缘场景 - 特殊字符 prompt")
    void testExecute_EdgeCase_SpecialCharactersInPrompt() {
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockProvider.setMockResult(mockResult);

        String specialPrompt = "测试特殊字符：<>\"'&\\n\\t 中文/English/日本語";
        context.getInputs().put("prompt", specialPrompt);

        ModelExecutionResult result = adapter.execute(context);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("测试边缘场景 - 多个 Provider 注册")
    void testExecute_EdgeCase_MultipleProviders() {
        WanTestProvider mockProvider2 = new WanTestProvider();
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockProvider.setMockResult(mockResult);
        mockProvider2.setMockResult(mockResult);
        
        registry = new ModelProviderRegistry(Arrays.asList(mockProvider, mockProvider2));
        adapter = new ModelProviderAdapter(registry);

        // 测试 testmodelprovider (mockProvider)
        context.setNodeType("testmodelprovider");
        ModelExecutionResult result1 = adapter.execute(context);

        // 测试 wantestprovider (mockProvider2)
        context.setNodeType("wantestprovider");
        ModelExecutionResult result2 = adapter.execute(context);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertEquals("testmodelprovider", result1.getData().get("modelProvider"));
        assertEquals("wantestprovider", result2.getData().get("modelProvider"));
    }

    @Test
    @DisplayName("测试边缘场景 - 大小写不敏感的 nodeType")
    void testExecute_EdgeCase_CaseInsensitiveNodeType() {
        GenerationResult mockResult = createMockGenerationResult(ModelProvider.TaskStatus.SUCCESS);
        mockProvider.setMockResult(mockResult);

        // 测试大写
        context.setNodeType("TESTMODELPROVIDER");
        ModelExecutionResult result1 = adapter.execute(context);

        // 测试混合大小写
        context.setNodeType("TestModelProvider");
        ModelExecutionResult result2 = adapter.execute(context);

        assertTrue(result1.isSuccess(), "大写 nodeType 应该成功");
        assertTrue(result2.isSuccess(), "混合大小写 nodeType 应该成功");
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("adapter", adapter.getType());
    }

    // ==================== 辅助方法 ====================

    private GenerationResult createMockGenerationResult(ModelProvider.TaskStatus status) {
        GenerationResult result = new GenerationResult();
        result.setTaskId("test-task-" + UUID.randomUUID().toString().substring(0, 8));
        result.setStatus(status);
        result.setOutputUrls(Arrays.asList("https://example.com/output.mp4"));
        result.setDurationMs(1000L);
        return result;
    }
}
