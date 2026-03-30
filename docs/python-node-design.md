# Python 脚本节点设计文档

## 🎯 需求

支持 Python 脚本节点，实现：
1. ✅ 接收上游节点数据输入（模型输出、文字、图片、音乐）
2. ✅ 执行 Python 脚本处理
3. ✅ 输出结果给下游节点

## 📊 数据类型定义

### 支持的数据类型

| 类型 | 说明 | 数据格式 |
|------|------|----------|
| `text` | 文本 | `{"type": "text", "content": "字符串内容"}` |
| `image` | 图片 | `{"type": "image", "url": "图片 URL", "width": 1024, "height": 1024}` |
| `video` | 视频 | `{"type": "video", "url": "视频 URL", "duration": 10, "fps": 30}` |
| `audio` | 音频/音乐 | `{"type": "audio", "url": "音频 URL", "duration": 180, "format": "mp3"}` |
| `json` | 结构化数据 | `{"type": "json", "data": {...}}` |

### 统一数据格式

```python
class WorkflowData:
    type: str  # text|image|video|audio|json
    content: Any  # 实际数据
    metadata: Dict  # 元数据
    source_node: str  # 来源节点 ID
```

## 🔧 节点设计

### Python 脚本节点结构

```json
{
  "nodeId": "python-1",
  "type": "PYTHON_SCRIPT",
  "inputs": [
    {"id": "input_1", "label": "输入 1", "type": "any"},
    {"id": "input_2", "label": "输入 2", "type": "any"}
  ],
  "outputs": [
    {"id": "output_1", "label": "输出 1", "type": "any"},
    {"id": "output_2", "label": "输出 2", "type": "any"}
  ],
  "config": {
    "script": "Python 代码字符串",
    "scriptPath": "/path/to/script.py (可选)",
    "timeout": 30,
    "requirements": ["requests", "pillow"],
    "pythonVersion": "3.9"
  }
}
```

### Python 脚本模板

```python
# 标准输入格式
inputs = {
    "input_1": {"type": "image", "url": "...", "width": 1024, "height": 1024},
    "input_2": {"type": "text", "content": "处理说明"}
}

# 标准输出格式
outputs = {
    "output_1": {
        "type": "image",
        "url": "处理后的图片 URL",
        "width": 512,
        "height": 512
    },
    "output_2": {
        "type": "text",
        "content": "处理日志"
    }
}
```

## 🏗️ 后端实现

### 1. Python 执行器服务

```java
@Component
public class PythonScriptExecutor {
    
    // 执行 Python 脚本
    public PythonExecutionResult execute(String script, Map<String, Object> inputs, PythonConfig config);
    
    // 支持的功能：
    // - 沙箱隔离执行
    // - 超时控制
    // - 依赖安装
    // - 标准输出/错误捕获
}
```

### 2. 执行流程

```
1. 接收节点配置和输入数据
   ↓
2. 准备 Python 执行环境
   - 创建临时目录
   - 安装 requirements
   - 准备输入数据 JSON
   ↓
3. 执行 Python 脚本
   - ProcessBuilder 启动 python 进程
   - 传入输入数据 (stdin 或文件)
   - 捕获输出 (stdout)
   - 超时控制
   ↓
4. 解析输出结果
   - 验证输出格式
   - 转换为 WorkflowData
   ↓
5. 清理临时文件
```

### 3. 安全沙箱

```java
public class PythonSandbox {
    // 限制：
    - 执行超时 (默认 30s)
    - 内存限制 (默认 512MB)
    - 禁止网络访问 (可选)
    - 禁止文件系统访问 (除临时目录)
    - 禁止危险函数 (eval, exec, __import__ 等)
}
```

## 🎨 前端设计

### 节点组件

```tsx
// PythonNode.tsx
- 代码编辑器 (Monaco Editor)
- 输入端口 (动态)
- 输出端口 (动态)
- 配置面板：
  - 脚本内容
  - 超时设置
  - 依赖管理
  - 测试运行按钮
```

### 数据类型图标

- 📝 text
- 🖼️ image
- 🎬 video
- 🎵 audio
- 📦 json

## 📁 文件结构

```
backend/
├── src/main/java/com/ben/workflow/
│   ├── engine/
│   │   └── PythonScriptExecutor.java      # Python 执行器
│   ├── model/
│   │   ├── PythonNodeConfig.java          # Python 节点配置
│   │   └── WorkflowDataType.java          # 数据类型枚举
│   └── sandbox/
│       ├── PythonSandbox.java             # 沙箱控制
│       └── ProcessExecutor.java           # 进程执行

frontend/
├── src/
│   ├── nodes/
│   │   └── PythonNode.tsx                 # Python 节点组件
│   └── types/
│       └── workflow.ts                    # 数据类型定义
```

## 🔌 API 设计

### 执行 Python 脚本

```
POST /api/v1/executions/{instanceId}/nodes/{nodeId}/execute
Body: {
  "script": "print('hello')",
  "inputs": {...},
  "config": {
    "timeout": 30,
    "requirements": ["pillow"]
  }
}

Response: {
  "status": "success",
  "outputs": {...},
  "logs": "...",
  "duration": 1.5
}
```

### 测试脚本

```
POST /api/v1/python/test
Body: {
  "script": "...",
  "sampleInputs": {...}
}

Response: {
  "success": true,
  "outputs": {...},
  "errors": []
}
```

## 📋 实现优先级

### P0 - 核心功能
- [ ] Python 执行器 (ProcessBuilder)
- [ ] 基本数据类型 (text, json)
- [ ] 输入输出传递
- [ ] 超时控制

### P1 - 增强功能
- [ ] 图片/视频/音频支持
- [ ] 依赖自动安装
- [ ] 前端代码编辑器
- [ ] 执行日志查看

### P2 - 高级功能
- [ ] 沙箱安全隔离
- [ ] 脚本版本管理
- [ ] 脚本模板市场
- [ ] 性能分析

## ⚠️ 注意事项

1. **安全性** - Python 脚本必须有沙箱隔离
2. **性能** - 避免频繁创建 Python 进程，考虑进程池
3. **依赖** - requirements 安装可能失败，需要重试机制
4. **错误处理** - Python 异常要转换为友好错误信息
5. **大文件** - 图片/视频通过 URL 传递，不要直接传二进制
