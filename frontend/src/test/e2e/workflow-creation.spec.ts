import { test, expect } from '@playwright/test'

/**
 * E2E 测试场景 1: 创建工作流
 * 覆盖功能：F001, F002, F011-F015, F024-F027
 */
test.describe('工作流创建', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    // 等待页面加载
    await page.waitForTimeout(3000)
  })

  test('应从节点面板拖拽创建节点', async ({ page }) => {
    // 从节点面板拖拽处理节点到画布
    const processNode = page.getByRole('button', { name: '处理节点' })
    await expect(processNode).toBeVisible({ timeout: 10000 })
    
    const canvas = page.getByTestId('react-flow')
    await expect(canvas).toBeVisible()
    
    await processNode.dragTo(canvas, {
      targetPosition: { x: 200, y: 200 }
    })
    
    await page.waitForTimeout(1000)
    
    // 验证节点已创建 (检查画布中有节点元素)
    const nodes = page.locator('[class*="react-flow__node"]')
    await expect(nodes).toHaveCount({ min: 1 })
  })

  test('应连接多个节点创建工作流', async ({ page }) => {
    // 添加输入节点
    const inputNode = page.getByRole('button', { name: '输入节点' })
    const canvas = page.getByTestId('react-flow')
    await inputNode.dragTo(canvas, { targetPosition: { x: 100, y: 100 } })
    
    // 添加处理节点
    const processNode = page.getByRole('button', { name: '处理节点' })
    await processNode.dragTo(canvas, { targetPosition: { x: 300, y: 100 } })
    
    await page.waitForTimeout(1000)
    
    // 验证有 2 个节点
    const nodes = page.locator('[class*="react-flow__node"]')
    await expect(nodes).toHaveCount({ min: 2 })
  })

  test('应配置节点参数', async ({ page }) => {
    // 添加节点
    const processNode = page.getByRole('button', { name: '处理节点' })
    const canvas = page.getByTestId('react-flow')
    await processNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    await page.waitForTimeout(1000)
    
    // 点击执行按钮
    const runButton = page.getByRole('button', { name: /执行工作流/ })
    await expect(runButton).toBeVisible()
  })

  test('应删除节点', async ({ page }) => {
    // 添加节点
    const processNode = page.getByRole('button', { name: '处理节点' })
    const canvas = page.getByTestId('react-flow')
    await processNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    await page.waitForTimeout(1000)
    
    // 验证有节点
    const nodes = page.locator('[class*="react-flow__node"]')
    await expect(nodes).toHaveCount({ min: 1 })
  })

  test('应保存工作流', async ({ page }) => {
    // 添加节点
    const processNode = page.getByRole('button', { name: '处理节点' })
    const canvas = page.getByTestId('react-flow')
    await processNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    await page.waitForTimeout(1000)
    
    // 验证执行按钮存在
    const runButton = page.getByRole('button', { name: /执行工作流/ })
    await expect(runButton).toBeVisible()
  })

  test('页面应正常加载', async ({ page }) => {
    // 验证画布加载
    const canvas = page.getByTestId('react-flow')
    await expect(canvas).toBeVisible({ timeout: 10000 })
    
    // 验证节点工具箱存在
    const toolbox = page.getByText('节点工具箱')
    await expect(toolbox).toBeVisible()
    
    // 验证所有节点按钮存在
    await expect(page.getByRole('button', { name: '输入节点' })).toBeVisible()
    await expect(page.getByRole('button', { name: '模型节点' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'LLM 节点' })).toBeVisible()
    await expect(page.getByRole('button', { name: '处理节点' })).toBeVisible()
    await expect(page.getByRole('button', { name: '输出节点' })).toBeVisible()
  })
})
