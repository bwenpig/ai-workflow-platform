package com.ben.workflow.engine;

import com.ben.workflow.model.PythonNodeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Python 脚本执行器测试
 */
public class PythonScriptExecutorTest {
    
    private final PythonScriptExecutor executor = new PythonScriptExecutor();
    
    @Test
    @DisplayName("执行简单 Python 脚本 - 文本处理")
    public void testExecuteSimpleScript() {
        String script = """
outputs['result'] = {
    'type': 'text',
    'content': 'Hello World'
}
""";
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs(), "输出不应为空，logs=" + result.getLogs());
    }
    
    @Test
    @DisplayName("执行带输入的 Python 脚本")
    public void testExecuteScriptWithInputs() {
        String script = """
text_input = inputs.get('text_input', {})
content = text_input.get('content', '')
outputs['output'] = {
    'type': 'text',
    'content': content.upper()
}
""";
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("text_input", Map.of("type", "text", "content", "hello"));
        
        PythonExecutionResult result = executor.execute(script, inputs, new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    @DisplayName("执行超时脚本")
    public void testExecuteTimeoutScript() {
        String script = """
import time
time.sleep(5)
outputs['result'] = {'type': 'text', 'content': 'done'}
""";
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setTimeout(2);
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), config);
        
        assertFalse(result.isSuccess(), "脚本应该超时失败");
        assertNotNull(result.getError());
    }
    
    @Test
    @DisplayName("执行错误脚本")
    public void testExecuteErrorScript() {
        String script = """
raise Exception("测试错误")
""";
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), new PythonNodeConfig());
        
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
    }
    
    @Test
    @DisplayName("处理图片类型数据")
    public void testProcessImageData() {
        String script = """
image_input = inputs.get('image_input', {})
outputs['output'] = {
    'type': 'image',
    'url': image_input.get('url', 'default.jpg'),
    'width': 512,
    'height': 512
}
""";
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("image_input", Map.of(
            "type", "image",
            "url", "https://example.com/test.jpg",
            "width", 1024,
            "height", 1024
        ));
        
        PythonExecutionResult result = executor.execute(script, inputs, new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    @DisplayName("处理视频类型数据")
    public void testProcessVideoData() {
        String script = """
video_input = inputs.get('video_input', {})
outputs['output'] = {
    'type': 'video',
    'url': video_input.get('url', ''),
    'duration': video_input.get('duration', 0)
}
""";
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("video_input", Map.of(
            "type", "video",
            "url", "https://example.com/test.mp4",
            "duration", 30,
            "fps", 24
        ));
        
        PythonExecutionResult result = executor.execute(script, inputs, new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    @DisplayName("处理音频类型数据")
    public void testProcessAudioData() {
        String script = """
audio_input = inputs.get('audio_input', {})
outputs['output'] = {
    'type': 'audio',
    'url': audio_input.get('url', ''),
    'format': audio_input.get('format', 'mp3')
}
""";
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("audio_input", Map.of(
            "type", "audio",
            "url", "https://example.com/test.mp3",
            "duration", 180,
            "format", "mp3"
        ));
        
        PythonExecutionResult result = executor.execute(script, inputs, new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    @DisplayName("处理 JSON 类型数据")
    public void testProcessJsonData() {
        String script = """
json_input = inputs.get('json_input', {})
outputs['output'] = {
    'type': 'json',
    'data': json_input
}
""";
        
        Map<String, Object> inputs = new HashMap<>();
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("key", "value");
        jsonData.put("count", 42);
        inputs.put("json_input", jsonData);
        
        PythonExecutionResult result = executor.execute(script, inputs, new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    @DisplayName("空输入测试")
    public void testEmptyInputs() {
        String script = """
outputs['result'] = {'type': 'text', 'content': 'default'}
""";
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    @DisplayName("null 输入测试")
    public void testNullInputs() {
        String script = """
outputs['result'] = {'type': 'text', 'content': 'default'}
""";
        
        PythonExecutionResult result = executor.execute(script, null, new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
    }
    
    @Test
    @DisplayName("复杂数据处理")
    public void testComplexDataProcessing() {
        String script = """
text = inputs.get('text', '')
number = inputs.get('number', 0)
data = inputs.get('data', {})

outputs['result'] = {
    'type': 'json',
    'processed': True,
    'text_upper': text.upper() if text else '',
    'number_squared': number * number,
    'data_keys': list(data.keys())
}
""";
        
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("text", "hello");
        inputs.put("number", 5);
        inputs.put("data", Map.of("key1", "value1", "key2", "value2"));
        
        PythonExecutionResult result = executor.execute(script, inputs, new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    @DisplayName("设置环境变量")
    public void testWithEnvironmentVariables() {
        String script = """
import os
api_key = os.environ.get('API_KEY', 'not_set')
outputs['result'] = {
    'type': 'text',
    'api_key_set': api_key != 'not_set'
}
""";
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setEnv(Map.of("API_KEY", "test-key-123"));
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), config);
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    @DisplayName("语法错误测试")
    public void testSyntaxError() {
        String script = """
def broken(
    # 缺少右括号，语法错误
    pass
""";
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), new PythonNodeConfig());
        
        assertFalse(result.isSuccess(), "脚本应该因语法错误失败");
        assertNotNull(result.getError());
    }
    
    @Test
    @DisplayName("导入错误测试")
    public void testImportError() {
        String script = """
import non_existent_module
outputs['result'] = {'type': 'text', 'content': 'should not reach here'}
""";
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), new PythonNodeConfig());
        
        assertFalse(result.isSuccess(), "脚本应该因导入错误失败");
        assertNotNull(result.getError());
    }
    
    @Test
    @DisplayName("多输出测试")
    public void testMultipleOutputs() {
        String script = """
outputs['output1'] = {'type': 'text', 'content': 'first'}
outputs['output2'] = {'type': 'text', 'content': 'second'}
outputs['output3'] = {'type': 'json', 'data': {'key': 'value'}}
""";
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
        assertTrue(result.getOutputs().containsKey("output1"));
        assertTrue(result.getOutputs().containsKey("output2"));
        assertTrue(result.getOutputs().containsKey("output3"));
    }
    
    @Test
    @DisplayName("大数据量处理")
    public void testLargeDataProcessing() {
        String script = """
data = inputs.get('data', [])
outputs['result'] = {
    'type': 'json',
    'count': len(data),
    'sum': sum(data) if data else 0
}
""";
        
        Map<String, Object> inputs = new HashMap<>();
        List<Integer> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add(i);
        }
        inputs.put("data", largeList);
        
        PythonExecutionResult result = executor.execute(script, inputs, new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    @DisplayName("带依赖安装的脚本执行")
    public void testScriptWithRequirements() {
        String script = """
outputs['result'] = {'type': 'text', 'content': 'test'}
""";
        
        PythonNodeConfig config = new PythonNodeConfig();
        config.setRequirements(Arrays.asList("requests"));
        config.setTimeout(120); // 给依赖安装更多时间
        
        // 注意：这个测试可能会因为网络问题或依赖安装失败而失败
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), config);
        
        // 即使依赖安装失败，脚本也应该能执行（依赖安装是可选的）
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
    }
    
    @Test
    @DisplayName("Python 脚本返回复杂嵌套结构")
    public void testComplexNestedOutput() {
        String script = """
outputs['result'] = {
    'type': 'json',
    'data': {
        'nested': {
            'key': 'value',
            'list': [1, 2, 3]
        }
    }
}
""";
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    @DisplayName("脚本输出包含特殊字符")
    public void testScriptWithSpecialCharacters() {
        String script = """
outputs['result'] = {
    'type': 'text',
    'content': 'Hello 世界！🌍',
    'unicode': '中文 日本語 한국어'
}
""";
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getOutputs());
    }
    
    @Test
    @DisplayName("最小脚本执行")
    public void testMinimalScript() {
        String script = "pass";
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), new PythonNodeConfig());
        
        // 最小脚本应该能执行成功
        assertTrue(result.isSuccess(), "最小脚本应该执行成功：" + result.getError());
    }
    
    @Test
    @DisplayName("脚本只打印不输出")
    public void testScriptOnlyPrint() {
        String script = """
print("Hello from Python")
print("This is a test")
""";
        
        PythonExecutionResult result = executor.execute(script, new HashMap<>(), new PythonNodeConfig());
        
        assertTrue(result.isSuccess(), "脚本应该执行成功：" + result.getError());
        assertNotNull(result.getLogs());
        assertTrue(result.getLogs().contains("Hello from Python"));
    }
}
