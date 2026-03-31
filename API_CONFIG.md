# 模型 API 配置说明

## 概述

阶段 3 已完成 Kling/Wan/Seedance/NanoBanana 四个模型的真实 API 对接，替换了原有的 mock 实现。

## 配置方式

### application.yml 配置

```yaml
model:
  # 可灵 AI (Kling) - 文生视频
  kling:
    api-url: https://api.klingai.com/v1
    api-key: ${KLING_API_KEY:}
  
  # 万相 AI (Wan) - 文生图
  wan:
    api-url: https://dashscope.aliyuncs.com/api/v1
    api-key: ${WAN_API_KEY:}
    model-version: wanx-v1
  
  # Seedance - 文生视频
  seedance:
    api-url: https://ark.cn-beijing.volces.com/api/v1
    api-key: ${SEEDANCE_API_KEY:}
  
  # NanoBanana - 文生图
  nanobanana:
    api-url: https://api.nanobanana.com/v1
    api-key: ${NANOBANANA_API_KEY:}
```

### 环境变量配置（推荐）

```bash
# 导出 API Key 到环境变量
export KLING_API_KEY="your-kling-api-key"
export WAN_API_KEY="your-wan-api-key"
export SEEDANCE_API_KEY="your-seedance-api-key"
export NANOBANANA_API_KEY="your-nanobanana-api-key"
```

## API 对接详情

### 1. KlingExecutor (可灵 AI)

- **类型**: 文生视频
- **API**: `POST /videos/text2video`
- **轮询**: `GET /videos/{task_id}`
- **参数**:
  - `prompt`: 提示词（必填）
  - `duration`: 时长 (5s/10s)
  - `resolution`: 分辨率 (720p/1080p)
  - `fps`: 帧率 (24/30)
  - `creativity`: 创意度 (0-1)

### 2. WanExecutor (万相 AI)

- **类型**: 文生图
- **API**: `POST /services/aigc/text-generation/generation`
- **同步**: 直接返回结果
- **参数**:
  - `prompt`: 提示词（必填）
  - `size`: 尺寸 (1024*1024)
  - `style`: 风格
  - `seed`: 种子值

### 3. SeedanceExecutor (火山方舟)

- **类型**: 文生视频
- **API**: `POST /video/generate`
- **轮询**: `GET /video/task/{task_id}`
- **参数**:
  - `prompt`: 提示词（必填）
  - `duration`: 时长 (秒)
  - `resolution`: 分辨率
  - `fps`: 帧率

### 4. NanoBananaExecutor

- **类型**: 文生图
- **API**: `POST /images/generate`
- **轮询**: `GET /images/task/{task_id}`
- **参数**:
  - `prompt`: 提示词（必填）
  - `width/height`: 尺寸
  - `style`: 风格
  - `steps`: 步数
  - `guidance_scale`: 引导系数

## 错误处理

所有 Executor 都实现了完善的错误处理：

1. **HTTP 错误**: 捕获 WebClientResponseException，记录状态码和响应体
2. **超时处理**: 轮询最多 60 次（2 分钟），超时抛出异常
3. **任务失败**: 检测任务状态为 failed 时立即终止并返回错误信息
4. **空响应**: 检查 API 返回是否为 null，避免 NPE

## 测试覆盖

每个 Executor 都有集成测试：

- `KlingExecutorTest`: 3 个测试用例
- `WanExecutorTest`: 3 个测试用例
- `SeedanceExecutorTest`: 3 个测试用例
- `NanoBananaExecutorTest`: 3 个测试用例

测试场景包括：
- 模型类型验证
- API 调用失败处理（无 API Key）
- 超时处理

## 使用示例

### 通过工作流节点调用

```json
{
  "type": "MODEL",
  "modelProvider": "kling",
  "config": {
    "prompt": "一个机器人在跳舞",
    "duration": "10s",
    "fps": 30
  }
}
```

### 直接调用 Executor

```java
@Autowired
private KlingExecutor klingExecutor;

GenerationRequest request = new GenerationRequest();
request.setPrompt("美丽的风景");
request.setParams(Map.of("duration", "5s", "fps", 24));

GenerationResult result = klingExecutor.generate(request)
    .block(Duration.ofMinutes(5));

if (result.getStatus() == ModelProvider.TaskStatus.SUCCESS) {
    System.out.println("视频 URL: " + result.getOutputUrls().get(0));
}
```

## 注意事项

1. **API Key 安全**: 不要将 API Key 提交到版本控制系统，使用环境变量或配置中心
2. **速率限制**: 各平台有不同的速率限制，生产环境需要实现重试和限流
3. **成本控制**: 视频生成成本较高，建议设置配额和告警
4. **超时配置**: 根据实际网络情况调整超时时间

## 后续优化

- [ ] 实现异步回调支持
- [ ] 添加请求重试机制
- [ ] 实现速率限制和配额管理
- [ ] 添加详细的访问日志
- [ ] 支持批量生成
