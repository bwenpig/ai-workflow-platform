package com.ben.workflow.api;

import com.ben.workflow.executor.extension.ExecutorMetadata;
import com.ben.workflow.executor.extension.ExecutorRegistration;
import com.ben.workflow.executor.extension.ParameterSchema;
import org.junit.jupiter.api.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutorRegistryController 单元测试
 * 使用 Stub 代替 Mock（避免 Java 25 + Mockito inline mock 问题）
 */
public class ExecutorRegistryControllerTest {

    private StubRegistry stubRegistry;
    private ExecutorRegistryController controller;

    @BeforeEach
    void setUp() {
        stubRegistry = new StubRegistry();
        controller = new ExecutorRegistryController(stubRegistry);
    }

    private ExecutorMetadata sampleMeta(String type, String category) {
        return ExecutorMetadata.builder()
                .type(type)
                .name(type + " executor")
                .description("desc")
                .category(category)
                .version("1.0.0")
                .build();
    }

    // ========== GET /api/v1/executors ==========

    @Test
    void listExecutors_returnsAll() {
        stubRegistry.addMeta("http_request", sampleMeta("http_request", "integration"));
        stubRegistry.addMeta("email", sampleMeta("email", "integration"));
        stubRegistry.addMeta("conditional", sampleMeta("conditional", "logic"));

        Mono<ResponseEntity<List<ExecutorMetadata>>> result = controller.listExecutors(null);

        StepVerifier.create(result)
            .assertNext(resp -> {
                assertEquals(200, resp.getStatusCode().value());
                assertEquals(3, resp.getBody().size());
            })
            .verifyComplete();
    }

    @Test
    void listExecutors_filterByCategory() {
        stubRegistry.addMeta("http_request", sampleMeta("http_request", "integration"));
        stubRegistry.addMeta("email", sampleMeta("email", "integration"));
        stubRegistry.addMeta("conditional", sampleMeta("conditional", "logic"));

        Mono<ResponseEntity<List<ExecutorMetadata>>> result = controller.listExecutors("integration");

        StepVerifier.create(result)
            .assertNext(resp -> {
                assertEquals(200, resp.getStatusCode().value());
                assertEquals(2, resp.getBody().size());
                assertTrue(resp.getBody().stream().allMatch(m -> "integration".equals(m.getCategory())));
            })
            .verifyComplete();
    }

    @Test
    void listExecutors_emptyCategory_returnsEmpty() {
        stubRegistry.addMeta("conditional", sampleMeta("conditional", "logic"));

        Mono<ResponseEntity<List<ExecutorMetadata>>> result = controller.listExecutors("nonexistent");

        StepVerifier.create(result)
            .assertNext(resp -> {
                assertEquals(200, resp.getStatusCode().value());
                assertTrue(resp.getBody().isEmpty());
            })
            .verifyComplete();
    }

    // ========== GET /api/v1/executors/{type} ==========

    @Test
    void getExecutor_found() {
        stubRegistry.addMeta("email", sampleMeta("email", "integration"));

        Mono<ResponseEntity<ExecutorMetadata>> result = controller.getExecutor("email");

        StepVerifier.create(result)
            .assertNext(resp -> {
                assertEquals(200, resp.getStatusCode().value());
                assertEquals("email", resp.getBody().getType());
            })
            .verifyComplete();
    }

    @Test
    void getExecutor_notFound() {
        Mono<ResponseEntity<ExecutorMetadata>> result = controller.getExecutor("unknown");

        StepVerifier.create(result)
            .assertNext(resp -> assertEquals(404, resp.getStatusCode().value()))
            .verifyComplete();
    }

    // ========== POST /api/v1/executors ==========

    @Test
    void registerExecutor_success() {
        ExecutorRegistration reg = ExecutorRegistration.builder()
                .type("slack")
                .className("com.ben.workflow.executor.SlackExecutor")
                .enabled(true)
                .build();

        Mono<ResponseEntity<Map<String, Object>>> result = controller.registerExecutor(reg);

        StepVerifier.create(result)
            .assertNext(resp -> {
                assertEquals(200, resp.getStatusCode().value());
                assertEquals("registered", resp.getBody().get("status"));
                assertEquals("slack", resp.getBody().get("type"));
            })
            .verifyComplete();

        assertTrue(stubRegistry.registerCalled);
    }

    @Test
    void registerExecutor_missingType() {
        ExecutorRegistration reg = ExecutorRegistration.builder()
                .className("com.ben.SomeExecutor")
                .build();
        reg.setType(null);

        Mono<ResponseEntity<Map<String, Object>>> result = controller.registerExecutor(reg);

        StepVerifier.create(result)
            .assertNext(resp -> {
                assertEquals(400, resp.getStatusCode().value());
                assertTrue(resp.getBody().containsKey("error"));
            })
            .verifyComplete();

        assertFalse(stubRegistry.registerCalled);
    }

    @Test
    void registerExecutor_missingClassName() {
        ExecutorRegistration reg = ExecutorRegistration.builder()
                .type("slack")
                .build();
        reg.setClassName(null);

        Mono<ResponseEntity<Map<String, Object>>> result = controller.registerExecutor(reg);

        StepVerifier.create(result)
            .assertNext(resp -> {
                assertEquals(400, resp.getStatusCode().value());
                assertTrue(resp.getBody().get("error").toString().contains("className"));
            })
            .verifyComplete();
    }

    @Test
    void registerExecutor_duplicate() {
        stubRegistry.addMeta("email", sampleMeta("email", "integration"));

        ExecutorRegistration reg = ExecutorRegistration.builder()
                .type("email")
                .className("com.ben.workflow.executor.EmailExecutor")
                .build();

        Mono<ResponseEntity<Map<String, Object>>> result = controller.registerExecutor(reg);

        StepVerifier.create(result)
            .assertNext(resp -> {
                assertEquals(400, resp.getStatusCode().value());
                assertTrue(resp.getBody().get("error").toString().contains("already registered"));
            })
            .verifyComplete();

        assertFalse(stubRegistry.registerCalled);
    }

    // ========== DELETE /api/v1/executors/{type} ==========

    @Test
    void unregisterExecutor_success() {
        stubRegistry.addMeta("email", sampleMeta("email", "integration"));

        Mono<ResponseEntity<Void>> result = controller.unregisterExecutor("email");

        StepVerifier.create(result)
            .assertNext(resp -> assertEquals(204, resp.getStatusCode().value()))
            .verifyComplete();

        assertTrue(stubRegistry.unregisterCalled);
    }

    @Test
    void unregisterExecutor_notFound() {
        Mono<ResponseEntity<Void>> result = controller.unregisterExecutor("nonexistent");

        StepVerifier.create(result)
            .assertNext(resp -> assertEquals(404, resp.getStatusCode().value()))
            .verifyComplete();

        assertFalse(stubRegistry.unregisterCalled);
    }

    // ========== Stub ==========

    /**
     * Stub for ConfigDrivenExecutorRegistry
     * Only implements methods used by the controller
     */
    static class StubRegistry extends com.ben.workflow.executor.extension.ConfigDrivenExecutorRegistry {

        private final Map<String, ExecutorMetadata> metas = new ConcurrentHashMap<>();
        boolean registerCalled = false;
        boolean unregisterCalled = false;

        StubRegistry() {
            // Skip @PostConstruct by not calling super.init()
        }

        void addMeta(String type, ExecutorMetadata meta) {
            metas.put(type, meta);
        }

        @Override
        public List<ExecutorMetadata> getAllMetadata() {
            return new ArrayList<>(metas.values());
        }

        @Override
        public ExecutorMetadata getMetadata(String type) {
            return metas.get(type);
        }

        @Override
        public boolean hasExecutor(String type) {
            return metas.containsKey(type);
        }

        @Override
        public void registerExecutor(ExecutorRegistration registration) {
            registerCalled = true;
        }

        @Override
        public void unregister(String type) {
            unregisterCalled = true;
            metas.remove(type);
        }
    }
}
