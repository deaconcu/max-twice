import { test, expect } from '@playwright/test'

/**
 * 用户认证测试
 */
test.describe('用户登录和注册', () => {
  test('登录页面基本元素显示', async ({ page }) => {
    await page.goto('/login')

    // 验证页面标题
    await expect(page.locator('h2, h1').filter({ hasText: /登录|Login/ })).toBeVisible({
      timeout: 10000,
    })

    // 验证表单元素存在（等待加载）
    await expect(page.locator('input').first()).toBeVisible({ timeout: 10000 })
    await expect(page.locator('button[type="submit"]')).toBeVisible()
  })

  test('登录成功流程', async ({ page }) => {
    await page.goto('/login')

    // 等待页面加载完成
    await page.waitForLoadState('domcontentloaded')

    // 填写登录表单（按顺序填写第一个和第二个输入框）
    const inputs = page.locator('input')
    await inputs.nth(0).fill('deaconcc@126.com')
    await inputs.nth(1).fill('password123')

    // 点击提交按钮
    await page.click('button[type="submit"]')

    // 等待跳转或加载完成
    await page.waitForLoadState('networkidle', { timeout: 10000 })
  })

  test('登录失败 - 密码错误', async ({ page }) => {
    await page.goto('/login')

    await page.waitForLoadState('domcontentloaded')

    const inputs = page.locator('input')
    await inputs.nth(0).fill('test@example.com')
    await inputs.nth(1).fill('wrongpassword')
    await page.click('button[type="submit"]')

    // 等待错误提示显示（使用 first() 避免 strict mode violation）
    await expect(
      page
        .locator('.v-alert')
        .filter({ hasText: /密码|错误|失败|不正确/ })
        .first()
    ).toBeVisible({ timeout: 5000 })
  })
})
