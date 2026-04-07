package com.ben.workflow.executor.extension;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutorConfigLoader 单元测试
 */
public class ExecutorConfigLoaderTest {

    private ExecutorConfigLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ExecutorConfigLoader();
    }

    @Test
    void testLoad_returnsRegistrations() {
        List<ExecutorRegistration> registrations = loader.load();

        assertNotNull(registrations);
        assertFalse(registrations.isEmpty(), "Should load at least one registration from executors.yaml");

        // 检查每个注册项基本字段
        for (ExecutorRegistration r : registrations) {
            assertNotNull(r.getType(), "type should not be null");
            assertFalse(r.getType().isBlank(), "type should not be blank");
            assertNotNull(r.getClassName(), "className should not be null");
        }
    }

    @Test
    void testLoad_containsKnownTypes() {
        List<ExecutorRegistration> registrations = loader.load();
        List<String> types = registrations.stream().map(ExecutorRegistration::getType).toList();

        assertTrue(types.contains("conditional"), "Should contain 'conditional'");
        assertTrue(types.contains("email"), "Should contain 'email'");
        assertTrue(types.contains("http_request"), "Should contain 'http_request'");
    }

    @Test
    void testLoad_emailConfig() {
        List<ExecutorRegistration> registrations = loader.load();

        ExecutorRegistration email = registrations.stream()
                .filter(r -> "email".equals(r.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(email, "Email executor should be in config");
        assertTrue(email.isEnabled());
        assertNotNull(email.getConfig());
        assertEquals("smtp.gmail.com", email.getConfig().get("smtpHost"));
        assertEquals(587, email.getConfig().get("smtpPort"));
    }

    @Test
    void testLoad_loopConfig() {
        List<ExecutorRegistration> registrations = loader.load();

        ExecutorRegistration loop = registrations.stream()
                .filter(r -> "loop".equals(r.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(loop);
        assertNotNull(loop.getConfig());
        assertEquals(1000, loop.getConfig().get("maxIterations"));
    }
}
