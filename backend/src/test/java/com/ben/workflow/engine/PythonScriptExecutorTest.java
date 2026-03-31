package com.ben.workflow.engine;

import com.ben.workflow.model.PythonNodeConfig;
import com.ben.workflow.spi.ModelExecutionContext;
import com.ben.workflow.spi.ModelExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PythonScriptExecutor 单元测试
 * 测试 Python 脚本执行器的各种场景
 */
class PythonScriptExecutorTest {

    private PythonScriptExecutor executor;
    private ModelExecutionContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        executor = new PythonScriptExecutor();
        context = new ModelExecutionContext();
        context.setNodeId("test-node");
        context.setNodeType("python_script");
        context.setInputs(new HashMap<>());
        context.setConfig(new HashMap<>());
    }

    // ==================== 成功场景测试 ====================

    @Test
    @DisplayName("测试成功执行 - 简单脚本")
    void testExecute_Success_SimpleScript() {
        String script = """
outputs['result'] = 'Hello, World!'
outputs['count'] = 42
""";
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "执行应该成功");
        assertNotNull(result.getData());
        assertEquals("Hello, World!", result.getData().get("result"));
        assertEquals(42, result.getData().get("count"));
    }

    @Test
    @DisplayName("测试成功执行 - 使用输入数据")
    void testExecute_Success_WithInputs() {
        String script = """
name = inputs.get('name', 'Guest')
outputs['greeting'] = f'Hello, {name}!'
outputs['length'] = len(name)
""";
        context.getInputs().put("name", "Python");
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("Hello, Python!", result.getData().get("greeting"));
        assertEquals(6, result.getData().get("length"));
    }

    @Test
    @DisplayName("测试成功执行 - 复杂数据处理")
    void testExecute_Success_ComplexDataProcessing() {
        String script = """
numbers = inputs.get('numbers', [1, 2, 3])
outputs['sum'] = sum(numbers)
outputs['average'] = sum(numbers) / len(numbers) if numbers else 0
outputs['max'] = max(numbers) if numbers else None
outputs['min'] = min(numbers) if numbers else None
""";
        context.getInputs().put("numbers", Arrays.asList(10, 20, 30, 40, 50));
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(150, result.getData().get("sum"));
        assertEquals(30.0, result.getData().get("average"));
        assertEquals(50, result.getData().get("max"));
        assertEquals(10, result.getData().get("min"));
    }

    @Test
    @DisplayName("测试成功执行 - 字符串操作")
    void testExecute_Success_StringOperations() {
        String script = """
text = inputs.get('text', '')
outputs['upper'] = text.upper()
outputs['lower'] = text.lower()
outputs['reversed'] = text[::-1]
outputs['word_count'] = len(text.split())
""";
        context.getInputs().put("text", "Hello World Test");
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("HELLO WORLD TEST", result.getData().get("upper"));
        assertEquals("hello world test", result.getData().get("lower"));
        assertEquals("tseT dlroW olleH", result.getData().get("reversed"));
        assertEquals(3, result.getData().get("word_count"));
    }

    @Test
    @DisplayName("测试成功执行 - 字典操作")
    void testExecute_Success_DictionaryOperations() {
        String script = """
data = inputs.get('data', {})
outputs['keys'] = list(data.keys())
outputs['values'] = list(data.values())
outputs['item_count'] = len(data)
outputs['has_name'] = 'name' in data
""";
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Test");
        data.put("value", 123);
        data.put("active", true);
        context.getInputs().put("data", data);
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(((List<?>) result.getData().get("keys")).contains("name"));
        assertEquals(3, result.getData().get("item_count"));
        assertEquals(true, result.getData().get("has_name"));
    }

    @Test
    @DisplayName("测试执行器类型")
    void testGetType() {
        assertEquals("python_script", executor.getType());
    }

    // ==================== 错误场景测试 ====================

    @Test
    @DisplayName("测试脚本路径不存在 - 无效脚本内容")
    void testExecute_ScriptPathNotFound_InvalidScript() {
        // 脚本语法错误
        String script = """
outputs['result'] = 'unclosed string
""";
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertFalse(result.isSuccess(), "语法错误的脚本应该失败");
        assertTrue(result.getError().contains("执行异常") || result.getError().contains("失败"), 
                   "错误信息应指示执行失败");
    }

    @Test
    @DisplayName("测试 Python 命令不存在 - 模拟环境")
    void testExecute_PythonCommandNotFound() {
        // 这个测试验证当 python3 不可用时的行为
        // 由于测试环境有 python3，我们通过传入无效配置来测试错误处理
        String script = """
import sys
sys.exit(1)
""";
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("失败") || result.getError().contains("异常"));
    }

    @Test
    @DisplayName("测试脚本执行超时")
    void testExecute_ScriptTimeout() {
        // 创建一个会超时的脚本（睡眠超过超时时间）
        String script = """
import time
time.sleep(5)  # 睡眠 5 秒
outputs['result'] = 'should not reach here'
""";
        Map<String, Object> config = new HashMap<>();
        config.put("script", script);
        config.put("timeout", 2);  // 设置 2 秒超时

        context.setConfig(config);

        long startTime = System.currentTimeMillis();
        ModelExecutionResult result = executor.execute(context);
        long elapsed = System.currentTimeMillis() - startTime;

        assertFalse(result.isSuccess(), "超时的脚本应该失败");
        assertTrue(result.getError().contains("超时"), "错误信息应包含超时");
        assertTrue(elapsed < 10000, "执行应该在 10 秒内返回（超时 + 缓冲）");
    }

    @Test
    @DisplayName("测试脚本输出为空")
    void testExecute_ScriptOutputEmpty() {
        // 脚本不产生任何输出
        String script = """
# 这是一个空脚本，不产生任何输出
pass
""";
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess(), "空输出脚本应该成功");
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty(), "输出数据应该为空");
    }

    @Test
    @DisplayName("测试脚本输出过大")
    void testExecute_ScriptOutputTooLarge() {
        // 创建大量输出的脚本
        String script = """
outputs['large_data'] = 'x' * 1000000  # 1MB 字符串
outputs['list'] = list(range(10000))
""";
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        // 大输出应该能处理，但可能需要更多时间
        assertTrue(result.isSuccess(), "大输出脚本应该能处理");
        assertNotNull(result.getData().get("large_data"));
        assertNotNull(result.getData().get("list"));
    }

    // ==================== 边缘场景测试 ====================

    @Test
    @DisplayName("测试边缘场景 - 空配置")
    void testExecute_EdgeCase_EmptyConfig() {
        context.setConfig(new HashMap<>());

        ModelExecutionResult result = executor.execute(context);

        // 空脚本应该失败或返回空结果
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("异常") || result.getError().contains("失败"));
    }

    @Test
    @DisplayName("测试边缘场景 - null 配置")
    void testExecute_EdgeCase_NullConfig() {
        context.setConfig(null);

        ModelExecutionResult result = executor.execute(context);

        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("测试边缘场景 - null 上下文")
    void testExecute_EdgeCase_NullContext() {
        // 直接调用 execute 方法
        PythonExecutionResult result = executor.execute("outputs['test'] = 1", null, null);

        // null inputs 应该被处理为空的 HashMap
        assertTrue(result.isSuccess());
        assertNotNull(result.getOutputs());
        assertEquals(1, result.getOutputs().get("test"));
    }

    @Test
    @DisplayName("测试边缘场景 - Unicode 字符")
    void testExecute_EdgeCase_UnicodeCharacters() {
        String script = """
outputs['chinese'] = '你好世界'
outputs['emoji'] = '🐲🚀💻'
outputs['japanese'] = 'こんにちは'
outputs['korean'] = '안녕하세요'
outputs['mixed'] = 'Hello 你好 🌍'
""";
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("你好世界", result.getData().get("chinese"));
        assertEquals("🐲🚀💻", result.getData().get("emoji"));
        assertEquals("こんにちは", result.getData().get("japanese"));
    }

    @Test
    @DisplayName("测试边缘场景 - 特殊数据类型")
    void testExecute_EdgeCase_SpecialDataTypes() {
        String script = """
outputs['boolean_true'] = True
outputs['boolean_false'] = False
outputs['none_value'] = None
outputs['float'] = 3.14159
outputs['negative'] = -42
outputs['zero'] = 0
""";
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(true, result.getData().get("boolean_true"));
        assertEquals(false, result.getData().get("boolean_false"));
        assertEquals(3.14159, result.getData().get("float"));
        assertEquals(-42, result.getData().get("negative"));
        assertEquals(0, result.getData().get("zero"));
    }

    @Test
    @DisplayName("测试边缘场景 - 嵌套数据结构")
    void testExecute_EdgeCase_NestedDataStructures() {
        String script = """
outputs['nested_dict'] = {
    'level1': {
        'level2': {
            'level3': 'deep value'
        }
    }
}
outputs['list_of_dicts'] = [
    {'name': 'Alice', 'age': 30},
    {'name': 'Bob', 'age': 25}
]
outputs['dict_of_lists'] = {
    'numbers': [1, 2, 3],
    'letters': ['a', 'b', 'c']
}
""";
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData().get("nested_dict"));
        assertNotNull(result.getData().get("list_of_dicts"));
        assertNotNull(result.getData().get("dict_of_lists"));
    }

    @Test
    @DisplayName("测试边缘场景 - Python 标准库使用")
    void testExecute_EdgeCase_StandardLibraryUsage() {
        String script = """
import json
import datetime
import math
import random

outputs['json_parsed'] = json.loads('{"key": "value"}')
outputs['current_year'] = datetime.datetime.now().year
outputs['sqrt'] = math.sqrt(16)
outputs['pi'] = math.pi
outputs['random_seed'] = 42  # 固定值用于测试
""";
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("value", ((Map<?, ?>) result.getData().get("json_parsed")).get("key"));
        assertEquals(4.0, result.getData().get("sqrt"));
    }

    @Test
    @DisplayName("测试边缘场景 - 错误处理脚本")
    void testExecute_EdgeCase_ErrorHandlingInScript() {
        String script = """
try:
    result = 10 / 0
except ZeroDivisionError as e:
    outputs['error_caught'] = True
    outputs['error_message'] = str(e)
    outputs['recovery_value'] = 0
""";
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(true, result.getData().get("error_caught"));
        assertTrue(result.getData().get("error_message").toString().contains("division by zero"));
    }

    @Test
    @DisplayName("测试边缘场景 - 条件逻辑")
    void testExecute_EdgeCase_ConditionalLogic() {
        String script = """
value = inputs.get('value', 0)
if value > 10:
    outputs['category'] = 'large'
elif value > 5:
    outputs['category'] = 'medium'
else:
    outputs['category'] = 'small'
outputs['is_positive'] = value > 0
""";
        context.getInputs().put("value", 7);
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("medium", result.getData().get("category"));
        assertEquals(true, result.getData().get("is_positive"));
    }

    @Test
    @DisplayName("测试边缘场景 - 循环处理")
    void testExecute_EdgeCase_LoopProcessing() {
        String script = """
items = inputs.get('items', [])
outputs['doubled'] = [x * 2 for x in items]
outputs['even_only'] = [x for x in items if x % 2 == 0]
outputs['sum'] = sum(items)

# 使用传统循环
total = 0
for item in items:
    total += item * item
outputs['sum_of_squares'] = total
""";
        context.getInputs().put("items", Arrays.asList(1, 2, 3, 4, 5));
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(Arrays.asList(2, 4, 6, 8, 10), result.getData().get("doubled"));
        assertEquals(Arrays.asList(2, 4), result.getData().get("even_only"));
        assertEquals(15, result.getData().get("sum"));
        assertEquals(55, result.getData().get("sum_of_squares"));
    }

    @Test
    @DisplayName("测试边缘场景 - 函数定义和调用")
    void testExecute_EdgeCase_FunctionDefinition() {
        String script = """
def fibonacci(n):
    if n <= 1:
        return n
    return fibonacci(n-1) + fibonacci(n-2)

def factorial(n):
    if n <= 1:
        return 1
    return n * factorial(n-1)

n = inputs.get('n', 5)
outputs['fibonacci'] = fibonacci(n)
outputs['factorial'] = factorial(n)
""";
        context.getInputs().put("n", 6);
        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(8, result.getData().get("fibonacci"));
        assertEquals(720, result.getData().get("factorial"));
    }

    @Test
    @DisplayName("测试边缘场景 - 文件操作（临时目录）")
    void testExecute_EdgeCase_FileOperations() throws IOException {
        // 创建临时输入文件
        Path inputFile = tempDir.resolve("input.txt");
        Files.writeString(inputFile, "line1\nline2\nline3");

        String script = String.format("""
with open('%s', 'r') as f:
    lines = f.readlines()
outputs['line_count'] = len(lines)
outputs['lines'] = [l.strip() for l in lines]
outputs['total_chars'] = sum(len(l) for l in lines)
""", inputFile.toString());

        context.getConfig().put("script", script);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getData().get("line_count"));
        assertEquals(17, result.getData().get("total_chars"));
    }

    @Test
    @DisplayName("测试边缘场景 - 环境变量")
    void testExecute_EdgeCase_EnvironmentVariables() {
        String script = """
import os
outputs['path_exists'] = os.path.exists('/')
outputs['cwd'] = os.getcwd()
outputs['sep'] = os.sep
""";
        Map<String, Object> config = new HashMap<>();
        config.put("script", script);
        Map<String, String> env = new HashMap<>();
        env.put("CUSTOM_VAR", "test_value");
        config.put("env", env);
        context.setConfig(config);

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(true, result.getData().get("path_exists"));
        assertNotNull(result.getData().get("cwd"));
    }

    @Test
    @DisplayName("测试边缘场景 - 多次执行独立性")
    void testExecute_EdgeCase_MultipleExecutionsIndependent() {
        String script = """
counter = inputs.get('counter', 0)
outputs['result'] = counter + 1
""";
        context.getConfig().put("script", script);

        // 第一次执行
        context.getInputs().put("counter", 1);
        ModelExecutionResult result1 = executor.execute(context);

        // 第二次执行
        context.getInputs().put("counter", 10);
        ModelExecutionResult result2 = executor.execute(context);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertEquals(2, result1.getData().get("result"));
        assertEquals(11, result2.getData().get("result"));
    }

    @Test
    @DisplayName("测试边缘场景 - 超时边界值")
    void testExecute_EdgeCase_TimeoutBoundary() throws InterruptedException {
        // 脚本执行时间接近但不超过超时
        String script = """
import time
time.sleep(0.5)
outputs['completed'] = True
""";
        Map<String, Object> config = new HashMap<>();
        config.put("script", script);
        config.put("timeout", 2);
        context.setConfig(config);

        long startTime = System.currentTimeMillis();
        ModelExecutionResult result = executor.execute(context);
        long elapsed = System.currentTimeMillis() - startTime;

        assertTrue(result.isSuccess(), "在超时范围内的脚本应该成功");
        assertTrue(elapsed >= 400, "执行应该至少花费 400ms");
        assertTrue(elapsed < 3000, "执行应该在 3 秒内完成");
    }

    // ==================== PythonNodeConfig 集成测试 ====================

    @Test
    @DisplayName("测试 PythonNodeConfig 集成 - 完整配置")
    void testExecute_PythonNodeConfig_FullConfig() {
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript("""
outputs['result'] = inputs.get('value', 0) * 2
""");
        config.setTimeout(30);  // 使用默认超时
        // 不设置 requirements，避免安装依赖耗时
        Map<String, String> env = new HashMap<>();
        env.put("TEST_VAR", "test");
        config.setEnv(env);

        context.getInputs().put("value", 21);
        context.setConfig(convertConfigToMap(config));

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(42, result.getData().get("result"));
    }

    @Test
    @DisplayName("测试 PythonNodeConfig 集成 - 最小配置")
    void testExecute_PythonNodeConfig_MinimalConfig() {
        PythonNodeConfig config = new PythonNodeConfig();
        config.setScript("outputs['minimal'] = True");

        context.setConfig(convertConfigToMap(config));

        ModelExecutionResult result = executor.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(true, result.getData().get("minimal"));
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> convertConfigToMap(PythonNodeConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("script", config.getScript());
        if (config.getTimeout() != null) {
            map.put("timeout", config.getTimeout());
        }
        if (config.getRequirements() != null) {
            map.put("requirements", config.getRequirements());
        }
        if (config.getEnv() != null) {
            map.put("env", config.getEnv());
        }
        return map;
    }
}
