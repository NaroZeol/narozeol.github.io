import assert from 'node:assert/strict';
import test from 'node:test';
import { chromium } from 'playwright';
const origin = process.env.BLOG_PREVIEW_URL || 'http://localhost:8765';

test('year/category/tag selection keeps navigation on screen and supports history', async () => {
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
    await page.route('https://**', route => route.abort());
    const assertNavigation = async () => {
      for (const selector of ['#site-nav', '.archive-navigation']) {
        const box = await page.locator(selector).boundingBox();
        assert.ok(box && box.y >= 0 && box.y + box.height <= page.viewportSize().height, `${selector} scrolled out of view: ${JSON.stringify(box)}`);
      }
      assert.deepEqual(await page.locator('.archive-navigation a').allTextContents(), ['全部', '分类', '标签', '年份']);
    };
    await page.goto(origin, { waitUntil: 'networkidle' });
    await page.locator('.archive-navigation a[href="/posts/"]').click();
    await page.locator('.taxonomy__index a').filter({ hasText: '2024' }).click();
    await page.waitForTimeout(700); // Catch the theme's old smooth-scroll handler.
    await assertNavigation();
    assert.equal(await page.locator('.taxonomy__section:visible').count(), 1);
    assert.equal(await page.locator('.taxonomy__section:visible').getAttribute('id'), '2024');

    for (const width of [1440, 390, 320]) {
      await page.setViewportSize({ width, height: 900 });
      for (const path of ['/posts/', '/categories/', '/tags/']) {
        await page.goto(origin + path, { waitUntil: 'networkidle' });
        const choices = page.locator('.taxonomy__index a[data-taxonomy]');
        const first = choices.nth(1), second = choices.nth(2);
        const id = await first.getAttribute('data-taxonomy');
        await first.click();
        await assertNavigation();
        assert.equal(await page.locator('.taxonomy__section:visible').count(), 1);
        assert.equal(await page.locator('.taxonomy__section:visible').getAttribute('id'), id);
        const selectedURL = page.url();
        await second.click(); await assertNavigation();
        await page.goBack(); await assertNavigation();
        assert.equal(await page.locator('.taxonomy__section:visible').getAttribute('id'), id);
        await page.reload({ waitUntil: 'networkidle' }); await assertNavigation();
        assert.equal(page.url(), selectedURL);
        assert.equal(await page.locator('.taxonomy__section:visible').getAttribute('id'), id);
        await choices.first().click(); await assertNavigation();
        assert.ok(await page.locator('.taxonomy__section:visible').count() > 1);
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1));
      }
    }
    await page.goto(origin + '/posts/#2024', { waitUntil: 'networkidle' });
    await assertNavigation();
    assert.equal(await page.locator('.taxonomy__section:visible').getAttribute('id'), '2024');
    await page.locator('.archive-navigation a[href="/"]').click();
    assert.equal(new URL(page.url()).pathname, '/');
    assert.equal(await page.locator('.archive__item').count(), 8);
  } finally { await browser.close(); }
});
