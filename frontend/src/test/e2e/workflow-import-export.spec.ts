import { test, expect } from '@playwright/test'
import * as path from 'path'

/**
 * E2E 测试场景 3: 导入导出工作流
 * 覆盖功能：F035-F038, F041-F042
 */
test.describe('工作流导入导出', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    // 跳过新手引导
    const skipButton = page.getByText(/跳过|Skip/)
    if (await skipButton.isVisible()) {
      await skipButton.click()
    }
  })

  test('应导出工作流为 JSON', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点 | 处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 导出
    const exportButton = page.getByText(/导出|Export/)
    await exportButton.click()
    
    // 选择 JSON 格式
    const jsonOption = page.getByText(/JSON/)
    await jsonOption.click()
    
    // 确认导出
    const confirmButton = page.getByText(/确定|OK/)
    await confirmButton.click()
    
    // 验证导出提示
    await expect(page.getByText(/导出成功/)).toBeVisible()
  })

  test('应导入工作流 JSON 文件', async ({ page }) => {
    // 导入
    const importButton = page.getByText(/导入|Import/)
    await importButton.click()
    
    // 上传文件
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles(path.join(__dirname, 'fixtures', 'test-workflow.json'))
    
    // 验证导入成功
    await expect(page.getByText(/导入成功/)).toBeVisible()
    await expect(page.getByTestId('canvas')).toBeVisible()
  })

  test('应支持导出为图片', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点 | 处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 导出
    const exportButton = page.getByText(/导出|Export/)
    await exportButton.click()
    
    // 选择图片格式
    const pngOption = page.getByText(/PNG|图片/)
    await pngOption.click()
    
    // 确认导出
    const confirmButton = page.getByText(/确定|OK/)
    await confirmButton.click()
    
    // 验证导出提示
    await expect(page.getByText(/导出成功/)).toBeVisible()
  })

  test('应支持导出为 PDF', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点 | 处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 导出
    const exportButton = page.getByText(/导出|Export/)
    await exportButton.click()
    
    // 选择 PDF 格式
    const pdfOption = page.getByText(/PDF/)
    await pdfOption.click()
    
    // 确认导出
    const confirmButton = page.getByText(/确定|OK/)
    await confirmButton.click()
    
    // 验证导出提示
    await expect(page.getByText(/导出成功/)).toBeVisible()
  })

  test('应验证导入文件格式', async ({ page }) => {
    // 导入
    const importButton = page.getByText(/导入|Import/)
    await importButton.click()
    
    // 上传无效文件
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles(path.join(__dirname, 'fixtures', 'invalid.txt'))
    
    // 验证错误提示
    await expect(page.getByText(/无效文件格式/)).toBeVisible()
  })

  test('应支持分享工作流链接', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点 | 处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 保存工作流
    const saveButton = page.getByText(/保存|Save/)
    await saveButton.click()
    await page.getByLabel(/工作流名称/).fill('测试工作流')
    await page.getByText(/确定|OK/).click()
    
    // 分享
    const shareButton = page.getByText(/分享|Share/)
    await shareButton.click()
    
    // 验证分享链接
    await expect(page.getByTestId('share-link')).toBeVisible()
  })

  test('应支持从模板创建工作流', async ({ page }) => {
    // 打开模板库
    const templateButton = page.getByText(/模板|Template/)
    await templateButton.click()
    
    // 选择模板
    const template = page.getByText(/示例|Example/).first()
    await template.click()
    
    // 应用模板
    const applyButton = page.getByText(/应用|Apply/)
    await applyButton.click()
    
    // 验证模板已加载
    await expect(page.getByTestId('canvas')).toBeVisible()
  })

  test('应自动保存工作流', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点 | 处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 等待自动保存
    await page.waitForTimeout(5000)
    
    // 验证自动保存提示
    await expect(page.getByText(/已自动保存|Auto-saved/)).toBeVisible()
  })

  test('应显示保存状态', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点 | 处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 验证保存状态显示
    await expect(page.getByTestId('save-status')).toBeVisible()
  })

  test('应支持版本历史', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点 | 处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 保存
    const saveButton = page.getByText(/保存|Save/)
    await saveButton.click()
    await page.getByLabel(/工作流名称/).fill('测试工作流')
    await page.getByText(/确定|OK/).click()
    
    // 修改
    await pythonNode.dragTo(canvas, { targetPosition: { x: 300, y: 300 } })
    await saveButton.click()
    
    // 查看历史
    const historyButton = page.getByText(/历史|History/)
    await historyButton.click()
    
    // 验证版本历史
    await expect(page.getByTestId('version-history')).toBeVisible()
  })

  test('应支持恢复历史版本', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点 | 处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 保存
    const saveButton = page.getByText(/保存|Save/)
    await saveButton.click()
    await page.getByLabel(/工作流名称/).fill('测试工作流')
    await page.getByText(/确定|OK/).click()
    
    // 修改
    await pythonNode.dragTo(canvas, { targetPosition: { x: 300, y: 300 } })
    await saveButton.click()
    
    // 查看历史
    const historyButton = page.getByText(/历史|History/)
    await historyButton.click()
    
    // 恢复旧版本
    const restoreButton = page.getByText(/恢复|Restore/).first()
    await restoreButton.click()
    
    // 验证恢复
    await expect(page.getByText(/恢复成功/)).toBeVisible()
  })

  test('应支持批量导出', async ({ page }) => {
    // 添加多个节点
    const pythonNode = page.getByText(/处理节点 | 处理|Process/)
    const canvas = page.getByTestId('canvas')
    
    await pythonNode.dragTo(canvas, { targetPosition: { x: 100, y: 100 } })
    await pythonNode.dragTo(canvas, { targetPosition: { x: 300, y: 100 } })
    
    // 导出
    const exportButton = page.getByText(/导出|Export/)
    await exportButton.click()
    
    // 选择批量导出
    const batchOption = page.getByText(/批量导出|Batch Export/)
    await batchOption.click()
    
    // 验证批量导出选项
    await expect(page.getByText(/选择导出内容/)).toBeVisible()
  })

  test('应支持导入校验', async ({ page }) => {
    // 导入
    const importButton = page.getByText(/导入|Import/)
    await importButton.click()
    
    // 上传空文件
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles(path.join(__dirname, 'fixtures', 'empty.json'))
    
    // 验证错误提示
    await expect(page.getByText(/文件为空或格式错误/)).toBeVisible()
  })

  test('应支持导出预览', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点 | 处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 导出
    const exportButton = page.getByText(/导出|Export/)
    await exportButton.click()
    
    // 验证预览
    await expect(page.getByTestId('export-preview')).toBeVisible()
  })

  test('应支持自定义导出选项', async ({ page }) => {
    // 添加节点
    const pythonNode = page.getByText(/处理节点 | 处理|Process/)
    const canvas = page.getByTestId('canvas')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
    
    // 导出
    const exportButton = page.getByText(/导出|Export/)
    await exportButton.click()
    
    // 自定义选项
    const includeConfig = page.getByText(/包含配置/)
    await includeConfig.click()
    
    const includeResults = page.getByText(/包含结果/)
    await includeResults.click()
    
    // 确认导出
    const confirmButton = page.getByText(/确定|OK/)
    await confirmButton.click()
    
    // 验证导出
    await expect(page.getByText(/导出成功/)).toBeVisible()
  })
})
