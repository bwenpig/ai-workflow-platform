package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.spi.NodeComponent;
import com.ben.workflow.util.ConfigUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 循环/批处理节点执行器
 * <p>
 * 对数组数据进行遍历，对每个元素执行处理。
 * <p>
 * config 参数说明：
 * <ul>
 *   <li>items (List, 必填) - 要遍历的数组数据</li>
 *   <li>item_var (String, 可选, 默认 "item") - 循环变量名</li>
 *   <li>index_var (String, 可选, 默认 "index") - 索引变量名</li>
 *   <li>concurrency (Integer, 可选, 默认 1) - 并发数</li>
 *   <li>max_iterations (Integer, 可选, 默认 100) - 最大迭代次数</li>
 * </ul>
 * <p>
 * 输出：
 * <ul>
 *   <li>results (List) - 每项处理结果</li>
 *   <li>count (int) - 处理数量</li>
 *   <li>first (Object) - 第一个结果</li>
 *   <li>last (Object) - 最后一个结果</li>
 * </ul>
 */
@NodeComponent(value = "loop", name = "循环", description = "对数组数据进行遍历处理")
public class LoopExecutor implements NodeExecutor {

    @Override
    public String getType() {
        return "loop";
    }

    private static final int DEFAULT_CONCURRENCY = 1;
    private static final int DEFAULT_MAX_ITERATIONS = 100;

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            Map<String, Object> config = context.getInputs();
            
            // 获取要遍历的数据
            Object itemsObj = config.get("items");
            if (itemsObj == null) {
                return NodeExecutionResult.failed(context.getNodeId(), "items 不能为空", null, startTime, LocalDateTime.now());
            }
            
            List<?> items;
            if (itemsObj instanceof List) {
                items = (List<?>) itemsObj;
            } else if (itemsObj instanceof Collection) {
                items = new ArrayList<>((Collection<?>) itemsObj);
            } else if (itemsObj instanceof Object[]) {
                items = Arrays.asList((Object[]) itemsObj);
            } else {
                items = Collections.singletonList(itemsObj);
            }
            
            // 获取配置（使用 ConfigUtils 消除重复）
            String itemVar = ConfigUtils.getString(config, "item_var", "item");
            String indexVar = ConfigUtils.getString(config, "index_var", "index");
            int concurrency = ConfigUtils.getInt(config, "concurrency", DEFAULT_CONCURRENCY);
            int maxIterations = ConfigUtils.getInt(config, "max_iterations", DEFAULT_MAX_ITERATIONS);
            
            // 限制迭代次数
            if (items.size() > maxIterations) {
                items = items.subList(0, maxIterations);
            }
            
            // 构建每个迭代的输入
            List<Map<String, Object>> iterationInputs = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> input = new HashMap<>();
                input.put(itemVar, items.get(i));
                input.put(indexVar, i);
                input.put("_original_items", items);
                iterationInputs.add(input);
            }
            
            // 执行循环处理
            List<Object> results = executeLoop(iterationInputs, concurrency);
            
            // 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("results", results);
            outputs.put("count", results.size());
            outputs.put("first", results.isEmpty() ? null : results.get(0));
            outputs.put("last", results.isEmpty() ? null : results.get(results.size() - 1));
            
            return NodeExecutionResult.success(context.getNodeId(), outputs, startTime, LocalDateTime.now());
            
        } catch (Exception e) {
            return NodeExecutionResult.failed(context.getNodeId(), e.getMessage(), null, startTime, LocalDateTime.now());
        }
    }
    
    private List<Object> executeLoop(List<Map<String, Object>> iterationInputs, int concurrency) throws Exception {
        List<Object> results = new ArrayList<>();
        
        if (concurrency <= 1) {
            // 串行执行
            for (Map<String, Object> input : iterationInputs) {
                Object item = input.get("item");
                results.add(processItem(item));
            }
        } else {
            // 并行执行
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(concurrency, 10));
            try {
                List<Future<Object>> futures = iterationInputs.stream()
                    .map(input -> executor.submit(() -> processItem(input.get("item"))))
                    .collect(Collectors.toList());
                
                for (Future<Object> future : futures) {
                    results.add(future.get());
                }
            } finally {
                executor.shutdown();
            }
        }
        
        return results;
    }
    
    private Object processItem(Object item) {
        // 这里简化处理：直接返回处理后的 item
        // 实际使用时，这个节点应该与子工作流配合
        if (item instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) item;
            Map<String, Object> result = new HashMap<>();
            result.put("processed", true);
            result.put("data", map);
            return result;
        }
        return Map.of("processed", true, "data", item != null ? item.toString() : "null");
    }
}
