package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 循环节点执行器 — 对数组数据遍历，对每个元素执行处理（支持子工作流回调）。
 *
 * <h3>输入参数</h3>
 * <table border="1">
 *   <tr><th>Key</th><th>Type</th><th>Required</th><th>Default</th><th>Desc</th></tr>
 *   <tr><td>items</td><td>List</td><td>是</td><td>—</td><td>要遍历的数组</td></tr>
 *   <tr><td>item_var</td><td>String</td><td>否</td><td>item</td><td>循环变量名</td></tr>
 *   <tr><td>index_var</td><td>String</td><td>否</td><td>index</td><td>索引变量名</td></tr>
 *   <tr><td>concurrency</td><td>int</td><td>否</td><td>1</td><td>并发度（1=串行）</td></tr>
 *   <tr><td>max_iterations</td><td>int</td><td>否</td><td>1000</td><td>最大迭代次数</td></tr>
 *   <tr><td>error_strategy</td><td>String</td><td>否</td><td>fail_fast</td><td>错误处理策略</td></tr>
 *   <tr><td>timeout_ms</td><td>long</td><td>否</td><td>0(无限制)</td><td>单元素超时</td></tr>
 * </table>
 *
 * <h3>错误策略</h3>
 * <ul>
 *   <li><b>fail_fast</b> — 首个元素失败立即终止，抛出异常</li>
 *   <li><b>continue_on_error</b> — 跳过失败元素，继续执行剩余元素，最终输出包含 errors 列表</li>
 * </ul>
 *
 * <h3>子工作流回调</h3>
 * 外部可通过 {@link #setItemProcessor(Function)} 注入子工作流执行逻辑。
 * 未设置时使用默认处理器（透传包装）。
 *
 * <h3>输出</h3>
 * <ul>
 *   <li>results (List) — 每项处理结果（成功项）</li>
 *   <li>errors (List) — 失败项详情（仅 continue_on_error 模式）</li>
 *   <li>count (int) — 成功数量</li>
 *   <li>total (int) — 总迭代数</li>
 *   <li>first / last (Object) — 首尾结果</li>
 * </ul>
 */
public class LoopExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(LoopExecutor.class);

    private static final int DEFAULT_CONCURRENCY = 1;
    private static final int DEFAULT_MAX_ITERATIONS = 1000;
    private static final String DEFAULT_ITEM_VAR = "item";
    private static final String DEFAULT_INDEX_VAR = "index";
    private static final String DEFAULT_ERROR_STRATEGY = "fail_fast";
    private static final int MAX_CONCURRENCY = 20;

    public enum ErrorStrategy {
        /** 首个失败立即终止 */
        FAIL_FAST,
        /** 跳过失败继续执行 */
        CONTINUE_ON_ERROR
    }

    /** 子工作流回调：输入 → 输出，允许抛出异常表示失败 */
    private volatile Function<Map<String, Object>, Object> itemProcessor;

    @Override
    public String getType() {
        return "loop";
    }

    @Override
    public String getName() {
        return "循环";
    }

    @Override
    public String getDescription() {
        return "对数组数据进行遍历，支持并发与错误策略";
    }

    /**
     * 注入子工作流处理器。未设置时使用默认透传处理器。
     */
    public void setItemProcessor(Function<Map<String, Object>, Object> processor) {
        this.itemProcessor = processor;
    }

    // ── SPI 入口 ──────────────────────────────────────────

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        LocalDateTime startTime = LocalDateTime.now();
        String nodeId = context.getNodeId();

        try {
            Map<String, Object> config = context.getInputs();
            if (config == null || config.isEmpty()) {
                return fail(nodeId, "配置不能为空", startTime);
            }

            // 1. 解析 items
            List<?> items = resolveItems(config.get("items"));
            if (items == null || items.isEmpty()) {
                return success(nodeId, buildEmptyOutput(), startTime);
            }

            // 2. 解析配置
            String itemVar = ConfigUtils.getString(config, "item_var", DEFAULT_ITEM_VAR);
            String indexVar = ConfigUtils.getString(config, "index_var", DEFAULT_INDEX_VAR);
            int concurrency = Math.min(Math.max(ConfigUtils.getInt(config, "concurrency", DEFAULT_CONCURRENCY), 1), MAX_CONCURRENCY);
            int maxIterations = ConfigUtils.getInt(config, "max_iterations", DEFAULT_MAX_ITERATIONS);
            ErrorStrategy errorStrategy = resolveErrorStrategy(ConfigUtils.getString(config, "error_strategy", DEFAULT_ERROR_STRATEGY));
            long timeoutMs = ConfigUtils.getInt(config, "timeout_ms", 0);

            // 3. 截断
            if (items.size() > maxIterations) {
                items = items.subList(0, maxIterations);
                log.warn("LoopExecutor [{}] items 超出 maxIterations={}, 截断至 {}", nodeId, maxIterations, items.size());
            }

            // 4. 执行
            LoopResult loopResult = executeLoop(nodeId, items, itemVar, indexVar, concurrency, errorStrategy, timeoutMs);

            // 5. 组装输出
            Map<String, Object> outputs = buildOutput(loopResult);
            return success(nodeId, outputs, startTime);

        } catch (Exception e) {
            log.error("LoopExecutor [{}] 执行异常: {}", nodeId, e.getMessage(), e);
            return fail(nodeId, e.getMessage(), startTime);
        }
    }

    // ── 核心循环 ──────────────────────────────────────────

    private LoopResult executeLoop(String nodeId,
                                   List<?> items,
                                   String itemVar,
                                   String indexVar,
                                   int concurrency,
                                   ErrorStrategy errorStrategy,
                                   long timeoutMs) throws Exception {

        int total = items.size();
        LoopResult result = new LoopResult(total);

        Function<Map<String, Object>, Object> processor =
                itemProcessor != null ? itemProcessor : this::defaultProcess;

        if (concurrency <= 1) {
            // 串行
            for (int i = 0; i < total; i++) {
                Object r = runSingle(nodeId, items.get(i), i, itemVar, indexVar, processor, timeoutMs);
                if (r == null) {
                    if (errorStrategy == ErrorStrategy.FAIL_FAST) {
                        throw new LoopAbortedException("元素[" + i + "]执行失败，fail_fast 终止");
                    }
                    // continue_on_error: 已记录到 errors，继续
                } else {
                    result.addSuccess(i, r);
                }
            }
        } else {
            // 并发
            ExecutorService pool = newFixedThreadPool(concurrency, "loop-" + nodeId);
            try {
                List<Future<LoopItemResult>> futures = new ArrayList<>();
                for (int i = 0; i < total; i++) {
                    final int idx = i;
                    futures.add(pool.submit(() -> {
                        try {
                            Object r = runSingle(nodeId, items.get(idx), idx, itemVar, indexVar, processor, timeoutMs);
                            return new LoopItemResult(idx, r, null);
                        } catch (Exception e) {
                            return new LoopItemResult(idx, null, e);
                        }
                    }));
                }

                for (Future<LoopItemResult> f : futures) {
                    LoopItemResult ir = f.get();
                    if (ir.error != null) {
                        result.addError(ir.index, ir.error);
                        if (errorStrategy == ErrorStrategy.FAIL_FAST) {
                            throw new LoopAbortedException("元素[" + ir.index + "]执行失败: " + ir.error.getMessage());
                        }
                    } else {
                        result.addSuccess(ir.index, ir.value);
                    }
                }
            } finally {
                pool.shutdown();
                if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            }
        }

        return result;
    }

    /** 执行单个元素，返回结果或 null（失败已记录） */
    private Object runSingle(String nodeId,
                             Object item,
                             int index,
                             String itemVar,
                             String indexVar,
                             Function<Map<String, Object>, Object> processor,
                             long timeoutMs) {
        Map<String, Object> input = new HashMap<>();
        input.put(itemVar, item);
        input.put(indexVar, index);
        input.put("_loop_index", index);
        input.put("_loop_total", -1); // 占位

        try {
            Callable<Object> task = () -> processor.apply(input);

            Object result;
            if (timeoutMs > 0) {
                ExecutorService es = Executors.newSingleThreadExecutor();
                try {
                    result = es.submit(task).get(timeoutMs, TimeUnit.MILLISECONDS);
                } finally {
                    es.shutdownNow();
                }
            } else {
                result = task.call();
            }

            log.debug("LoopExecutor [{}] 元素[{}] 执行成功", nodeId, index);
            return result;

        } catch (TimeoutException e) {
            String msg = String.format("元素[%d]执行超时(%dms)", index, timeoutMs);
            log.error("LoopExecutor [{}] {}", nodeId, msg);
            return null;
        } catch (Exception e) {
            log.error("LoopExecutor [{}] 元素[{}] 执行失败: {}", nodeId, index, e.getMessage());
            return null;
        }
    }

    /** 默认处理器：简单包装 */
    private Object defaultProcess(Map<String, Object> input) {
        Object item = input.get("item");
        if (item instanceof Map) {
            Map<String, Object> wrapped = new HashMap<>();
            wrapped.put("processed", true);
            wrapped.put("data", item);
            return wrapped;
        }
        return Map.of("processed", true, "data", item != null ? item.toString() : "null");
    }

    // ── 工具方法 ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<?> resolveItems(Object obj) {
        if (obj == null) return null;
        if (obj instanceof List) return (List<?>) obj;
        if (obj instanceof Collection) return new ArrayList<>((Collection<?>) obj);
        if (obj.getClass().isArray()) return Arrays.asList((Object[]) obj);
        // 非集合当作单元素
        return Collections.singletonList(obj);
    }

    private ErrorStrategy resolveErrorStrategy(String s) {
        if (s == null) return ErrorStrategy.FAIL_FAST;
        switch (s.toLowerCase()) {
            case "continue_on_error":
            case "continue":
                return ErrorStrategy.CONTINUE_ON_ERROR;
            default:
                return ErrorStrategy.FAIL_FAST;
        }
    }

    private Map<String, Object> buildEmptyOutput() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("results", Collections.emptyList());
        m.put("errors", Collections.emptyList());
        m.put("count", 0);
        m.put("total", 0);
        m.put("first", null);
        m.put("last", null);
        return m;
    }

    private Map<String, Object> buildOutput(LoopResult result) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("results", result.successValues());
        m.put("errors", result.errorDetails);
        m.put("count", result.successCount());
        m.put("total", result.total);
        m.put("first", result.firstSuccess());
        m.put("last", result.lastSuccess());
        return m;
    }

    private NodeExecutionResult success(String nodeId, Map<String, Object> outputs, LocalDateTime start) {
        return NodeExecutionResult.success(nodeId, outputs, start, LocalDateTime.now());
    }

    private NodeExecutionResult fail(String nodeId, String msg, LocalDateTime start) {
        return NodeExecutionResult.failed(nodeId, msg, null, start, LocalDateTime.now());
    }

    private ExecutorService newFixedThreadPool(int n, String namePrefix) {
        ThreadFactory tf = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger(0);
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, namePrefix + "-" + seq.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        return Executors.newFixedThreadPool(n, tf);
    }

    // ── 内部结构 ──────────────────────────────────────────

    /** 循环执行结果聚合 */
    static class LoopResult {
        final int total;
        final Map<Integer, Object> successMap = new ConcurrentHashMap<>();
        final List<Map<String, Object>> errorDetails = Collections.synchronizedList(new ArrayList<>());

        /** 有序的成功值列表 */
        List<Object> successValues() {
            return successMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
        }

        LoopResult(int total) { this.total = total; }
        void addSuccess(int index, Object value) { successMap.put(index, value); }
        void addError(int index, Throwable e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("index", index);
            err.put("error", e.getMessage());
            err.put("type", e.getClass().getSimpleName());
            errorDetails.add(err);
        }
        int successCount() { return successMap.size(); }
        Object firstSuccess() {
            return successMap.entrySet().stream()
                    .min(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue).orElse(null);
        }
        Object lastSuccess() {
            return successMap.entrySet().stream()
                    .max(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue).orElse(null);
        }

        // 便捷访问
        List<Object> getSuccessValues() { return successValues(); }
    }

    /** 并发单元素结果 */
    static class LoopItemResult {
        final int index;
        final Object value;
        final Throwable error;
        LoopItemResult(int index, Object value, Throwable error) {
            this.index = index; this.value = value; this.error = error;
        }
    }

    /** 循环中断异常 */
    static class LoopAbortedException extends RuntimeException {
        LoopAbortedException(String msg) { super(msg); }
    }
}
