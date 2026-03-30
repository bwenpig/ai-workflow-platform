# API 接口文档

## 基础信息

- **Base URL:** `http://localhost:8080/api/v1`
- **认证方式:** `X-User-Id` Header (简化版，后续改为 JWT)
- **数据格式:** JSON

---

## 工作流管理 API

### 1. 创建工作流

**请求：**
```http
POST /api/v1/workflows
Content-Type: application/json
X-User-Id: user-123
```

**请求体：**
```json
{
  "name": "可灵视频生成工作流",
  "description": "使用可灵模型生成视频",
  "nodes": [
    {
      "nodeId": "input-1",
      "type": "INPUT",
      "position": {"x": 100, "y": 100},
      "config": {"label": "输入提示词"}
    },
    {
      "nodeId": "model-kling",
      "type": "MODEL",
      "modelProvider": "kling",
      "position": {"x": 300, "y": 100},
      "config": {
        "prompt": "a cat running",
        "duration": 5
      }
    }
  ],
  "edges": [
    {
      "id": "e1-2",
      "source": "input-1",
      "target": "model-kling",
      "dataType": "text"
    }
  ]
}
```

**响应：** `200 OK`
```json
{
  "id": "wf-abc123",
  "name": "可灵视频生成工作流",
  "description": "使用可灵模型生成视频",
  "nodes": [...],
  "edges": [...],
  "createdBy": "user-123",
  "createdAt": "2026-03-29T09:00:00Z",
  "version": 1,
  "published": false
}
```

---

### 2. 获取工作流列表

**请求：**
```http
GET /api/v1/workflows?published=false&createdBy=user-123
```

**查询参数：**
- `published` (可选): `true`/`false` - 是否只返回已发布
- `createdBy` (可选): 按创建者过滤

**响应：** `200 OK`
```json
[
  {
    "id": "wf-abc123",
    "name": "可灵视频生成工作流",
    "version": 1,
    "published": false,
    "createdAt": "2026-03-29T09:00:00Z"
  }
]
```

---

### 3. 获取工作流详情

**请求：**
```http
GET /api/v1/workflows/{id}
```

**响应：** `200 OK`
```json
{
  "id": "wf-abc123",
  "name": "可灵视频生成工作流",
  "nodes": [...],
  "edges": [...],
  "createdAt": "2026-03-29T09:00:00Z",
  "version": 1
}
```

---

### 4. 更新工作流

**请求：**
```http
PUT /api/v1/workflows/{id}
Content-Type: application/json
X-User-Id: user-123
```

**请求体：**
```json
{
  "name": "更新后的名称",
  "nodes": [...],
  "edges": [...]
}
```

**响应：** `200 OK`
```json
{
  "id": "wf-abc123",
  "name": "更新后的名称",
  "version": 2,
  "updatedAt": "2026-03-29T10:00:00Z"
}
```

---

### 5. 删除工作流

**请求：**
```http
DELETE /api/v1/workflows/{id}
X-User-Id: user-123
```

**响应：** `204 No Content`

---

### 6. 发布/取消发布

**请求：**
```http
POST /api/v1/workflows/{id}/publish
X-User-Id: user-123
```

**响应：** `200 OK`
```json
{
  "id": "wf-abc123",
  "published": true,
  "updatedAt": "2026-03-29T10:00:00Z"
}
```

---

### 7. 执行工作流

**请求：**
```http
POST /api/v1/workflows/{id}/execute
Content-Type: application/json
X-User-Id: user-123
```

**请求体：**
```json
{
  "input-1": {
    "prompt": "a beautiful cat"
  }
}
```

**响应：** `202 Accepted`
```json
{
  "executionId": "exec-xyz789",
  "status": "PENDING",
  "workflowId": "wf-abc123"
}
```

---

## 执行管理 API

### 8. 获取执行状态

**请求：**
```http
GET /api/v1/executions/{id}
```

**响应：** `200 OK`
```json
{
  "id": "exec-xyz789",
  "workflowId": "wf-abc123",
  "status": "RUNNING",
  "inputs": {...},
  "nodeStates": {
    "input-1": {
      "nodeId": "input-1",
      "status": "SUCCESS",
      "startedAt": "2026-03-29T10:00:00Z",
      "endedAt": "2026-03-29T10:00:01Z"
    },
    "model-kling": {
      "nodeId": "model-kling",
      "status": "RUNNING"
    }
  },
  "startedAt": "2026-03-29T10:00:00Z"
}
```

**状态说明：**
- `PENDING` - 等待执行
- `RUNNING` - 执行中
- `SUCCESS` - 执行成功
- `FAILED` - 执行失败
- `CANCELLED` - 已取消

---

### 9. 取消执行

**请求：**
```http
POST /api/v1/executions/{id}/cancel
X-User-Id: user-123
```

**响应：** `200 OK`
```json
{
  "id": "exec-xyz789",
  "status": "CANCELLED",
  "endedAt": "2026-03-29T10:05:00Z"
}
```

---

### 10. 获取执行历史

**请求：**
```http
GET /api/v1/executions/history?userId=user-123&limit=20
```

**查询参数：**
- `userId`: 用户 ID
- `limit` (可选): 返回数量，默认 20

**响应：** `200 OK`
```json
[
  {
    "id": "exec-xyz789",
    "workflowId": "wf-abc123",
    "status": "SUCCESS",
    "startedAt": "2026-03-29T10:00:00Z",
    "endedAt": "2026-03-29T10:02:00Z",
    "durationMs": 120000
  }
]
```

---

## 错误响应

**通用错误格式：**
```json
{
  "timestamp": "2026-03-29T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "工作流不存在：wf-invalid"
}
```

**常见错误码：**
- `400 Bad Request` - 请求参数错误
- `404 Not Found` - 资源不存在
- `500 Internal Server Error` - 服务器错误

---

## WebSocket 接口 (待实现)

**连接 URL:** `ws://localhost:8080/ws`

**订阅主题:** `/topic/executions/{executionId}`

**推送消息格式：**
```json
{
  "type": "execution:progress",
  "executionId": "exec-xyz789",
  "nodeId": "model-kling",
  "status": "RUNNING",
  "progress": 50,
  "timestamp": "2026-03-29T10:01:00Z"
}
```
