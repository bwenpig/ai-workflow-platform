package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoopExecutor 单元测试
 */
public class LoopExecutorTest {

    // ── 基础循环 ──────────────────────────────────────────

    @Test
    @DisplayName("测试基础循环 - 遍历数组")
    void testBasicLoopArray() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);
        
        NodeExecutionContext context = new NodeExecutionContext("loop-node", "循环节点", "loop", inputs, 0);
        LoopExecutor executor = new LoopExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "循环应该成功执行");
        assertNotNull(result.getOutputs(), "输出不应为空");
        
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutput("results");
        assertEquals(5, results.size(), "应该遍历5个元素");
        
        Integer count = (Integer) result.getOutput("count");
        assertEquals(5, count);
        Integer total = (Integer) result.getOutput("total");
        assertEquals(5, total);
    }

    @Test
    @DisplayName("测试空列表循环")
    void testEmptyList() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", Collections.emptyList());
        
        NodeExecutionContext context = new NodeExecutionContext("loop-node", "循环节点", "loop", inputs, 0);
        LoopExecutor executor = new LoopExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "空列表循环应该成功");
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutput("results");
        assertEquals(0, results.size());
    }

    @Test
    @DisplayName("测试缺少 items 配置")
    void testMissingItems() {
        Map<String, Object> inputs = new HashMap<>();
        
        NodeExecutionContext context = new NodeExecutionContext("loop-node", "循环节点", "loop", inputs, 0);
        LoopExecutor executor = new LoopExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertFalse(result.isSuccess(), "缺少 items 应该失败");
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("测试 null items")
    void testNullItems() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", null);
        
        NodeExecutionContext context = new NodeExecutionContext("loop-node", "循环节点", "loop", inputs, 0);
        LoopExecutor executor = new LoopExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "null items 应该当作空列表处理");
    }

    // ── 自定义变量名 ──────────────────────────────────────

    @Test
    @DisplayName("测试自定义循环变量名")
    void testCustomVarNames() {
        List<String> items = Arrays.asList("a", "b", "c");
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);
        inputs.put("item_var", "val");
        inputs.put("index_var", "idx");

        final List<Map<String, Object>> captured = new ArrayList<>();
        LoopExecutor executor = new LoopExecutor();
        executor.setItemProcessor(ctx -> {
            captured.add(ctx);
            return ctx.get("val");
        });

        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(3, captured.size());
        assertEquals("a", captured.get(0).get("val"));
        assertEquals(0, captured.get(0).get("idx"));
        assertEquals("b", captured.get(1).get("val"));
        assertEquals(1, captured.get(1).get("idx"));
    }

    // ── 并发执行 ──────────────────────────────────────────

    @Test
    @DisplayName("测试并发执行")
    void testConcurrency() {
        List<Integer> items = new ArrayList<>();
        for (int i = 0; i < 20; i++) items.add(i);
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);
        inputs.put("concurrency", 5);

        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger current = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(20);

        LoopExecutor executor = new LoopExecutor();
        executor.setItemProcessor(ctx -> {
            int cur = current.incrementAndGet();
            maxConcurrent.updateAndGet(max -> Math.max(max, cur));
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            current.decrementAndGet();
            latch.countDown();
            return ctx.get("item");
        });

        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(maxConcurrent.get() > 1, "应该并发执行，最大并发=" + maxConcurrent.get());
        assertTrue(latch.await(10, TimeUnit.SECONDS), "所有任务应该完成");
    }

    @Test
    @DisplayName("测试并发数上限为 20")
    void testConcurrencyCappedAt20() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", Arrays.asList(1, 2, 3));
        inputs.put("concurrency", 100); // 超限

        LoopExecutor executor = new LoopExecutor();
        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "并发数超限不应导致失败");
    }

    // ── 错误策略：fail_fast ───────────────────────────────

    @Test
    @DisplayName("测试 fail_fast — 首个失败即终止")
    void testFailFast() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);
        inputs.put("error_strategy", "fail_fast");

        AtomicInteger executed = new AtomicInteger(0);
        LoopExecutor executor = new LoopExecutor();
        executor.setItemProcessor(ctx -> {
            int idx = (int) ctx.get("index");
            executed.incrementAndGet();
            if (idx == 2) throw new RuntimeException("元素 2 失败");
            return ctx.get("item");
        });

        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertFalse(result.isSuccess(), "fail_fast 应该失败");
        assertTrue(executed.get() <= 3, "fail_fast 应该在失败后停止，实际执行=" + executed.get());
    }

    // ── 错误策略：continue_on_error ───────────────────────

    @Test
    @DisplayName("测试 continue_on_error — 跳过失败继续")
    void testContinueOnError() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);
        inputs.put("error_strategy", "continue_on_error");

        LoopExecutor executor = new LoopExecutor();
        executor.setItemProcessor(ctx -> {
            int idx = (int) ctx.get("index");
            if (idx == 1 || idx == 3) throw new RuntimeException("元素 " + idx + " 失败");
            return ctx.get("item");
        });

        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "continue_on_error 应该整体成功");
        
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutput("results");
        assertEquals(3, results.size(), "成功 3 个");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) result.getOutput("errors");
        assertEquals(2, errors.size(), "失败 2 个");
        
        Integer count = (Integer) result.getOutput("count");
        assertEquals(3, count);
    }

    @Test
    @DisplayName("测试 continue_on_error 并发模式")
    void testContinueOnErrorConcurrent() {
        List<Integer> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) items.add(i);
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);
        inputs.put("concurrency", 4);
        inputs.put("error_strategy", "continue_on_error");

        LoopExecutor executor = new LoopExecutor();
        executor.setItemProcessor(ctx -> {
            int idx = (int) ctx.get("index");
            if (idx % 3 == 0) throw new RuntimeException("index " + idx + " failed");
            return ctx.get("item");
        });

        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutput("results");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) result.getOutput("errors");
        
        assertEquals(7, results.size()); // 0,3,6,9 失败 → 剩 7 个
        assertEquals(3, errors.size());
    }

    // ── 最大迭代次数 ──────────────────────────────────────

    @Test
    @DisplayName("测试 max_iterations 截断")
    void testMaxIterations() {
        List<Integer> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) items.add(i);
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);
        inputs.put("max_iterations", 10);

        AtomicInteger executed = new AtomicInteger(0);
        LoopExecutor executor = new LoopExecutor();
        executor.setItemProcessor(ctx -> {
            executed.incrementAndGet();
            return ctx.get("item");
        });

        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(10, executed.get(), "应该只执行 10 次");
    }

    // ── 超时 ──────────────────────────────────────────────

    @Test
    @DisplayName("测试单元素超时")
    void testItemTimeout() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", Arrays.asList(1, 2, 3));
        inputs.put("timeout_ms", 100);

        AtomicInteger executed = new AtomicInteger(0);
        LoopExecutor executor = new LoopExecutor();
        executor.setItemProcessor(ctx -> {
            int idx = (int) ctx.get("index");
            if (idx == 1) {
                try { Thread.sleep(500); } catch (InterruptedException e) { /* ok */ }
            }
            executed.incrementAndGet();
            return ctx.get("item");
        });

        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        // fail_fast 模式下超时导致整体失败
        assertFalse(result.isSuccess(), "超时应该导致失败");
    }

    @Test
    @DisplayName("测试超时 + continue_on_error")
    void testTimeoutContinueOnError() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", Arrays.asList(1, 2, 3));
        inputs.put("timeout_ms", 100);
        inputs.put("error_strategy", "continue_on_error");

        LoopExecutor executor = new LoopExecutor();
        executor.setItemProcessor(ctx -> {
            int idx = (int) ctx.get("index");
            if (idx == 1) {
                try { Thread.sleep(500); } catch (InterruptedException e) { /* ok */ }
            }
            return ctx.get("item");
        });

        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutput("results");
        assertEquals(2, results.size(), "超时元素被跳过，剩余 2 个成功");
    }

    // ── 非集合输入 ────────────────────────────────────────

    @Test
    @DisplayName("测试非集合输入当作单元素")
    void testNonCollectionInput() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", "single_value");

        LoopExecutor executor = new LoopExecutor();
        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutput("results");
        assertEquals(1, results.size());
    }

    // ── first / last ──────────────────────────────────────

    @Test
    @DisplayName("测试 first 和 last 输出")
    void testFirstAndLast() {
        List<String> items = Arrays.asList("first", "middle", "last");
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);

        LoopExecutor executor = new LoopExecutor();
        executor.setItemProcessor(ctx -> ctx.get("item"));

        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertEquals("first", result.getOutput("first"));
        assertEquals("last", result.getOutput("last"));
    }

    @Test
    @DisplayName("测试空列表的 first/last 为 null")
    void testFirstLastEmpty() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", Collections.emptyList());

        LoopExecutor executor = new LoopExecutor();
        NodeExecutionContext context = new NodeExecutionContext("loop-node", "loop", "loop", inputs, 0);
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertNull(result.getOutput("first"));
        assertNull(result.getOutput("last"));
    }

    // ── getType / getName / getDescription ────────────────

    @Test
    @DisplayName("测试元信息")
    void testMetaInfo() {
        LoopExecutor executor = new LoopExecutor();
        assertEquals("loop", executor.getType());
        assertEquals("循环", executor.getName());
        assertNotNull(executor.getDescription());
        assertTrue(executor.isAvailable());
    }
}
