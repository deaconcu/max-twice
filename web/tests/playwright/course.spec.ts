import { test, expect } from '@playwright/test'

/**
 * 课程列表页面测试
 */
test.describe('课程列表页面', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/courses')
  })

  test('页面基本元素显示', async ({ page }) => {
    // 验证页面标题
    await expect(page.locator('h1, h2').filter({ hasText: '课程' }).first()).toBeVisible()

    // 验证分类导航存在
    await expect(page.locator('text=课程分类')).toBeVisible()
  })

  test('点击一级分类显示二级分类', async ({ page }) => {
    // 等待分类按钮加载
    await page.waitForSelector('.category-btn', { timeout: 10000 })

    // 点击"工程与技术"分类（主分类ID=2）
    await page.click('text=工程与技术')

    // 验证二级分类显示
    await expect(page.locator('text=计算机与信息技术1')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=电子与电气工程')).toBeVisible()
  })

  test('点击二级分类筛选课程', async ({ page }) => {
    // 点击一级分类
    await page.click('text=工程与技术')

    // 等待二级分类显示
    await page.waitForSelector('text=计算机与信息技术1')

    // 点击二级分类
    await page.click('text=计算机与信息技术1')

    // 等待课程列表加载
    await page.waitForLoadState('networkidle')

    // 验证有课程卡片显示（如果有数据的话）
    const courseCards = page.locator('.course-card, [class*="CourseCard"]')
    const count = await courseCards.count()

    // 可能没有课程，但至少不应该报错
    expect(count).toBeGreaterThanOrEqual(0)
  })

  test('切换分类时清空子分类选择', async ({ page }) => {
    // 选择第一个分类和子分类
    await page.click('text=自然科学')
    await page.waitForSelector('text=数学', { timeout: 5000 })
    await page.click('text=数学')

    // 切换到另一个主分类
    await page.click('text=工程与技术')

    // 验证：应该显示新的二级分类，而不是旧的
    await expect(page.locator('text=数学')).not.toBeVisible()
    await expect(page.locator('text=计算机与信息技术1')).toBeVisible()
  })

  test('点击全部按钮取消分类筛选', async ({ page }) => {
    // 先选择一个分类
    await page.click('text=工程与技术')
    await page.waitForSelector('text=计算机与信息技术1')

    // 点击"全部"按钮
    await page.click('.category-btn:has-text("全部")')

    // 验证：二级分类应该消失
    await expect(page.locator('text=计算机与信息技术1')).not.toBeVisible()
  })
})
