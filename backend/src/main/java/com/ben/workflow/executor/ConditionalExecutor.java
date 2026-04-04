package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.workflow.spi.NodeComponent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 条件分支节点执行器
 * <p>
 * 根据条件表达式判断，路由到不同分支。
 * <p>
 * config 参数说明：
 * <ul>
 *   <li>expression (String, 必填) - 条件表达式，支持: ==, !=, >, <, >=, <=, contains, startsWith, endsWith, regex</li>
 *   <li>value (Object, 可选) - 要比较的值，默认使用 inputs</li>
 *   <li>branch_true (String, 可选) - 条件为 true 时的输出键</li>
 *   <li>branch_false (String, 可选) - 条件为 false 时的输出键</li>
 * </ul>
 * <p>
 * 输出：
 * <ul>
 *   <li>result (boolean) - 条件判断结果</li>
 *   <li>matched_branch (String) - 匹配的分支名</li>
 * </ul>
 */
@NodeComponent(value = "conditional", name = "条件分支", description = "根据条件表达式路由到不同分支")
public class ConditionalExecutor implements NodeExecutor {

    @Override
    public String getType() {
        return "conditional";
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            Map<String, Object> inputs = context.getInputs();
            Map<String, Object> config = context.getInputs();
            
            String expression = getStringValue(config, "expression", "");
            Object value = config.get("value");
            
            // 如果没有指定 value，默认使用第一个输入值
            if (value == null && inputs != null && !inputs.isEmpty()) {
                value = inputs.values().iterator().next();
            }
            
            Object targetValue = getObjectValue(config, "target", inputs);
            
            boolean result = evaluateExpression(expression, targetValue, value);
            
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("result", result);
            outputs.put("matched_branch", result ? "true" : "false");
            
            return NodeExecutionResult.success(context.getNodeId(), outputs, startTime, LocalDateTime.now());
            
        } catch (Exception e) {
            return NodeExecutionResult.failed(context.getNodeId(), e.getMessage(), null, startTime, LocalDateTime.now());
        }
    }
    
    private boolean evaluateExpression(String expression, Object target, Object value) {
        if (expression == null || expression.isEmpty()) {
            return target != null;
        }
        
        expression = expression.trim();
        
        // 解析操作符
        if (expression.contains("==")) {
            String[] parts = expression.split("==");
            return evaluateEquals(parts[0].trim(), parts[1].trim(), target);
        }
        if (expression.contains("!=")) {
            String[] parts = expression.split("!=");
            return !evaluateEquals(parts[0].trim(), parts[1].trim(), target);
        }
        if (expression.contains(">=")) {
            String[] parts = expression.split(">=");
            return evaluateCompare(parts[0].trim(), parts[1].trim(), target) >= 0;
        }
        if (expression.contains("<=")) {
            String[] parts = expression.split("<=");
            return evaluateCompare(parts[0].trim(), parts[1].trim(), target) <= 0;
        }
        if (expression.contains(">")) {
            String[] parts = expression.split(">");
            return evaluateCompare(parts[0].trim(), parts[1].trim(), target) > 0;
        }
        if (expression.contains("<")) {
            String[] parts = expression.split("<");
            return evaluateCompare(parts[0].trim(), parts[1].trim(), target) < 0;
        }
        if (expression.contains("contains")) {
            String[] parts = expression.split("contains");
            return evaluateContains(parts[0].trim(), parts[1].trim(), target);
        }
        if (expression.contains("startsWith")) {
            String[] parts = expression.split("startsWith");
            return evaluateStartsWith(parts[0].trim(), parts[1].trim(), target);
        }
        if (expression.contains("endsWith")) {
            String[] parts = expression.split("endsWith");
            return evaluateEndsWith(parts[0].trim(), parts[1].trim(), target);
        }
        if (expression.contains("regex")) {
            String[] parts = expression.split("regex");
            return evaluateRegex(parts[0].trim(), parts[1].trim(), target);
        }
        
        // 默认：检查 target 是否为真
        return target != null && !target.toString().isEmpty();
    }
    
    private boolean evaluateEquals(String field, String expectedValue, Object target) {
        Object actual = getFieldValue(target, field);
        if (actual == null) return "null".equalsIgnoreCase(expectedValue) || "".equals(expectedValue);
        return actual.toString().equals(expectedValue);
    }
    
    private int evaluateCompare(String field, String expectedValue, Object target) {
        Object actual = getFieldValue(target, field);
        if (actual == null) return -1;
        
        try {
            double actualNum = Double.parseDouble(actual.toString());
            double expectedNum = Double.parseDouble(expectedValue);
            return Double.compare(actualNum, expectedNum);
        } catch (NumberFormatException e) {
            return actual.toString().compareTo(expectedValue);
        }
    }
    
    private boolean evaluateContains(String field, String searchValue, Object target) {
        Object actual = getFieldValue(target, field);
        if (actual == null) return false;
        return actual.toString().contains(searchValue);
    }
    
    private boolean evaluateStartsWith(String field, String prefix, Object target) {
        Object actual = getFieldValue(target, field);
        if (actual == null) return false;
        return actual.toString().startsWith(prefix);
    }
    
    private boolean evaluateEndsWith(String field, String suffix, Object target) {
        Object actual = getFieldValue(target, field);
        if (actual == null) return false;
        return actual.toString().endsWith(suffix);
    }
    
    private boolean evaluateRegex(String field, String pattern, Object target) {
        Object actual = getFieldValue(target, field);
        if (actual == null) return false;
        return Pattern.matches(pattern, actual.toString());
    }
    
    private Object getFieldValue(Object target, String field) {
        if (target == null) return null;
        if (field == null || field.isEmpty() || field.equals(".")) return target;
        
        if (target instanceof Map) {
            return ((Map<?, ?>) target).get(field);
        }
        
        // 尝试反射获取字段
        try {
            var fieldObj = target.getClass().getDeclaredField(field);
            fieldObj.setAccessible(true);
            return fieldObj.get(target);
        } catch (Exception e) {
            return target.toString();
        }
    }
    
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private Object getObjectValue(Map<String, Object> map, String key, Object defaultValue) {
        Object value = map.get(key);
        return value != null ? value : defaultValue;
    }
}