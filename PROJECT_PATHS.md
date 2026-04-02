# 项目路径规范 - 严禁放错位置！

## ⚠️ 血泪教训
**2026-04-01 Sprint 2:** 龙波儿把测试脚本放到 `workspace-spider/backend/`，导致：
- 验收测试无法找到文件
- 浪费 30 分钟排查
- 两次验收报告错误

**根本原因：** 没有在项目开始时确认文件保存位置

---

## ✅ 正确路径规范

### 主项目目录
```
/Users/ben/.openclaw/workspace-coder/ai-workflow/
├── backend/
│   ├── src/
│   │   ├── main/java/com/ben/workflow/
│   │   │   ├── engine/           # 执行引擎
│   │   │   ├── security/         # 安全模块（Sprint 2）
│   │   │   ├── model/            # 数据模型
│   │   │   └── adapter/          # 模型适配器
│   │   └── test/java/com/ben/workflow/
│   │       └── security/         # 单元测试
│   └── src/test/resources/
│       └── security_test_scripts/ # Python 测试脚本
├── frontend/
└── 文档目录
```

### 各角色工作目录

| 角色 | 工作目录 | 文件保存位置 |
|------|----------|--------------|
| **龙傲天 (Coder)** | `workspace-coder/ai-workflow/` | 同上 |
| **龙波儿 (Tester)** | `workspace-coder/ai-workflow/` | **必须与龙傲天一致** |
| **龙霸天 (Knowledge)** | `workspace-coder/ai-workflow/` | 文档保存在项目根目录 |
| **波妞 (Acceptor)** | `workspace-boniu/` | 调研报告保存在 `workspace-boniu/xxx-research/` |

---

## 🚫 禁止行为

| 错误 | 正确 |
|------|------|
| ❌ 测试脚本放到 `workspace-spider/` | ✅ 测试脚本放到 `workspace-coder/ai-workflow/backend/src/test/resources/` |
| ❌ 代码放到 `workspace/` | ✅ 代码放到 `workspace-coder/ai-workflow/` |
| ❌ 文档放到 `workspace-coder/` | ✅ 文档放到项目根目录 |

---

## ✅ 任务开始时必须确认

**接收任务时第一句话：**
```
确认项目路径：/Users/ben/.openclaw/workspace-coder/ai-workflow/
确认文件保存位置：[具体路径]
```

**示例：**
```
🫡 收到任务！

确认项目路径：/Users/ben/.openclaw/workspace-coder/ai-workflow/backend/
确认测试脚本位置：/Users/ben/.openclaw/workspace-coder/ai-workflow/backend/src/test/resources/security_test_scripts/
确认测试类位置：/Users/ben/.openclaw/workspace-coder/ai-workflow/backend/src/test/java/com/ben/workflow/security/

立即开始！
```

---

## 📋 检查清单

每次保存文件前执行：
```bash
# 1. 确认当前目录
pwd

# 2. 确认目标目录存在
ls -la /Users/ben/.openclaw/workspace-coder/ai-workflow/backend/

# 3. 确认文件保存位置正确
echo "保存位置：$(pwd)"
```

---

**违反此规范的后果：**
- 团队时间浪费
- 验收报告错误
- 需要重新执行任务

**再犯处理：**
- 第一次：警告 + 重新执行
- 第二次：暂停任务 + 重新培训
- 第三次：更换角色

---

**最后更新：** 2026-04-01 08:48
**触发事件：** Sprint 2 测试脚本位置错误（浪费 30 分钟）
