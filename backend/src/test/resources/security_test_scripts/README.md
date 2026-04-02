# 安全测试脚本说明

## 目录结构

```
security_test_scripts/
├── README.md                    # 本说明文件
├── test_dangerous_functions.py  # 危险函数拦截测试
├── test_blocked_modules.py      # 模块导入限制测试
├── test_allowed_modules.py      # 允许模块测试
└── test_bypass_attempts.py      # 绕过尝试测试
```

## 测试用例说明

### 1. test_dangerous_functions.py
**测试目标**: 验证 eval/exec/__import__/compile 等危险函数被正确拦截

**测试内容**:
- `eval()` 函数拦截
- `exec()` 函数拦截
- `__import__()` 函数拦截
- `compile()` 函数拦截
- 通过 `__builtins__` 访问危险函数

**预期结果**: 所有危险函数调用都应该被阻止并抛出安全异常

---

### 2. test_blocked_modules.py
**测试目标**: 验证 os/subprocess/socket 等危险模块被正确拦截

**测试内容**:
- `os` 模块导入拦截
- `subprocess` 模块导入拦截
- `socket` 模块导入拦截
- `ctypes` 模块导入拦截（可用于绕过沙箱）
- `sys` 模块危险功能拦截
- `platform` 模块导入拦截（信息泄露）

**预期结果**: 所有危险模块导入都应该被阻止

---

### 3. test_allowed_modules.py
**测试目标**: 验证 math/json/requests 等安全模块可以正常使用

**测试内容**:
- `math` 模块正常使用
- `json` 模块正常使用
- `requests` 模块正常使用（网络可能受限）
- `datetime` 模块正常使用
- `re` 模块正常使用
- `collections` 模块正常使用

**预期结果**: 所有允许的模块都应该可以正常导入和使用

---

### 4. test_bypass_attempts.py
**测试目标**: 验证各种绕过沙箱的尝试都被正确拦截

**测试内容**:
- 动态导入绕过 (getattr)
- importlib.import_module 绕过
- globals()['__import__'] 绕过
- locals()['__import__'] 绕过
- 字符串拼接绕过
- base64 编码绕过
- 文件导入绕过
- os.popen 绕过
- subprocess.call 绕过
- reload() 内置函数绕过

**预期结果**: 所有绕过尝试都应该被阻止

---

## 执行测试

### 前置条件
- Python 3.8+
- 安全沙箱环境已部署
- requests 库已安装（可选）

### 执行命令

```bash
# 执行单个测试
python3 test_dangerous_functions.py
python3 test_blocked_modules.py
python3 test_allowed_modules.py
python3 test_bypass_attempts.py

# 执行所有测试
for test in test_*.py; do
    echo "Running $test..."
    python3 "$test"
    echo ""
done
```

### 输出格式

每个测试用例会输出：
- ✅ PASS - 测试通过，安全措施有效
- ❌ FAIL - 测试失败，发现安全漏洞

### 退出码
- `0` - 所有测试通过
- `1` - 发现安全漏洞

---

## 缺陷报告模板

发现安全漏洞时，请记录以下信息：

```markdown
## 安全漏洞报告

**漏洞 ID**: SEC-001
**发现日期**: 2024-01-01
**严重程度**: [高/中/低]
**测试用例**: test_xxx.py - test_xxx()

**漏洞描述**:
[详细描述漏洞]

**复现步骤**:
1. [步骤 1]
2. [步骤 2]
3. [步骤 3]

**影响范围**:
[说明可能的影响]

**修复建议**:
[提供修复建议]

**状态**: [待修复/修复中/已修复]
```

---

## 测试报告模板

```markdown
# 安全测试报告

**测试日期**: 2024-01-01
**测试人员**: 龙波儿
**环境版本**: [Python Node 版本号]

## 测试概览

| 测试套件 | 通过 | 失败 | 通过率 |
|---------|------|------|--------|
| 危险函数拦截 | X/5 | X | XX% |
| 模块导入限制 | X/6 | X | XX% |
| 允许模块 | X/6 | X | XX% |
| 绕过尝试 | X/10 | X | XX% |
| **总计** | **X/27** | **X** | **XX%** |

## 发现的问题

[列出所有发现的安全漏洞]

## 结论

[测试结论和建议]
```

---

## 注意事项

1. **测试环境**: 所有测试应在隔离的沙箱环境中执行
2. **权限控制**: 确保测试环境没有生产环境权限
3. **日志记录**: 保存所有测试输出用于审计
4. **定期执行**: 每次代码变更后应重新执行安全测试
5. **持续改进**: 发现新的绕过方式后应及时更新测试用例

---

## 更新日志

| 日期 | 版本 | 更新内容 |
|------|------|----------|
| 2024-01-01 | 1.0.0 | 初始版本，包含 4 个测试套件 |
