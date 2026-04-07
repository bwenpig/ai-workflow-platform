package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoopExecutor 单元测试
 */
public class LoopExecutorTest {

    @Test
    @DisplayName("测试基础循环 - 遍历数组")
    void testBasicLoopArray() {
        // 准备输入 (config 放在 inputs 中)
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", items);  // config 放在 inputs 里
        
        NodeExecutionContext context = new NodeExecutionContext(
            "loop-node", "循环节点", "loop", inputs, 0
        );
        
        LoopExecutor executor = new LoopExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "循环应该成功执行");
        assertNotNull(result.getOutputs(), "输出不应为空");
        
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutput("results");
        assertEquals(5, results.size(), "应该遍历5个元素");
    }

    @Test
    @DisplayName("测试空列表循环")
    void testEmptyList() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("items", Collections.emptyList());
        
        NodeExecutionContext context = new NodeExecutionContext(
            "loop-node", "循环节点", "loop", inputs, 0
        );
        
        LoopExecutor executor = new LoopExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "空列表循环应该成功");
        
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutput("results");
        assertEquals(0, results.size(), "空列表应该返回空结果");
    }

    @Test
    @DisplayName("测试缺少items配置")
    void testMissingItems() {
        Map<String, Object> inputs = new HashMap<>();
        // 缺少 items
        
        NodeExecutionContext context = new NodeExecutionContext(
            "loop-node", "循环节点", "loop", inputs, 0
        );
        
        LoopExecutor executor = new LoopExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertFalse(result.isSuccess(), "缺少items应该失败");
        assertNotNull(result.getErrorMessage(), "应该返回错误信息");
    }

    @Test
    @DisplayName("测试map循环")
    void testMapLoop() {
        Map<String, Object> inputs = new HashMap<>();
        Map<String, String> mapData = new LinkedHashMap<>();
        mapData.put("key1", "value1");
        mapData.put("key2", "value2");
        inputs.put("items", mapData);
        
        NodeExecutionContext context = new NodeExecutionContext(
            "loop-node", "循环节点", "loop", inputs, 0
        );
        
        LoopExecutor executor = new LoopExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "Map循环应该成功");
    }
}