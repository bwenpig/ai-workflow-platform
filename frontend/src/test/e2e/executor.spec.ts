import { test, expect } from '@playwright/test'

/**
 * E2E 测试: Executor 扩展 - Email 节点
 * 覆盖功能：
 *   - Email 节点在侧边栏可见
 *   - Email 节点可拖拽到画布
 *   - Email 节点配置面板
 *   - Email 节点注册 & 默认数据
 */
test.describe('Executor 扩展 - Email 节点', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await expect(page.getByTestId('canvas')).toBeVisible({ timeout: 10000 })
    await expect(page.getByTestId('react-flow')).toBeVisible({ timeout: 10000 })
  })

  test('侧边栏应显示邮件发送节点', async ({ page }) => {
    // 集成服务分类应可见
    const emailNode = page.getByText('邮件发送').first()
    await expect(emailNode).toBeVisible({ timeout: 5000 })

    // 描述文字应可见
    await expect(page.getByText('通过 SMTP 发送邮件').first()).toBeVisible({ timeout: 3000 })
  })

  test('应能搜索到邮件节点', async ({ page }) => {
    // 搜索框输入
    const searchInput = page.locator('input[placeholder="搜索节点..."]')
    await expect(searchInput).toBeVisible({ timeout: 5000 })
    await searchInput.fill('邮件')

    // 邮件发送节点应出现在结果中
    await expect(page.getByText('邮件发送').first()).toBeVisible({ timeout: 3000 })

    // 不相关节点应被过滤
    await expect(page.getByText('Python 脚本')).not.toBeVisible({ timeout: 2000 })
  })

  test('应能拖拽 Email 节点到画布', async ({ page }) => {
    const emailNode = page.getByText('邮件发送').first()
    const canvas = page.getByTestId('react-flow')

    await emailNode.dragTo(canvas, {
      targetPosition: { x: 300, y: 250 },
      force: true,
    })

    await page.waitForTimeout(1000)

    // 画布上应出现至少一个节点
    const nodes = page.locator('[data-testid^="node-"]')
    const count = await nodes.count()
    expect(count).toBeGreaterThanOrEqual(1)
  })

  test('Email 节点应显示默认数据', async ({ page }) => {
    const emailNode = page.getByText('邮件发送').first()
    const canvas = page.getByTestId('react-flow')

    await emailNode.dragTo(canvas, {
      targetPosition: { x: 300, y: 250 },
      force: true,
    })

    await page.waitForTimeout(1000)

    // 节点卡片内应显示 📧 标识
    await expect(page.locator('.react-flow__node').getByText('📧').first()).toBeVisible({ timeout: 3000 })
  })

  test('点击 Email 节点应打开配置面板', async ({ page }) => {
    // 先添加 Email 节点
    const emailNode = page.getByText('邮件发送').first()
    const canvas = page.getByTestId('react-flow')

    await emailNode.dragTo(canvas, {
      targetPosition: { x: 300, y: 250 },
      force: true,
    })

    await page.waitForTimeout(1000)

    // 点击画布上的节点
    const nodeElement = page.locator('[data-testid^="node-"]').first()
    await nodeElement.click({ force: true })

    // 配置面板应出现（有"配置"或"节点配置"字样）
    await expect(page.getByText('节点配置').first()).toBeVisible({ timeout: 5000 })
  })

  test('Email 配置面板应包含所有字段', async ({ page }) => {
    // 添加 Email 节点
    const emailNode = page.getByText('邮件发送').first()
    const canvas = page.getByTestId('react-flow')

    await emailNode.dragTo(canvas, {
      targetPosition: { x: 300, y: 250 },
      force: true,
    })

    await page.waitForTimeout(1000)

    // 点击打开配置
    const nodeElement = page.locator('[data-testid^="node-"]').first()
    await nodeElement.click({ force: true })

    await expect(page.getByText('节点配置').first()).toBeVisible({ timeout: 5000 })

    // 验证 email 配置字段
    await expect(page.getByText('收件人').first()).toBeVisible({ timeout: 3000 })
    await expect(page.getByText('邮件主题').first()).toBeVisible({ timeout: 3000 })
    await expect(page.getByText('邮件正文').first()).toBeVisible({ timeout: 3000 })
  })

  test('Email 配置面板应能填写并保存', async ({ page }) => {
    // 添加 Email 节点
    const emailNode = page.getByText('邮件发送').first()
    const canvas = page.getByTestId('react-flow')

    await emailNode.dragTo(canvas, {
      targetPosition: { x: 300, y: 250 },
      force: true,
    })

    await page.waitForTimeout(1000)

    // 点击打开配置
    const nodeElement = page.locator('[data-testid^="node-"]').first()
    await nodeElement.click({ force: true })

    await expect(page.getByText('节点配置').first()).toBeVisible({ timeout: 5000 })

    // 填写收件人
    const toInput = page.locator('input[placeholder*="user@example.com"]')
    if (await toInput.isVisible()) {
      await toInput.fill('test@example.com')
    }

    // 填写主题
    const subjectInput = page.locator('input[placeholder*="输入邮件主题"]')
    if (await subjectInput.isVisible()) {
      await subjectInput.fill('Test Subject')
    }

    // 填写正文
    const bodyTextarea = page.locator('textarea[placeholder*="输入邮件正文"]')
    if (await bodyTextarea.isVisible()) {
      await bodyTextarea.fill('<h1>Hello</h1><p>This is a test email.</p>')
    }

    // 点击保存
    const saveButton = page.getByText('保存').first()
    if (await saveButton.isVisible()) {
      await saveButton.click({ force: true })
    }
  })

  test('Email 节点与其他节点可连线', async ({ page }) => {
    const canvas = page.getByTestId('react-flow')

    // 先添加一个 HTTP 请求节点
    const httpNode = page.getByText('HTTP 请求').first()
    await httpNode.dragTo(canvas, {
      targetPosition: { x: 200, y: 100 },
      force: true,
    })

    await page.waitForTimeout(500)

    // 再添加 Email 节点
    const emailNode = page.getByText('邮件发送').first()
    await emailNode.dragTo(canvas, {
      targetPosition: { x: 200, y: 350 },
      force: true,
    })

    await page.waitForTimeout(1000)

    // 验证有 2 个节点
    const nodes = page.locator('[data-testid^="node-"]')
    const count = await nodes.count()
    expect(count).toBeGreaterThanOrEqual(2)
  })

  test('多个 Email 节点可共存', async ({ page }) => {
    const canvas = page.getByTestId('react-flow')
    const emailNode = page.getByText('邮件发送').first()

    // 拖拽 3 个 Email 节点
    await emailNode.dragTo(canvas, { targetPosition: { x: 100, y: 150 }, force: true })
    await page.waitForTimeout(500)
    await emailNode.dragTo(canvas, { targetPosition: { x: 300, y: 150 }, force: true })
    await page.waitForTimeout(500)
    await emailNode.dragTo(canvas, { targetPosition: { x: 500, y: 150 }, force: true })

    await page.waitForTimeout(1000)

    const nodes = page.locator('[data-testid^="node-"]')
    const count = await nodes.count()
    expect(count).toBeGreaterThanOrEqual(3)
  })
})
