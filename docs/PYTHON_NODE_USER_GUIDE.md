# Python 脚本节点 - 用户使用指南

> **版本：** v0.1.1  
> **日期：** 2026-03-30  
> **适用对象：** AI 应用开发者、算法工程师、产品经理

---

## 📖 目录

1. [什么是 Python 脚本节点](#什么是-python-脚本节点)
2. [快速开始](#快速开始)
3. [数据类型](#数据类型)
4. [常用场景](#常用场景)
5. [最佳实践](#最佳实践)
6. [故障排查](#故障排查)
7. [FAQ](#faq)

---

## 什么是 Python 脚本节点

Python 脚本节点允许你在工作流中插入自定义的 Python 代码，实现灵活的数据处理。

### 核心价值

- **灵活性：** 不受预定义节点限制，可编写任意 Python 代码
- **扩展性：** 可安装第三方库（Pillow、ffmpeg、numpy 等）
- **易用性：** 简单的输入输出接口，5 分钟上手

### 适用场景

- ✅ 数据格式转换（JSON ↔ 文本）
- ✅ 图片/视频预处理
- ✅ 结果后处理（添加水印、裁剪等）
- ✅ 多节点输出聚合
- ✅ 条件分支逻辑
- ✅ 自定义计算逻辑

### 不适用场景

- ❌ 长时间运行任务（>5 分钟）
- ❌ 需要持久化存储
- ❌ 需要访问外部服务（需明确声明依赖）

---

## 快速开始

### 步骤 1: 创建节点

在工作流画布中添加 Python 脚本节点：

```json
{
  "nodeId": "python-1",
  "type": "PYTHON_SCRIPT",
  "inputs": [
    {"id": "input_1", "label": "输入", "type": "any"}
  ],
  "outputs": [
    {"id": "output_1", "label": "输出", "type": "any"}
  ],
  "config": {
    "script": "# 编写你的 Python 代码",
    "timeout": 30,
    "requirements": []
  }
}
```

### 步骤 2: 编写代码

```python
# 读取输入
input_data = inputs.get('input_1', {})

# 处理数据
if input_data.get('type') == 'text':
    text = input_data.get('content', '')
    result = text.upper()
else:
    result = '不支持的数据类型'

# 输出结果
outputs['output_1'] = {
    'type': 'text',
    'content': result
}
```

### 步骤 3: 连接节点

1. 将上游节点的输出连接到 Python 节点的输入端口
2. 将 Python 节点的输出连接到下游节点的输入端口
3. 点击"运行"按钮测试

---

## 数据类型

Python 节点支持 5 种数据类型：

### 📝 text - 文本

```python
# 输入
inputs['text_input'] = {
    'type': 'text',
    'content': 'Hello World'
}

# 输出
outputs['text_output'] = {
    'type': 'text',
    'content': '处理后的文本'
}
```

### 🖼️ image - 图片

```python
# 输入
inputs['image_input'] = {
    'type': 'image',
    'url': 'https://example.com/image.jpg',
    'width': 1024,
    'height': 1024
}

# 输出
outputs['image_output'] = {
    'type': 'image',
    'url': 'https://example.com/processed.jpg',
    'width': 512,
    'height': 512
}
```

### 🎬 video - 视频

```python
# 输入
inputs['video_input'] = {
    'type': 'video',
    'url': 'https://example.com/video.mp4',
    'duration': 30,
    'fps': 30
}

# 输出
outputs['video_output'] = {
    'type': 'video',
    'url': 'https://example.com/processed.mp4',
    'duration': 15,
    'fps': 30
}
```

### 🎵 audio - 音频

```python
# 输入
inputs['audio_input'] = {
    'type': 'audio',
    'url': 'https://example.com/audio.mp3',
    'duration': 180,
    'format': 'mp3'
}

# 输出
outputs['audio_output'] = {
    'type': 'audio',
    'url': 'https://example.com/processed.mp3',
    'duration': 180,
    'format': 'mp3'
}
```

### 📦 json - 结构化数据

```python
# 输入
inputs['json_input'] = {
    'type': 'json',
    'data': {
        'key1': 'value1',
        'key2': 123
    }
}

# 输出
outputs['json_output'] = {
    'type': 'json',
    'data': {
        'processed': True,
        'result': 'success'
    }
}
```

---

## 常用场景

### 场景 1: 文本预处理

```python
# 清理文本中的特殊字符
import re

text_data = inputs.get('input_text', {})
text = text_data.get('content', '')

# 移除特殊字符
cleaned = re.sub(r'[^\w\s]', '', text)

outputs['cleaned_text'] = {
    'type': 'text',
    'content': cleaned
}
```

### 场景 2: 图片尺寸调整

```python
# requirements = ['pillow', 'requests']
from PIL import Image
import requests
import io

image_data = inputs.get('image', {})
url = image_data.get('url', '')

# 下载并调整尺寸
response = requests.get(url)
img = Image.open(io.BytesIO(response.content))
img_resized = img.resize((512, 512))

# 保存（实际应上传到对象存储）
output_path = '/tmp/output.jpg'
img_resized.save(output_path)

outputs['resized_image'] = {
    'type': 'image',
    'url': output_path,
    'width': 512,
    'height': 512
}
```

### 场景 3: 多结果聚合

```python
# 收集所有上游节点的输出
results = []

for key, value in inputs.items():
    if isinstance(value, dict):
        if 'content' in value:
            results.append(f"{key}: {value['content']}")
        elif 'data' in value:
            results.append(f"{key}: {value['data']}")

# 合并输出
outputs['summary'] = {
    'type': 'text',
    'content': '\n\n'.join(results)
}
```

### 场景 4: 条件分支

```python
text_data = inputs.get('input', {})
text = text_data.get('content', '')

# 根据内容长度决定处理逻辑
if len(text) > 500:
    # 长文本：提取摘要
    summary = text[:500] + '...'
    outputs['result'] = {
        'type': 'text',
        'content': f'[摘要] {summary}'
    }
else:
    # 短文本：直接输出
    outputs['result'] = {
        'type': 'text',
        'content': text
    }
```

### 场景 5: 数据格式转换

```python
# JSON 转 Markdown 报告
json_data = inputs.get('data', {})
data = json_data.get('data', {})

report = ['# 数据报告', '']
for key, value in data.items():
    report.append(f'## {key}')
    report.append(f'{value}')
    report.append('')

outputs['report'] = {
    'type': 'text',
    'content': '\n'.join(report)
}
```

---

## 最佳实践

### ✅ 推荐做法

1. **错误处理**
   ```python
   try:
       result = process(inputs)
       outputs['result'] = result
   except Exception as e:
       outputs['_error'] = {
           'type': 'text',
           'content': f'处理失败：{str(e)}'
       }
   ```

2. **日志输出**
   ```python
   print(f"开始处理，输入：{len(inputs)} 个")
   # ... 处理逻辑 ...
   print(f"处理完成，输出：{len(outputs)} 个")
   ```

3. **依赖管理**
   ```json
   {
     "requirements": [
       "requests==2.31.0",
       "pillow==10.2.0"
     ]
   }
   ```

4. **超时设置**
   ```json
   {
     "timeout": 60  // 长任务适当增加超时时间
   }
   ```

### ❌ 避免做法

1. **不要在 JSON 中传递大文件**
   ```python
   # ❌ 错误：base64 会导致 JSON 过大
   image_base64 = inputs['image']['base64']
   
   # ✅ 正确：使用 URL 传递
   image_url = inputs['image']['url']
   ```

2. **不要硬编码敏感信息**
   ```python
   # ❌ 错误：硬编码 API Key
   api_key = "sk-123456"
   
   # ✅ 正确：从环境变量读取
   import os
   api_key = os.getenv('API_KEY')
   ```

3. **不要无限循环**
   ```python
   # ❌ 错误：可能超时
   while True:
       process()
   
   # ✅ 正确：设置循环上限
   for i in range(100):
       process()
   ```

---

## 故障排查

### 问题 1: 脚本执行超时

**症状：** 错误信息 "Execution timeout after 30s"

**解决方案：**
1. 检查代码是否有死循环
2. 增加超时时间配置
3. 优化算法复杂度

```json
{
  "timeout": 60  // 增加到 60 秒
}
```

### 问题 2: 依赖安装失败

**症状：** 错误信息 "Failed to install requirements"

**解决方案：**
1. 检查依赖包名称是否正确
2. 指定明确的版本号
3. 检查网络连接

```json
{
  "requirements": [
    "requests==2.31.0",  // 指定版本
    "pillow==10.2.0"
  ]
}
```

### 问题 3: 输入数据格式错误

**症状：** 错误信息 "KeyError: 'input_1'"

**解决方案：**
1. 检查输入端口名称是否匹配
2. 使用 `.get()` 方法提供默认值
3. 添加数据验证

```python
# ✅ 安全做法
input_data = inputs.get('input_1', {})
if not input_data:
    raise ValueError("缺少输入数据")
```

### 问题 4: 输出格式不正确

**症状：** 下游节点无法接收数据

**解决方案：**
1. 确保输出包含 `type` 字段
2. 检查数据类型是否匹配
3. 验证输出端口名称

```python
# ✅ 正确格式
outputs['output_1'] = {
    'type': 'text',  // 必须
    'content': '结果'  // 根据 type 变化
}
```

---

## FAQ

### Q: Python 节点支持哪些版本？

**A:** 支持 Python 3.9+，推荐使用 Python 3.11。

### Q: 可以访问互联网吗？

**A:** 可以，但需要在 `requirements` 中声明依赖。建议使用 `requests` 等库访问外部 API。

### Q: 脚本有大小限制吗？

**A:** 建议脚本不超过 10KB。复杂逻辑建议拆分为多个节点。

### Q: 可以读写文件吗？

**A:** 只能读写临时目录（`/tmp`），不能访问系统文件。

### Q: 如何调试脚本？

**A:** 使用 `print()` 输出调试信息，执行日志会显示所有输出。

### Q: 支持多线程/多进程吗？

**A:** 支持，但需要注意资源限制。高频场景建议使用进程池优化。

### Q: 执行失败会怎样？

**A:** 工作流会停止执行，错误信息会记录到执行日志中。

### Q: 可以调用其他 Python 节点吗？

**A:** 不推荐。建议通过工作流连接实现节点间通信。

---

## 📚 相关文档

- [设计文档](python-node-design.md) - 架构设计
- [使用示例](python-node-examples.md) - 7 个场景示例
- [实现总结](PYTHON_NODE_IMPLEMENTATION.md) - 技术细节
- [API 文档](API.md) - REST API 接口

---

**最后更新：** 2026-03-30  
**维护者：** AI Workflow Team  
**反馈：** 如有问题请提交 Issue
