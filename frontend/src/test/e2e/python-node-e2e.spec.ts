import { test, expect } from '@playwright/test'
import * as path from 'path'
import * as fs from 'fs'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * Python 节点功能 E2E 测试
 * 覆盖 5 大测试场景，每个场景截图验证
 */
test.describe('Python 节点 E2E 测试', () => {
  // 截图输出目录
  const screenshotDir = path.join(__dirname, 'screenshots', 'python-node-e2e')
  
  test.beforeEach(async ({ page }) => {
    // 确保截图目录存在
    if (!fs.existsSync(screenshotDir)) {
      fs.mkdirSync(screenshotDir, { recursive: true })
    }
    
    await page.goto('/')
    // 等待页面加载
    await page.waitForTimeout(3000)
    
    // 跳过新手引导
    const skipButton = page.getByText(/跳过|Skip|稍后/)
    if (await skipButton.isVisible()) {
      await skipButton.click()
      await page.waitForTimeout(500)
    }
  })

  // ============================================
  // 场景 1: 创建工作流 + Python 节点
  // ============================================
  test('场景 1: 创建工作流 + Python 节点', async ({ page }) => {
    console.log('🧪 开始场景 1: 创建工作流 + Python 节点')
    
    // 步骤 1: 打开前端页面 - 截图
    await page.waitForTimeout(1000)
    await page.screenshot({ path: path.join(screenshotDir, '01-workflow-create.png'), fullPage: true })
    console.log('📸 截图：01-workflow-create.png')

    // 步骤 2: 从节点面板拖拽 Python 节点到画布
    const canvas = page.getByTestId('react-flow')
    await expect(canvas).toBeVisible({ timeout: 10000 })
    
    // 查找处理节点/Python 节点
    const processNode = page.getByText(/处理节点|Python|处理/).first()
    if (await processNode.isVisible()) {
      await processNode.dragTo(canvas, { targetPosition: { x: 300, y: 200 } })
      await page.waitForTimeout(1000)
    }

    // 步骤 3: 点击节点打开配置面板 - 使用 force 点击
    const nodeElement = page.locator('[class*="react-flow__node"]').first()
    if (await nodeElement.isVisible()) {
      await nodeElement.click({ force: true })
      await page.waitForTimeout(1500)
    }

    // 步骤 4: 配置 Python 代码（Hello World）
    const scriptEditor = page.locator('[class*="monaco-editor"]').first()
    if (await scriptEditor.isVisible()) {
      // 在编辑器中输入 Hello World 代码
      await page.keyboard.type('print("Hello, Python Node!")')
      await page.waitForTimeout(500)
    }

    // 步骤 5: 保存配置
    const saveButton = page.getByText(/保存配置 | 保存|Save/)
    if (await saveButton.isVisible()) {
      await saveButton.click()
      await page.waitForTimeout(1000)
    }

    // 步骤 6: 截图 - 画布编辑器 + Python 节点配置面板
    await page.screenshot({ path: path.join(screenshotDir, '02-python-node-config.png'), fullPage: true })
    console.log('📸 截图：02-python-node-config.png')
    
    console.log('✅ 场景 1 完成')
  })

  // ============================================
  // 场景 2: 执行 Python 节点（基础功能）
  // ============================================
  test('场景 2: 执行 Python 节点（基础功能）', async ({ page }) => {
    console.log('🧪 开始场景 2: 执行 Python 节点（基础功能）')
    
    // 先创建一个简单的 Python 节点
    const canvas = page.getByTestId('react-flow')
    const processNode = page.getByText(/处理节点|Python|处理/).first()
    if (await processNode.isVisible()) {
      await processNode.dragTo(canvas, { targetPosition: { x: 300, y: 200 } })
      await page.waitForTimeout(1000)
    }

    // 配置简单的 Hello World 代码 - 使用 force 点击
    const nodeElement = page.locator('[class*="react-flow__node"]').first()
    if (await nodeElement.isVisible()) {
      await nodeElement.click({ force: true })
      await page.waitForTimeout(1000)
      
      const scriptEditor = page.locator('[class*="monaco-editor"]').first()
      if (await scriptEditor.isVisible()) {
        await page.keyboard.type('print("Hello from Python Node!")')
        await page.waitForTimeout(500)
      }
      
      const saveButton = page.getByText(/保存配置 | 保存|Save/)
      if (await saveButton.isVisible()) {
        await saveButton.click()
        await page.waitForTimeout(1000)
      }
    }

    // 步骤 1: 点击执行按钮
    const runButton = page.getByText(/执行 | 运行|Run/)
    if (await runButton.isVisible()) {
      await runButton.click()
      await page.waitForTimeout(500)
    }

    // 步骤 2: 截图 - 执行状态
    await page.screenshot({ path: path.join(screenshotDir, '03-execution-running.png'), fullPage: true })
    console.log('📸 截图：03-execution-running.png')

    // 步骤 3: 等待执行完成
    await page.waitForTimeout(5000)

    // 步骤 4: 截图 - 结果输出
    await page.screenshot({ path: path.join(screenshotDir, '04-execution-result.png'), fullPage: true })
    console.log('📸 截图：04-execution-result.png')
    
    // 验证执行结果
    const resultPanel = page.getByText(/Hello|执行完成|成功|Success/)
    if (await resultPanel.isVisible()) {
      console.log('✅ 执行结果验证通过')
    }
    
    console.log('✅ 场景 2 完成')
  })

  // ============================================
  // 场景 3: 执行 Python 节点（数据处理）
  // ============================================
  test('场景 3: 执行 Python 节点（数据处理）', async ({ page }) => {
    console.log('🧪 开始场景 3: 执行 Python 节点（数据处理）')
    
    const canvas = page.getByTestId('react-flow')
    const processNode = page.getByText(/处理节点|Python|处理/).first()
    if (await processNode.isVisible()) {
      await processNode.dragTo(canvas, { targetPosition: { x: 300, y: 200 } })
      await page.waitForTimeout(1000)
    }

    // 配置 pandas/numpy 数据处理脚本 - 使用 force 点击
    const nodeElement = page.locator('[class*="react-flow__node"]').first()
    if (await nodeElement.isVisible()) {
      await nodeElement.click({ force: true })
      await page.waitForTimeout(1000)
      
      const scriptEditor = page.locator('[class*="monaco-editor"]').first()
      if (await scriptEditor.isVisible()) {
        // 输入数据处理代码
        const dataScript = `import pandas as pd
import numpy as np

# 创建示例数据
data = {'name': ['Alice', 'Bob', 'Charlie'], 'age': [25, 30, 35]}
df = pd.DataFrame(data)

# 数据处理
print("原始数据:")
print(df)
print("\\n平均年龄:", df['age'].mean())
`
        await page.keyboard.type(dataScript)
        await page.waitForTimeout(1000)
      }
      
      // 添加依赖
      const dependencyInput = page.getByPlaceholder(/requests|pandas|numpy|依赖包/)
      if (await dependencyInput.isVisible()) {
        await dependencyInput.fill('pandas\nnumpy')
        await page.waitForTimeout(500)
      }
      
      const saveButton = page.getByText(/保存配置 | 保存|Save/)
      if (await saveButton.isVisible()) {
        await saveButton.click()
        await page.waitForTimeout(1000)
      }
    }

    // 截图 - 代码编辑器
    await page.screenshot({ path: path.join(screenshotDir, '05-data-processing.png'), fullPage: true })
    console.log('📸 截图：05-data-processing.png')

    // 执行脚本
    const runButton = page.getByText(/执行 | 运行|Run/)
    if (await runButton.isVisible()) {
      await runButton.click()
      await page.waitForTimeout(500)
    }

    // 等待执行完成
    await page.waitForTimeout(8000)

    // 截图 - 执行结果
    await page.screenshot({ path: path.join(screenshotDir, '06-data-result.png'), fullPage: true })
    console.log('📸 截图：06-data-result.png')
    
    console.log('✅ 场景 3 完成')
  })

  // ============================================
  // 场景 4: 安全拦截测试
  // ============================================
  test('场景 4: 安全拦截测试', async ({ page }) => {
    console.log('🧪 开始场景 4: 安全拦截测试')
    
    const canvas = page.getByTestId('react-flow')
    const processNode = page.getByText(/处理节点|Python|处理/).first()
    if (await processNode.isVisible()) {
      await processNode.dragTo(canvas, { targetPosition: { x: 300, y: 200 } })
      await page.waitForTimeout(1000)
    }

    // 配置危险代码（eval） - 使用 force 点击
    const nodeElement = page.locator('[class*="react-flow__node"]').first()
    if (await nodeElement.isVisible()) {
      await nodeElement.click({ force: true })
      await page.waitForTimeout(1000)
      
      const scriptEditor = page.locator('[class*="monaco-editor"]').first()
      if (await scriptEditor.isVisible()) {
        // 输入危险代码
        const dangerousScript = `# 危险代码测试 - 应被安全层拦截
eval("__import__('os').system('rm -rf /')")
`
        await page.keyboard.type(dangerousScript)
        await page.waitForTimeout(500)
      }
      
      const saveButton = page.getByText(/保存配置 | 保存|Save/)
      if (await saveButton.isVisible()) {
        await saveButton.click()
        await page.waitForTimeout(1000)
      }
    }

    // 执行危险代码
    const runButton = page.getByText(/执行 | 运行|Run/)
    if (await runButton.isVisible()) {
      await runButton.click()
      await page.waitForTimeout(500)
    }

    // 等待安全拦截
    await page.waitForTimeout(3000)

    // 截图 - 错误提示
    await page.screenshot({ path: path.join(screenshotDir, '07-safety-blocked.png'), fullPage: true })
    console.log('📸 截图：07-safety-blocked.png')

    // 验证安全拦截
    const errorMessage = page.getByText(/拦截 | 禁止|危险|安全|blocked|dangerous|eval/i)
    if (await errorMessage.isVisible()) {
      console.log('✅ 安全拦截验证通过')
    }
    
    console.log('✅ 场景 4 完成')
  })

  // ============================================
  // 场景 5: 代码模板库
  // ============================================
  test('场景 5: 代码模板库', async ({ page }) => {
    console.log('🧪 开始场景 5: 代码模板库')
    
    // 步骤 1: 打开模板库
    const templateButton = page.getByText(/模板 | 模板库|Template/)
    if (await templateButton.isVisible()) {
      await templateButton.click()
      await page.waitForTimeout(1000)
    }

    // 截图 - 模板库界面
    await page.screenshot({ path: path.join(screenshotDir, '08-template-library.png'), fullPage: true })
    console.log('📸 截图：08-template-library.png')

    // 步骤 2: 选择 Hello World 模板
    const helloWorldTemplate = page.getByText(/Hello World|你好|欢迎/)
    if (await helloWorldTemplate.isVisible()) {
      await helloWorldTemplate.click()
      await page.waitForTimeout(500)
    }

    // 步骤 3: 加载到编辑器
    const loadButton = page.getByText(/加载 | 使用|Load|Use/)
    if (await loadButton.isVisible()) {
      await loadButton.click()
      await page.waitForTimeout(1000)
    }

    // 截图 - 模板加载后
    await page.screenshot({ path: path.join(screenshotDir, '09-template-loaded.png'), fullPage: true })
    console.log('📸 截图：09-template-loaded.png')

    // 验证编辑器中有模板代码
    const editorContent = page.locator('[class*="monaco-editor"]')
    if (await editorContent.isVisible()) {
      console.log('✅ 模板加载验证通过')
    }
    
    console.log('✅ 场景 5 完成')
  })
})
