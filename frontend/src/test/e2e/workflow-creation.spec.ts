import { test, expect } from '@playwright/test'

/**
 * E2E 测试场景 1: 创建工作流
 * 覆盖功能：F001, F002, F011-F015, F024-F027
 */
test.describe('工作流创建', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    // 跳过新手引导
    const skipButton = page.getByText(/跳过|Skip/)
    if (await skipButton.isVisible()) {
      await skipButton.click()
    }
  })

  test('应从节点面板拖拽创建节点', async ({ page }) => {
    // 从节点面板拖拽 Python 节点到画布
    const pythonNode = page.getByText(/Python 脚本/)
    const canvas = page.getByTestId('canvas')
    
    await pythonNode.dragTo(canvas, {
      targetPosition: { x: 200, y: 200 }
    })
    
    // 验证节点已创建
    await expect(page.getByTestId('node-python')).toBeVisible()
  })

  test('应连接多个节点创建工作流', async ({ page }) => {
    // 添加输入节点
    const inputNode = page.getByText(/输入|Input/)
    const canvas = page.getByTestId('canvas')
    await inputNode.dragTo(canvas, { targetPosition: { x: 100, y: 100 } })
    
    // 添加处理节点
    const processNode = page.getByText(/处理|Process|Python/)
    await processNode.dragTo(canvas, { targetPosition: { x: 300, y: 100 } })
    
    // 添加输出节点
    const outputNode = page.getByText(/输出|Output/)
    await outputNode.dragTo(canvas, { targetPosition: { x: 500, y: 100 } })
    
    // 连接节点
    const outputHandle = page.locator('[data-handle-position="right"]').first()
    const inputHandle = page.locator('[data-handle-position="left"]').nth(1)
    
    await outputHandle.dragTo(inputHandle)
    
    // 验证连线已创建
    await expect(page.locator('[class*="edge"]')).toHaveCount({ min: 1 })
  })

  test('应配置节点参数', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/Python 脚本/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 点击节点打开配置面板
    await page.getByTestId('node-python').click()
    
    // 配置脚本路径
    const scriptInput = page.getByLabel(/脚本路径/)
    await scriptInput.fill('/path/to/script.py')
    
    // 配置参数
    const paramInput = page.getByLabel(/参数/)
    await paramInput.fill('--verbose')
    
    // 保存配置
    const saveButton = page.getByText(/保存|Save/)
    await saveButton.click()
    
    // 验证配置已保存
    await expect(page.getByText(/保存成功/)).toBeVisible()
  })

  test('应删除节点和连线', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/Python 脚本/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 选中并删除
    await page.getByTestId('node-python').click()
    await page.keyboard.press('Delete')
    
    // 验证节点已删除
    await expect(page.getByTestId('node-python')).not.toBeVisible()
  })

  test('应保存工作流', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/Python 脚本/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 保存工作流
    const saveButton = page.getByText(/保存|Save/)
    await saveButton.click()
    
    // 输入工作流名称
    const nameInput = page.getByLabel(/工作流名称/)
    await nameInput.fill('测试工作流')
    
    // 确认保存
    const confirmButton = page.getByText(/确定|OK/)
    await confirmButton.click()
    
    // 验证保存成功
    await expect(page.getByText(/保存成功/)).toBeVisible()
  })

  test('应支持撤销重做', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/Python 脚本/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 撤销
    await page.keyboard.press('ControlOrMeta+Z')
    
    // 验证节点已撤销
    await expect(page.getByTestId('node-python')).not.toBeVisible()
    
    // 重做
    await page.keyboard.press('ControlOrMeta+Shift+Z')
    
    // 验证节点已恢复
    await expect(page.getByTestId('node-python')).toBeVisible()
  })
})
