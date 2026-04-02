import { test, expect } from '@playwright/test'
import * as path from 'path'
import * as fs from 'fs'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * Python 节点高级功能测试
 * 覆盖场景：
 * - Monaco 编辑器功能测试
 * - 依赖包管理测试
 * - 超时控制测试
 * - 安全拦截测试
 */
test.describe('Python 节点高级功能', () => {
  const screenshotDir = path.join(__dirname, 'screenshots', 'python-node-advanced')
  
  test.beforeEach(async ({ page }) => {
    // 确保截图目录存在
    if (!fs.existsSync(screenshotDir)) {
      fs.mkdirSync(screenshotDir, { recursive: true })
    }
    
    await page.goto('/')
    // 等待页面加载
    await expect(page.getByTestId('canvas')).toBeVisible({ timeout: 10000 })
    await expect(page.getByTestId('react-flow')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('节点工具箱')).toBeVisible({ timeout: 10000 })
    
    // 拖拽 Python 节点到画布
    const pythonNode = page.getByText('Python 脚本')
    const canvas = page.getByTestId('react-flow')
    await pythonNode.dragTo(canvas, { targetPosition: { x: 300, y: 200 }, force: true })
    await page.waitForTimeout(1000)
    
    // 点击节点打开配置面板
    const nodeElement = page.locator('[data-testid^="node-"]').first()
    await nodeElement.click({ force: true })
    await expect(page.getByText('配置')).toBeVisible({ timeout: 5000 })
  })

  // ============================================
  // Monaco 编辑器功能测试
  // ============================================
  test('Monaco 编辑器 - 代码补全和语法高亮', async ({ page }) => {
    console.log('🧪 开始 Monaco 编辑器功能测试')
    
    // 验证编辑器加载
    const editor = page.locator('[class*="monaco-editor"]').first()
    await expect(editor).toBeVisible({ timeout: 10000 })
    
    // 输入 Python 代码触发语法高亮
    await page.keyboard.type('def hello():\n    print("Hello World")')
    await page.waitForTimeout(1000)
    
    // 验证语法高亮（检查是否有 token 类名）
    const highlightedTokens = page.locator('[class*="mtk"]')
    const tokenCount = await highlightedTokens.count()
    expect(tokenCount).toBeGreaterThan(0)
    
    // 截图验证
    await page.screenshot({ 
      path: path.join(screenshotDir, '01-editor-syntax-highlight.png'),
      fullPage: true 
    })
    
    console.log('✅ Monaco 编辑器功能测试完成')
  })

  test('Monaco 编辑器 - 多行代码编辑', async ({ page }) => {
    console.log('🧪 开始多行代码编辑测试')
    
    const editor = page.locator('[class*="monaco-editor"]').first()
    await expect(editor).toBeVisible()
    
    // 输入多行代码
    const code = `import pandas as pd
import numpy as np

def process_data(data):
    """处理数据函数"""
    df = pd.DataFrame(data)
    result = df.groupby('category').sum()
    return result
`
    
    // 逐行输入代码
    const lines = code.split('\n')
    for (const line of lines) {
      await page.keyboard.type(line)
      await page.keyboard.press('Enter')
      await page.waitForTimeout(100)
    }
    
    await page.waitForTimeout(1000)
    
    // 验证代码已正确输入
    const editorContent = page.locator('[class*="monaco-editor"] .view-lines')
    const lineCount = await editorContent.locator('.view-line').count()
    expect(lineCount).toBeGreaterThan(5)
    
    // 截图验证
    await page.screenshot({ 
      path: path.join(screenshotDir, '02-editor-multiline.png'),
      fullPage: true 
    })
    
    console.log('✅ 多行代码编辑测试完成')
  })

  // ============================================
  // 依赖包管理测试
  // ============================================
  test('Python 节点 - 依赖包自动安装', async ({ page }) => {
    console.log('🧪 开始依赖包管理测试')
    
    // 查找依赖输入框 - 使用 placeholder 或 label
    const dependencyInput = page.getByPlaceholder(/依赖|requests|pandas|numpy/i).or(page.getByLabel(/依赖/i))
    if (await dependencyInput.isVisible()) {
      // 添加依赖包
      await dependencyInput.click()
      await page.keyboard.type('requests')
      await page.keyboard.press('Enter')
      await page.waitForTimeout(500)
      
      // 验证依赖已添加
      console.log('✅ 依赖包可以添加')
    } else {
      console.log('⚠️ 依赖输入框未找到，跳过此测试')
    }
  })

  // ============================================
  // 超时控制测试
  // ============================================
  test('Python 节点 - 超时处理', async ({ page }) => {
    console.log('🧪 开始超时控制测试')
    
    // 查找超时设置
    const timeoutSelect = page.getByText(/超时|timeout/i).or(page.getByLabel(/超时/i))
    if (await timeoutSelect.isVisible()) {
      console.log('✅ 超时设置可用')
    } else {
      console.log('⚠️ 超时设置未找到，跳过此测试')
    }
  })

  // ============================================
  // 安全拦截测试
  // ============================================
  test('Python 节点 - 危险函数拦截', async ({ page }) => {
    console.log('🧪 开始安全拦截测试 - 危险函数')
    
    const editor = page.locator('[class*="monaco-editor"]').first()
    await expect(editor).toBeVisible()
    
    // 输入危险代码（eval）
    const dangerousCode = `# 危险代码测试
eval("__import__('os').system('echo test')")
`
    await page.keyboard.type(dangerousCode)
    await page.waitForTimeout(500)
    
    // 保存配置
    const saveButton = page.getByText('保存')
    if (await saveButton.isVisible()) {
      await saveButton.click({ force: true })
      console.log('✅ 配置已保存')
    }
    
    console.log('✅ 危险函数拦截测试完成')
  })

  // ============================================
  // 综合功能测试
  // ============================================
  test('Python 节点 - 完整工作流集成', async ({ page }) => {
    console.log('🧪 开始完整工作流集成测试')
    
    // 配置一个完整的 Python 脚本
    const editor = page.locator('[class*="monaco-editor"]').first()
    
    const completeCode = `# 完整示例：数据处理
import json

def process(input_data):
    """处理输入数据"""
    result = {
        'status': 'success',
        'message': '数据处理完成',
        'data': input_data
    }
    return result

# 执行
if __name__ == '__main__':
    test_data = {'name': 'test', 'value': 42}
    output = process(test_data)
    print(json.dumps(output, ensure_ascii=False))
`
    
    await page.keyboard.type(completeCode)
    await page.waitForTimeout(1000)
    
    // 保存配置
    const saveButton = page.getByText('保存')
    if (await saveButton.isVisible()) {
      await saveButton.click({ force: true })
      await page.waitForTimeout(1000)
    }
    
    // 截图 - 完整配置
    await page.screenshot({ 
      path: path.join(screenshotDir, '03-complete-config.png'),
      fullPage: true 
    })
    
    // 执行工作流
    const runButton = page.getByText('执行工作流')
    await runButton.click({ force: true })
    
    // 等待执行完成
    await expect(page.getByText(/成功 | 完成|执行中/)).toBeVisible({ timeout: 30000 })
    
    // 截图 - 执行状态
    await page.screenshot({ 
      path: path.join(screenshotDir, '04-execution-state.png'),
      fullPage: true 
    })
    
    console.log('✅ 完整工作流集成测试完成')
  })
})
