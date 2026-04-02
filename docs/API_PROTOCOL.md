# AI Workflow Platform - 前后端接口协议

## 版本：v1.0.0
## 日期：2026-04-02

---

## 1. 工作流相关

### 1.1 获取工作流列表
```
GET /api/v1/workflows
Response: { workflows: Workflow[] }
```

### 1.2 获取工作流详情
```
GET /api/v1/workflows/{id}
Response: { workflow: Workflow }
```

### 1.3 保存工作流
```
POST /api/v1/workflows
Body: { name, nodes, edges, viewport }
Response: { workflowId }
```

### 1.4 执行工作流
```
POST /api/v1/workflows/{id}/execute
Response: { executionId, status }
```

---

## 2. 节点状态（WebSocket）

### 2.1 连接
```
WS /api/v1/ws/status
```

### 2.2 消息格式
```json
{
  "type": "node_status",
  "data": {
    "nodeId": "node_1",
    "status": "running|success|error|idle",
    "progress": 50,
    "message": "处理中...",
    "timestamp": "2026-04-02T11:30:00Z"
  }
}
```

### 2.3 节点状态枚举
- `idle` - 空闲
- `pending` - 等待中
- `running` - 运行中
- `success` - 成功
- `error` - 失败
- `skipped` - 跳过

---

## 3. 日志流（SSE）

### 3.1 获取日志流
```
GET /api/v1/executions/{id}/logs/stream
Event: log
Data: {
  "id": "log_1",
  "timestamp": "2026-04-02T11:30:00Z",
  "level": "info|warn|error",
  "nodeId": "node_1",
  "message": "日志内容"
}
```

---

## 4. 项目文件

### 4.1 导出项目
```
GET /api/v1/projects/{id}/export
Response: .awf.json 文件
```

### 4.2 导入项目
```
POST /api/v1/projects/import
Body: multipart/form-data (.awf.json)
Response: { projectId }
```

---

## 5. 实时推送（统一 WebSocket）

### 5.1 连接
```
WS /api/v1/ws/realtime?executionId={id}
```

### 5.2 消息类型
| type | 说明 |
|------|------|
| node_status | 节点状态变更 |
| log | 日志推送 |
| execution_complete | 执行完成 |
| error | 错误通知 |

---

## 6. 前端调用示例

```typescript
// 获取工作流
const workflows = await fetch('/api/v1/workflows').then(r => r.json());

// 执行工作流
const result = await fetch('/api/v1/workflows/wf_123/execute', { 
  method: 'POST' 
}).then(r => r.json());

// WebSocket 监听状态
const ws = new WebSocket('ws://localhost:8080/api/v1/ws/realtime?executionId=exec_123');
ws.onmessage = (e) => {
  const msg = JSON.parse(e.data);
  if (msg.type === 'node_status') {
    updateNodeStatus(msg.data);
  }
};
```