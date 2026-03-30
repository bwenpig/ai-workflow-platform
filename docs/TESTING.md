# 全流程测试指南

## 前置条件

### 1. 启动 MongoDB
```bash
docker run -d --name mongodb -p 27017:27017 mongo:6
```

### 2. 启动后端
```bash
cd ~/.openclaw/workspace-coder/ai-workflow/backend
mvn spring-boot:run
```

**预期输出：**
```
2026-03-29 10:00:00 [main] INFO  - Started Application in 5.123 seconds
2026-03-29 10:00:00 [main] INFO  - Tomcat started on port(s): 8080 (http)
```

---

## 测试步骤

### 方案 A: 一键测试脚本（推荐）

```bash
cd ~/.openclaw/workspace-coder/ai-workflow
./scripts/test-full-flow.sh
```

**预期输出：**
```
======================================
AI Workflow Platform - 全流程测试
======================================

🔍 检查后端服务...
✅ 后端服务运行中

1️⃣  创建可灵视频生成工作流...
{
  "id": "wf-abc123",
  "name": "可灵视频生成工作流",
  ...
}

✅ 工作流创建成功：wf-abc123

2️⃣  获取工作流详情...
...

3️⃣  执行工作流...
{
  "executionId": "exec-xyz789",
  "status": "PENDING",
  "workflowId": "wf-abc123"
}

✅ 执行已启动：exec-xyz789

4️⃣  等待执行完成...

5️⃣  查询执行状态...
{
  "id": "exec-xyz789",
  "status": "SUCCESS",
  "nodeStates": {
    "input-1": {"status": "SUCCESS"},
    "model-kling": {
      "status": "SUCCESS",
      "result": {
        "type": "video",
        "url": "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "previewUrl": "https://via.placeholder.com/640x360.png?text=Kling+Video",
        "duration": 5,
        "fps": 24
      }
    }
  }
}

执行状态：SUCCESS

✅ 全流程测试通过！
======================================
```

---

### 方案 B: 手动测试

#### 1. 创建工作流
```bash
curl -X POST http://localhost:8080/api/v1/workflows \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ben-test" \
  -d '{
    "name": "可灵视频生成工作流",
    "nodes": [
      {
        "nodeId": "input-1",
        "type": "INPUT",
        "position": {"x": 100, "y": 100},
        "config": {"value": "a cat running"}
      },
      {
        "nodeId": "model-kling",
        "type": "MODEL",
        "modelProvider": "kling",
        "position": {"x": 300, "y": 100},
        "config": {"prompt": "a beautiful cat", "duration": 5}
      },
      {
        "nodeId": "output-1",
        "type": "OUTPUT",
        "position": {"x": 500, "y": 100}
      }
    ],
    "edges": [
      {"id": "e1-2", "source": "input-1", "target": "model-kling", "dataType": "text"},
      {"id": "e2-3", "source": "model-kling", "target": "output-1", "dataType": "video"}
    ]
  }'
```

#### 2. 执行工作流
```bash
curl -X POST http://localhost:8080/api/v1/workflows/{workflowId}/execute \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ben-test" \
  -d '{}'
```

#### 3. 查询执行状态
```bash
curl http://localhost:8080/api/v1/executions/{executionId}
```

---

## Mock 数据说明

### 可灵 (Kling) - 视频生成
```json
{
  "type": "video",
  "url": "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
  "previewUrl": "https://via.placeholder.com/640x360.png?text=Kling+Video",
  "duration": 5,
  "fps": 24
}
```

### 万相 (Wan) - 图片生成
```json
{
  "type": "image",
  "url": "https://via.placeholder.com/1024x1024.png?text=Wan+Image",
  "width": 1024,
  "height": 1024
}
```

### Seedance - 视频生成
```json
{
  "type": "video",
  "url": "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
  "previewUrl": "https://via.placeholder.com/640x360.png?text=Seedance+Video",
  "duration": 10,
  "fps": 30
}
```

### NanoBanana - 图片生成
```json
{
  "type": "image",
  "url": "https://via.placeholder.com/1024x1024.png?text=NanoBanana+Image",
  "width": 1024,
  "height": 1024
}
```

---

## WebSocket 测试

### 使用 wscat 测试

```bash
# 安装 wscat
npm install -g wscat

# 连接 WebSocket
wscat -c ws://localhost:8080/ws-native

# 订阅主题（需要 STOMP 协议，建议使用前端测试）
```

### 前端测试

在浏览器控制台执行：
```javascript
// 执行工作流
fetch('http://localhost:8080/api/v1/workflows/{workflowId}/execute', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-User-Id': 'test-user'
  },
  body: '{}'
})
.then(r => r.json())
.then(data => {
  console.log('Execution ID:', data.executionId);
  
  // 订阅 WebSocket
  const ws = new WebSocket('ws://localhost:8080/ws-native');
  ws.onmessage = (event) => {
    console.log('收到消息:', JSON.parse(event.data));
  };
});
```

---

## 常见问题

### Q1: MongoDB 连接失败
```
com.mongodb.MongoTimeoutException: Timed out after 30000 ms
```
**解决：** 确保 MongoDB 已启动
```bash
docker ps | grep mongodb
```

### Q2: 端口被占用
```
Port 8080 is already in use
```
**解决：** 查找并停止占用端口的进程
```bash
lsof -i :8080
kill -9 <PID>
```

### Q3: 工作流执行失败
**检查：**
1. 工作流定义是否正确（节点/边）
2. 模型 Provider 名称是否匹配
3. 查看后端日志

---

## 下一步

✅ 后端 API + WebSocket + Mock 数据 已完成

**待完成：**
- [ ] 前端画布编辑器
- [ ] 节点配置面板
- [ ] 执行结果预览
- [ ] 真实 API 对接
