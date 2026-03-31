package com.ben.workflow.spi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KlingExecutorTest {
    private com.ben.workflow.adapter.kling.KlingExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new com.ben.workflow.adapter.kling.KlingExecutor();
    }

    @Test
    void testGetType() {
        assertEquals("kling", executor.getType());
    }

    @Test
    void testExecute_Success() {
        ModelExecutionContext context = new ModelExecutionContext();
        Map<String, Object> config = new HashMap<>();
        config.put("prompt", "A beautiful sunset");
        context.setConfig(config);
        ModelExecutionResult result = executor.execute(context);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }
}
