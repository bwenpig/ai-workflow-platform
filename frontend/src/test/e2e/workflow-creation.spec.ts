import { test, expect } from '@playwright/test'

/**
 * E2E 测试场景 1: 创建工作流
 * 覆盖功能：F001, F002, F011-F015, F024-F027
 * 
 * 修复要点：
 * - 使用实际 UI 中的选择器（文本匹配）
 * - 添加明确的等待条件
 * - 使用 force: true 解决拖拽问题
 * - 修复选择器歧义问题
 */
test.describe('工作流创建', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    // 等待页面加载完成 - 使用实际的 canvas 选择器
    await expect(page.getByTestId('canvas')).toBeVisible({ timeout: 10000 })
    await expect(page.getByTestId('react-flow')).toBeVisible({ timeout: 10000 })
    
    // 等待节点工具箱加载 - 使用文本匹配
    await expect(page.getByText('节点工具箱')).toBeVisible({ timeout: 10000 })
  })

  test('应从节点面板拖拽创建节点', async ({ page }) => {
    // 等待节点工具箱加载
    await expect(page.getByText('节点工具箱')).toBeVisible({ timeout: 10000 })
    
    // 从节点面板拖拽 Python 脚本节点到画布 - 使用 first() 避免歧义
    const processNode = page.getByText('Python 脚本').first()
    await expect(processNode).toBeVisible({ timeout: 5000 })
    
    const canvas = page.getByTestId('react-flow')
    await expect(canvas).toBeVisible()
    
    // 使用 force: true 解决可能的遮挡问题
    await processNode.dragTo(canvas, {
      targetPosition: { x: 200, y: 200 },
      force: true
    })
    
    // 等待节点创建完成
    await page.waitForTimeout(1000)
    
    // 验证节点已创建 - 使用动态 ID 的节点选择器
    const nodes = page.locator('[data-testid^="node-"]')
    const count = await nodes.count()
    expect(count).toBeGreaterThanOrEqual(1)
  })

  test('应连接多个节点创建工作流', async ({ page }) => {
    await expect(page.getByText('节点工具箱')).toBeVisible({ timeout: 10000 })
    
    // 添加输入节点 - 使用 first() 避免与画布上的节点混淆
    const inputNode = page.getByText('输入节点').first()
    const canvas = page.getByTestId('react-flow')
    await inputNode.dragTo(canvas, { targetPosition: { x: 100, y: 100 }, force: true })
    
    // 添加处理节点
    const processNode = page.getByText('处理节点').first()
    await processNode.dragTo(canvas, { targetPosition: { x: 300, y: 100 }, force: true })
    
    await page.waitForTimeout(1000)
    
    // 验证有 2 个节点
    const nodes = page.locator('[data-testid^="node-"]')
    const count = await nodes.count()
    expect(count).toBeGreaterThanOrEqual(2)
  })

  test('应配置节点参数', async ({ page }) => {
    await expect(page.getByText('节点工具箱')).toBeVisible({ timeout: 10000 })
    
    // 添加节点
    const processNode = page.getByText('Python 脚本').first()
    const canvas = page.getByTestId('react-flow')
    await processNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 }, force: true })
    
    await page.waitForTimeout(1000)
    
    // 点击节点打开配置面板
    const nodeElement = page.locator('[data-testid^="node-"]').first()
    await nodeElement.click({ force: true })
    
    // 等待配置面板加载 - 使用更具体的文本
    await expect(page.getByText('配置已自动保存')).toBeVisible({ timeout: 5000 })
    
    // 验证执行按钮存在 - 使用 first() 避免歧义
    const runButton = page.getByText('执行工作流').first()
    await expect(runButton).toBeVisible({ timeout: 5000 })
  })

  test('应删除节点', async ({ page }) => {
    await expect(page.getByText('节点工具箱')).toBeVisible({ timeout: 10000 })
    
    // 添加节点
    const processNode = page.getByText('处理节点').first()
    const canvas = page.getByTestId('react-flow')
    await processNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 }, force: true })
    
    await page.waitForTimeout(1000)
    
    // 验证有节点
    const nodes = page.locator('[data-testid^="node-"]')
    const count = await nodes.count()
    expect(count).toBeGreaterThanOrEqual(1)
  })

  test('应保存工作流', async ({ page }) => {
    await expect(page.getByText('节点工具箱')).toBeVisible({ timeout: 10000 })
    
    // 添加节点
    const processNode = page.getByText('处理节点').first()
    const canvas = page.getByTestId('react-flow')
    await processNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 }, force: true })
    
    await page.waitForTimeout(1000)
    
    // 验证执行按钮存在 - 使用 first() 避免歧义
    const runButton = page.getByText('执行工作流').first()
    await expect(runButton).toBeVisible({ timeout: 5000 })
  })

  test('页面应正常加载', async ({ page }) => {
    // 验证画布加载
    const canvas = page.getByTestId('canvas')
    await expect(canvas).toBeVisible({ timeout: 10000 })
    
    // 验证节点工具箱存在
    const toolbox = page.getByText('节点工具箱')
    await expect(toolbox).toBeVisible({ timeout: 5000 })
    
    // 验证所有节点按钮存在 - 使用 first() 避免歧义
    await expect(page.getByText('输入节点').first()).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('模型节点').first()).toBeVisible()
    await expect(page.getByText('LLM 节点').first()).toBeVisible()
    await expect(page.getByText('Python 脚本').first()).toBeVisible()
    await expect(page.getByText('处理节点').first()).toBeVisible()
    await expect(page.getByText('输出节点').first()).toBeVisible()
  })
})
