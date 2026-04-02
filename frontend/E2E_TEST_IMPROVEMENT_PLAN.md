# E2E 测试改进计划

## 📊 当前测试状态

### 现有测试文件
| 文件 | 测试场景 | 状态 |
|------|----------|------|
| `python-node-e2e.spec.ts` | Python 节点功能（5 场景） | ✅ 100% 通过 |
| `full-workflow-e2e.spec.ts` | 完整工作流（5 场景） | ⚠️ 部分通过 |
| `workflow-creation.spec.ts` | 工作流创建（6 用例） | ❌ 失败 |
| `workflow-execution.spec.ts` | 工作流执行（10 用例） | ❌ 失败 |
| `workflow-import-export.spec.ts` | 导入导出（15 用例） | ❌ 失败 |

### 问题分析
1. **UI 选择器不稳定** - 使用 `getByText()` 匹配，文本变化导致失败
2. **缺少等待机制** - 页面加载/元素出现未正确等待
3. **后端依赖** - 部分测试需要真实后端服务
4. **截图验证缺失** - 视觉回归测试未实现
5. **数据清理** - 测试数据未隔离/清理

---

## 🎯 改进目标

### 短期（本周）
- [ ] 修复现有失败测试（21 个用例）
- [ ] 添加稳定选择器（data-testid）
- [ ] 完善等待机制
- [ ] 添加视觉回归测试
- [ ] 实现测试数据隔离

### 中期（下周）
- [ ] 添加 API Mock 层
- [ ] 实现并行测试执行
- [ ] 添加性能测试
- [ ] 完善错误场景测试

### 长期（本月）
- [ ] 测试覆盖率 >90%
- [ ] CI/CD 集成
- [ ] 自动化截图对比
- [ ] 测试报告可视化

---

## 📝 测试改进清单

### 1. Python 节点测试增强

#### 新增测试场景
```typescript
// python-node-advanced.spec.ts

// 场景 1: Monaco 编辑器功能测试
test('Monaco 编辑器 - 代码补全和语法高亮', async ({ page }) => {
  // 验证编辑器加载
  // 验证语法高亮
  // 验证错误提示
})

// 场景 2: 依赖包管理测试
test('Python 节点 - 依赖包自动安装', async ({ page }) => {
  // 添加 numpy/pandas 依赖
  // 验证依赖安装成功
  // 执行使用依赖的代码
})

// 场景 3: 超时控制测试
test('Python 节点 - 超时处理', async ({ page }) => {
  // 配置 5 秒超时
  // 执行 10 秒的脚本
  // 验证超时错误提示
})

// 场景 4: 安全拦截测试
test('Python 节点 - 危险函数拦截', async ({ page }) => {
  // 尝试执行 eval()
  // 验证被安全层拦截
  // 验证错误消息
})

// 场景 5: 大数据量测试
test('Python 节点 - 大数据处理', async ({ page }) => {
  // 执行处理 1000 条数据的脚本
  // 验证执行结果
  // 验证性能指标
})
```

#### 改进现有测试
```typescript
// 改进点：
// 1. 使用 data-testid 替代 getByText
// 2. 添加明确的等待条件
// 3. 添加截图验证
// 4. 添加错误恢复测试

// 改进前：
const saveButton = page.getByText(/保存配置 | 保存|Save/)
await saveButton.click()

// 改进后：
const saveButton = page.getByTestId('python-node-save-btn')
await expect(saveButton).toBeVisible({ timeout: 5000 })
await saveButton.click()
await expect(page.getByTestId('toast-success')).toBeVisible()
```

---

### 2. 工作流创建测试修复

#### 问题修复
```typescript
// workflow-creation.spec.ts

// 问题 1: 节点拖拽失败
// 原因：minimap 遮挡
// 修复：关闭 minimap 或使用 force: true

test('拖拽节点到画布', async ({ page }) => {
  // 关闭 minimap
  await page.getByTestId('minimap-toggle').click()
  
  // 拖拽节点
  const canvas = page.getByTestId('react-flow-canvas')
  const node = page.getByTestId('node-python')
  await node.dragTo(canvas, { 
    targetPosition: { x: 300, y: 200 },
    force: true  // 强制拖拽
  })
  
  // 验证节点出现
  await expect(page.getByTestId('flow-node-1')).toBeVisible()
})

// 问题 2: 保存失败
// 原因：未等待表单验证完成
// 修复：添加明确等待

test('保存工作流', async ({ page }) => {
  await page.getByTestId('workflow-save-btn').click()
  
  // 等待保存完成
  await expect(page.getByTestId('toast-success')).toBeVisible({ timeout: 10000 })
  
  // 验证工作流已保存
  const workflows = await page.getByTestId('workflow-list-item').all()
  expect(workflows.length).toBeGreaterThan(0)
})
```

---

### 3. 工作流执行测试修复

#### 添加 Mock 层
```typescript
// fixtures/mock-api.ts

import { test as base } from '@playwright/test'

export const test = base.extend<{
  mockWorkflowExecution: () => void
}>({
  mockWorkflowExecution: async ({ page }, use) => {
    // Mock 工作流执行 API
    await page.route('**/api/workflows/*/execute', async route => {
      await route.fulfill({
        status: 200,
        body: JSON.stringify({
          executionId: 'test-exec-123',
          status: 'running'
        })
      })
    })
    
    // Mock 执行状态 API
    await page.route('**/api/executions/*/status', async route => {
      await route.fulfill({
        status: 200,
        body: JSON.stringify({
          status: 'success',
          result: { output: 'Hello from Python!' }
        })
      })
    })
    
    await use()
  }
})

export { expect } from '@playwright/test'
```

#### 使用 Mock 的测试
```typescript
// workflow-execution-mock.spec.ts

import { test, expect } from './fixtures/mock-api'

test('工作流执行 - 成功场景', async ({ page, mockWorkflowExecution }) => {
  await mockWorkflowExecution()
  
  // 执行工作流
  await page.getByTestId('workflow-run-btn').click()
  
  // 验证执行状态
  await expect(page.getByTestId('execution-status'))
    .toHaveText('成功', { timeout: 10000 })
  
  // 验证输出结果
  await expect(page.getByTestId('execution-output'))
    .toContainText('Hello from Python!')
})
```

---

### 4. 导入导出测试修复

#### 添加文件处理
```typescript
// workflow-import-export.spec.ts

import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

test('工作流导出', async ({ page }) => {
  // 设置下载目录
  const downloadDir = path.join(__dirname, 'downloads')
  if (!fs.existsSync(downloadDir)) {
    fs.mkdirSync(downloadDir, { recursive: true })
  }
  
  // 导出工作流
  await page.getByTestId('workflow-export-btn').click()
  
  // 等待下载完成
  const download = await page.waitForEvent('download', { timeout: 10000 })
  const filePath = path.join(downloadDir, download.suggestedFilename())
  await download.saveAs(filePath)
  
  // 验证文件存在
  expect(fs.existsSync(filePath)).toBe(true)
  
  // 验证 JSON 格式
  const content = fs.readFileSync(filePath, 'utf-8')
  const workflow = JSON.parse(content)
  expect(workflow.nodes).toBeDefined()
  expect(workflow.edges).toBeDefined()
})

test('工作流导入', async ({ page }) => {
  // 准备测试文件
  const testWorkflow = {
    name: 'Test Workflow',
    nodes: [
      { id: '1', type: 'python_script', config: { script: 'print("test")' } }
    ],
    edges: []
  }
  
  const filePath = path.join(__dirname, 'fixtures', 'test-workflow.json')
  fs.writeFileSync(filePath, JSON.stringify(testWorkflow, null, 2))
  
  // 导入工作流
  const fileInput = page.getByTestId('workflow-import-input')
  await fileInput.setInputFiles(filePath)
  
  // 验证导入成功
  await expect(page.getByTestId('toast-success')).toBeVisible()
  await expect(page.getByTestId('flow-node-1')).toBeVisible()
})
```

---

### 5. 视觉回归测试

#### 添加截图对比
```typescript
// visual-regression.spec.ts

import { test, expect } from '@playwright/test'

test('画布界面 - 视觉回归', async ({ page }) => {
  await page.goto('/')
  
  // 等待页面加载
  await page.waitForLoadState('networkidle')
  
  // 截图对比
  await expect(page.getByTestId('react-flow-canvas'))
    .toHaveScreenshot('canvas-initial.png', {
      maxDiffPixels: 100,  // 允许的最大差异像素
      threshold: 0.1       // 差异阈值
    })
})

test('Python 节点配置面板 - 视觉回归', async ({ page }) => {
  await page.goto('/')
  
  // 添加 Python 节点
  const canvas = page.getByTestId('react-flow-canvas')
  const node = page.getByTestId('node-python')
  await node.dragTo(canvas, { targetPosition: { x: 300, y: 200 } })
  
  // 点击节点打开配置面板
  await page.getByTestId('flow-node-1').click()
  
  // 验证配置面板
  await expect(page.getByTestId('python-config-panel'))
    .toHaveScreenshot('python-config-panel.png', {
      maxDiffPixels: 50,
      threshold: 0.1
    })
})
```

---

### 6. 性能测试

#### 添加性能指标收集
```typescript
// performance.spec.ts

import { test, expect } from '@playwright/test'

test('页面加载性能', async ({ page }) => {
  // 启用性能监控
  await page.addInitScript(() => {
    (window as any).performanceMetrics = []
  })
  
  const startTime = Date.now()
  await page.goto('/')
  await page.waitForLoadState('networkidle')
  const loadTime = Date.now() - startTime
  
  // 验证加载时间 < 3 秒
  expect(loadTime).toBeLessThan(3000)
  
  // 收集性能指标
  const metrics = await page.metrics()
  console.log('JS Heap Size:', metrics.JSHeapUsedSize)
})

test('节点拖拽性能', async ({ page }) => {
  await page.goto('/')
  
  // 拖拽 10 个节点
  const startTime = Date.now()
  for (let i = 0; i < 10; i++) {
    const canvas = page.getByTestId('react-flow-canvas')
    const node = page.getByTestId('node-python')
    await node.dragTo(canvas, { 
      targetPosition: { x: 300 + i * 50, y: 200 + i * 50 }
    })
  }
  const dragTime = Date.now() - startTime
  
  // 验证拖拽性能 < 5 秒
  expect(dragTime).toBeLessThan(5000)
})
```

---

## 📋 测试执行策略

### 测试分类
```
测试/
├── e2e/
│   ├── smoke/           # 冒烟测试（核心功能）
│   │   ├── python-node-basic.spec.ts
│   │   └── workflow-crud.spec.ts
│   ├── regression/      # 回归测试（全量）
│   │   ├── python-node-advanced.spec.ts
│   │   ├── workflow-execution.spec.ts
│   │   └── workflow-import-export.spec.ts
│   ├── visual/          # 视觉回归测试
│   │   └── visual-regression.spec.ts
│   └── performance/     # 性能测试
│       └── performance.spec.ts
```

### 执行命令
```bash
# 冒烟测试（快速验证）
npm run test:e2e:smoke

# 回归测试（完整）
npm run test:e2e:full

# 视觉回归测试
npm run test:e2e:visual

# 性能测试
npm run test:e2e:perf

# 单个测试文件
npm run test:e2e -- python-node-e2e.spec.ts

# 带 UI 模式（调试用）
npm run test:e2e:ui
```

---

## 🎯 验收标准

### 测试覆盖率
| 指标 | 目标 | 当前 |
|------|------|------|
| E2E 测试通过率 | >95% | 待改进 |
| 核心功能覆盖 | 100% | 待改进 |
| 视觉回归覆盖 | >80% | 0% |
| 性能测试覆盖 | >50% | 0% |

### 质量标准
- [ ] 所有测试使用 data-testid 选择器
- [ ] 所有异步操作有明确等待
- [ ] 所有测试有错误恢复机制
- [ ] 所有测试有截图验证
- [ ] 测试数据完全隔离
- [ ] CI/CD 集成自动化

---

## 📅 实施计划

### Day 1-2: 修复现有测试
- [ ] 修复 workflow-creation.spec.ts（6 用例）
- [ ] 修复 workflow-execution.spec.ts（10 用例）
- [ ] 修复 workflow-import-export.spec.ts（15 用例）

### Day 3-4: 增强 Python 节点测试
- [ ] 添加 Monaco 编辑器测试
- [ ] 添加依赖包管理测试
- [ ] 添加超时控制测试
- [ ] 添加安全拦截测试
- [ ] 添加大数据处理测试

### Day 5-6: 视觉回归测试
- [ ] 配置截图对比
- [ ] 添加画布界面测试
- [ ] 添加配置面板测试
- [ ] 添加节点样式测试

### Day 7: 性能测试
- [ ] 添加页面加载测试
- [ ] 添加节点拖拽测试
- [ ] 添加执行性能测试

---

*文档生成时间：2026-04-01 20:21*  
*测试平台：Playwright + Chromium*
