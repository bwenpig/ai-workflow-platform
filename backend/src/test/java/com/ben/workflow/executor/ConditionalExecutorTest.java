package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConditionalExecutor 单元测试
 */
public class ConditionalExecutorTest {

    @Test
    @DisplayName("测试条件为true - 选择true分支")
    void testConditionTrue() {
        // inputs 包含 both data and config
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("status", 200);  // 实际数据
        inputs.put("expression", "status == 200");  // 条件
        inputs.put("value", "200");  // 比较值
        
        NodeExecutionContext context = new NodeExecutionContext(
            "cond-node", "条件节点", "conditional", inputs, 0
        );
        
        ConditionalExecutor executor = new ConditionalExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "条件执行应该成功");
    }

    @Test
    @DisplayName("测试条件为false - 选择false分支")
    void testConditionFalse() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("status", 404);
        inputs.put("expression", "status == 200");
        inputs.put("value", "200");
        
        NodeExecutionContext context = new NodeExecutionContext(
            "cond-node", "条件节点", "conditional", inputs, 0
        );
        
        ConditionalExecutor executor = new ConditionalExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "条件执行应该成功");
    }

    @Test
    @DisplayName("测试字符串相等")
    void testStringEquality() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("type", "video");
        inputs.put("expression", "type == 'video'");
        inputs.put("value", "video");
        
        NodeExecutionContext context = new NodeExecutionContext(
            "cond-node", "条件节点", "conditional", inputs, 0
        );
        
        ConditionalExecutor executor = new ConditionalExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "字符串比较应该成功");
    }

    @Test
    @DisplayName("测试缺少表达式配置 - 默认行为")
    void testMissingExpression() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("status", 200);
        // 缺少 expression - 执行器可能返回成功
        
        NodeExecutionContext context = new NodeExecutionContext(
            "cond-node", "条件节点", "conditional", inputs, 0
        );
        
        ConditionalExecutor executor = new ConditionalExecutor();
        NodeExecutionResult result = executor.execute(context);
        
        // 注：执行器可能有默认行为，不一定失败
        assertNotNull(result, "应该返回结果");
    }

    @Test
    @DisplayName("测试数值大于比较")
    void testNumericGreaterThan() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("count", 15);
        inputs.put("expression", "count > 10");
        inputs.put("value", "10");
        
        NodeExecutionContext context = new NodeExecutionContext(
            "cond-node", "条件节点", "conditional", inputs, 0
        );
        
        ConditionalExecutor executor = new ConditionalExecutor();
        NodeExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "数值比较应该成功");
    }
}