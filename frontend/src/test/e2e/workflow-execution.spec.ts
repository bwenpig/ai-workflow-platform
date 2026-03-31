import { test, expect } from '@playwright/test'

/**
 * E2E 测试场景 2: 执行工作流
 * 覆盖功能：F028-F034, F016-F023
 */
test.describe('工作流执行', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    // 跳过新手引导
    const skipButton = page.getByText(/跳过|Skip/)
    if (await skipButton.isVisible()) {
      await skipButton.click()
    }
  })

  test('应运行工作流并显示执行状态', async ({ page }) => {
    // 添加节点
    const inputNode = page.getByText(/输入|Input/)
    const canvas = page.getByTestId('canvas')
    await inputNode.dragTo(canvas, { targetPosition: { x: 100, y: 100 } })
    
    // 运行工作流
    const runButton = page.getByText(/运行|Run|执行/)
    await runButton.click()
    
    // 验证执行状态
    await expect(page.getByText(/运行中|Running/)).toBeVisible()
    
    // 等待执行完成
    await expect(page.getByText(/完成|Success|成功/)).toBeVisible({ timeout: 30000 })
  })

  test('应显示节点执行结果', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点|处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 运行
    const runButton = page.getByText(/运行|Run/)
    await runButton.click()
    
    // 等待执行完成
    await expect(page.getByText(/完成|Success/)).toBeVisible({ timeout: 30000 })
    
    // 点击查看结果
    const resultButton = page.getByText(/查看结果|结果/)
    await resultButton.click()
    
    // 验证结果显示
    await expect(page.getByTestId('result-preview')).toBeVisible()
  })

  test('应处理执行失败', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点|处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 配置错误的脚本路径
    await page.getByTestId('node-python').click()
    const scriptInput = page.getByLabel(/脚本路径/)
    await scriptInput.fill('/invalid/path/script.py')
    await page.getByText(/保存/).click()
    
    // 运行
    const runButton = page.getByText(/运行|Run/)
    await runButton.click()
    
    // 验证失败状态
    await expect(page.getByText(/失败|Failed|Error/)).toBeVisible({ timeout: 30000 })
  })

  test('应支持停止执行', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点|处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 运行
    const runButton = page.getByText(/运行|Run/)
    await runButton.click()
    
    // 停止执行
    const stopButton = page.getByText(/停止|Stop/)
    await stopButton.click()
    
    // 验证已停止
    await expect(page.getByText(/已停止|Stopped/)).toBeVisible()
  })

  test('应显示执行日志', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点|处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 运行
    const runButton = page.getByText(/运行|Run/)
    await runButton.click()
    
    // 查看日志
    const logButton = page.getByText(/日志|Log/)
    await logButton.click()
    
    // 验证日志显示
    await expect(page.getByTestId('execution-log')).toBeVisible()
  })

  test('应支持重新执行', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点|处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 第一次运行
    const runButton = page.getByText(/运行|Run/)
    await runButton.click()
    await expect(page.getByText(/完成|Success/)).toBeVisible({ timeout: 30000 })
    
    // 重新执行
    const rerunButton = page.getByText(/重新执行|Rerun/)
    await rerunButton.click()
    
    // 验证重新执行
    await expect(page.getByText(/运行中|Running/)).toBeVisible()
  })

  test('应显示执行历史记录', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点|处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 运行
    const runButton = page.getByText(/运行|Run/)
    await runButton.click()
    await expect(page.getByText(/完成|Success/)).toBeVisible({ timeout: 30000 })
    
    // 查看历史
    const historyButton = page.getByText(/历史|History/)
    await historyButton.click()
    
    // 验证历史记录
    await expect(page.getByTestId('execution-history')).toBeVisible()
  })

  test('应支持并行执行多个节点', async ({ page }) => {
    // 添加多个节点
    const pythonNode = page.getByText(/处理节点|处理|Process/)
    const canvas = page.getByTestId('canvas')
    
    await pythonNode.dragTo(canvas, { targetPosition: { x: 100, y: 100 } })
    await pythonNode.dragTo(canvas, { targetPosition: { x: 300, y: 100 } })
    await pythonNode.dragTo(canvas, { targetPosition: { x: 500, y: 100 } })
    
    // 运行
    const runButton = page.getByText(/运行|Run/)
    await runButton.click()
    
    // 验证并行执行
    const runningNodes = page.locator('[data-status="RUNNING"]')
    await expect(runningNodes).toHaveCount({ min: 2 })
  })

  test('应显示执行进度', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点|处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 运行
    const runButton = page.getByText(/运行|Run/)
    await runButton.click()
    
    // 验证进度显示
    await expect(page.getByTestId('execution-progress')).toBeVisible()
  })

  test('应支持导出执行结果', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点|处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 运行
    const runButton = page.getByText(/运行|Run/)
    await runButton.click()
    await expect(page.getByText(/完成|Success/)).toBeVisible({ timeout: 30000 })
    
    // 导出结果
    const exportButton = page.getByText(/导出|Export/)
    await exportButton.click()
    
    // 验证导出选项
    await expect(page.getByText(/导出格式|Export Format/)).toBeVisible()
  })
})
