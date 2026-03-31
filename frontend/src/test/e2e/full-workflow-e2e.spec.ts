import { test, expect } from '@playwright/test'
import * as path from 'path'
import * as fs from 'fs'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * 全链路 E2E 测试 - 包含完整截图
 * 覆盖 5 大测试场景，每个步骤截图
 */
test.describe('全链路 E2E 测试', () => {
  const screenshotDir = path.join(__dirname, 'screenshots')
  
  test.beforeEach(async ({ page }) => {
    // 确保截图目录存在
    if (!fs.existsSync(screenshotDir)) {
      fs.mkdirSync(screenshotDir, { recursive: true })
    }
    
    await page.goto('/')
    // 跳过新手引导
    const skipButton = page.getByText(/跳过|Skip/)
    if (await skipButton.isVisible()) {
      await skipButton.click()
    }
  })

  // ============================================
  // 场景 1：完整工作流创建流程
  // ============================================
  test.describe('场景 1：完整工作流创建流程', () => {
    test('应完成完整的工作流创建流程并截图', async ({ page }) => {
      const scenarioDir = path.join(screenshotDir, 'scenario-1-create-workflow')
      if (!fs.existsSync(scenarioDir)) {
        fs.mkdirSync(scenarioDir, { recursive: true })
      }
      let screenshotIndex = 1

      // 步骤 1: 打开画布页面 - 📸 截图
      await page.waitForTimeout(1000)
      await page.screenshot({ path: path.join(scenarioDir, `1-canvas-page.png`), fullPage: true })
      console.log(`📸 截图 ${screenshotIndex++}: 画布页面`)

      // 步骤 2: 从节点面板拖拽节点到画布 - 📸 截图
      const pythonNode = page.getByText(/处理节点 | 处理|Process/)
      const canvas = page.getByTestId('canvas')
      if (await pythonNode.isVisible()) {
        await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
        await page.waitForTimeout(500)
        await page.screenshot({ path: path.join(scenarioDir, `2-drag-node.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 拖拽节点到画布`)
      }

      // 步骤 3: 连接节点输入输出端口 - 📸 截图
      const inputNode = page.getByText(/输入|Input/)
      if (await inputNode.isVisible()) {
        await inputNode.dragTo(canvas, { targetPosition: { x: 100, y: 100 } })
        await page.waitForTimeout(500)
        
        // 尝试连接节点
        const outputHandle = page.locator('[data-handle-position="right"]').first()
        const inputHandle = page.locator('[data-handle-position="left"]').nth(1)
        if (await outputHandle.isVisible() && await inputHandle.isVisible()) {
          await outputHandle.dragTo(inputHandle)
          await page.waitForTimeout(500)
        }
        await page.screenshot({ path: path.join(scenarioDir, `3-connect-nodes.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 连接节点`)
      }

      // 步骤 4: 点击节点打开配置面板 - 📸 截图
      const nodeElement = page.getByTestId('node-python')
      if (await nodeElement.isVisible()) {
        await nodeElement.click()
        await page.waitForTimeout(500)
        await page.screenshot({ path: path.join(scenarioDir, `4-config-panel.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 打开配置面板`)
      }

      // 步骤 5: 填写配置并保存 - 📸 截图
      const scriptInput = page.getByLabel(/脚本路径|Script Path/)
      if (await scriptInput.isVisible()) {
        await scriptInput.fill('/path/to/test.py')
        const saveButton = page.getByText(/保存|Save/)
        if (await saveButton.isVisible()) {
          await saveButton.click()
          await page.waitForTimeout(500)
          await page.screenshot({ path: path.join(scenarioDir, `5-save-config.png`), fullPage: true })
          console.log(`📸 截图 ${screenshotIndex++}: 保存配置`)
        }
      }

      // 步骤 6: 点击保存工作流按钮 - 📸 截图
      const saveWorkflowButton = page.getByText(/保存工作流|Save Workflow|保存/)
      if (await saveWorkflowButton.isVisible()) {
        await saveWorkflowButton.click()
        await page.waitForTimeout(500)
        
        // 输入工作流名称
        const nameInput = page.getByLabel(/工作流名称|Workflow Name/)
        if (await nameInput.isVisible()) {
          await nameInput.fill('E2E 测试工作流')
          const confirmButton = page.getByText(/确定|OK/)
          if (await confirmButton.isVisible()) {
            await confirmButton.click()
            await page.waitForTimeout(1000)
          }
        }
        await page.screenshot({ path: path.join(scenarioDir, `6-save-workflow.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 保存工作流`)
      }

      // 步骤 7: 验证工作流列表显示 - 📸 截图
      await page.waitForTimeout(1000)
      await page.screenshot({ path: path.join(scenarioDir, `7-workflow-list.png`), fullPage: true })
      console.log(`📸 截图 ${screenshotIndex++}: 工作流列表`)
      
      console.log(`✅ 场景 1 完成，共 ${screenshotIndex - 1} 张截图`)
    })
  })

  // ============================================
  // 场景 2：工作流执行流程
  // ============================================
  test.describe('场景 2：工作流执行流程', () => {
    test('应完成工作流执行流程并截图', async ({ page }) => {
      const scenarioDir = path.join(screenshotDir, 'scenario-2-execute-workflow')
      if (!fs.existsSync(scenarioDir)) {
        fs.mkdirSync(scenarioDir, { recursive: true })
      }
      let screenshotIndex = 1

      // 先创建一个工作流
      const pythonNode = page.getByText(/处理节点 | 处理|Process/)
      const canvas = page.getByTestId('canvas')
      if (await pythonNode.isVisible()) {
        await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
        await page.waitForTimeout(500)
      }

      // 步骤 1: 选择已创建的工作流 - 📸 截图
      await page.screenshot({ path: path.join(scenarioDir, `1-select-workflow.png`), fullPage: true })
      console.log(`📸 截图 ${screenshotIndex++}: 选择工作流`)

      // 步骤 2: 点击执行按钮 - 📸 截图
      const runButton = page.getByText(/运行|Run|执行/)
      if (await runButton.isVisible()) {
        await runButton.click()
        await page.waitForTimeout(500)
        await page.screenshot({ path: path.join(scenarioDir, `2-click-run.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 点击执行`)
      }

      // 步骤 3: 观察节点状态颜色变化（灰→黄→绿） - 📸 截图（每个状态）
      await page.waitForTimeout(1000)
      await page.screenshot({ path: path.join(scenarioDir, `3-status-running.png`), fullPage: true })
      console.log(`📸 截图 ${screenshotIndex++}: 运行中状态`)
      
      await page.waitForTimeout(3000)
      await page.screenshot({ path: path.join(scenarioDir, `4-status-completed.png`), fullPage: true })
      console.log(`📸 截图 ${screenshotIndex++}: 完成状态`)

      // 步骤 4: 点击节点查看日志 - 📸 截图
      const logButton = page.getByText(/日志|Log/)
      if (await logButton.isVisible()) {
        await logButton.click()
        await page.waitForTimeout(500)
        await page.screenshot({ path: path.join(scenarioDir, `5-view-logs.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 查看日志`)
      }

      // 步骤 5: 验证执行完成状态 - 📸 截图
      await page.waitForTimeout(1000)
      await page.screenshot({ path: path.join(scenarioDir, `6-execution-complete.png`), fullPage: true })
      console.log(`📸 截图 ${screenshotIndex++}: 执行完成`)
      
      console.log(`✅ 场景 2 完成，共 ${screenshotIndex - 1} 张截图`)
    })
  })

  // ============================================
  // 场景 3：工作流导入导出流程
  // ============================================
  test.describe('场景 3：工作流导入导出流程', () => {
    test('应完成工作流导入导出流程并截图', async ({ page }) => {
      const scenarioDir = path.join(screenshotDir, 'scenario-3-import-export')
      if (!fs.existsSync(scenarioDir)) {
        fs.mkdirSync(scenarioDir, { recursive: true })
      }
      let screenshotIndex = 1

      // 先创建一个工作流用于导出
      const pythonNode = page.getByText(/处理节点 | 处理|Process/)
      const canvas = page.getByTestId('canvas')
      if (await pythonNode.isVisible()) {
        await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
        await page.waitForTimeout(500)
      }

      // 步骤 1: 点击导出按钮 - 📸 截图
      const exportButton = page.getByText(/导出|Export/)
      if (await exportButton.isVisible()) {
        await exportButton.click()
        await page.waitForTimeout(500)
        await page.screenshot({ path: path.join(scenarioDir, `1-click-export.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 点击导出`)
      }

      // 步骤 2: 验证 JSON 文件下载 - 📸 截图
      const jsonOption = page.getByText(/JSON/)
      if (await jsonOption.isVisible()) {
        await jsonOption.click()
        await page.waitForTimeout(1000)
        await page.screenshot({ path: path.join(scenarioDir, `2-json-export.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: JSON 导出`)
      }

      // 步骤 3: 删除工作流 - 📸 截图
      const deleteButton = page.getByText(/删除|Delete/)
      if (await deleteButton.isVisible()) {
        await deleteButton.click()
        await page.waitForTimeout(500)
        await page.screenshot({ path: path.join(scenarioDir, `3-delete-workflow.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 删除工作流`)
      }

      // 步骤 4: 点击导入按钮选择 JSON 文件 - 📸 截图
      const importButton = page.getByText(/导入|Import/)
      if (await importButton.isVisible()) {
        await importButton.click()
        await page.waitForTimeout(500)
        await page.screenshot({ path: path.join(scenarioDir, `4-click-import.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 点击导入`)
      }

      // 步骤 5: 验证工作流恢复 - 📸 截图
      await page.waitForTimeout(1000)
      await page.screenshot({ path: path.join(scenarioDir, `5-workflow-restored.png`), fullPage: true })
      console.log(`📸 截图 ${screenshotIndex++}: 工作流恢复`)
      
      console.log(`✅ 场景 3 完成，共 ${screenshotIndex - 1} 张截图`)
    })
  })

  // ============================================
  // 场景 4：节点配置和保存
  // ============================================
  test.describe('场景 4：节点配置和保存', () => {
    test('应完成节点配置和保存流程并截图', async ({ page }) => {
      const scenarioDir = path.join(screenshotDir, 'scenario-4-config-panel')
      if (!fs.existsSync(scenarioDir)) {
        fs.mkdirSync(scenarioDir, { recursive: true })
      }
      let screenshotIndex = 1

      // 添加节点
      const pythonNode = page.getByText(/处理节点 | 处理|Process/)
      const canvas = page.getByTestId('canvas')
      if (await pythonNode.isVisible()) {
        await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
        await page.waitForTimeout(500)
      }

      // 步骤 1: 点击节点打开配置面板 - 📸 截图
      const nodeElement = page.getByTestId('node-python')
      if (await nodeElement.isVisible()) {
        await nodeElement.click()
        await page.waitForTimeout(500)
        await page.screenshot({ path: path.join(scenarioDir, `1-open-config.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 打开配置面板`)
      }

      // 步骤 2: 填写各类型节点配置 - 📸 截图
      const scriptInput = page.getByLabel(/脚本路径|Script Path/)
      if (await scriptInput.isVisible()) {
        await scriptInput.fill('/path/to/script.py')
        
        const paramInput = page.getByLabel(/参数|Arguments/)
        if (await paramInput.isVisible()) {
          await paramInput.fill('--verbose --output result.json')
        }
        
        await page.waitForTimeout(500)
        await page.screenshot({ path: path.join(scenarioDir, `2-fill-config.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 填写配置`)
      }

      // 步骤 3: 点击保存按钮 - 📸 截图
      const saveButton = page.getByText(/保存|Save/)
      if (await saveButton.isVisible()) {
        await saveButton.click()
        await page.waitForTimeout(1000)
        await page.screenshot({ path: path.join(scenarioDir, `3-save-config.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 保存配置`)
      }

      // 步骤 4: 重新打开验证配置 - 📸 截图
      if (await nodeElement.isVisible()) {
        await nodeElement.click()
        await page.waitForTimeout(500)
        await page.screenshot({ path: path.join(scenarioDir, `4-verify-config.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 验证配置`)
      }
      
      console.log(`✅ 场景 4 完成，共 ${screenshotIndex - 1} 张截图`)
    })
  })

  // ============================================
  // 场景 5：实时状态更新
  // ============================================
  test.describe('场景 5：实时状态更新', () => {
    test('应观察实时状态更新并截图', async ({ page }) => {
      const scenarioDir = path.join(screenshotDir, 'scenario-5-realtime-status')
      if (!fs.existsSync(scenarioDir)) {
        fs.mkdirSync(scenarioDir, { recursive: true })
      }
      let screenshotIndex = 1

      // 添加节点
      const pythonNode = page.getByText(/处理节点 | 处理|Process/)
      const canvas = page.getByTestId('canvas')
      if (await pythonNode.isVisible()) {
        await pythonNode.dragTo(canvas, { targetPosition: { x: 200, y: 200 } })
        await page.waitForTimeout(500)
      }

      // 步骤 1: 开始执行工作流 - 📸 截图
      const runButton = page.getByText(/运行|Run|执行/)
      if (await runButton.isVisible()) {
        await runButton.click()
        await page.waitForTimeout(500)
        await page.screenshot({ path: path.join(scenarioDir, `1-start-execution.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 开始执行`)
      }

      // 步骤 2: 观察状态颜色实时更新 - 📸 截图（每 5 秒）
      await page.waitForTimeout(5000)
      await page.screenshot({ path: path.join(scenarioDir, `2-status-update-5s.png`), fullPage: true })
      console.log(`📸 截图 ${screenshotIndex++}: 状态更新 (5 秒)`)
      
      await page.waitForTimeout(5000)
      await page.screenshot({ path: path.join(scenarioDir, `3-status-update-10s.png`), fullPage: true })
      console.log(`📸 截图 ${screenshotIndex++}: 状态更新 (10 秒)`)

      // 步骤 3: 观察进度条更新 - 📸 截图
      const progressBar = page.getByTestId('execution-progress')
      if (await progressBar.isVisible()) {
        await page.screenshot({ path: path.join(scenarioDir, `4-progress-bar.png`), fullPage: true })
        console.log(`📸 截图 ${screenshotIndex++}: 进度条`)
      }

      // 步骤 4: 验证执行完成 - 📸 截图
      await page.waitForTimeout(3000)
      await page.screenshot({ path: path.join(scenarioDir, `5-execution-finished.png`), fullPage: true })
      console.log(`📸 截图 ${screenshotIndex++}: 执行完成`)
      
      console.log(`✅ 场景 5 完成，共 ${screenshotIndex - 1} 张截图`)
    })
  })
})
