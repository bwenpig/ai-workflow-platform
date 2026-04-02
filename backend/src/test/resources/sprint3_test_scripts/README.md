# Sprint 3 测试脚本说明

## 目录

- `test_memory_limit.py` - 内存限制测试
- `test_cpu_limit.py` - CPU 限制测试
- `test_preinstalled_libs.py` - 预装库验证
- `test_templates.py` - 代码模板测试

---

## 1. 内存限制测试 (test_memory_limit.py)

### 测试目标
验证内存超过 128MB 时进程应被终止

### 测试类型
```bash
# 正常内存使用（约 50MB）
python test_memory_limit.py normal

# 内存超限（目标 200MB，应被终止）
python test_memory_limit.py exceeded

# 内存接近限制（约 120MB）
python test_memory_limit.py near_limit
```

### 预期结果
| 测试类型 | 预期行为 |
|----------|----------|
| normal | 成功完成，内存使用约 50MB |
| exceeded | 进程被终止或抛出 MemoryError |
| near_limit | 成功完成，内存使用约 120MB |

---

## 2. CPU 限制测试 (test_cpu_limit.py)

### 测试目标
验证高 CPU 负载时不会阻塞系统，且能在合理时间内完成或被限制

### 测试类型
```bash
# 正常 CPU 使用
python test_cpu_limit.py normal

# 高 CPU 负载（1000 万次浮点运算）
python test_cpu_limit.py high_load

# 无限循环（应被超时终止）
python test_cpu_limit.py infinite

# CPU 密集型任务（带时间检查）
python test_cpu_limit.py intensive
```

### 预期结果
| 测试类型 | 预期行为 |
|----------|----------|
| normal | 快速完成（<1 秒） |
| high_load | 完成但耗时较长（数秒） |
| infinite | 被超时机制终止 |
| intensive | 在时限内完成 |

---

## 3. 预装库验证 (test_preinstalled_libs.py)

### 测试目标
验证预装的第三方库可正常导入和使用

### 测试类型
```bash
# 测试所有库
python test_preinstalled_libs.py all

# 测试单个库
python test_preinstalled_libs.py numpy
python test_preinstalled_libs.py pandas
python test_preinstalled_libs.py pillow
python test_preinstalled_libs.py requests
python test_preinstalled_libs.py beautifulsoup4
```

### 预期结果
| 库名 | 预期版本 | 测试内容 |
|------|----------|----------|
| numpy | 1.26.x | 数组创建、运算、统计 |
| pandas | 2.1.x | DataFrame、统计、筛选 |
| Pillow | 10.1.x | 图像创建、旋转、保存 |
| requests | 2.31.x | 导入成功（网络访问可能被禁用） |
| beautifulsoup4 | 4.12.x | HTML 解析 |

---

## 4. 代码模板测试 (test_templates.py)

### 测试目标
验证代码模板的加载与执行

### 测试类型
```bash
# 测试所有模板
python test_templates.py all

# 测试单个模板
python test_templates.py json_to_text
python test_templates.py text_to_json
python test_templates.py data_aggregation
python test_templates.py conditional_branch
python test_templates.py image_processing
python test_templates.py data_filter
```

### 模板列表
| 模板名 | 功能 | 输入参数 |
|--------|------|----------|
| json_to_text | JSON 转文本 | json_data, format |
| text_to_json | 文本转 JSON | text, parse_type |
| data_aggregation | 数据聚合 | items, aggregation_type |
| conditional_branch | 条件分支 | value, condition |
| image_processing | 图像处理 | image_data, operation |
| data_filter | 数据过滤 | items, filter_type, filter_value |

---

## 执行方式

### 方式 1: 直接执行
```bash
cd /Users/ben/.openclaw/workspace-coder/ai-workflow/backend/src/test/resources/sprint3_test_scripts/
python test_memory_limit.py normal
```

### 方式 2: 通过 Python 节点执行
在工作流中创建 Python 节点，将脚本内容粘贴到编辑器，配置输入数据后执行。

### 方式 3: 带输入文件执行
```bash
# 创建输入文件
echo '{"json_data": {"name": "test"}}' > inputs.json

# 执行模板测试
python test_templates.py json_to_text inputs.json
```

---

## 验收标准

### 内存限制
- [ ] 128MB 限制生效，超限进程被终止
- [ ] 120MB 以下使用正常
- [ ] 内存使用有监控和日志

### CPU 限制
- [ ] 无限循环被超时机制终止
- [ ] 高负载任务不阻塞系统
- [ ] CPU 使用有监控

### 预装库
- [ ] numpy 可正常导入和使用
- [ ] pandas 可正常导入和使用
- [ ] Pillow 可正常导入和使用
- [ ] 所有预装库版本符合要求

### 代码模板
- [ ] 所有模板可正常加载
- [ ] 模板执行结果正确
- [ ] 模板参数配置生效

---

## 问题反馈

测试失败时请记录：
1. 测试脚本名称和测试类型
2. 错误信息和堆栈跟踪
3. 执行环境（Docker/子进程）
4. 资源配置（内存/CPU/超时）

---

**创建日期:** 2026-04-01  
**Sprint:** 3  
**Tester:** 龙波儿
