import assert from "node:assert/strict";
import test from "node:test";
import { publicationDay, withinDateRange } from "../assets/js/thought-date-picker.mjs";

test("publication dates follow Shanghai's day and year boundaries", () => {
  assert.equal(publicationDay("2025-12-31T15:59:59Z"), "2025-12-31");
  assert.equal(publicationDay("2025-12-31T16:00:00Z"), "2026-01-01");
  assert.equal(publicationDay("2024-02-29T15:59:59Z"), "2024-02-29");
  assert.equal(publicationDay("invalid"), "");
});

test("a single-day range includes its final second but excludes the next day", () => {
  const range = { start: "2024-02-29", end: "2024-02-29" };
  assert.equal(withinDateRange("2024-02-28T15:59:59Z", range), false);
  assert.equal(withinDateRange("2024-02-28T16:00:00Z", range), true);
  assert.equal(withinDateRange("2024-02-29T15:59:59.999Z", range), true);
  assert.equal(withinDateRange("2024-02-29T16:00:00Z", range), false);
});

test("open-ended and cleared ranges work, including records without a valid date", () => {
  assert.equal(withinDateRange("2026-01-01T00:00:00+08:00", { start: "2026-01-01", end: "" }), true);
  assert.equal(withinDateRange("2025-12-31T23:59:59+08:00", { start: "2026-01-01", end: "" }), false);
  assert.equal(withinDateRange("2026-01-01T00:00:00+08:00", { start: "", end: "2025-12-31" }), false);
  assert.equal(withinDateRange("2025-12-31T23:59:59+08:00", { start: "", end: "2025-12-31" }), true);
  assert.equal(withinDateRange("invalid", { start: "2026-01-01", end: "" }), false);
  assert.equal(withinDateRange("invalid", { start: "", end: "" }), true);
});
