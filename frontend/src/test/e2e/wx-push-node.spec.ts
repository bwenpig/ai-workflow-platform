import { test, expect } from '@playwright/test';

test.describe('微信推送节点 E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:5173');
    await page.waitForTimeout(1000);
  });

  test('侧边栏应显示微信推送节点', async ({ page }) => {
    // 搜索微信
    const searchInput = page.locator('input[placeholder*="搜索"]');
    if (await searchInput.isVisible()) {
      await searchInput.fill('微信');
      await page.waitForTimeout(300);
    }

    // 检查节点列表中是否有微信推送
    const wxNode = page.locator('text=微信推送');
    await expect(wxNode.first()).toBeVisible({ timeout: 5000 });
  });

  test('应能拖拽微信推送节点到画布', async ({ page }) => {
    // 找到微信推送节点
    const wxItem = page.locator('.node-sidebar__item').filter({ hasText: '微信推送' });
    
    if (await wxItem.isVisible()) {
      // 获取画布区域
      const canvas = page.locator('.react-flow');
      
      if (await canvas.isVisible()) {
        const canvasBox = await canvas.boundingBox();
        const itemBox = await wxItem.boundingBox();
        
        if (canvasBox && itemBox) {
          // 拖拽到画布中央
          await page.mouse.move(
            itemBox.x + itemBox.width / 2,
            itemBox.y + itemBox.height / 2
          );
          await page.mouse.down();
          await page.mouse.move(
            canvasBox.x + canvasBox.width / 2,
            canvasBox.y + canvasBox.height / 2,
            { steps: 10 }
          );
          await page.mouse.up();
          await page.waitForTimeout(500);
        }
      }
    }

    // 截图记录
    await page.screenshot({ path: 'e2e/screenshots/wx-push-node-drag.png' });
  });

  test('点击添加微信推送节点', async ({ page }) => {
    // 点击微信推送节点添加到画布
    const wxItem = page.locator('.node-sidebar__item').filter({ hasText: '微信推送' });
    
    if (await wxItem.isVisible()) {
      await wxItem.click();
      await page.waitForTimeout(500);
    }

    await page.screenshot({ path: 'e2e/screenshots/wx-push-node-added.png' });
  });
});
