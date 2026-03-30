# AI Workflow Platform - 多模型工作流画布

> 可视化编排生图/生视频 AI 模型，降低应用开发门槛

## 🎯 产品定位

- **目标用户：** AI 应用开发者、算法工程师、技术 Leader、产品经理
- **核心价值：** 拖拽式节点编排，分钟级调整模型链路，无需重新部署

## 📋 功能规划

### MVP (P0) - 4-6 周
- [x] 项目骨架创建
- [x] 模型适配器框架
- [x] 第三方 API 接入（可灵/Wan/Seedance/NanoBanana）
- [x] 执行引擎（DAG 拓扑排序 + Python 脚本节点）
- [x] Python 脚本节点支持
- [ ] 画布编辑器（React Flow）
- [ ] 参数配置面板
- [ ] 执行结果查看
- [ ] 工作流 CRUD

### 进阶 (P1) - 8-12 周
- [ ] 版本管理
- [ ] 协作功能
- [ ] 监控告警
- [ ] 异步执行
- [ ] 批量调度

### 差异化 (P2) - 12 周+
- [x] Python 脚本节点 (自定义数据处理)
- [ ] AI 专用节点
- [ ] 智能参数推荐
- [ ] 输出质量评估
- [ ] 模板市场

## 🛠️ 技术栈

### 后端
- **框架：** Spring Boot 3.x + WebFlux
- **数据库：** MongoDB (工作流存储)
- **缓存：** Redis (状态缓存/任务队列)
- **消息队列：** Kafka (异步事件)
- **对象存储：** MinIO (图片/视频)

### 前端
- **框架：** React 18
- **画布：** React Flow (@xyflow/react)
- **状态管理：** Zustand
- **样式：** Tailwind CSS

### 执行引擎
- **语言：** Java 21 (Virtual Threads)
- **DAG 引擎：** 自研
- **异步：** CompletableFuture + WebFlux
- **脚本支持：** Python 3.9+ (沙箱隔离)

## 📁 项目结构

```
ai-workflow/
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/ben/workflow/
│   │       ├── adapter/     # 模型适配器
│   │       │   ├── kling/       # 可灵
│   │       │   ├── wan/         # 万相
│   │       │   ├── seedance/    # 字节 Seedance
│   │       │   ├── nanobanana/  # NanoBanana
│   │       │   └── stablediffusion/ # SDXL (P1)
│   │       ├── engine/      # 工作流引擎
│   │       │   ├── DagWorkflowEngine.java
│   │       │   ├── PythonScriptExecutor.java
│   │       │   └── ...
│   │       ├── model/       # 数据模型
│   │       │   ├── PythonNodeConfig.java
│   │       │   ├── WorkflowData.java
│   │       │   └── ...
│   │       ├── api/         # REST API
│   │       └── config/      # 配置类
│   └── pom.xml
│
├── frontend/                # React 前端
│   ├── src/
│   │   ├── components/      # 通用组件
│   │   ├── nodes/           # 节点组件
│   │   │   └── PythonNode.tsx (待实现)
│   │   └── hooks/           # 自定义 Hooks
│   └── package.json
│
├── docs/                    # 文档
│   ├── python-node-design.md        # Python 节点设计
│   ├── python-node-examples.md      # 使用示例
│   └── PYTHON_NODE_IMPLEMENTATION.md # 实现总结
└── scripts/                 # 部署脚本
```

## 🚀 快速开始

### 环境要求
- Java 21+
- Node.js 18+
- MongoDB 6+
- Redis 7+
- Kafka 3+
- MinIO (可选)

### 1. 启动 MongoDB
```bash
docker run -d --name mongodb -p 27017:27017 mongo:6
```

### 2. 启动后端
```bash
cd backend
mvn spring-boot:run
```

### 3. 运行测试
```bash
# 一键全流程测试
./scripts/test-full-flow.sh

# 或单独测试 API
./scripts/test-api.sh
```

**预期结果：**
```
✅ 工作流创建成功
✅ 执行已启动
✅ 全流程测试通过！
```

### 4. 启动前端（待实现）
```bash
cd frontend
npm install
npm run dev
```

### 访问
- 后端 API：http://localhost:8080/api/v1
- WebSocket：ws://localhost:8080/ws-native
- API 文档：http://localhost:8080/api-docs (待实现)

## 🐍 Python 脚本节点

**新增于 v0.1.1 (2026-03-30)**

支持在工作流中插入自定义 Python 脚本节点，实现灵活的数据处理。

### 核心功能
- ✅ 接收上游节点数据（文本/图片/视频/音频/JSON）
- ✅ 执行 Python 脚本处理
- ✅ 输出结果给下游节点
- ✅ 依赖自动安装 (requirements)
- ✅ 超时控制 (默认 30s)
- ✅ 沙箱隔离执行

### 快速示例

```python
# 输入：上游节点数据
inputs = {
    'image_input': {
        'type': 'image',
        'url': 'https://...',
        'width': 1024,
        'height': 1024
    }
}

# 处理：使用 Pillow 调整图片尺寸
from PIL import Image
import requests

response = requests.get(inputs['image_input']['url'])
img = Image.open(response.content)
img_resized = img.resize((512, 512))

# 输出：给下游节点
outputs['output_image'] = {
    'type': 'image',
    'url': '保存后的 URL',
    'width': 512,
    'height': 512
}
```

### 节点配置

```json
{
  "nodeId": "python-1",
  "type": "PYTHON_SCRIPT",
  "config": {
    "script": "# Python 代码",
    "timeout": 30,
    "requirements": ["requests", "pillow"]
  }
}
```

### 📚 详细文档
- [📖 用户指南](docs/PYTHON_NODE_USER_GUIDE.md) - 完整使用教程
- [📐 设计文档](docs/python-node-design.md) - 架构设计
- [💡 使用示例](docs/python-node-examples.md) - 7 个场景示例
- [🔧 实现总结](docs/PYTHON_NODE_IMPLEMENTATION.md) - 技术细节

---

## 📊 模型支持

### ✅ 已实现 (P0)
| 模型 | 类型 | 提供方 | 状态 |
|------|------|--------|------|
| **可灵 (Kling)** | 生视频 | 快手 | 🟡 适配器框架完成，待 API 对接 |
| **Wan (万相)** | 生图/视频 | 阿里云 | 🟡 适配器框架完成，待 API 对接 |
| **Seedance** | 生视频 | 字节跳动 | 🟡 适配器框架完成，待 API 对接 |
| **NanoBanana** | 生图 | 第三方 | 🟡 适配器框架完成，待 API 对接 |

### 📋 计划中 (P1)
| 模型 | 类型 | 提供方 | 优先级 |
|------|------|--------|--------|
| SDXL | 生图 | Replicate/Fal.ai | P1 |
| Flux | 生图 | Fal.ai | P1 |
| Midjourney | 生图 | Discord Bot | P2 |
| DALL-E 3 | 生图 | OpenAI | P2 |

### 🔮 未来 (P2)
- 自部署 Stable Diffusion
- 自部署 SVD (视频)
- 其他开源模型

## 🔧 API 配置说明

### 可灵 (Kling)
```bash
# 获取 API Key
# 1. 访问 https://platform.kuaishou.com/
# 2. 创建应用获取 Key
# 3. 设置 KLING_API_KEY 环境变量
```

### 万相 (Wan)
```bash
# 获取 API Key
# 1. 访问阿里云 DashScope
# 2. 开通万相服务
# 3. 创建 API Key
# 4. 设置 WAN_API_KEY 环境变量
```

### Seedance
```bash
# 获取 API Key
# 1. 访问火山引擎方舟平台
# 2. 开通 Seedance 服务
# 3. 创建访问令牌
# 4. 设置 SEEDANCE_API_KEY 环境变量
```

### NanoBanana
```bash
# 获取 API Key
# 1. 访问 NanoBanana 官网
# 2. 注册账号获取 Key
# 3. 设置 NANOBANANA_API_KEY 环境变量
```

## 📝 变更记录

### v0.1.1 (2026-03-30) - Python 脚本节点
- [x] Python 脚本节点后端实现
  - [x] `PythonNodeConfig.java` - 节点配置模型
  - [x] `WorkflowData.java` - 统一数据类型
  - [x] `PythonScriptExecutor.java` - 脚本执行器
  - [x] `PythonExecutionResult.java` - 执行结果封装
  - [x] `DagWorkflowEngine.java` - 集成 Python 节点执行
- [x] 前端类型定义 (`python-node.ts`)
- [x] 完整文档
  - [x] 设计文档
  - [x] 使用示例 (7 个场景)
  - [x] 实现总结
- [x] 单元测试 (11/11 通过)

### v0.1.0 (2026-03-29) - 项目初始化
- [x] 项目初始化
- [x] 核心模型定义
- [x] 工作流引擎骨架
- [x] 第三方模型适配器框架
  - [x] 可灵 (Kling)
  - [x] 万相 (Wan)
  - [x] Seedance
  - [x] NanoBanana

## 📄 License

MIT
