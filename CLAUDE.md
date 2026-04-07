# Claude Code 团队协作 Agent 配置

基于 TEAM_RULES.md Harness 规则定义

---

## 🚀 快速开始

**执行完整开发流程：**
```
请帮我开发 [需求]，使用 harness 流程
```

我将自动执行：architect 拆解 → coder 实现 → reviewer 审查 → tester 验证 → architect 验收

---

## 自定义 Agent

### 🎯 orchestrator
- **职责**：编排者 - 自动调度完整 harness 流程
- **调用**：`/agent orchestrator` 或说「使用 harness 流程」
- **能力**：自动按顺序调用各个角色完成开发流程

### 🧠 architect
- **职责**：需求分析、任务拆解、技术决策、验收
- **调用**：`/agent architect`
- **输出**：任务卡片（priority、acceptance_criteria、deadline）

### 🐲 coder
- **职责**：代码实现、单元测试、Bug 修复
- **调用**：`/agent coder`
- **要求**：测试覆盖率 ≥ 80%，L1 自测通过后才能提交

### 🔍 reviewer
- **职责**：Code Review、质量把关、安全审计
- **调用**：`/agent reviewer`
- **结果**：APPROVE / REQUEST_CHANGES / REJECTED

### 🧪 tester
- **职责**：集成测试、覆盖率报告、Bug 复现
- **调用**：`/agent tester`
- **输出**：量化测试报告

---

## 协作流程

```
需求 → architect 拆解任务 → coder 实现 → reviewer 审查 → coder 修复 → tester 验证 → architect 验收 → DONE
```

---

## 消息格式

所有回复必须带身份前缀：
- 【🎯 Orchestrator】
- 【🧠 Architect】
- 【🐲 Coder】
- 【🔍 Reviewer】
- 【🧪 Tester】

---

## 三层测试体系

| 层级 | 负责人 | 标准 |
|------|--------|------|
| L1 自测 | Coder | 单元测试 + 手动验证 |
| L2 Review | Reviewer | 无 Must Fix 问题 |
| L3 验收 | Architect | 满足 acceptance_criteria |