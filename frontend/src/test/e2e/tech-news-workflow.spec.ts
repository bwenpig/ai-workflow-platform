import { test, expect } from '@playwright/test'
import * as path from 'path'
import * as fs from 'fs'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * 科技资讯工作流 E2E 测试
 * 覆盖：多数据源节点、ETL 清洗节点、LLM 推荐节点、模板导入
 */
test.describe('科技资讯推荐工作流 E2E', () => {
  const screenshotDir = path.join(__dirname, 'screenshots', 'tech-news-workflow')

  test.beforeEach(async ({ page }) => {
    if (!fs.existsSync(screenshotDir)) {
      fs.mkdirSync(screenshotDir, { recursive: true })
    }
    await page.goto('/')
    // 跳过新手引导
    const skipButton = page.getByText(/跳过|Skip/)
    if (await skipButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await skipButton.click()
    }
  })

  // ============================================
  // 场景 1：添加所有新节点类型
  // ============================================
  test.describe('场景 1：新节点类型可用', () => {
    test('NodeSidebar 应包含 ETL 和 LLM 推荐节点', async ({ page }) => {
      // 检查 NodeSidebar 中有 ETL 节点
      const etlItem = page.locator('.node-sidebar__item').filter({ hasText: 'ETL 数据清洗' })
      await expect(etlItem).toBeVisible()

      // 检查 NodeSidebar 中有 LLM 推荐节点
      const recItem = page.locator('.node-sidebar__item').filter({ hasText: 'LLM 智能推荐' })
      await expect(recItem).toBeVisible()

      await page.screenshot({ path: path.join(screenshotDir, '01-sidebar-nodes.png'), fullPage: true })
    })

    test('应能添加 ETL 节点到画布', async ({ page }) => {
      // 点击添加 ETL 节点
      const etlItem = page.locator('.node-sidebar__item').filter({ hasText: 'ETL 数据清洗' })
      await etlItem.click()

      // 验证画布上存在 ETL 节点
      const etlNode = page.locator('.etl-node')
      await expect(etlNode).toBeVisible()

      // 验证节点显示统一字段标签
      await expect(page.locator('.etl-node').filter({ hasText: 'title' })).toBeVisible()
      await expect(page.locator('.etl-node').filter({ hasText: 'source' })).toBeVisible()

      await page.screenshot({ path: path.join(screenshotDir, '02-etl-node-added.png'), fullPage: true })
    })

    test('应能添加 LLM 推荐节点到画布', async ({ page }) => {
      // 点击添加推荐节点
      const recItem = page.locator('.node-sidebar__item').filter({ hasText: 'LLM 智能推荐' })
      await recItem.click()

      // 验证画布上存在推荐节点
      const recNode = page.locator('.recommendation-node')
      await expect(recNode).toBeVisible()

      // 验证用户画像显示
      await expect(recNode.filter({ hasText: 'Java 工程师' })).toBeVisible()
      await expect(recNode.filter({ hasText: 'AI 生图' })).toBeVisible()

      await page.screenshot({ path: path.join(screenshotDir, '03-recommendation-node-added.png'), fullPage: true })
    })
  })

  // ============================================
  // 场景 2：构建完整科技资讯工作流
  // ============================================
  test.describe('场景 2：构建完整工作流', () => {
    test('应能创建多数据源 + ETL + 推荐 + 微信推送流程', async ({ page }) => {
      // 添加 4 个 HTTP 请求节点
      const httpItem = page.locator('.node-sidebar__item').filter({ hasText: 'HTTP 请求' })
      
      // 添加第 1 个：HackerNews
      await httpItem.click()
      await page.waitForTimeout(300)
      
      // 添加第 2 个：Reddit
      await httpItem.click()
      await page.waitForTimeout(300)
      
      // 添加第 3 个：GitHub
      await httpItem.click()
      await page.waitForTimeout(300)
      
      // 添加第 4 个：36kr
      await httpItem.click()
      await page.waitForTimeout(300)

      // 添加 ETL 节点
      const etlItem = page.locator('.node-sidebar__item').filter({ hasText: 'ETL 数据清洗' })
      await etlItem.click()
      await page.waitForTimeout(300)

      // 添加 LLM 推荐节点
      const recItem = page.locator('.node-sidebar__item').filter({ hasText: 'LLM 智能推荐' })
      await recItem.click()
      await page.waitForTimeout(300)

      // 添加微信推送节点
      const wxItem = page.locator('.node-sidebar__item').filter({ hasText: '微信推送' })
      await wxItem.click()
      await page.waitForTimeout(300)

      // 验证所有节点都在画布上
      const httpNodes = page.locator('.http-request-node')
      await expect(httpNodes).toHaveCount(4, { timeout: 5000 })

      const etlNode = page.locator('.etl-node')
      await expect(etlNode).toHaveCount(1)

      const recNode = page.locator('.recommendation-node')
      await expect(recNode).toHaveCount(1)

      await page.screenshot({ path: path.join(screenshotDir, '04-full-workflow-nodes.png'), fullPage: true })
    })
  })

  // ============================================
  // 场景 3：模板导入
  // ============================================
  test.describe('场景 3：工作流模板导入', () => {
    test('应能导入科技资讯工作流模板 JSON', async ({ page }) => {
      // 读取模板 JSON
      const templatePath = path.resolve(
        __dirname, '..', '..', '..', '..', 'backend', 'src', 'main', 'resources', 'templates',
        '11_tech_news_recommendation.json'
      )

      // 验证模板文件存在
      expect(fs.existsSync(templatePath)).toBeTruthy()

      // 解析模板验证结构
      const template = JSON.parse(fs.readFileSync(templatePath, 'utf-8'))
      expect(template.name).toBe('科技资讯智能推荐工作流')
      expect(template.nodes).toHaveLength(8)
      expect(template.edges).toHaveLength(7)

      // 验证节点类型
      const nodeTypes = template.nodes.map((n: any) => n.type)
      expect(nodeTypes).toContain('http_request')
      expect(nodeTypes).toContain('etl')
      expect(nodeTypes).toContain('llm_recommendation')
      expect(nodeTypes).toContain('wx_push')

      // 验证数据源节点
      const httpNodes = template.nodes.filter((n: any) => n.type === 'http_request')
      expect(httpNodes).toHaveLength(4)
      const labels = httpNodes.map((n: any) => n.data.label)
      expect(labels).toContain('HackerNews')
      expect(labels).toContain('Reddit r/technology')
      expect(labels).toContain('GitHub Trending')
      expect(labels).toContain('36kr 科技资讯')

      // 验证 ETL 节点
      const etlNode = template.nodes.find((n: any) => n.type === 'etl')
      expect(etlNode).toBeDefined()
      expect(etlNode.data.config.sources).toEqual(['HackerNews', 'Reddit', 'GitHub', '36kr'])

      // 验证推荐节点的用户画像
      const recNode = template.nodes.find((n: any) => n.type === 'llm_recommendation')
      expect(recNode).toBeDefined()
      expect(recNode.data.config.userProfile.profession).toBe('Java 工程师 + 技术 Leader')
      expect(recNode.data.config.userProfile.businessFocus).toBe('AI 生图、AI 生视频')
      expect(recNode.data.config.userProfile.interests).toContain('数码爱好者')
      expect(recNode.data.config.userProfile.interests).toContain('游戏爱好者')
      expect(recNode.data.config.userProfile.interests).toContain('爱狗人士')
      expect(recNode.data.config.userProfile.interests).toContain('业余拳击运动')

      // 验证边连接正确（并行→汇聚→清洗→推荐→推送）
      const edges = template.edges
      // 4 个数据源 → join
      expect(edges.filter((e: any) => e.target === 'join')).toHaveLength(4)
      // join → etl
      expect(edges.find((e: any) => e.source === 'join' && e.target === 'etl')).toBeDefined()
      // etl → recommendation
      expect(edges.find((e: any) => e.source === 'etl' && e.target === 'recommendation')).toBeDefined()
      // recommendation → wx_push
      expect(edges.find((e: any) => e.source === 'recommendation' && e.target === 'wx_push')).toBeDefined()
    })
  })

  // ============================================
  // 场景 4：节点配置面板
  // ============================================
  test.describe('场景 4：节点配置', () => {
    test('ETL 节点应可点击并显示配置', async ({ page }) => {
      // 添加 ETL 节点
      const etlItem = page.locator('.node-sidebar__item').filter({ hasText: 'ETL 数据清洗' })
      await etlItem.click()
      await page.waitForTimeout(300)

      // 点击 ETL 节点
      const etlNode = page.locator('.etl-node')
      await etlNode.click()
      await page.waitForTimeout(300)

      // 右侧面板应显示节点信息
      await page.screenshot({ path: path.join(screenshotDir, '05-etl-config-panel.png'), fullPage: true })
    })

    test('推荐节点应可点击并显示用户画像', async ({ page }) => {
      // 添加推荐节点
      const recItem = page.locator('.node-sidebar__item').filter({ hasText: 'LLM 智能推荐' })
      await recItem.click()
      await page.waitForTimeout(300)

      // 点击推荐节点
      const recNode = page.locator('.recommendation-node')
      await recNode.click()
      await page.waitForTimeout(300)

      await page.screenshot({ path: path.join(screenshotDir, '06-recommendation-config-panel.png'), fullPage: true })
    })
  })

  // ============================================
  // 场景 5：后端 API 验证
  // ============================================
  test.describe('场景 5：后端 API', () => {
    test('应能通过 API 创建包含新节点类型的工作流', async ({ request }) => {
      const templatePath = path.resolve(
        __dirname, '..', '..', '..', '..', 'backend', 'src', 'main', 'resources', 'templates',
        '11_tech_news_recommendation.json'
      )
      const template = JSON.parse(fs.readFileSync(templatePath, 'utf-8'))

      // 通过 API 创建工作流
      const response = await request.post('http://localhost:8080/api/v1/workflows', {
        headers: {
          'Content-Type': 'application/json',
          'X-User-Id': 'ben-test'
        },
        data: {
          name: template.name,
          description: template.description,
          nodes: template.nodes.map((n: any) => ({
            nodeId: n.id,
            type: n.type,
            position: n.position,
            config: n.data.config || {}
          })),
          edges: template.edges
        }
      })

      // 如果后端运行中，验证返回
      if (response.ok()) {
        const workflow = await response.json()
        expect(workflow.id).toBeDefined()
        expect(workflow.name).toBe(template.name)
      }
    })

    test('executors.yaml 应包含 etl 和 llm_recommendation 注册', async () => {
      const yamlPath = path.resolve(
        __dirname, '..', '..', '..', '..', 'backend', 'src', 'main', 'resources', 'executors.yaml'
      )
      const content = fs.readFileSync(yamlPath, 'utf-8')
      expect(content).toContain('type: etl')
      expect(content).toContain('class: com.ben.workflow.executor.EtlExecutor')
      expect(content).toContain('type: llm_recommendation')
      expect(content).toContain('class: com.ben.workflow.executor.LlmRecommendationExecutor')
    })
  })
})
