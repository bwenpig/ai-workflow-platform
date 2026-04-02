# Sprint 3 快速参考

## 资源限制配置

### Java 代码配置

```java
import com.ben.workflow.model.PythonNodeConfig;
import com.ben.workflow.engine.PythonDockerExecutor;

// 创建执行器
PythonDockerExecutor executor = new PythonDockerExecutor();

// 配置资源限制
PythonNodeConfig config = new PythonNodeConfig();
config.setScript("print('Hello World')");
config.setMemoryLimit(256L);  // 256MB (默认 128MB)
config.setCpuLimit(1.0);      // 1 CPU 核心 (默认 0.5)
config.setTimeout(30);        // 30 秒超时

// 执行
Map<String, Object> inputs = new HashMap<>();
PythonExecutionResult result = executor.execute(script, inputs, config);
```

### 默认值

| 资源 | 默认值 | 说明 |
|------|--------|------|
| 内存限制 | 128 MB | Docker cgroups 内存上限 |
| CPU 限制 | 0.5 核心 | Docker cgroups CPU quota |
| 超时时间 | 30 秒 | 脚本执行超时保护 |

---

## 代码模板库

### API 端点

```bash
# 获取所有模板列表
GET /api/templates

# 获取单个模板详情
GET /api/templates/{templateId}
示例：GET /api/templates/01_hello_world

# 按分类获取模板
GET /api/templates/category/{category}
示例：GET /api/templates/category/数据

# 获取所有分类
GET /api/templates/categories

# 搜索模板
GET /api/templates/search?keyword={keyword}
示例：GET /api/templates/search?keyword=HTTP

# 获取统计信息
GET /api/templates/stats
```

### 模板分类

| 分类 | 模板数量 | 模板 |
|------|----------|------|
| 基础 | 5 个 | Hello World, JSON 解析，文件操作，文本处理，日期时间 |
| 数据 | 3 个 | 数据处理，数据可视化，Excel 处理 |
| 网络 | 1 个 | HTTP 请求 |
| 图像 | 1 个 | 图像处理 |

### 使用模板

**1. 通过 API 获取模板:**
```bash
curl http://localhost:8080/api/templates/03_data_processing
```

**2. 解析模板 JSON:**
```json
{
  "name": "数据处理",
  "description": "使用 pandas 进行数据处理和分析",
  "category": "数据",
  "script": "...",
  "inputs": {...},
  "requirements": ["pandas", "numpy"],
  "timeout": 30
}
```

**3. 使用模板执行:**
```java
// 从模板获取配置
TemplateLibraryService.TemplateInfo template = 
    templateLibraryService.getTemplate("03_data_processing");

PythonNodeConfig config = new PythonNodeConfig();
config.setScript(template.getScript());
config.setRequirements(template.getRequirements());
config.setTimeout(template.getTimeout());

// 执行
PythonExecutionResult result = executor.execute(
    template.getScript(), 
    template.getInputs(), 
    config
);
```

---

## 预装库列表

### 基础层
```python
import requests      # HTTP 请求
import numpy         # 数值计算
import pandas        # 数据处理
```

### 图像层
```python
from PIL import Image      # 图像处理
import cv2                 # OpenCV 计算机视觉
import imageio             # 图像读写
```

### 数据层
```python
import matplotlib.pyplot   # 绘图
import seaborn             # 统计图表
import scipy               # 科学计算
```

### 工具层
```python
from bs4 import BeautifulSoup  # HTML 解析
import lxml                    # XML 解析
import yaml                    # YAML 解析
from openpyxl import Workbook  # Excel 读写
import docx                    # Word 文档
from fpdf import FPDF          # PDF 生成
from reportlab.pdfgen import canvas  # PDF 生成
```

---

## Docker 镜像构建

### 构建镜像
```bash
cd /Users/ben/.openclaw/workspace-coder/ai-workflow/backend
docker build -t python-sandbox:latest .
```

### 测试镜像
```bash
# 测试基础功能
docker run --rm python-sandbox:latest python3 -c "import pandas; print(pandas.__version__)"

# 测试资源限制
docker run --rm \
    --memory=128m \
    --cpu-quota=50000 \
    python-sandbox:latest \
    python3 -c "print('OK')"
```

---

## 单元测试

### 运行模板库测试
```bash
cd /Users/ben/.openclaw/workspace-coder/ai-workflow/backend
mvn test -Dtest=TemplateLibraryServiceTest
```

### 运行资源限制测试（需要 Docker）
```bash
mvn test -Dtest=PythonDockerExecutorTest#testMemoryLimitConfig
mvn test -Dtest=PythonDockerExecutorTest#testCpuLimitConfig
mvn test -Dtest=PythonDockerExecutorTest#testCombinedResourceLimits
```

### 运行所有测试（跳过 Docker 测试）
```bash
mvn test -Dtest='!PythonDockerExecutorTest'
```

---

## 故障排查

### 容器启动失败
```bash
# 检查 Docker 日志
docker logs <container-id>

# 检查系统资源
docker stats
```

### 内存限制过严
```java
// 增加内存限制
config.setMemoryLimit(512L);  // 512MB
```

### CPU 限制过严
```java
// 增加 CPU 配额
config.setCpuLimit(2.0);  // 2 CPU 核心
```

### 模板加载失败
```bash
# 检查模板文件
ls -la src/main/resources/templates/

# 查看日志
tail -f logs/application.log | grep TemplateLibrary
```

---

## 性能调优建议

### 内存优化
- 默认 128MB 适合简单脚本
- 数据处理建议 256-512MB
- 图像处理建议 512MB+

### CPU 优化
- 默认 0.5 CPU 适合 I/O 密集型
- 计算密集型建议 1.0-2.0 CPU
- 并行任务按需分配

### 预装库优化
- 按需加载：只 import 需要的库
- 延迟导入：在函数内部 import
- 使用轻量级替代：如 `ujson` 替代 `json`

---

## 安全注意事项

1. **资源限制必须配置** - 防止恶意脚本耗尽系统资源
2. **网络默认禁用** - 需要时显式开启 `networkEnabled=true`
3. **文件系统只读** - 只允许在 `/sandbox` 和 `/tmp` 写入
4. **非 root 用户** - 容器内使用 `sandbox` 用户（UID 1000）

---

**📚 详细文档:** `SPRINT3_DELIVERY.md`
