/**
 * Python 脚本节点数据类型
 */

export interface PythonNodeData {
  script: string;
  timeout?: number;
  requirements?: string[];
  pythonVersion?: string;
}

export interface WorkflowInput {
  id: string;
  label: string;
  type: 'any' | 'text' | 'image' | 'video' | 'audio' | 'json';
}

export interface WorkflowOutput {
  id: string;
  label: string;
  type: 'any' | 'text' | 'image' | 'video' | 'audio' | 'json';
}

/**
 * 工作流数据类型
 */
export type DataType = 'text' | 'image' | 'video' | 'audio' | 'json';

export interface WorkflowData {
  type: DataType;
  content: any;
  metadata?: {
    width?: number;
    height?: number;
    duration?: number;
    fps?: number;
    format?: string;
    [key: string]: any;
  };
  sourceNode?: string;
}

/**
 * 数据类型图标映射
 */
export const DataTypeIcons: Record<DataType, string> = {
  text: '📝',
  image: '🖼️',
  video: '🎬',
  audio: '🎵',
  json: '📦',
};

/**
 * Python 脚本模板
 */
export const PythonTemplates = {
  text: `# 文本处理示例
text_data = inputs.get('input_1', {})
text = text_data.get('content', '')

outputs['output_1'] = {
    'type': 'text',
    'content': f'[处理] {text}'
}
`,

  image: `# 图片处理示例
# requirements = ['pillow', 'requests']
import requests
from PIL import Image
import io

image_data = inputs.get('input_1', {})
image_url = image_data.get('url', '')

# 下载并处理图片
response = requests.get(image_url)
img = Image.open(io.BytesIO(response.content))

# 这里添加你的图片处理逻辑
# img = img.resize((512, 512))

outputs['output_1'] = {
    'type': 'image',
    'url': image_url,
    'width': 512,
    'height': 512
}
`,

  video: `# 视频处理示例
# requirements = ['ffmpeg-python']
import ffmpeg

video_data = inputs.get('input_1', {})
video_url = video_data.get('url', '')

# 视频处理逻辑
# ffmpeg.input(video_url).output('output.mp4').run()

outputs['output_1'] = {
    'type': 'video',
    'url': video_url,
    'duration': 10
}
`,

  audio: `# 音频处理示例
# requirements = ['mutagen']
from mutagen.mp3 import MP3

audio_data = inputs.get('input_1', {})
audio_url = audio_data.get('url', '')

# 分析音频
# audio = MP3(audio_url)

outputs['output_1'] = {
    'type': 'audio',
    'url': audio_url,
    'duration': 180
}
`,

  json: `# JSON 数据处理示例
json_data = inputs.get('input_1', {})
data = json_data.get('data', {})

# 数据处理逻辑
result = {'processed': True, 'data': data}

outputs['output_1'] = {
    'type': 'json',
    'data': result
}
`,

  multi: `# 多输入聚合示例
results = []

for key, value in inputs.items():
    if isinstance(value, dict) and 'content' in value:
        results.append(value['content'])

outputs['output_1'] = {
    'type': 'text',
    'content': '\\n'.join(results)
}
`,
};
