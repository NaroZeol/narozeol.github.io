import assert from 'node:assert/strict';
import test from 'node:test';
import { chromium } from 'playwright';

const fixtures = Array.from({ length: 35 }, (_, i) => ({
  id: String(i), content: `想法 ${i + 1}`, created_at: new Date(Date.UTC(2026, 8, i + 1)).toISOString(),
  tags: [i % 2 ? '日常' : '技术'],
}));
fixtures.push({ id: 'boundary', content: '当天最后一刻', created_at: '2026-10-01T15:59:59.999Z', tags: ['技术'] });
fixtures.push({ id: 'year', content: '跨年内容', created_at: '2025-12-31T16:00:00Z', tags: [] });

test('range picker: real inputs, validation, calendar, presets and responsive layout', async () => {
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 }, timezoneId: 'America/Los_Angeles' });
    await page.clock.setFixedTime(new Date('2026-10-04T17:00:00Z')); // Already Oct 5 in Shanghai.
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    let failed = true;
    await page.route('https://gist.githubusercontent.com/**', route => failed
      ? route.fulfill({ status: 503, body: 'unavailable' })
      : route.fulfill({ json: fixtures }));
    await page.goto(`${process.env.BLOG_PREVIEW_URL || 'http://localhost:8765'}/thoughts/`);
    await page.getByRole('button', { name: '重试', exact: true }).waitFor();
    failed = false;
    await page.getByRole('button', { name: '重试', exact: true }).click();
    await page.locator('.thought-card').first().waitFor();
    const start = page.locator('#thought-date-start');
    const end = page.locator('#thought-date-end');
    const popup = page.locator('.arco-picker-range-container');
    const count = page.locator('#thought-count');
    const inputs = () => page.locator('#thought-date-root input').evaluateAll(nodes => nodes.map(node => node.value));
    const enter = async (a, b) => {
      await start.click(); await start.fill(a); await start.press('Tab');
      await end.click(); await end.fill(b); await end.press('Enter');
      await popup.waitFor({ state: 'hidden' });
    };
    await enter('2026-10-01', '2026-10-01');
    assert.equal(await count.textContent(), '共 2 条想法');
    assert.match(await page.locator('#thought-feed').textContent(), /当天最后一刻/);
    await enter('2025-12-31', '2026-01-01');
    assert.equal(await count.textContent(), '共 1 条想法');
    assert.match(await page.locator('#thought-feed').textContent(), /跨年内容/);
    await enter('2026-10-05', '2026-10-01');
    assert.deepEqual(await inputs(), ['2026-10-01', '2026-10-05']);
    assert.equal(await count.textContent(), '共 6 条想法');

    await start.click(); await start.fill('2026-02-30'); await start.press('Enter');
    assert.equal(await page.getByRole('alert').isVisible(), true);
    assert.equal(await count.textContent(), '共 6 条想法');
    await start.press('Escape'); await popup.waitFor({ state: 'hidden' });
    assert.deepEqual(await inputs(), ['2026-10-01', '2026-10-05']);
    await start.click(); await start.fill('2026-09-01'); await page.locator('#thought-count').click();
    await popup.waitFor({ state: 'hidden' });
    assert.deepEqual(await inputs(), ['2026-10-01', '2026-10-05']);

    for (const [name, range] of [
      ['最近 7 天', ['2026-09-29', '2026-10-05']],
      ['最近 30 天', ['2026-09-06', '2026-10-05']],
      ['本月', ['2026-10-01', '2026-10-05']],
      ['今年', ['2026-01-01', '2026-10-05']],
    ]) {
      await start.click(); await page.getByRole('button', { name, exact: true }).click();
      await popup.waitFor({ state: 'hidden' });
      assert.deepEqual(await inputs(), range);
    }
    await page.locator('#thought-reset').click();
    assert.deepEqual(await inputs(), ['', '']);
    assert.equal(await page.locator('.thought-card').count(), 20);
    await page.locator('#load-more').click();
    assert.equal(await page.locator('.thought-card').count(), fixtures.length);

    // Use the actual calendar cells, including the same date for both ends.
    await start.click();
    const left = page.locator('.arco-panel-date').first();
    const day = value => left.locator('.arco-picker-cell-in-view').filter({ hasText: new RegExp(`^${value}$`) });
    await day(1).click(); await day(1).click();
    await popup.waitFor({ state: 'hidden' });
    assert.deepEqual(await inputs(), ['2026-10-01', '2026-10-01']);
    assert.equal(await count.textContent(), '共 2 条想法');
    await page.locator('#thought-tag').selectOption('技术');
    await page.locator('#thought-sort').selectOption('oldest');
    assert.equal(await count.textContent(), '共 2 条想法');

    // Year and month headings lead to the component's selection grids.
    await start.click();
    await left.locator('.arco-picker-header-label').first().click();
    assert.equal(await page.locator('.arco-panel-year').count(), 1);
    await page.locator('.arco-panel-year .arco-picker-cell').filter({ hasText: /^2025$/ }).click();
    assert.equal(await page.locator('.arco-panel-month').count(), 1);
    await page.locator('.arco-panel-month .arco-picker-cell').first().click();
    assert.match(await left.locator('.arco-picker-header-value').textContent(), /2025年.*1月/);
    await start.press('Escape'); await popup.waitFor({ state: 'hidden' });

    for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }, { width: 320, height: 568 }]) {
      await page.setViewportSize(viewport);
      await start.click(); await popup.waitFor();
      await page.waitForTimeout(250); // Arco's opening animation and auto-fit positioning.
      const box = await popup.boundingBox();
      assert.ok(box.x >= 0 && box.x + box.width <= viewport.width + 1, JSON.stringify(box));
      assert.ok(box.y >= 0 && box.y + box.height <= viewport.height + 1, JSON.stringify(box));
      assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1));
      await page.getByRole('button', { name: '最近 7 天', exact: true }).click();
      await popup.waitFor({ state: 'hidden' });
      assert.deepEqual(await inputs(), ['2026-09-29', '2026-10-05']);
    }
    assert.deepEqual(errors, []);
  } finally { await browser.close(); }
});
