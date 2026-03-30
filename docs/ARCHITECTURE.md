# 技术架构设计文档

## 1. 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    前端 (React + React Flow)                 │
│  画布编辑器 | 节点配置 | 结果预览 | WebSocket 实时推送          │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP/WebSocket
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              API 层 (Spring Boot 3.x + WebFlux)              │
│  工作流 CRUD | 任务提交 | 状态查询 | WebSocket 网关            │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│  MongoDB     │      │    Redis     │      │    MinIO     │
│  工作流存储   │      │  缓存/队列    │      │  对象存储     │
└──────────────┘      └──────────────┘      └──────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│            执行引擎 (DagWorkflowEngine)                      │
│  DAG 解析 | 拓扑排序 | 并行执行 | 错误重试 | 断点续跑          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              模型适配层 (ModelProvider)                      │
│  SDXL | Flux | Midjourney | Runway | Pika | 可灵 ...        │
└─────────────────────────────────────────────────────────────┘
```

## 2. 核心模块

### 2.1 模型适配层

**设计模式：** 适配器模式 + 策略模式

```java
interface ModelProvider {
    ModelType getType();
    Mono<GenerationResult> generate(GenerationRequest request);
    Mono<TaskStatus> getStatus(String taskId);
    Mono<Void> cancel(String taskId);
}
```

**已实现适配器：**
- `SdxlAdapter` - Stable Diffusion XL (Replicate/Fal.ai)
- `FluxAdapter` - Flux 模型 (待实现)
- `MidjourneyAdapter` - Midjourney (待实现)
- `RunwayAdapter` - Runway Gen-2 (待实现)

### 2.2 工作流引擎

**核心算法：** Kahn 拓扑排序

**执行流程：**
1. 解析工作流，构建 DAG
2. 拓扑排序，确定执行顺序
3. 并行执行无依赖的节点
4. 节点完成后触发下游节点
5. 所有节点完成后结束

**状态机：**
```
PENDING → RUNNING → SUCCESS/FAILED/CANCELLED
```

### 2.3 数据模型

**Workflow (工作流定义)**
```json
{
  "id": "wf-001",
  "name": "SDXL 生图工作流",
  "nodes": [...],
  "edges": [...],
  "createdAt": "2026-03-29T09:00:00Z",
  "version": 1
}
```

**WorkflowNode (节点)**
```json
{
  "nodeId": "node-1",
  "type": "MODEL",
  "modelProvider": "sdxl",
  "config": {
    "prompt": "a beautiful cat",
    "width": 1024,
    "height": 1024
  },
  "position": {"x": 100, "y": 200}
}
```

**WorkflowEdge (边)**
```json
{
  "id": "edge-1",
  "source": "node-1",
  "target": "node-2",
  "dataType": "image"
}
```

## 3. API 设计

### 3.1 工作流管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/workflows | 创建工作流 |
| GET | /api/workflows | 获取工作流列表 |
| GET | /api/workflows/{id} | 获取工作流详情 |
| PUT | /api/workflows/{id} | 更新工作流 |
| DELETE | /api/workflows/{id} | 删除工作流 |

### 3.2 执行管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/workflows/{id}/execute | 执行工作流 |
| GET | /api/executions/{id} | 获取执行状态 |
| POST | /api/executions/{id}/cancel | 取消执行 |
| POST | /api/executions/{id}/retry | 重试失败节点 |

### 3.3 WebSocket

| 事件 | 方向 | 描述 |
|------|------|------|
| `execution:start` | Server→Client | 执行开始 |
| `execution:progress` | Server→Client | 执行进度 |
| `execution:complete` | Server→Client | 执行完成 |
| `execution:failed` | Server→Client | 执行失败 |

## 4. 部署架构

### 4.1 开发环境
```
单机部署：
- Spring Boot (8080)
- MongoDB (27017)
- Redis (6379)
- Kafka (9092)
- MinIO (9000)
```

### 4.2 生产环境
```
容器化部署 (Docker Compose / K8s):
- API 服务 × N (横向扩展)
- 执行引擎 × N (按 GPU 资源)
- MongoDB 副本集
- Redis Sentinel
- Kafka 集群
- MinIO 集群
```

## 5. 性能优化

### 5.1 前端
- 虚拟滚动（大量节点时）
- Web Worker（布局计算）
- 增量保存（localStorage）

### 5.2 后端
- 响应式编程（WebFlux）
- 异步执行（CompletableFuture）
- 连接池（数据库/Redis）

### 5.3 执行引擎
- 并行执行（无依赖节点）
- 惰性执行（只执行变更部分）
- 结果缓存（中间产物复用）

## 6. 安全考虑

- API 认证（JWT）
- 工作流权限控制
- 模型 API Key 加密存储
- 文件上传大小限制
- WebSocket 连接鉴权
