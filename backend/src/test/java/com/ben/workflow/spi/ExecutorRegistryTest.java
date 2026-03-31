package com.ben.workflow.spi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorRegistryTest {
    private ExecutorRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ExecutorRegistry();
    }

    @Test
    void testRegisterAndGet() {
        ModelExecutor executor = new TestExecutor("test");
        registry.register(executor);
        ModelExecutor retrieved = registry.getExecutor("test");
        assertNotNull(retrieved);
        assertEquals("test", retrieved.getType());
    }

    @Test
    void testGetRegisteredTypes() {
        registry.register(new TestExecutor("type1"));
        registry.register(new TestExecutor("type2"));
        Set<String> types = registry.getRegisteredTypes();
        assertEquals(2, types.size());
        assertTrue(types.contains("type1"));
        assertTrue(types.contains("type2"));
    }

    @Test
    void testHasExecutor() {
        registry.register(new TestExecutor("exists"));
        assertTrue(registry.hasExecutor("exists"));
        assertFalse(registry.hasExecutor("not_exists"));
    }

    static class TestExecutor implements ModelExecutor {
        private final String type;
        TestExecutor(String type) { this.type = type; }
        @Override
        public String getType() { return type; }
        @Override
        public ModelExecutionResult execute(ModelExecutionContext context) {
            return ModelExecutionResult.success(new java.util.HashMap<>());
        }
    }
}
