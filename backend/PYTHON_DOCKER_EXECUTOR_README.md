# Python Docker 沙箱执行引擎

基于 Docker 容器的 Python 脚本安全执行环境。

## 🎯 功能特性

- ✅ **容器级隔离** - 每个脚本在独立 Docker 容器中执行
- ✅ **资源限制** - 内存、CPU、超时控制
- ✅ **网络隔离** - 默认禁用网络访问
- ✅ **文件系统隔离** - 只读根目录 + 临时目录挂载
- ✅ **日志收集** - 完整的执行日志输出
- ✅ **错误处理** - 详细的错误信息和堆栈跟踪

## 📦 依赖要求

- Docker 20.10+
- Java 17+
- Maven 3.8+

## 🚀 快速开始

### 1. 构建 Docker 镜像

```bash
cd /Users/ben/.openclaw/workspace-coder/ai-workflow/backend
./build-docker-image.sh
```

或者手动构建：

```bash
docker build -t ben/python-sandbox:1.0.0 .
```

### 2. 在代码中使用

```java
import com.ben.workflow.engine.PythonDockerExecutor;
import com.ben.workflow.model.PythonNodeConfig;
import com.ben.workflow.engine.PythonExecutionResult;

// 创建执行器
PythonDockerExecutor executor = new PythonDockerExecutor();

// 准备脚本
String script = """
    result = inputs.get('a', 0) + inputs.get('b', 0)
    outputs['result'] = result
    print(f"计算结果：{result}")
    """;

// 准备输入
Map<String, Object> inputs = new HashMap<>();
inputs.put("a", 10);
inputs.put("b", 20);

// 配置
PythonNodeConfig config = new PythonNodeConfig();
config.setScript(script);
config.setTimeout(30);
config.setMemoryLimit(128L);

// 执行
PythonExecutionResult result = executor.execute(script, inputs, config);

// 处理结果
if (result.isSuccess()) {
    System.out.println("输出：" + result.getOutputs());
    System.out.println("日志：" + result.getLogs());
} else {
    System.err.println("错误：" + result.getError());
}
```

### 3. 自定义配置

```java
PythonNodeConfig config = new PythonNodeConfig();

// 超时控制（秒）
config.setTimeout(60);

// 内存限制（MB）
config.setMemoryLimit(256L);

// 环境变量
config.setEnv(Map.of(
    "API_KEY", "your-api-key",
    "DEBUG", "true"
));

// Python 依赖（动态安装）
config.setRequirements(List.of("requests", "pandas"));
```

## 📊 技术架构

```
┌─────────────────────────────────────┐
│      Java Application               │
│  ┌─────────────────────────────┐   │
│  │  PythonDockerExecutor       │   │
│  │  - 创建容器                 │   │
│  │  - 挂载输入                 │   │
│  │  - 启动执行                 │   │
│  │  - 收集输出                 │   │
│  │  - 清理容器                 │   │
│  └─────────────────────────────┘   │
└──────────────┬──────────────────────┘
               │ Docker API
               ▼
┌─────────────────────────────────────┐
│      Docker Daemon                  │
│  ┌─────────────────────────────┐   │
│  │  Python Sandbox Container   │   │
│  │  - python:3.11-slim         │   │
│  │  - 预装库                   │   │
│  │  - 资源限制                 │   │
│  │  - 网络隔离                 │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

## 🔒 安全机制

### 容器隔离
- 非 root 用户运行（UID 1000）
- 只读文件系统
- 网络禁用（`--network=none`）
- 临时目录隔离

### 资源限制
- 内存：默认 128MB
- CPU：默认 0.5 core
- 超时：默认 30s
- 磁盘：临时目录 64MB

### 预装库白名单
```python
# 标准库
json, datetime, re, math, random, collections, ...

# 第三方库
requests, pandas, numpy, Pillow, beautifulsoup4, ...
```

## 🧪 测试

### 运行单元测试

```bash
cd /Users/ben/.openclaw/workspace-coder/ai-workflow/backend
mvn test -Dtest=PythonDockerExecutorTest
```

**注意:** 测试需要 Docker Daemon 运行。

### 测试用例

- ✅ 执行器类型检查
- ✅ 简单脚本执行
- ✅ 字符串处理
- ✅ JSON 处理
- ✅ 超时控制
- ✅ 错误处理（语法错误、运行时错误）
- ✅ 空输入测试
- ✅ 复杂数据结构
- ✅ 内存限制配置
- ✅ 环境变量传递

## 📝 示例脚本

### 数据转换

```python
# 输入：{"text": "hello world"}
text = inputs.get('text', '')
outputs['uppercase'] = text.upper()
outputs['length'] = len(text)
outputs['words'] = text.split()
```

### 数学计算

```python
# 输入：{"numbers": [1, 2, 3, 4, 5]}
numbers = inputs.get('numbers', [])
outputs['sum'] = sum(numbers)
outputs['average'] = sum(numbers) / len(numbers)
outputs['max'] = max(numbers)
outputs['min'] = min(numbers)
```

### API 调用（需要网络白名单）

```python
# 注意：默认网络被禁用
# 如需要网络访问，需配置 networkEnabled=true
import requests
response = requests.get('https://api.example.com/data')
outputs['data'] = response.json()
```

### 文件处理

```python
# 处理图片（Pillow 预装）
from PIL import Image
import io

image_data = inputs.get('image')  # base64 编码
image = Image.open(io.BytesIO(image_data))
outputs['size'] = image.size
outputs['format'] = image.format
```

## ⚠️ 注意事项

1. **Docker 依赖**: 需要 Docker Daemon 运行
2. **镜像拉取**: 首次执行会拉取 `python:3.11-slim` 镜像（约 100MB）
3. **性能开销**: 容器冷启动约 1-2 秒，可使用容器预热池优化
4. **网络访问**: 默认禁用，如需要需显式配置
5. **临时文件**: 执行完成后自动清理

## 🐛 故障排查

### 问题：测试失败 "NoClassDefFoundError"

**解决:** 确保 pom.xml 包含所有必需依赖：
```xml
<dependency>
    <groupId>com.github.docker-java</groupId>
    <artifactId>docker-java-transport-httpclient5</artifactId>
    <version>3.3.4</version>
</dependency>
```

### 问题：容器创建失败 "Docker not running"

**解决:** 启动 Docker Desktop 或 Docker Daemon：
```bash
# macOS
open -a Docker

# Linux
sudo systemctl start docker
```

### 问题：执行超时

**解决:** 增加超时时间或优化脚本：
```java
config.setTimeout(60);  // 增加到 60 秒
```

### 问题：内存不足

**解决:** 增加内存限制或优化数据处理：
```java
config.setMemoryLimit(512L);  // 增加到 512MB
```

## 📚 相关文档

- [Sprint 1 交付报告](SPRINT1_DELIVERY.md)
- [需求文档](../PYTHON_NODE_REQUIREMENTS.md)
- [技术方案](../PYTHON_NODE_TECH_PLAN.md)

## 📝 更新日志

### v1.0.0 (2026-04-01)
- ✅ 初始版本
- ✅ Docker SDK 集成
- ✅ 容器生命周期管理
- ✅ 资源限制
- ✅ 日志收集
- ✅ 单元测试

---

**维护者:** 龙傲天 🐉  
**最后更新:** 2026-04-01
