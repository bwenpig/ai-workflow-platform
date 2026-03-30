# Python 脚本节点使用示例

> **版本：** v0.1.1 (2026-03-30)  
> **状态：** ✅ 已完成  
> **测试：** 11/11 通过

## 🚀 快速入门

### 1. 创建 Python 节点

在工作流画布中添加一个 Python 脚本节点，配置如下：

```json
{
  "nodeId": "python-1",
  "type": "PYTHON_SCRIPT",
  "inputs": [
    {"id": "input_text", "label": "输入文本", "type": "text"}
  ],
  "outputs": [
    {"id": "output_text", "label": "输出文本", "type": "text"}
  ],
  "config": {
    "script": "# 在这里编写 Python 代码",
    "timeout": 30,
    "requirements": []
  }
}
```

### 2. 编写脚本

```python
# 读取输入
text_data = inputs.get('input_text', {})
text = text_data.get('content', '')

# 处理数据
result = text.upper()

# 输出结果
outputs['output_text'] = {
    'type': 'text',
    'content': result
}
```

### 3. 连接节点

- 将上游节点的输出连接到 Python 节点的输入
- 将 Python 节点的输出连接到下游节点

---

## 📝 示例 1: 文本处理

**功能：** 将上游文本转为大写并添加前缀

```python
# 输入：input_text (text 类型)
# 输出：output_text (text 类型)

text_data = inputs.get('input_text', {})
text = text_data.get('content', '')

outputs['output_text'] = {
    'type': 'text',
    'content': f'[处理结果] {text.upper()}'
}
```

---

## 🖼️ 示例 2: 图片处理

**功能：** 使用 Pillow 调整图片尺寸

```python
# 需要先安装依赖：requirements = ['pillow', 'requests']
import requests
from PIL import Image
import io

# 获取上游图片
image_data = inputs.get('image_input', {})
image_url = image_data.get('url', '')

# 下载图片
response = requests.get(image_url)
img = Image.open(io.BytesIO(response.content))

# 调整尺寸
new_size = (512, 512)
img_resized = img.resize(new_size)

# 保存到临时位置 (实际应该上传到对象存储)
output_path = 'output.jpg'
img_resized.save(output_path)

outputs['output_image'] = {
    'type': 'image',
    'url': output_path,
    'width': 512,
    'height': 512
}
```

---

## 🎬 示例 3: 视频处理

**功能：** 使用 ffmpeg 裁剪视频

```python
# requirements = ['ffmpeg-python']
import ffmpeg

# 获取上游视频
video_data = inputs.get('video_input', {})
video_url = video_data.get('url', '')

# 裁剪前 10 秒
output_path = 'output.mp4'
ffmpeg.input(video_url).output(output_path, t=10).run()

outputs['output_video'] = {
    'type': 'video',
    'url': output_path,
    'duration': 10,
    'fps': 30
}
```

---

## 🎵 示例 4: 音频处理

**功能：** 提取音频时长信息

```python
# requirements = ['mutagen']
from mutagen.mp3 import MP3

# 获取上游音频
audio_data = inputs.get('audio_input', {})
audio_url = audio_data.get('url', '')

# 分析音频
audio = MP3(audio_url)
duration = audio.info.length

outputs['audio_info'] = {
    'type': 'json',
    'data': {
        'duration': duration,
        'bitrate': audio.info.bitrate,
        'sample_rate': audio.info.sample_rate
    }
}
```

---

## 📦 示例 5: 多输入聚合

**功能：** 合并多个上游节点的输出

```python
# 收集所有输入
results = []

for key, value in inputs.items():
    if isinstance(value, dict) and 'content' in value:
        results.append(value['content'])

# 合并输出
outputs['merged'] = {
    'type': 'text',
    'content': '\n---\n'.join(results)
}
```

---

## 🔄 示例 6: 条件分支

**功能：** 根据输入内容决定输出

```python
text_data = inputs.get('input_text', {})
text = text_data.get('content', '')

# 根据内容长度决定输出类型
if len(text) > 100:
    outputs['output'] = {
        'type': 'text',
        'content': f'长文本 ({len(text)} 字)'
    }
else:
    outputs['output'] = {
        'type': 'text',
        'content': f'短文本 ({len(text)} 字)'
    }
```

---

## 📊 示例 7: 数据格式转换

**功能：** JSON 转文本报告

```python
# 获取 JSON 数据
json_data = inputs.get('json_input', {})
data = json_data.get('data', {})

# 生成报告
report = []
report.append("# 数据报告")
report.append("")
for key, value in data.items():
    report.append(f"- **{key}**: {value}")

outputs['report'] = {
    'type': 'text',
    'content': '\n'.join(report)
}
```

---

## ⚠️ 注意事项

### 1. 输入数据格式

所有输入都是字典格式：
```python
inputs = {
    'input_1': {
        'type': 'text|image|video|audio|json',
        'content': '实际内容',
        'metadata': {...}  # 可选
    }
}
```

### 2. 输出数据格式

必须通过 `outputs` 字典输出：
```python
outputs['output_name'] = {
    'type': 'text|image|video|audio|json',
    'content': '实际内容',
    'metadata': {...}  # 可选
}
```

### 3. 错误处理

```python
try:
    # 你的代码
    pass
except Exception as e:
    outputs['_error'] = {
        'type': 'text',
        'content': f'处理失败：{str(e)}'
    }
```

### 4. 依赖管理

在节点配置中指定：
```json
{
  "requirements": ["requests", "pillow", "ffmpeg-python"]
}
```

### 5. 超时设置

默认 30 秒，可在节点配置中调整：
```json
{
  "timeout": 60
}
```

---

## 🔌 完整工作流示例

```
[输入节点：文本] 
       ↓
[Python 节点：文本处理]
       ↓
[模型节点：AI 生图]
       ↓
[Python 节点：图片水印]
       ↓
[输出节点：结果]
```

每个节点都可以通过 Python 脚本进行自定义处理！

---

## 💡 最佳实践

### 1. 错误处理

始终捕获异常并提供友好的错误信息：

```python
try:
    # 你的处理逻辑
    result = process_data(inputs)
    outputs['result'] = {'type': 'json', 'data': result}
except Exception as e:
    outputs['_error'] = {
        'type': 'text',
        'content': f'处理失败：{str(e)}',
        'metadata': {'error_type': type(e).__name__}
    }
```

### 2. 日志记录

使用 print 输出调试信息（会记录到执行日志）：

```python
print(f"开始处理，输入数量：{len(inputs)}")
# ... 处理逻辑 ...
print(f"处理完成，输出数量：{len(outputs)}")
```

### 3. 性能优化

- 避免在脚本中下载大文件，使用 URL 传递
- 使用高效的库（如 numpy 处理数值计算）
- 合理设置超时时间

```python
# ✅ 好的做法：通过 URL 传递图片
image_url = inputs['image']['url']

# ❌ 避免：直接在 JSON 中传递 base64
# image_data = inputs['image']['base64']  # 可能导致 JSON 过大
```

### 4. 依赖管理

- 只安装必需的依赖
- 指定版本号确保可重复性

```json
{
  "requirements": [
    "requests==2.31.0",
    "pillow==10.2.0",
    "numpy==1.26.3"
  ]
}
```

### 5. 数据类型验证

在处理前验证输入数据类型：

```python
def validate_input(data, expected_type):
    if not isinstance(data, dict):
        raise ValueError(f"期望字典类型，得到 {type(data)}")
    if data.get('type') != expected_type:
        raise ValueError(f"期望 {expected_type} 类型，得到 {data.get('type')}")
    return data

# 使用
image_data = validate_input(inputs.get('image'), 'image')
```

---

## 📚 相关文档

- [设计文档](python-node-design.md) - 架构设计
- [实现总结](PYTHON_NODE_IMPLEMENTATION.md) - 技术细节
- [API 文档](API.md) - REST API 接口

---

**最后更新：** 2026-03-30  
**维护者：** AI Workflow Team
