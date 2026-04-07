package com.ben.workflow.executor.extension;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseExecutor 单元测试
 */
public class BaseExecutorTest {

    /**
     * 测试用具体实现
     */
    @ExecutorMeta(
        type = "test-executor",
        name = "Test Executor",
        description = "For testing",
        category = "test",
        icon = "🧪",
        version = "0.1.0"
    )
    static class TestExecutor extends BaseExecutor {

        boolean initCalled = false;
        boolean destroyCalled = false;

        @Override
        public String getType() { return "test-executor"; }
        @Override
        public String getName() { return "Test Executor"; }
        @Override
        public String getDescription() { return "For testing"; }

        @Override
        protected void doInitialize() {
            initCalled = true;
        }

        @Override
        protected void doDestroy() {
            destroyCalled = true;
        }

        @Override
        protected Map<String, Object> doExecute(NodeExecutionContext context) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("echo", getInputString(context, "message", "default"));
            return out;
        }

        @Override
        protected List<ParameterSchema> defineInputParams() {
            return List.of(
                ParameterSchema.builder().name("message").type("string").label("Message").required(true).build()
            );
        }

        @Override
        protected List<ParameterSchema> defineOutputParams() {
            return List.of(
                ParameterSchema.builder().name("echo").type("string").label("Echo").build()
            );
        }
    }

    /**
     * 无注解的 Executor
     */
    static class PlainExecutor extends BaseExecutor {
        @Override
        public String getType() { return "plain"; }
        @Override
        public String getName() { return "Plain"; }
        @Override
        public String getDescription() { return "No annotation"; }

        @Override
        protected Map<String, Object> doExecute(NodeExecutionContext context) {
            return Map.of("ok", true);
        }
    }

    /**
     * 带 requireInput 校验的 Executor
     */
    static class StrictExecutor extends BaseExecutor {
        @Override
        public String getType() { return "strict"; }
        @Override
        public String getName() { return "Strict"; }
        @Override
        public String getDescription() { return "Strict validation"; }

        @Override
        protected void doValidate(NodeExecutionContext context) throws ValidationException {
            super.doValidate(context);
            requireInput(context, "required_field");
        }

        @Override
        protected Map<String, Object> doExecute(NodeExecutionContext context) {
            return Map.of("value", getInputString(context, "required_field", ""));
        }
    }

    // ========== Tests ==========

    @Test
    void testLifecycle() throws Exception {
        TestExecutor exec = new TestExecutor();

        assertFalse(exec.isInitialized());
        assertFalse(exec.isAvailable());

        exec.init();
        assertTrue(exec.initCalled);
        assertTrue(exec.isInitialized());
        assertTrue(exec.isAvailable());

        // 重复 init 不会再调
        exec.initCalled = false;
        exec.init();
        assertFalse(exec.initCalled, "Should not re-init");

        exec.destroy();
        assertTrue(exec.destroyCalled);
        assertFalse(exec.isInitialized());
        assertFalse(exec.isAvailable());
    }

    @Test
    void testExecuteSuccess() throws Exception {
        TestExecutor exec = new TestExecutor();
        exec.init();

        Map<String, Object> inputs = Map.of("message", "hello");
        NodeExecutionContext ctx = new NodeExecutionContext(
            "n1", "Node1", "test-executor", inputs, 0
        );

        NodeExecutionResult result = exec.execute(ctx);
        assertTrue(result.isSuccess());
        assertEquals("hello", result.getOutputs().get("echo"));
    }

    @Test
    void testExecuteWithDefaultValue() throws Exception {
        TestExecutor exec = new TestExecutor();
        exec.init();

        // 不传 message，使用默认值
        NodeExecutionContext ctx = new NodeExecutionContext(
            "n1", "Node1", "test-executor", Map.of(), 0
        );

        NodeExecutionResult result = exec.execute(ctx);
        assertTrue(result.isSuccess());
        assertEquals("default", result.getOutputs().get("echo"));
    }

    @Test
    void testValidateInputs_nullContext() throws Exception {
        TestExecutor exec = new TestExecutor();
        exec.init();

        assertFalse(exec.validateInputs(null));
    }

    @Test
    void testValidateInputs_valid() throws Exception {
        TestExecutor exec = new TestExecutor();
        exec.init();

        NodeExecutionContext ctx = new NodeExecutionContext(
            "n1", "Node1", "test-executor", Map.of("message", "hi"), 0
        );
        assertTrue(exec.validateInputs(ctx));
    }

    @Test
    void testExecuteNullContext_fails() throws Exception {
        TestExecutor exec = new TestExecutor();
        exec.init();

        NodeExecutionResult result = exec.execute(null);
        assertFalse(result.isSuccess());
    }

    @Test
    void testRequireInput_missing() throws Exception {
        StrictExecutor exec = new StrictExecutor();
        exec.init();

        NodeExecutionContext ctx = new NodeExecutionContext(
            "n1", "Node1", "strict", Map.of(), 0
        );

        // validateInputs returns false
        assertFalse(exec.validateInputs(ctx));

        // execute returns failed result
        NodeExecutionResult result = exec.execute(ctx);
        assertFalse(result.isSuccess());
    }

    @Test
    void testRequireInput_present() throws Exception {
        StrictExecutor exec = new StrictExecutor();
        exec.init();

        NodeExecutionContext ctx = new NodeExecutionContext(
            "n1", "Node1", "strict", Map.of("required_field", "data"), 0
        );

        assertTrue(exec.validateInputs(ctx));
        NodeExecutionResult result = exec.execute(ctx);
        assertTrue(result.isSuccess());
        assertEquals("data", result.getOutputs().get("value"));
    }

    @Test
    void testRequireInput_blankString() throws Exception {
        StrictExecutor exec = new StrictExecutor();
        exec.init();

        NodeExecutionContext ctx = new NodeExecutionContext(
            "n1", "Node1", "strict", Map.of("required_field", "   "), 0
        );

        assertFalse(exec.validateInputs(ctx));
    }

    @Test
    void testMetadata_withAnnotation() {
        TestExecutor exec = new TestExecutor();
        ExecutorMetadata meta = exec.getMetadata();

        assertEquals("test-executor", meta.getType());
        assertEquals("Test Executor", meta.getName());
        assertEquals("For testing", meta.getDescription());
        assertEquals("test", meta.getCategory());
        assertEquals("🧪", meta.getIcon());
        assertEquals("0.1.0", meta.getVersion());
        assertFalse(meta.isExperimental());
        assertEquals(1, meta.getInputParams().size());
        assertEquals("message", meta.getInputParams().get(0).getName());
        assertEquals(1, meta.getOutputParams().size());
        assertEquals("echo", meta.getOutputParams().get(0).getName());
    }

    @Test
    void testMetadata_withoutAnnotation() {
        PlainExecutor exec = new PlainExecutor();
        ExecutorMetadata meta = exec.getMetadata();

        assertEquals("plain", meta.getType());
        assertEquals("Plain", meta.getName());
        assertEquals("No annotation", meta.getDescription());
        assertEquals("general", meta.getCategory());
        assertEquals("1.0.0", meta.getVersion());
    }

    @Test
    void testSetConfiguration() {
        TestExecutor exec = new TestExecutor();
        ExecutorConfiguration config = ExecutorConfiguration.builder()
                .params(Map.of("key1", "value1"))
                .build();
        exec.setConfiguration(config);

        assertNotNull(exec.configuration);
        assertEquals("value1", exec.configuration.getString("key1", ""));
    }

    @Test
    void testInputHelpers() throws Exception {
        TestExecutor exec = new TestExecutor();
        exec.init();

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("str", "hello");
        inputs.put("num", 42);
        inputs.put("dbl", 3.14);
        inputs.put("obj", List.of(1, 2));

        NodeExecutionContext ctx = new NodeExecutionContext(
            "n1", "Node1", "test-executor", inputs, 0
        );

        assertEquals("hello", exec.getInputString(ctx, "str", ""));
        assertEquals("fallback", exec.getInputString(ctx, "missing", "fallback"));
        assertEquals(42, exec.getInputInt(ctx, "num", 0));
        assertEquals(0, exec.getInputInt(ctx, "missing", 0));
        assertEquals(3.14, exec.getInputDouble(ctx, "dbl", 0.0), 0.001);
        assertNotNull(exec.getInput(ctx, "obj"));
        assertNull(exec.getInput(ctx, "nonexistent"));
    }
}
