import { test, expect } from '@playwright/test';

test.describe('Todo E2E', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.fill('input[type="email"]', 'test@example.com');
    await page.fill('input[type="password"]', 'password1');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/todos');
  });

  test('로그인 후 할 일 목록이 표시된다', async ({ page }) => {
    await expect(page.getByText('할 일 목록')).toBeVisible();
    await expect(page.locator('li')).not.toHaveCount(0);
  });

  test('새 할 일을 추가할 수 있다', async ({ page }) => {
    const title = `E2E 테스트 할 일 ${Date.now()}`;

    await page.fill('input[placeholder="새 할 일 입력"]', title);
    await page.click('button[type="submit"]');

    await expect(page.locator('li').filter({ hasText: title })).toBeVisible();
  });

  test('할 일을 완료 처리할 수 있다', async ({ page }) => {
    const title = `완료 테스트 ${Date.now()}`;

    await page.fill('input[placeholder="새 할 일 입력"]', title);
    await page.click('button[type="submit"]');

    const todoItem = page.locator('li').filter({ hasText: title });
    await expect(todoItem).toBeVisible();

    await todoItem.locator('input[type="checkbox"]').click();

    await expect(todoItem.locator('span')).toHaveClass(/line-through/);
  });

  test('할 일을 삭제할 수 있다', async ({ page }) => {
    const title = `삭제 테스트 ${Date.now()}`;

    await page.fill('input[placeholder="새 할 일 입력"]', title);
    await page.click('button[type="submit"]');

    const todoItem = page.locator('li').filter({ hasText: title });
    await expect(todoItem).toBeVisible();

    await todoItem.getByRole('button', { name: '삭제' }).click();
    await page.getByRole('dialog').getByRole('button', { name: '삭제' }).click();

    await expect(todoItem).not.toBeVisible();
  });

  test('로그인 → 할 일 생성 → 완료 → 삭제 전체 흐름', async ({ page }) => {
    const title = `전체 흐름 테스트 ${Date.now()}`;

    await page.fill('input[placeholder="새 할 일 입력"]', title);
    await page.click('button[type="submit"]');

    const todoItem = page.locator('li').filter({ hasText: title });
    await expect(todoItem).toBeVisible();

    await todoItem.locator('input[type="checkbox"]').click();
    await expect(todoItem.locator('span')).toHaveClass(/line-through/);

    await todoItem.getByRole('button', { name: '삭제' }).click();
    await page.getByRole('dialog').getByRole('button', { name: '삭제' }).click();
    await expect(todoItem).not.toBeVisible();
  });

  test('필터 탭으로 완료 항목만 볼 수 있다', async ({ page }) => {
    await page.getByRole('button', { name: '완료', exact: true }).click();

    const items = page.locator('li');
    const count = await items.count();

    for (let i = 0; i < count; i++) {
      await expect(items.nth(i).locator('input[type="checkbox"]')).toBeChecked();
    }
  });

  test('필터 탭으로 미완료 항목만 볼 수 있다', async ({ page }) => {
    await page.getByRole('button', { name: '미완료', exact: true }).click();

    const items = page.locator('li');
    const count = await items.count();

    for (let i = 0; i < count; i++) {
      await expect(items.nth(i).locator('input[type="checkbox"]')).not.toBeChecked();
    }
  });
});
