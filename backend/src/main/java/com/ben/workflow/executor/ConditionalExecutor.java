package com.ben.workflow.executor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ben.dagscheduler.spi.NodeExecutor;
import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.dagscheduler.spi.NodeExecutionResult;
import com.ben.dagscheduler.annotation.NodeComponent;

/**
 * 条件执行器
 * 
 * 根据条件表达式判断，路由到不同分支（true/false 或多个分支）
 * 支持表达式：==, !=, >, <, >=, <=, contains, regex
 * 
 * @author Ben
 * @since 0.1.0
 */
@Component
@NodeComponent(value = "conditional", name = "Conditional Executor", description = "条件分支执行器")
public class ConditionalExecutor implements NodeExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(ConditionalExecutor.class);
    
    /**
     * 支持的操作符
     */
    private static final String OP_EQUALS = "==";
    private static final String OP_NOT_EQUALS = "!=";
    private static final String OP_GREATER = ">";
    private static final String OP_LESS = "<";
    private static final String OP_GREATER_EQUALS = ">=";
    private static final String OP_LESS_EQUALS = "<=";
    private static final String OP_CONTAINS = "contains";
    private static final String OP_REGEX = "regex";
    
    @Override
    public String getType() {
        return "conditional";
    }
    
    @Override
    public String getName() {
        return "Conditional Executor";
    }
    
    @Override
    public String getDescription() {
        return "根据条件表达式判断，路由到不同分支";
    }
    
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // 获取条件配置
            String expression = context.getInput("expression", String.class);
            String operator = context.getInput("operator", String.class);
            Object leftValue = context.getInput("leftValue");
            Object rightValue = context.getInput("rightValue");
            
            // 支持简化的单参数模式
            if (expression != null && !expression.isEmpty()) {
                return executeByExpression(context, expression, startTime);
            }
            
            // 标准模式：operator + leftValue + rightValue
            if (operator == null || operator.isEmpty()) {
                return NodeExecutionResult.failed(
                    context.getNodeId(),
                    "operator parameter is required",
                    null,
                    startTime,
                    LocalDateTime.now()
                );
            }
            
            if (leftValue == null) {
                return NodeExecutionResult.failed(
                    context.getNodeId(),
                    "leftValue parameter is required",
                    null,
                    startTime,
                    LocalDateTime.now()
                );
            }
            
            boolean result = evaluate(operator, leftValue, rightValue);
            
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("result", result);
            outputs.put("branch", result ? "true" : "false");
            
            logger.info("Conditional evaluation: {} {} {} = {}", 
                       leftValue, operator, rightValue, result);
            
            return NodeExecutionResult.success(
                context.getNodeId(),
                outputs,
                startTime,
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Conditional execution failed", e);
            return NodeExecutionResult.failed(
                context.getNodeId(),
                e.getMessage(),
                NodeExecutionResult.getStackTrace(e),
                startTime,
                LocalDateTime.now()
            );
        }
    }
    
    /**
     * 通过表达式字符串执行条件判断
     * 
     * 格式：${variable} operator value
     * 例如：${status} == "success"
     */
    private NodeExecutionResult executeByExpression(NodeExecutionContext context, 
                                                     String expression,
                                                     LocalDateTime startTime) {
        try {
            // 解析表达式
            Expression parsed = parseExpression(expression);
            if (parsed == null) {
                return NodeExecutionResult.failed(
                    context.getNodeId(),
                    "Failed to parse expression: " + expression,
                    null,
                    startTime,
                    LocalDateTime.now()
                );
            }
            
            // 获取左值（可能是变量引用或字面值）
            Object leftValue = resolveValue(parsed.leftOperand, context);
            Object rightValue = resolveValue(parsed.rightOperand, context);
            
            boolean result = evaluate(parsed.operator, leftValue, rightValue);
            
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("result", result);
            outputs.put("branch", result ? "true" : "false");
            outputs.put("expression", expression);
            
            logger.info("Conditional expression '{}': {} {} {} = {}", 
                       expression, leftValue, parsed.operator, rightValue, result);
            
            return NodeExecutionResult.success(
                context.getNodeId(),
                outputs,
                startTime,
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Expression evaluation failed: {}", expression, e);
            return NodeExecutionResult.failed(
                context.getNodeId(),
                "Expression evaluation failed: " + e.getMessage(),
                NodeExecutionResult.getStackTrace(e),
                startTime,
                LocalDateTime.now()
            );
        }
    }
    
    /**
     * 解析表达式字符串
     */
    private Expression parseExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        
        expression = expression.trim();
        
        // 尝试匹配不同的操作符（按长度降序，避免 >= 被解析为 > 和 =）
        String[] operators = {OP_GREATER_EQUALS, OP_LESS_EQUALS, OP_NOT_EQUALS, 
                             OP_EQUALS, OP_GREATER, OP_LESS, OP_CONTAINS, OP_REGEX};
        
        for (String op : operators) {
            int idx = expression.indexOf(op);
            if (idx > 0) {
                Expression expr = new Expression();
                expr.operator = op;
                expr.leftOperand = expression.substring(0, idx).trim();
                expr.rightOperand = expression.substring(idx + op.length()).trim();
                return expr;
            }
        }
        
        return null;
    }
    
    /**
     * 解析值（支持变量引用 ${var} 和字面值）
     */
    private Object resolveValue(String valueStr, NodeExecutionContext context) {
        if (valueStr == null) {
            return null;
        }
        
        valueStr = valueStr.trim();
        
        // 检查是否是变量引用 ${variable}
        if (valueStr.startsWith("${") && valueStr.endsWith("}")) {
            String varName = valueStr.substring(2, valueStr.length() - 1);
            // 先从输入参数中查找
            Object value = context.getInput(varName);
            // 再从共享数据中查找
            if (value == null) {
                value = context.getSharedData(varName);
            }
            return value;
        }
        
        // 处理字符串字面值（去掉引号）
        if ((valueStr.startsWith("\"") && valueStr.endsWith("\"")) ||
            (valueStr.startsWith("'") && valueStr.endsWith("'"))) {
            return valueStr.substring(1, valueStr.length() - 1);
        }
        
        // 尝试解析数字
        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            } else {
                return Long.parseLong(valueStr);
            }
        } catch (NumberFormatException e) {
            // 不是数字，返回原始字符串
        }
        
        // 处理布尔值
        if ("true".equalsIgnoreCase(valueStr)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valueStr)) {
            return false;
        }
        
        // 处理 null
        if ("null".equalsIgnoreCase(valueStr)) {
            return null;
        }
        
        return valueStr;
    }
    
    /**
     * 执行条件判断
     */
    private boolean evaluate(String operator, Object leftValue, Object rightValue) {
        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null");
        }
        
        switch (operator) {
            case OP_EQUALS:
                return evaluateEquals(leftValue, rightValue);
            case OP_NOT_EQUALS:
                return !evaluateEquals(leftValue, rightValue);
            case OP_GREATER:
                return evaluateCompare(leftValue, rightValue) > 0;
            case OP_LESS:
                return evaluateCompare(leftValue, rightValue) < 0;
            case OP_GREATER_EQUALS:
                return evaluateCompare(leftValue, rightValue) >= 0;
            case OP_LESS_EQUALS:
                return evaluateCompare(leftValue, rightValue) <= 0;
            case OP_CONTAINS:
                return evaluateContains(leftValue, rightValue);
            case OP_REGEX:
                return evaluateRegex(leftValue, rightValue);
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }
    
    /**
     * 相等性判断
     */
    private boolean evaluateEquals(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        
        // 类型转换和比较
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() == ((Number) right).doubleValue();
        }
        
        return left.equals(right);
    }
    
    /**
     * 比较大小
     */
    @SuppressWarnings("unchecked")
    private int evaluateCompare(Object left, Object right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Cannot compare null values");
        }
        
        // 数字比较
        if (left instanceof Number && right instanceof Number) {
            double leftNum = ((Number) left).doubleValue();
            double rightNum = ((Number) right).doubleValue();
            return Double.compare(leftNum, rightNum);
        }
        
        // 字符串比较
        if (left instanceof String && right instanceof String) {
            return ((String) left).compareTo((String) right);
        }
        
        // Comparable 比较
        if (left instanceof Comparable && right instanceof Comparable) {
            try {
                return ((Comparable<Object>) left).compareTo(right);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Cannot compare different types: " + 
                    left.getClass() + " and " + right.getClass());
            }
        }
        
        throw new IllegalArgumentException("Cannot compare non-comparable types: " + 
            left.getClass() + " and " + right.getClass());
    }
    
    /**
     * 包含判断
     */
    private boolean evaluateContains(Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }
        
        String leftStr = left.toString();
        String rightStr = right.toString();
        
        return leftStr.contains(rightStr);
    }
    
    /**
     * 正则匹配
     */
    private boolean evaluateRegex(Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }
        
        String input = left.toString();
        String pattern = right.toString();
        
        try {
            return Pattern.matches(pattern, input);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex pattern: " + pattern, e);
        }
    }
    
    @Override
    public boolean validateInputs(NodeExecutionContext context) {
        String expression = context.getInput("expression", String.class);
        String operator = context.getInput("operator", String.class);
        
        // 至少要有 expression 或 operator
        return (expression != null && !expression.isEmpty()) || 
               (operator != null && !operator.isEmpty());
    }
    
    /**
     * 表达式解析结果
     */
    private static class Expression {
        String operator;
        String leftOperand;
        String rightOperand;
    }
}