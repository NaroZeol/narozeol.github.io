import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

test("the thoughts reader uses Gist without a management-server fallback", () => {
  const source = readFileSync(new URL("../assets/js/thoughts.js", import.meta.url), "utf8");
  const page = readFileSync(new URL("../_pages/thoughts.md", import.meta.url), "utf8");
  assert.ok(source.includes("feed.dataset.gist"));
  assert.ok(source.includes("fetch("));
  assert.ok(!source.includes("/api/"));
  assert.ok(page.includes("thoughts_gist_url"));
});
