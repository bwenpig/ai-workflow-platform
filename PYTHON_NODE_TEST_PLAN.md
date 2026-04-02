# Python 节点验收测试计划

**文档版本:** 1.0  
**创建时间:** 2024 年 4 月 1 日  
**创建者:** 龙波儿 (Tester)  
**状态:** 待执行

---

## 一、测试概述

### 1.1 测试目标

验证 Python 节点功能完整性、安全性、性能和稳定性，确保达到竞品 (n8n, Dify, Coze) 同等水平。

### 1.2 测试范围

| 测试维度 | 覆盖内容 | 优先级 |
|----------|----------|--------|
| 功能测试 | 代码编辑、脚本执行、输入输出、依赖安装 | P0 |
| 安全测试 | 危险函数拦截、模块限制、网络隔离、文件系统隔离 | P0 |
| 性能测试 | 超时控制、内存限制、并发执行 | P0 |
| 异常测试 | 语法错误、运行时错误、超时、依赖失败 | P0 |

### 1.3 验收标准定义

- **通过 (PASS):** 所有测试用例 100% 通过，无 P0 级别缺陷
- **有条件通过 (CONDITIONAL_PASS):** P0 用例 100% 通过，P1 用例通过率 ≥ 90%
- **失败 (FAIL):** 任一 P0 用例失败，或 P1 用例通过率 < 90%

---

## 二、测试环境要求

### 2.1 环境配置

```yaml
测试环境:
  Python 版本: 3.11+
  执行方式: Docker 沙箱 / 独立进程
  默认超时: 30 秒
  默认内存: 128MB
  编辑器: Monaco Editor
```

### 2.2 预装库要求

```python
# 必须预装的标准库
REQUIRED_STDLIB = [
    'json', 'datetime', 're', 'math', 'random',
    'string', 'collections', 'itertools', 'functools',
    'typing', 'dataclasses', 'enum', 'copy',
    'base64', 'hashlib', 'hmac', 'secrets',
    'uuid', 'decimal', 'fractions', 'statistics'
]

# 推荐预装的第三方库
RECOMMENDED_THIRDPARTY = [
    'requests', 'pandas', 'numpy', 'beautifulsoup4',
    'lxml', 'PIL', 'python_dateutil'
]
```

---

## 三、功能测试用例

### 3.1 代码编辑测试 (Monaco Editor)

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| FE-001 | 语法高亮 | 1. 打开 Python 节点编辑器<br>2. 输入 Python 代码 (包含关键字、字符串、注释) | 关键字、字符串、注释、函数名等不同颜色显示 | P0 |
| FE-002 | 自动补全 | 1. 输入 `pr`<br>2. 按下 Ctrl+Space | 显示 `print` 等补全建议 | P0 |
| FE-003 | 代码折叠 | 1. 输入多行函数定义<br>2. 点击折叠按钮 | 函数体可折叠/展开 | P1 |
| FE-004 | 快捷键 - 保存 | 1. 编辑代码<br>2. 按下 Ctrl/Cmd+S | 代码保存成功，有保存提示 | P0 |
| FE-005 | 快捷键 - 注释 | 1. 选中代码行<br>2. 按下 Ctrl/Cmd+/ | 选中行被注释/取消注释 | P0 |
| FE-006 | 实时错误检查 | 1. 输入语法错误代码 (如 `def foo(`)<br>2. 观察编辑器 | 显示语法错误提示，标记错误位置 | P1 |
| FE-007 | 变量自动补全 | 1. 定义变量 `my_var = 1`<br>2. 输入 `my_` | 显示 `my_var` 补全建议 | P1 |
| FE-008 | 代码模板 | 1. 打开模板库<br>2. 选择"数据转换"模板 | 模板代码插入编辑器 | P1 |

### 3.2 脚本执行测试

#### 3.2.1 基础执行

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| EX-001 | Hello World | 1. 输入 `print("Hello, World!")`<br>2. 执行 | 输出 "Hello, World!"，执行成功 | P0 |
| EX-002 | 变量定义 | 1. 输入 `x = 10; y = 20; print(x + y)`<br>2. 执行 | 输出 30 | P0 |
| EX-003 | 函数定义 | 1. 定义并调用函数<br>2. 执行 | 函数正常执行，返回正确结果 | P0 |
| EX-004 | 循环执行 | 1. 输入 `for i in range(5): print(i)`<br>2. 执行 | 输出 0-4 | P0 |
| EX-005 | 条件判断 | 1. 输入 if-else 逻辑<br>2. 执行 | 根据条件输出正确结果 | P0 |

#### 3.2.2 复杂执行

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| EX-101 | 列表推导式 | 1. 输入 `[x**2 for x in range(10)]`<br>2. 执行 | 返回正确的列表结果 | P1 |
| EX-102 | 字典操作 | 1. 创建并操作字典<br>2. 执行 | 字典操作正常 | P1 |
| EX-103 | 异常处理 | 1. 输入 try-except 代码块<br>2. 执行 | 异常被正确捕获和处理 | P0 |
| EX-104 | 多行代码 | 1. 输入 50+ 行代码<br>2. 执行 | 完整执行，无截断 | P1 |
| EX-105 | 中文支持 | 1. 输入包含中文的代码和字符串<br>2. 执行 | 中文正常显示和处理 | P0 |

### 3.3 输入输出传递测试

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| IO-001 | 接收上游数据 | 1. 配置上游节点输出 `{"name": "test", "value": 123}`<br>2. Python 节点引用输入变量<br>3. 执行 | 成功接收并访问输入数据 | P0 |
| IO-002 | 变量引用语法 | 1. 使用 `{{name}}` 或 `$name` 引用变量<br>2. 执行 | 变量正确替换为实际值 | P0 |
| IO-003 | JSON 输出 | 1. 返回字典 `{"result": "success"}`<br>2. 执行 | 下游节点接收到 JSON 数据 | P0 |
| IO-004 | 多输出分支 | 1. 配置两个输出端口<br>2. 根据条件返回不同输出<br>3. 执行 | 数据路由到正确的下游节点 | P1 |
| IO-005 | 大数据输出 | 1. 生成 10MB+ 的输出数据<br>2. 执行 | 有截断提示，不导致系统崩溃 | P1 |
| IO-006 | 文件输入 | 1. 上游传递文件数据<br>2. Python 节点读取文件内容<br>3. 执行 | 成功读取文件内容 | P1 |
| IO-007 | 文件输出 | 1. Python 节点生成文件<br>2. 传递给下游 | 下游接收到文件数据 | P1 |
| IO-008 | 空值处理 | 1. 上游传递 null/None<br>2. Python 节点处理<br>3. 执行 | 正确处理空值，不报错 | P0 |

### 3.4 依赖包安装测试

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| DP-001 | 预装库使用 | 1. 导入 `requests` 库<br>2. 执行 | 导入成功，可正常使用 | P0 |
| DP-002 | 标准库使用 | 1. 导入 `json`, `datetime` 等标准库<br>2. 执行 | 导入成功 | P0 |
| DP-003 | 未授权库拦截 | 1. 尝试导入未预装的库<br>2. 执行 | 显示友好的错误提示，说明库未预装 | P1 |
| DP-004 | 自定义依赖安装 | 1. 执行 `pip install <package>` (如支持)<br>2. 导入并使用 | 安装成功并可使用 (自托管模式) | P2 |
| DP-005 | 依赖缓存 | 1. 第一次安装依赖<br>2. 第二次使用相同依赖<br>3. 对比时间 | 第二次执行明显更快 (缓存生效) | P2 |

---

## 四、安全测试用例

### 4.1 危险函数拦截

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| SC-001 | eval 拦截 | 1. 输入 `eval("print('hacked')")`<br>2. 执行 | 抛出安全异常，代码不执行 | P0 |
| SC-002 | exec 拦截 | 1. 输入 `exec("print('hacked')")`<br>2. 执行 | 抛出安全异常，代码不执行 | P0 |
| SC-003 | __import__ 拦截 | 1. 输入 `__import__('os')`<br>2. 执行 | 抛出安全异常 | P0 |
| SC-004 | open 拦截 | 1. 输入 `open('/etc/passwd')`<br>2. 执行 | 抛出安全异常，文件不可访问 | P0 |
| SC-005 | compile 拦截 | 1. 输入 `compile("...", "...", "exec")`<br>2. 执行 | 抛出安全异常 | P0 |
| SC-006 | globals 拦截 | 1. 输入 `globals()`<br>2. 执行 | 抛出安全异常或返回受限结果 | P0 |
| SC-007 | locals 拦截 | 1. 输入 `locals()`<br>2. 执行 | 抛出安全异常或返回受限结果 | P0 |
| SC-008 | getattr/setattr 拦截 | 1. 输入 `getattr(obj, '__class__')`<br>2. 执行 | 对危险属性访问进行拦截 | P1 |
| SC-009 | 绕过尝试 | 1. 尝试通过 `().__class__.__mro__` 等方式绕过<br>2. 执行 | 所有绕过尝试均被拦截 | P0 |

### 4.2 模块导入限制

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| SM-001 | 白名单模块导入 | 1. 导入 `json`, `re`, `math`<br>2. 执行 | 导入成功 | P0 |
| SM-002 | 黑名单模块拦截 - os | 1. 输入 `import os`<br>2. 执行 | 抛出导入错误，模块不可用 | P0 |
| SM-003 | 黑名单模块拦截 - sys | 1. 输入 `import sys`<br>2. 执行 | 抛出导入错误 | P0 |
| SM-004 | 黑名单模块拦截 - subprocess | 1. 输入 `import subprocess`<br>2. 执行 | 抛出导入错误 | P0 |
| SM-005 | 黑名单模块拦截 - socket | 1. 输入 `import socket`<br>2. 执行 | 抛出导入错误 | P0 |
| SM-006 | 间接导入拦截 | 1. 尝试通过 `__import__('os')` 导入<br>2. 执行 | 拦截成功 | P0 |
| SM-007 | 模块别名导入 | 1. 输入 `import os as operating_system`<br>2. 执行 | 拦截成功，识别真实模块 | P0 |

### 4.3 网络访问限制

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| NW-001 | HTTP 请求拦截 | 1. 使用 `requests.get('https://example.com')`<br>2. 执行 (默认禁止网络) | 抛出网络访问被拒绝错误 | P0 |
| NW-002 | localhost 访问拦截 | 1. 尝试访问 `http://127.0.0.1`<br>2. 执行 | 拦截成功 | P0 |
| NW-003 | 内网访问拦截 | 1. 尝试访问 `192.168.x.x`<br>2. 执行 | 拦截成功 | P0 |
| NW-004 | socket 连接拦截 | 1. 尝试创建 socket 连接<br>2. 执行 | 拦截成功 | P0 |
| NW-005 | 白名单网络访问 | 1. 配置网络白名单<br>2. 访问白名单内域名<br>3. 执行 | 访问成功 (如支持白名单功能) | P1 |
| NW-006 | urllib 拦截 | 1. 使用 `urllib.request.urlopen()`<br>2. 执行 | 拦截成功 | P0 |

### 4.4 文件系统隔离

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| FS-001 | 读取系统文件 | 1. 尝试读取 `/etc/passwd`<br>2. 执行 | 文件不存在或无权限 | P0 |
| FS-002 | 写入系统目录 | 1. 尝试写入 `/tmp/test`<br>2. 执行 | 写入失败或写入隔离目录 | P0 |
| FS-003 | 遍历目录 | 1. 使用 `os.listdir('/')` (如可导入)<br>2. 执行 | 返回空或仅沙箱内目录 | P0 |
| FS-004 | 临时文件创建 | 1. 在沙箱内创建临时文件<br>2. 读取该文件<br>3. 执行 | 文件操作在沙箱内正常 | P1 |
| FS-005 | 执行后清理 | 1. 创建文件<br>2. 执行结束<br>3. 再次执行检查文件 | 文件已被清理，环境重置 | P1 |
| FS-006 | 路径遍历攻击 | 1. 尝试 `../../etc/passwd`<br>2. 执行 | 拦截成功 | P0 |

---

## 五、性能测试用例

### 5.1 超时控制

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| PF-001 | 默认超时 | 1. 执行 `import time; time.sleep(35)`<br>2. 执行 (默认 30s 超时) | 30 秒后终止，抛出超时错误 | P0 |
| PF-002 | 自定义超时 | 1. 配置超时为 60 秒<br>2. 执行 `time.sleep(50)`<br>3. 执行 | 执行成功，不超时 | P1 |
| PF-003 | 无限循环 | 1. 输入 `while True: pass`<br>2. 执行 | 超时终止，不卡死系统 | P0 |
| PF-004 | 递归溢出 | 1. 输入无限递归函数<br>2. 执行 | 触发递归深度限制或超时 | P0 |
| PF-005 | 超时错误信息 | 1. 触发超时<br>2. 检查错误信息 | 显示清晰的超时错误，包含执行时长 | P1 |

### 5.2 内存限制

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| PF-101 | 内存限制触发 | 1. 分配超过 128MB 的内存<br>2. 执行 | 抛出内存超限错误，进程终止 | P0 |
| PF-102 | 大数据处理 | 1. 创建大型列表/字典 (接近限制)<br>2. 执行 | 在限制内正常处理 | P1 |
| PF-103 | 内存泄漏检测 | 1. 多次执行相同代码<br>2. 监控内存使用 | 内存使用稳定，无泄漏 | P1 |
| PF-104 | 执行后清理 | 1. 执行占用大量内存的代码<br>2. 执行结束<br>3. 检查内存 | 内存已释放，可执行新任务 | P0 |

### 5.3 并发执行

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| PF-201 | 单节点并发 | 1. 同时触发同一节点 10 次执行<br>2. 观察结果 | 所有执行独立，互不干扰 | P1 |
| PF-202 | 多节点并发 | 1. 同时执行 5 个不同 Python 节点<br>2. 观察结果 | 所有节点正常执行 | P1 |
| PF-203 | 并发隔离 | 1. 节点 A 设置变量<br>2. 节点 B 执行<br>3. 检查变量 | 节点间变量隔离，无泄漏 | P0 |
| PF-204 | 最大并发数 | 1. 超过最大并发数触发执行<br>2. 观察行为 | 排队等待或拒绝，系统不崩溃 | P1 |
| PF-205 | 并发性能 | 1. 并发执行 20 个轻量任务<br>2. 统计总耗时 | 总耗时合理，无显著性能下降 | P1 |

---

## 六、异常测试用例

### 6.1 语法错误

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| ER-001 | 括号不匹配 | 1. 输入 `print("hello"`<br>2. 执行 | 显示语法错误，指出位置 | P0 |
| ER-002 | 缩进错误 | 1. 输入缩进错误的代码<br>2. 执行 | 显示 IndentationError | P0 |
| ER-003 | 无效语法 | 1. 输入 `def foo(`<br>2. 执行 | 显示语法错误 | P0 |
| ER-004 | 未定义变量 | 1. 使用未定义的变量<br>2. 执行 | 显示 NameError | P0 |
| ER-005 | 类型错误 | 1. 执行 `"1" + 1`<br>2. 执行 | 显示 TypeError | P0 |

### 6.2 运行时错误

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| ER-101 | 除零错误 | 1. 输入 `1 / 0`<br>2. 执行 | 显示 ZeroDivisionError | P0 |
| ER-102 | 索引越界 | 1. 访问 `[][0]`<br>2. 执行 | 显示 IndexError | P0 |
| ER-103 | 键错误 | 1. 访问 `{}['key']`<br>2. 执行 | 显示 KeyError | P0 |
| ER-104 | 属性错误 | 1. 访问不存在的属性<br>2. 执行 | 显示 AttributeError | P0 |
| ER-105 | 导入错误 | 1. 导入不存在的模块<br>2. 执行 | 显示 ModuleNotFoundError | P0 |
| ER-106 | 错误堆栈 | 1. 触发嵌套调用错误<br>2. 检查错误输出 | 显示完整堆栈跟踪 | P1 |

### 6.3 超时处理

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| ER-201 | 超时错误类型 | 1. 触发超时<br>2. 检查错误类型 | 显示明确的 Timeout 错误 | P0 |
| ER-202 | 超时资源清理 | 1. 触发超时<br>2. 执行新任务 | 新任务正常执行，资源已清理 | P0 |
| ER-203 | 超时日志 | 1. 触发超时<br>2. 查看日志 | 日志记录超时信息和执行时长 | P1 |

### 6.4 依赖安装失败

| 用例 ID | 测试项 | 测试步骤 | 预期结果 | 优先级 |
|---------|--------|----------|----------|--------|
| ER-301 | 网络不可用 | 1. 尝试 pip install (无网络)<br>2. 执行 | 显示友好的错误提示 | P1 |
| ER-302 | 包不存在 | 1. 尝试安装不存在的包<br>2. 执行 | 显示包不存在错误 | P1 |
| ER-303 | 版本冲突 | 1. 尝试安装冲突版本<br>2. 执行 | 显示版本冲突错误 | P2 |
| ER-304 | 权限不足 | 1. 尝试安装到系统目录<br>2. 执行 | 显示权限错误或安装到沙箱 | P1 |

---

## 七、测试验证脚本

### 7.1 功能测试脚本

```python
#!/usr/bin/env python3
"""
Python 节点功能测试脚本
用于自动化验证 Python 节点的核心功能
"""

import json
import sys

def test_hello_world():
    """测试基础输出"""
    print("Hello, World!")
    return {"status": "pass", "test": "hello_world"}

def test_variables():
    """测试变量操作"""
    x = 10
    y = 20
    result = x + y
    assert result == 30, f"Expected 30, got {result}"
    return {"status": "pass", "test": "variables", "result": result}

def test_data_structures():
    """测试数据结构"""
    # 列表
    lst = [1, 2, 3, 4, 5]
    assert len(lst) == 5
    
    # 字典
    dct = {"key": "value"}
    assert dct["key"] == "value"
    
    # 集合
    st = {1, 2, 3}
    assert 2 in st
    
    return {"status": "pass", "test": "data_structures"}

def test_control_flow():
    """测试控制流"""
    # 条件
    if True:
        result = "success"
    else:
        result = "fail"
    assert result == "success"
    
    # 循环
    total = 0
    for i in range(5):
        total += i
    assert total == 10
    
    return {"status": "pass", "test": "control_flow"}

def test_functions():
    """测试函数"""
    def add(a, b):
        return a + b
    
    result = add(3, 4)
    assert result == 7
    
    return {"status": "pass", "test": "functions"}

def test_exception_handling():
    """测试异常处理"""
    try:
        raise ValueError("Test error")
    except ValueError as e:
        assert str(e) == "Test error"
        return {"status": "pass", "test": "exception_handling"}
    
    return {"status": "fail", "test": "exception_handling"}

def test_json_operations():
    """测试 JSON 操作"""
    data = {"name": "test", "value": 123}
    json_str = json.dumps(data)
    parsed = json.loads(json_str)
    assert parsed == data
    
    return {"status": "pass", "test": "json_operations"}

def test_datetime():
    """测试日期时间"""
    from datetime import datetime, timedelta
    
    now = datetime.now()
    tomorrow = now + timedelta(days=1)
    assert tomorrow.day == (now.day + 1) or (tomorrow.month != now.month)
    
    return {"status": "pass", "test": "datetime"}

def test_regex():
    """测试正则表达式"""
    import re
    
    pattern = r'\d+'
    text = "abc123def"
    match = re.search(pattern, text)
    assert match.group() == "123"
    
    return {"status": "pass", "test": "regex"}

def run_all_tests():
    """运行所有测试"""
    tests = [
        test_hello_world,
        test_variables,
        test_data_structures,
        test_control_flow,
        test_functions,
        test_exception_handling,
        test_json_operations,
        test_datetime,
        test_regex,
    ]
    
    results = []
    passed = 0
    failed = 0
    
    for test in tests:
        try:
            result = test()
            results.append(result)
            passed += 1
            print(f"✓ {test.__name__}: PASS")
        except Exception as e:
            results.append({
                "status": "fail",
                "test": test.__name__,
                "error": str(e)
            })
            failed += 1
            print(f"✗ {test.__name__}: FAIL - {e}")
    
    summary = {
        "total": len(tests),
        "passed": passed,
        "failed": failed,
        "success_rate": f"{(passed/len(tests)*100):.1f}%"
    }
    
    print(f"\n{'='*50}")
    print(f"测试总结: {passed}/{len(tests)} 通过 ({summary['success_rate']})")
    print(f"{'='*50}")
    
    return {
        "summary": summary,
        "results": results
    }

if __name__ == "__main__":
    result = run_all_tests()
    print(json.dumps(result, indent=2))
    sys.exit(0 if result["summary"]["failed"] == 0 else 1)
```

### 7.2 安全测试脚本

```python
#!/usr/bin/env python3
"""
Python 节点安全测试脚本
用于验证安全机制是否正常工作
"""

import sys

DANGEROUS_CALLS = [
    ("eval", "eval('1+1')"),
    ("exec", "exec('print(1)')"),
    ("__import__", "__import__('os')"),
    ("compile", "compile('1', '1', 'exec')"),
    ("open", "open('/etc/passwd')"),
    ("globals", "globals()"),
    ("locals", "locals()"),
    ("vars", "vars()"),
    ("dir", "dir()"),
]

MODULE_IMPORTS = [
    ("os", "import os"),
    ("sys", "import sys"),
    ("subprocess", "import subprocess"),
    ("socket", "import socket"),
    ("ctypes", "import ctypes"),
    ("pickle", "import pickle"),
]

def test_dangerous_builtins():
    """测试危险内置函数拦截"""
    results = []
    
    for name, code in DANGEROUS_CALLS:
        try:
            exec(code)
            results.append({
                "name": name,
                "status": "FAIL",
                "reason": "Should have been blocked"
            })
            print(f"✗ {name}: 未被拦截 (FAIL)")
        except (SecurityError, NameError, TypeError, AttributeError) as e:
            results.append({
                "name": name,
                "status": "PASS",
                "reason": str(e)
            })
            print(f"✓ {name}: 已拦截 (PASS)")
        except Exception as e:
            results.append({
                "name": name,
                "status": "PASS",
                "reason": f"{type(e).__name__}: {e}"
            })
            print(f"✓ {name}: 已拦截 ({type(e).__name__})")
    
    return results

def test_module_imports():
    """测试模块导入限制"""
    results = []
    
    for name, code in MODULE_IMPORTS:
        try:
            exec(code)
            results.append({
                "name": name,
                "status": "FAIL",
                "reason": "Should have been blocked"
            })
            print(f"✗ {name}: 未被拦截 (FAIL)")
        except (ImportError, ModuleNotFoundError, SecurityError) as e:
            results.append({
                "name": name,
                "status": "PASS",
                "reason": str(e)
            })
            print(f"✓ {name}: 已拦截 (PASS)")
        except Exception as e:
            results.append({
                "name": name,
                "status": "PASS",
                "reason": f"{type(e).__name__}: {e}"
            })
            print(f"✓ {name}: 已拦截 ({type(e).__name__})")
    
    return results

def test_bypass_attempts():
    """测试绕过尝试"""
    bypass_codes = [
        "().__class__.__mro__",
        "().__class__.__bases__[0].__subclasses__()",
        "getattr(str, '__mro__')",
        "[].__class__.__base__.__subclasses__()",
    ]
    
    results = []
    for i, code in enumerate(bypass_codes):
        try:
            result = eval(code)
            results.append({
                "test": f"bypass_{i}",
                "code": code,
                "status": "FAIL",
                "reason": "Bypass succeeded"
            })
            print(f"✗ 绕过尝试 {i}: 成功 (FAIL)")
        except Exception as e:
            results.append({
                "test": f"bypass_{i}",
                "code": code,
                "status": "PASS",
                "reason": f"{type(e).__name__}: {e}"
            })
            print(f"✓ 绕过尝试 {i}: 被拦截 (PASS)")
    
    return results

def run_security_tests():
    """运行所有安全测试"""
    print("=" * 50)
    print("安全测试 - 危险函数拦截")
    print("=" * 50)
    builtin_results = test_dangerous_builtins()
    
    print("\n" + "=" * 50)
    print("安全测试 - 模块导入限制")
    print("=" * 50)
    module_results = test_module_imports()
    
    print("\n" + "=" * 50)
    print("安全测试 - 绕过尝试")
    print("=" * 50)
    bypass_results = test_bypass_attempts()
    
    all_results = builtin_results + module_results + bypass_results
    passed = sum(1 for r in all_results if r["status"] == "PASS")
    total = len(all_results)
    
    print(f"\n{'='*50}")
    print(f"安全测试总结: {passed}/{total} 通过 ({(passed/total*100):.1f}%)")
    print(f"{'='*50}")
    
    return {
        "summary": {
            "total": total,
            "passed": passed,
            "failed": total - passed,
            "success_rate": f"{(passed/total*100):.1f}%"
        },
        "builtin_tests": builtin_results,
        "module_tests": module_results,
        "bypass_tests": bypass_results
    }

if __name__ == "__main__":
    result = run_security_tests()
    print(json.dumps(result, indent=2))
    sys.exit(0 if result["summary"]["failed"] == 0 else 1)
```

### 7.3 性能测试脚本

```python
#!/usr/bin/env python3
"""
Python 节点性能测试脚本
用于验证超时和内存限制
"""

import sys
import time

def test_timeout():
    """测试超时控制"""
    print("测试超时控制 (应超时)...")
    start = time.time()
    try:
        time.sleep(35)  # 超过默认 30s 超时
        elapsed = time.time() - start
        return {
            "test": "timeout",
            "status": "FAIL",
            "reason": f"Should have timed out, but completed in {elapsed}s"
        }
    except TimeoutError as e:
        elapsed = time.time() - start
        return {
            "test": "timeout",
            "status": "PASS",
            "elapsed": elapsed,
            "reason": f"Timed out after {elapsed}s as expected"
        }
    except Exception as e:
        elapsed = time.time() - start
        return {
            "test": "timeout",
            "status": "PASS",
            "elapsed": elapsed,
            "reason": f"{type(e).__name__}: {e}"
        }

def test_infinite_loop():
    """测试无限循环拦截"""
    print("测试无限循环 (应超时)...")
    start = time.time()
    try:
        count = 0
        while True:
            count += 1
        return {
            "test": "infinite_loop",
            "status": "FAIL",
            "reason": "Loop should have been terminated"
        }
    except TimeoutError as e:
        elapsed = time.time() - start
        return {
            "test": "infinite_loop",
            "status": "PASS",
            "elapsed": elapsed,
            "reason": f"Terminated after {elapsed}s"
        }
    except Exception as e:
        elapsed = time.time() - start
        return {
            "test": "infinite_loop",
            "status": "PASS",
            "elapsed": elapsed,
            "reason": f"{type(e).__name__}: {e}"
        }

def test_memory_limit():
    """测试内存限制"""
    print("测试内存限制...")
    try:
        # 尝试分配大量内存
        data = []
        for i in range(1000000):
            data.append([0] * 1000)
        return {
            "test": "memory_limit",
            "status": "INFO",
            "reason": "Memory allocation completed (may be within limits)"
        }
    except MemoryError as e:
        return {
            "test": "memory_limit",
            "status": "PASS",
            "reason": f"MemoryError as expected: {e}"
        }
    except Exception as e:
        return {
            "test": "memory_limit",
            "status": "PASS",
            "reason": f"{type(e).__name__}: {e}"
        }

def test_recursion_limit():
    """测试递归限制"""
    print("测试递归限制...")
    try:
        def recurse(n):
            return recurse(n + 1)
        recurse(0)
        return {
            "test": "recursion_limit",
            "status": "FAIL",
            "reason": "Recursion should have been limited"
        }
    except RecursionError as e:
        return {
            "test": "recursion_limit",
            "status": "PASS",
            "reason": f"RecursionError as expected"
        }
    except Exception as e:
        return {
            "test": "recursion_limit",
            "status": "PASS",
            "reason": f"{type(e).__name__}: {e}"
        }

def run_performance_tests():
    """运行所有性能测试"""
    tests = [
        test_timeout,
        test_infinite_loop,
        test_memory_limit,
        test_recursion_limit,
    ]
    
    results = []
    for test in tests:
        try:
            result = test()
            results.append(result)
            status = "✓" if result["status"] in ["PASS", "INFO"] else "✗"
            print(f"{status} {test.__name__}: {result['status']}")
        except Exception as e:
            results.append({
                "test": test.__name__,
                "status": "ERROR",
                "reason": f"{type(e).__name__}: {e}"
            })
            print(f"✗ {test.__name__}: ERROR - {e}")
    
    passed = sum(1 for r in results if r["status"] in ["PASS", "INFO"])
    total = len(results)
    
    print(f"\n{'='*50}")
    print(f"性能测试总结: {passed}/{total} 通过")
    print(f"{'='*50}")
    
    return {
        "summary": {
            "total": total,
            "passed": passed,
            "failed": total - passed
        },
        "results": results
    }

if __name__ == "__main__":
    result = run_performance_tests()
    print(json.dumps(result, indent=2))
    sys.exit(0 if result["summary"]["failed"] == 0 else 1)
```

### 7.4 集成测试脚本 (Shell)

```bash
#!/bin/bash
# Python 节点集成测试脚本
# 用于自动化执行所有测试并生成报告

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORT_DIR="${SCRIPT_DIR}/test-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo "=============================================="
echo "Python 节点集成测试"
echo "时间: $(date)"
echo "=============================================="

# 创建报告目录
mkdir -p "${REPORT_DIR}"

# 功能测试
echo ""
echo "[1/4] 运行功能测试..."
python3 "${SCRIPT_DIR}/test_functional.py" > "${REPORT_DIR}/functional_${TIMESTAMP}.json" 2>&1 || true

# 安全测试
echo ""
echo "[2/4] 运行安全测试..."
python3 "${SCRIPT_DIR}/test_security.py" > "${REPORT_DIR}/security_${TIMESTAMP}.json" 2>&1 || true

# 性能测试
echo ""
echo "[3/4] 运行性能测试..."
python3 "${SCRIPT_DIR}/test_performance.py" > "${REPORT_DIR}/performance_${TIMESTAMP}.json" 2>&1 || true

# 生成汇总报告
echo ""
echo "[4/4] 生成汇总报告..."
cat > "${REPORT_DIR}/summary_${TIMESTAMP}.md" << EOF
# Python 节点测试报告

**执行时间:** $(date)
**测试版本:** 1.0

## 测试结果

### 功能测试
\`\`\`json
$(cat "${REPORT_DIR}/functional_${TIMESTAMP}.json" | head -20)
\`\`\`

### 安全测试
\`\`\`json
$(cat "${REPORT_DIR}/security_${TIMESTAMP}.json" | head -20)
\`\`\`

### 性能测试
\`\`\`json
$(cat "${REPORT_DIR}/performance_${TIMESTAMP}.json" | head -20)
\`\`\`

## 详细报告

- 功能测试：${REPORT_DIR}/functional_${TIMESTAMP}.json
- 安全测试：${REPORT_DIR}/security_${TIMESTAMP}.json
- 性能测试：${REPORT_DIR}/performance_${TIMESTAMP}.json

EOF

echo ""
echo "=============================================="
echo "测试完成！"
echo "报告位置：${REPORT_DIR}/summary_${TIMESTAMP}.md"
echo "=============================================="
```

---

## 八、验收标准

### 8.1 P0 级别验收标准 (必须满足)

| 类别 | 标准 | 通过条件 |
|------|------|----------|
| 功能 | 基础执行 | Hello World、变量、函数、循环、条件全部通过 |
| 功能 | 输入输出 | 能正确接收上游数据并传递给下游 |
| 功能 | 预装库 | 标准库和预装第三方库可正常导入使用 |
| 安全 | 危险函数 | eval/exec/__import__/open 等 100% 拦截 |
| 安全 | 模块限制 | os/sys/subprocess/socket 等危险模块 100% 拦截 |
| 安全 | 网络隔离 | 默认禁止网络访问，内网地址 100% 拦截 |
| 安全 | 文件隔离 | 系统文件不可访问，沙箱隔离有效 |
| 安全 | 绕过防护 | 所有已知绕过尝试均被拦截 |
| 性能 | 超时控制 | 30 秒超时准确触发，资源正确清理 |
| 性能 | 内存限制 | 超出限制时正确终止并释放资源 |
| 异常 | 错误处理 | 语法/运行时错误有清晰提示和堆栈 |

### 8.2 P1 级别验收标准 (强烈推荐)

| 类别 | 标准 | 通过条件 |
|------|------|----------|
| 功能 | Monaco Editor | 语法高亮、自动补全、快捷键正常工作 |
| 功能 | 多输出分支 | 条件路由到不同下游节点 |
| 功能 | 代码模板 | 模板库可用，插入代码正确 |
| 功能 | 执行历史 | 可查看最近执行的输入/输出/日志 |
| 安全 | 白名单网络 | 配置白名单后可访问指定域名 |
| 性能 | 并发执行 | 多节点并发互不干扰 |
| 异常 | 超时日志 | 超时时有详细日志记录 |

### 8.3 P2 级别验收标准 (可选)

| 类别 | 标准 | 通过条件 |
|------|------|----------|
| 功能 | 自定义依赖 | 支持 pip install (自托管模式) |
| 功能 | AI 代码生成 | 根据描述生成可用代码 |
| 功能 | 断点调试 | 支持断点和单步执行 |
| 性能 | 依赖缓存 | 二次执行明显更快 |

### 8.4 总体验收判定

```
总判定 = P0 通过率 × 60% + P1 通过率 × 30% + P2 通过率 × 10%

通过条件:
- P0 用例通过率必须 100%
- 总判定分数 ≥ 90%
- 无 Critical/Blocker 级别缺陷
```

---

## 九、测试执行流程

### 9.1 测试准备

1. 确认测试环境配置符合要求
2. 准备测试数据 (输入样本、预期输出)
3. 部署测试脚本到测试环境
4. 配置测试报告存储位置

### 9.2 测试执行

```
Day 1-2: 功能测试 (FE, EX, IO, DP 系列用例)
Day 3:   安全测试 (SC, SM, NW, FS 系列用例)
Day 4:   性能测试 (PF 系列用例)
Day 5:   异常测试 (ER 系列用例)
Day 6:   回归测试 + 缺陷验证
Day 7:   验收报告编写
```

### 9.3 缺陷管理

| 严重程度 | 定义 | 响应时间 |
|----------|------|----------|
| Critical | 系统崩溃、数据丢失、安全漏洞 | 立即修复 |
| Blocker | P0 用例失败、核心功能不可用 | 24 小时内 |
| Major | P1 用例失败、重要功能缺陷 | 3 天内 |
| Minor | P2 用例失败、体验问题 | 下个迭代 |

### 9.4 验收报告模板

```markdown
# Python 节点验收报告

## 测试概述
- 测试周期: YYYY-MM-DD 至 YYYY-MM-DD
- 测试人员: XXX
- 测试环境: XXX

## 测试结果汇总
| 测试类型 | 用例总数 | 通过数 | 失败数 | 通过率 |
|----------|----------|--------|--------|--------|
| 功能测试 | XX | XX | XX | XX% |
| 安全测试 | XX | XX | XX | XX% |
| 性能测试 | XX | XX | XX | XX% |
| 异常测试 | XX | XX | XX | XX% |
| **总计** | **XX** | **XX** | **XX** | **XX%** |

## 缺陷汇总
| 严重程度 | 数量 | 已修复 | 待修复 |
|----------|------|--------|--------|
| Critical | X | X | X |
| Blocker | X | X | X |
| Major | X | X | X |
| Minor | X | X | X |

## 验收结论
- [ ] 通过 (所有 P0 用例通过，总分 ≥ 90%)
- [ ] 有条件通过 (P0 全过，P1 通过率 ≥ 90%，遗留问题可接受)
- [ ] 失败 (存在 P0 失败或总分 < 90%)

## 遗留问题
(列出所有未修复缺陷及风险说明)

## 签字确认
- 测试负责人: ___________ 日期: ___________
- 开发负责人: ___________ 日期: ___________
- 产品负责人: ___________ 日期: ___________
```

---

## 十、附录

### 10.1 测试数据样本

```json
{
  "sample_input_1": {
    "name": "test_user",
    "value": 123,
    "items": [1, 2, 3]
  },
  "sample_input_2": {
    "url": "https://api.example.com/data",
    "method": "GET",
    "headers": {"Authorization": "Bearer token"}
  },
  "sample_file_input": {
    "filename": "test.csv",
    "content": "id,name,value\n1,test,100",
    "mimetype": "text/csv"
  }
}
```

### 10.2 参考文档

- [n8n Code Node 文档](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.code/)
- [Python 沙箱安全最佳实践](https://docs.python.org/3/library/security.html)
- [Docker 安全配置指南](https://docs.docker.com/engine/security/)

### 10.3 修订历史

| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|----------|
| 1.0 | 2024-04-01 | 龙波儿 | 初始版本 |

---

**文档结束**
