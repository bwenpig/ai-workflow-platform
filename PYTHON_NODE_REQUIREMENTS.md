# Python 节点功能需求规格说明书

> **版本：** v1.0  
> **日期：** 2026-04-01  
> **整理者：** Knowledge Manager (龙霸天 🐉)  
> **来源：** 竞品调研报告 + 当前实现文档整合

---

## 1. 功能概述

### 产品定位
Python 节点是 AI 工作流引擎中的**可编程数据处理节点**，允许用户通过编写 Python 脚本实现自定义的数据转换、处理和逻辑控制。它填补了预定义节点无法覆盖的长尾需求，是工作流灵活性和扩展性的核心保障。

### 目标用户
- **技术用户**：具备 Python 编程能力的开发者、数据工程师
- **AI 应用构建者**：需要自定义数据处理逻辑的 AI 应用开发者
- **自动化工作流用户**：需要灵活数据转换的自动化场景用户

### 使用场景
1. **数据格式转换**：JSON ↔ 文本、图片元数据提取、视频信息解析
2. **数据聚合/分发**：多路输入合并、条件路由输出
3. **API 调用**：调用外部 HTTP 服务获取/推送数据
4. **文件处理**：图片resize、视频裁剪、音频分析
5. **逻辑控制**：条件判断、数据过滤、异常处理
6. **自定义计算**：数值计算、统计分析、加密解密

---

## 2. 功能清单

### P0 - 必须实现 ✅

#### 2.1 代码编辑器
- [x] Monaco Editor 集成（VS Code 同款）
- [x] Python 3 语法高亮（vs-dark 主题）
- [x] 代码自动补全（变量、内置函数）
- [x] 代码折叠功能
- [x] 快捷键支持（Ctrl+S 保存、Ctrl+/ 注释、Ctrl+Space 补全）
- [x] 编辑器高度可配置（默认 300px）

#### 2.2 输入输出机制
- [x] 接收上游 JSON 数据（通过 `inputs` 字典）
- [x] 变量引用语法（`inputs['input_name']`）
- [x] 返回 JSON 数据到下游（通过 `outputs` 字典）
- [x] 支持 5 种数据类型：`text`、`image`、`video`、`audio`、`json`
- [x] 大数据截断提示（输出过大时）

#### 2.3 执行环境
- [x] 独立进程执行（ProcessBuilder）
- [x] 临时目录隔离（每次执行创建独立工作目录）
- [x] 可配置超时（默认 30s，范围 1-300s）
- [x] Python 3.9+ 支持
- [x] 输入输出通过 JSON 文件传递（inputs.json / outputs.json）

#### 2.4 安全机制
- [ ] 危险函数黑名单（`eval`、`exec`、`__import__`、`open` 等）
- [ ] 模块导入白名单
- [ ] 网络访问默认禁止
- [ ] 文件系统只读（除临时目录）
- [x] 超时强制终止

#### 2.5 错误处理
- [x] try/except 支持
- [x] 详细错误信息（行号、堆栈跟踪）
- [x] 错误高亮显示（前端）
- [x] 语法错误捕获
- [x] 运行时错误捕获

#### 2.6 日志输出
- [x] print() 捕获到执行日志
- [x] 执行日志面板（前端查看）
- [ ] 日志级别支持（INFO, WARNING, ERROR）

#### 2.7 依赖管理
- [x] requirements.txt 支持（节点配置中指定）
- [x] pip install 自动执行
- [ ] 依赖缓存（避免重复安装）
- [x] 依赖安装失败错误处理

#### 2.8 预装库
- [x] 标准库（json, datetime, re, math, random, collections 等）
- [ ] 常用第三方库预装（requests, pandas, numpy, Pillow）

---

### P1 - 建议实现 🔶

#### 2.9 代码模板
- [ ] 数据转换模板（JSON↔文本、格式转换）
- [ ] API 调用模板（HTTP Request 封装）
- [ ] 数据处理模板（图片、视频、音频处理）
- [ ] 条件分支模板
- [ ] 模板搜索功能
- [ ] 模板市场/分享

#### 2.10 多输出分支
- [ ] 定义多个输出端口（动态配置）
- [ ] 条件路由配置（根据输出内容决定下游路径）
- [ ] 可视化连接（画布上显示分支）

#### 2.11 执行历史
- [ ] 保存最近 N 次执行记录（默认 10 次）
- [ ] 查看输入/输出数据快照
- [ ] 查看执行日志
- [ ] 执行时间统计
- [ ] 执行状态（成功/失败/超时）

#### 2.12 重试机制
- [ ] 配置重试次数（默认 0，最大 3）
- [ ] 配置重试间隔（默认 1s，指数退避）
- [ ] 重试条件配置（仅特定错误类型）

#### 2.13 实时错误检查
- [ ] 语法错误实时提示（Linter 集成）
- [ ] 未定义变量警告
- [ ] 类型提示（基于输入定义）
- [ ] 导入错误提示

#### 2.14 变量智能提示
- [ ] 输入变量自动补全（基于上游节点）
- [ ] 变量类型显示（悬停提示）
- [ ] 变量文档（docstring 展示）
- [ ] 输出变量自动注册

#### 2.15 文件数据支持
- [ ] 接收文件输入（本地文件上传）
- [ ] 文件内容读取（二进制/文本）
- [ ] 生成文件输出（下载链接）
- [ ] 临时文件管理

#### 2.16 测试运行
- [ ] 使用示例数据测试（沙箱模式）
- [ ] 单步执行（调试模式）
- [ ] 结果预览（实时查看输出）
- [ ] 测试数据保存/加载

---

### P2 - 未来规划 🔮

#### 2.17 自定义依赖
- [ ] requirements.txt 文件上传
- [ ] 自定义 Docker 镜像支持
- [ ] 依赖版本锁定（Pipfile/Poetry）
- [ ] 私有 PyPI 源支持

#### 2.18 AI 代码生成
- [ ] 自然语言描述生成代码（"帮我写一个图片 resize 脚本"）
- [ ] 代码解释（选中代码段解释功能）
- [ ] 代码优化建议（性能、安全性）
- [ ] 自动补全增强（AI 辅助）

#### 2.19 多语言支持
- [ ] JavaScript 支持（Node.js 执行环境）
- [ ] 语言切换（Python ↔ JavaScript）
- [ ] 语言特定库管理
- [ ] 跨语言数据传递

#### 2.20 代码版本管理
- [ ] 历史版本保存（自动版本化）
- [ ] 版本对比（diff 视图）
- [ ] 版本回滚（一键恢复）
- [ ] 版本注释（变更说明）

#### 2.21 断点调试
- [ ] 设置断点（点击行号）
- [ ] 单步执行（Step In/Out/Over）
- [ ] 变量查看器（运行时变量值）
- [ ] 调用栈查看
- [ ] 条件断点

#### 2.22 性能分析
- [ ] 执行时间分析（函数级耗时）
- [ ] 内存使用分析（峰值/平均）
- [ ] 性能瓶颈提示（慢操作告警）
- [ ] 资源使用报告

#### 2.23 协作功能
- [ ] 多人编辑（实时协同）
- [ ] 编辑锁（避免冲突）
- [ ] 变更历史（谁改了什么）
- [ ] 评论/批注

#### 2.24 高级安全
- [ ] 网络访问白名单（允许特定域名）
- [ ] 资源使用配额（CPU/内存/磁盘）
- [ ] 审计日志（谁执行了什么脚本）
- [ ] 安全扫描（代码静态分析）
- [ ] Docker 沙箱隔离（生产环境）

---

## 3. 技术规格

### 3.1 编辑器规格（Monaco Editor）

```typescript
// Monaco Editor 配置
{
  language: 'python',
  theme: 'vs-dark',
  automaticLayout: true,
  minimap: { enabled: false },
  fontSize: 14,
  lineNumbers: 'on',
  renderWhitespace: 'selection',
  suggestOnTriggerCharacters: true,
  quickSuggestions: {
    other: 'on',
    comments: 'off',
    strings: 'off'
  },
  acceptSuggestionOnEnter: 'smart',
  tabCompletion: 'on',
  formatOnPaste: true,
  formatOnType: true,
  autoIndent: 'full',
  folding: true,
  foldingStrategy: 'indentation',
  wordWrap: 'off',
  scrollBeyondLastLine: true,
  cursorSmoothCaretAnimation: 'on',
  cursorSmoothCaretAnimation: 'on'
}
```

**快捷键绑定：**
| 快捷键 | 功能 |
|--------|------|
| Ctrl/Cmd + S | 保存代码 |
| Ctrl/Cmd + / | 切换行注释 |
| Ctrl/Cmd + Space | 触发自动补全 |
| Ctrl/Cmd + F | 查找 |
| Ctrl/Cmd + H | 替换 |
| Ctrl/Cmd + D | 选择下一个匹配项 |
| Alt + Up/Down | 移动行 |
| Shift + Alt + Up/Down | 复制行 |
| Ctrl/Cmd + K, Ctrl/Cmd + 0 | 折叠所有区域 |
| Ctrl/Cmd + K, Ctrl/Cmd + J | 展开所有区域 |

---

### 3.2 执行环境规格（Docker 沙箱）

#### 当前实现（临时目录隔离）
```bash
# 执行流程
1. 创建临时目录：/tmp/python-exec-{uuid}/
2. 写入 inputs.json（输入数据）
3. 写入 script.py（用户脚本 + 包装代码）
4. 执行：pip install -r requirements.txt（如果有）
5. 执行：python script.py inputs.json
6. 读取 outputs.json（输出结果）
7. 清理临时目录
```

#### 生产环境（Docker 沙箱）
```yaml
# Docker 容器配置
resources:
  memory:
    default: 128MB
    max: 512MB
  cpu:
    default: 0.5 core
    max: 2 cores
  timeout:
    default: 30s
    max: 300s
  disk:
    default: 10MB
    max: 100MB

security:
  network: disabled  # 默认禁止网络访问
  filesystem: readonly  # 只读文件系统
  capabilities: []  # 无特殊权限
  seccomp: default  # 系统调用过滤
```

---

### 3.3 安全规格

#### 危险函数黑名单
```python
DANGEROUS_BUILTINS = {
    # 代码执行
    'eval', 'exec', 'compile',
    
    # 导入机制
    '__import__', 'importlib',
    
    # 文件操作
    'open', 'file', 'input', 'raw_input',
    
    # 内省
    'globals', 'locals', 'vars', 'dir',
    'getattr', 'setattr', 'delattr',
    
    # 类/对象内部
    '__class__', '__bases__', '__subclasses__',
    '__builtins__', '__globals__', '__code__',
    '__mro__', '__init__', '__new__'
}
```

#### 模块导入白名单
```python
ALLOWED_MODULES = {
    # 标准库（安全）
    'json', 'datetime', 're', 'math', 'random',
    'string', 'collections', 'itertools', 'functools',
    'typing', 'dataclasses', 'enum', 'copy',
    'base64', 'hashlib', 'hmac', 'secrets',
    'uuid', 'decimal', 'fractions', 'statistics',
    'io', 'pathlib', 'os.path', 'tempfile',
    
    # 第三方库（预装，需审核）
    'requests', 'pandas', 'numpy', 'beautifulsoup4',
    'lxml', 'PIL', 'python_dateutil', 'mutagen',
    'ffmpeg_python'
}
```

#### 网络访问限制
```python
# 禁止访问的地址段
BLOCKED_NETWORKS = [
    '127.0.0.0/8',      # localhost
    '10.0.0.0/8',       # 内网 A 类
    '172.16.0.0/12',    # 内网 B 类
    '192.168.0.0/16',   # 内网 C 类
    '169.254.0.0/16',   # 链路本地
    '::1/128',          # IPv6 localhost
    'fc00::/7',         # IPv6 内网
]

# 白名单模式（可选）
ALLOWED_DOMAINS = [
    'api.example.com',
    'cdn.example.com'
]
```

---

### 3.4 资源规格

```yaml
# 默认资源配置
resources:
  memory:
    default: 128MB
    max: 512MB
    warning_threshold: 100MB
    
  cpu:
    default: 0.5 core
    max: 2 cores
    
  timeout:
    default: 30s
    max: 300s
    warning_threshold: 25s
    
  disk:
    default: 10MB
    max: 100MB
    
  output_size:
    default: 1MB
    max: 10MB
    truncation_hint: true  # 超过阈值时提示截断
```

---

## 4. 验收标准

### 4.1 功能验收标准

| 功能 | 验收标准 | 优先级 |
|------|----------|--------|
| 代码编辑 | Monaco Editor 正常加载，支持 Python 语法高亮和自动补全 | P0 |
| 脚本执行 | 简单 Python 脚本能正确执行并返回结果 | P0 |
| 输入传递 | 上游节点数据能正确传递到 `inputs` 字典 | P0 |
| 输出传递 | `outputs` 字典数据能正确传递给下游节点 | P0 |
| 超时控制 | 超时脚本能被强制终止，返回 TIMEOUT 错误 | P0 |
| 错误处理 | 语法错误和运行时错误能被捕获并显示详细信息 | P0 |
| 依赖安装 | requirements 中指定的包能自动安装 | P0 |
| 日志查看 | print() 输出能在日志面板中查看 | P0 |
| 数据类型 | 支持 text/image/video/audio/json 五种类型 | P0 |
| 代码模板 | 提供至少 5 个常用场景的代码模板 | P1 |
| 执行历史 | 能查看最近 10 次执行记录和结果 | P1 |
| 变量提示 | 输入变量能自动补全并显示类型 | P1 |
| 实时检查 | 语法错误能实时提示（不执行也能发现） | P1 |
| 测试运行 | 能用示例数据测试脚本而不影响工作流 | P1 |

---

### 4.2 性能验收标准

| 指标 | 目标值 | 测量方法 |
|------|--------|----------|
| 编辑器加载时间 | < 500ms | 从打开面板到可编辑 |
| 脚本启动时间 | < 2s | 从点击执行到 Python 进程启动 |
| 简单脚本执行 | < 1s | 无依赖、纯计算脚本 |
| 依赖安装时间 | < 30s | 安装 3 个常用包（requests/pandas/numpy） |
| 并发执行支持 | ≥ 10 个 | 同时执行 10 个 Python 节点不阻塞 |
| 内存泄漏 | 无 | 连续执行 100 次后内存占用无显著增长 |
| 临时文件清理 | 100% | 执行完成后临时目录完全清理 |

---

### 4.3 安全验收标准

| 检查项 | 验收标准 | 测试方法 |
|--------|----------|----------|
| 黑名单函数 | 调用 `eval()` 等危险函数应被阻止 | 尝试执行 `eval("1+1")` |
| 白名单模块 | 导入未授权模块应失败 | 尝试 `import socket` |
| 网络访问 | 默认禁止外网访问 | 尝试 `requests.get("http://example.com")` |
| 文件系统 | 只能访问临时目录 | 尝试读写 `/etc/passwd` |
| 超时保护 | 超时脚本必须被终止 | 执行 `while True: pass` |
| 内存限制 | 超出内存限制应终止 | 创建超大列表/字符串 |
| 进程隔离 | 无法访问其他进程 | 尝试 `ps aux` 或信号发送 |
| 环境变量 | 敏感环境变量应被过滤 | 检查 `os.environ` |

---

## 5. 竞品参考

### 5.1 关键功能对标

| 功能 | n8n | Dify | Coze | 本项目 |
|------|-----|------|------|--------|
| **代码编辑器** | Monaco Editor | 基础编辑器 | 基础编辑器 | ✅ Monaco Editor |
| **语法高亮** | ✅ | ✅ | ✅ | ✅ |
| **自动补全** | ✅ | ❌ | ❌ | ✅ |
| **实时错误检查** | ✅ | ❌ | ❌ | 🔶 P1 |
| **代码模板** | ✅ (20+ 模板) | ✅ (10+ 模板) | ✅ (15+ 模板) | 🔶 P1 |
| **执行沙箱** | ✅ (Docker) | ✅ (Docker) | ✅ (Docker) | ⚠️ 临时目录 |
| **超时控制** | ✅ | ✅ | ✅ | ✅ |
| **内存限制** | ✅ | ✅ | ✅ | 🔶 P1 |
| **依赖管理** | ✅ (npm/pip) | ✅ (pip) | ✅ (pip) | ✅ (pip) |
| **依赖缓存** | ✅ | ✅ | ✅ | 🔶 P1 |
| **执行历史** | ✅ (50 次) | ✅ (20 次) | ✅ (30 次) | 🔶 P1 |
| **重试机制** | ✅ | ✅ | ❌ | 🔶 P1 |
| **调试功能** | ✅ (断点) | ❌ | ❌ | 🔮 P2 |
| **多语言** | ✅ (JS/Python) | ✅ (Python) | ✅ (Python) | 🔮 P2 |
| **AI 代码生成** | ❌ | ✅ | ✅ | 🔮 P2 |
| **协作编辑** | ✅ (企业版) | ❌ | ❌ | 🔮 P2 |

### 5.2 差异化优势

1. **Monaco Editor 完整集成**：提供 VS Code 级别的编码体验（自动补全、代码折叠、快捷键）
2. **数据类型丰富**：原生支持 text/image/video/audio/json 五种 AI 工作流常用类型
3. **HARNESS 合规**：符合严格的工程规范（100% 测试覆盖率、Checkstyle 通过）
4. **Java 生态集成**：与现有 DAG 引擎无缝集成，支持混合工作流

### 5.3 待补齐短板

1. **生产级沙箱**：当前使用临时目录隔离，需升级为 Docker 沙箱
2. **执行历史**：缺少执行记录保存和查看功能
3. **代码模板**：缺少预置模板库
4. **AI 辅助**：缺少 AI 代码生成和优化建议

---

## 附录

### A. 错误代码参考

| 错误码 | 说明 | 建议操作 |
|--------|------|----------|
| `TIMEOUT` | 代码执行超时 | 优化代码或增加超时时间 |
| `MEMORY_LIMIT` | 超出内存限制 | 减少数据处理量 |
| `IMPORT_ERROR` | 导入未授权的模块 | 使用预装库或申请添加 |
| `NETWORK_DENIED` | 网络访问被拒绝 | 使用 HTTP Request 节点 |
| `DANGEROUS_CALL` | 调用危险函数 | 使用安全的替代方法 |
| `SYNTAX_ERROR` | 语法错误 | 检查代码语法 |
| `OUTPUT_TOO_LARGE` | 输出数据过大 | 截断或分批处理 |
| `DEPENDENCY_INSTALL_FAILED` | 依赖安装失败 | 检查包名或网络 |

---

### B. 推荐预装库列表

#### 基础库（必须）
- `requests` - HTTP 请求
- `json` - JSON 处理（标准库）
- `datetime` - 日期时间（标准库）
- `re` - 正则表达式（标准库）

#### 数据处理（推荐）
- `pandas` - 数据分析
- `numpy` - 数值计算
- `python-dateutil` - 日期处理

#### 网页解析（可选）
- `beautifulsoup4` - HTML 解析
- `lxml` - XML/HTML 解析

#### 图像处理（可选）
- `Pillow` - 图像处理

#### 音视频处理（可选）
- `mutagen` - 音频元数据
- `ffmpeg-python` - 视频处理

#### 加密安全（可选）
- `cryptography` - 加密算法
- `pyjwt` - JWT 处理

---

### C. 文档索引

- **竞品调研**：`~/.openclaw/workspace-boniu/python-node-research/feature-checklist.md`
- **设计文档**：`/Users/ben/.openclaw/workspace-coder/ai-workflow/docs/python-node-design.md`
- **实现总结**：`/Users/ben/.openclaw/workspace-coder/ai-workflow/docs/PYTHON_NODE_IMPLEMENTATION.md`
- **前端实现**：`/Users/ben/.openclaw/workspace-coder/ai-workflow/frontend/PYTHON_NODE_IMPLEMENTATION.md`
- **使用示例**：`/Users/ben/.openclaw/workspace-coder/ai-workflow/docs/python-node-examples.md`

---

**文档版本：** v1.0  
**创建日期：** 2026-04-01  
**维护者：** Knowledge Manager (龙霸天 🐉)
