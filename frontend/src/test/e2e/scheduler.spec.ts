import { test, expect } from '@playwright/test';

/**
 * 定时任务管理 E2E 测试
 */
test.describe('定时任务管理', () => {
  test.beforeEach(async ({ page }) => {
    // Mock scheduler API
    await page.route('**/api/v1/scheduler/jobs', async (route, request) => {
      if (request.method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 'job-001',
              workflowId: 'wf-001',
              workflowName: '每日数据处理',
              cronExpression: '0 0 9 * * *',
              status: 'RUNNING',
              nextFireTime: new Date(Date.now() + 86400000).toISOString(),
              lastFireTime: new Date(Date.now() - 86400000).toISOString(),
              lastExecutionId: 'exec-001',
              description: '每天上午9点执行',
              createdBy: 'ben',
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString(),
            },
            {
              id: 'job-002',
              workflowId: 'wf-002',
              workflowName: '周报生成',
              cronExpression: '0 0 10 * * MON',
              status: 'PAUSED',
              nextFireTime: null,
              lastFireTime: null,
              lastExecutionId: null,
              description: '每周一上午10点',
              createdBy: 'ben',
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString(),
            },
          ]),
        });
      } else if (request.method() === 'POST') {
        const body = JSON.parse(request.postData() || '{}');
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'job-new',
            workflowId: body.workflowId,
            workflowName: 'Mock Workflow',
            cronExpression: body.cronExpression,
            status: 'RUNNING',
            nextFireTime: new Date(Date.now() + 86400000).toISOString(),
            lastFireTime: null,
            lastExecutionId: null,
            description: body.description || null,
            createdBy: 'ben',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          }),
        });
      }
    });

    // Mock pause
    await page.route('**/api/v1/scheduler/jobs/*/pause', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'job-001',
          status: 'PAUSED',
          workflowId: 'wf-001',
          workflowName: '每日数据处理',
          cronExpression: '0 0 9 * * *',
          nextFireTime: null,
          createdBy: 'ben',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }),
      });
    });

    // Mock resume
    await page.route('**/api/v1/scheduler/jobs/*/resume', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'job-002',
          status: 'RUNNING',
          workflowId: 'wf-002',
          workflowName: '周报生成',
          cronExpression: '0 0 10 * * MON',
          nextFireTime: new Date(Date.now() + 604800000).toISOString(),
          createdBy: 'ben',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }),
      });
    });

    // Mock delete
    await page.route('**/api/v1/scheduler/jobs/*', async (route, request) => {
      if (request.method() === 'DELETE') {
        await route.fulfill({ status: 204 });
      } else {
        await route.fallback();
      }
    });

    // Mock workflows list (for create modal)
    await page.route('**/api/v1/workflows', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { id: 'wf-001', name: '每日数据处理', description: '处理每日报表' },
          { id: 'wf-002', name: '周报生成', description: '自动生成周报' },
          { id: 'wf-003', name: 'AI 图片生成', description: 'Flux 批量生图' },
        ]),
      });
    });

    await page.goto('http://localhost:5173');
  });

  test('应该能导航到定时任务页面', async ({ page }) => {
    // 点击定时任务 tab
    await page.click('text=⏰ 定时任务');

    // 等待页面加载
    await expect(page.locator('text=定时任务管理')).toBeVisible();
  });

  test('应该显示定时任务列表', async ({ page }) => {
    await page.click('text=⏰ 定时任务');

    // 验证表格中有两条数据
    await expect(page.locator('text=每日数据处理')).toBeVisible();
    await expect(page.locator('text=周报生成')).toBeVisible();

    // 验证状态标签
    await expect(page.locator('text=运行中')).toBeVisible();
    await expect(page.locator('text=已暂停')).toBeVisible();

    // 验证 cron 表达式
    await expect(page.locator('text=0 0 9 * * *')).toBeVisible();
    await expect(page.locator('text=0 0 10 * * MON')).toBeVisible();
  });

  test('应该能打开新建定时任务对话框', async ({ page }) => {
    await page.click('text=⏰ 定时任务');

    // 点击新建按钮
    await page.click('text=新建定时任务');

    // 验证 modal 出现
    await expect(page.locator('.ant-modal-title >> text=新建定时任务')).toBeVisible();

    // 验证表单元素
    await expect(page.locator('text=选择工作流')).toBeVisible();
    await expect(page.locator('text=Cron 表达式')).toBeVisible();
  });

  test('应该能通过快捷选择填充 cron', async ({ page }) => {
    await page.click('text=⏰ 定时任务');
    await page.click('text=新建定时任务');

    // 点击 "每天 9:00" 预设
    await page.click('.ant-tag >> text=每天 9:00');

    // 验证 cron 输入框已填充
    const cronInput = page.locator('input[placeholder="0 0 9 * * *"]');
    await expect(cronInput).toHaveValue('0 0 9 * * *');
  });

  test('应该能刷新列表', async ({ page }) => {
    await page.click('text=⏰ 定时任务');

    // 点击刷新
    await page.click('text=刷新');

    // 列表仍然显示
    await expect(page.locator('text=每日数据处理')).toBeVisible();
  });

  test('定时任务页面不显示日志面板', async ({ page }) => {
    await page.click('text=⏰ 定时任务');

    // 日志面板切换按钮应不可见
    await expect(page.locator('text=📋 隐藏日志')).not.toBeVisible();
    await expect(page.locator('text=📋 显示日志')).not.toBeVisible();
  });

  test('从定时任务切换回画布应恢复日志面板', async ({ page }) => {
    await page.click('text=⏰ 定时任务');
    await expect(page.locator('text=定时任务管理')).toBeVisible();

    // 切换回画布
    await page.click('text=📋 画布编辑器');

    // 日志按钮应重新出现
    await expect(page.locator('button >> text=📋 隐藏日志').or(page.locator('button >> text=📋 显示日志'))).toBeVisible();
  });
});
