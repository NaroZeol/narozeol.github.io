import { createDatePicker } from "../vendor/date-picker/date-picker.js";
import { withinDateRange } from "./thought-date-picker.mjs";

(() => {
  const feed = document.querySelector("#thought-feed");
  const more = document.querySelector("#load-more");
  const filters = document.querySelector("#thought-filters");
  const tag = document.querySelector("#thought-tag");
  const tagFilter = document.querySelector("#thought-tag-filter");
  const sort = document.querySelector("#thought-sort");
  const reset = document.querySelector("#thought-reset");
  const count = document.querySelector("#thought-count");
  const datePicker = createDatePicker(updateFilters);
  // Use the blog's timezone for both filtering and displayed timestamps.
  const dateFormat = new Intl.DateTimeFormat("zh-CN", {
    timeZone: "Asia/Shanghai",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  });
  const timestamp = (value) => {
    const parsed = Date.parse(value);
    return Number.isFinite(parsed) ? parsed : null;
  };
  const updatedTime = (item) =>
    Math.max(timestamp(item.updated_at) ?? 0, timestamp(item.created_at) ?? 0);
  let records = [],
    visible = 20,
    loaded = false;
  const element = (tag, text, className) => {
    const node = document.createElement(tag);
    node.textContent = text;
    if (className) node.className = className;
    return node;
  };
  function populateFilters() {
    const tags = new Map();
    for (const item of records) {
      for (const value of item.tags)
        tags.set(value, (tags.get(value) || 0) + 1);
    }
    for (const [value, total] of [...tags].sort((a, b) => a[0].localeCompare(b[0], "zh-CN"))) {
      const option = element("option", `${value}（${total}）`);
      option.value = value;
      tag.append(option);
    }
    tagFilter.hidden = tags.size === 0;
    filters.hidden = records.length === 0;
  }
  function render() {
    const matches = records.filter(
      (item) => withinDateRange(item.created_at, datePicker.range)
        && (!tag.value || item.tags.includes(tag.value)),
    );
    matches.sort((a, b) => {
      const created = (timestamp(b.created_at) ?? 0) - (timestamp(a.created_at) ?? 0);
      if (sort.value === "oldest") return -created;
      if (sort.value === "updated") return updatedTime(b) - updatedTime(a) || created;
      return created;
    });
    reset.hidden = !datePicker.range.start && !datePicker.range.end && !tag.value && sort.value === "newest";
    count.hidden = false;
    count.textContent = `共 ${matches.length} 条想法`;
    if (matches.length > visible) count.textContent += `，已显示 ${visible} 条`;
    feed.replaceChildren();
    if (!matches.length)
      feed.append(element("p", records.length ? "这个筛选条件下还没有想法。" : "还没有想法。", "feed-message"));
    for (const item of matches.slice(0, visible)) {
      const card = element("article", "", "thought-card");
      const meta = element("div", "", "thought-meta");
      const created = timestamp(item.created_at);
      const updated = timestamp(item.updated_at);
      if (created !== null) {
        const date = element("time", dateFormat.format(created));
        date.dateTime = item.created_at;
        date.title = "发布时间（北京时间）";
        meta.append(date);
      }
      if (updated !== null && created !== null && updated > created) {
        const label = dateFormat.format(updated) === dateFormat.format(created)
          ? "已编辑"
          : `更新于 ${dateFormat.format(updated)}`;
        const date = element("time", label);
        date.dateTime = item.updated_at;
        date.title = `更新于 ${dateFormat.format(updated)}（北京时间）`;
        meta.append(date);
      }
      card.append(meta);
      const body = [
        item.excerpt,
        item.content !== item.excerpt ? item.content : "",
      ]
        .filter(Boolean)
        .join("\n\n");
      if (body.length > 400) {
        const details = document.createElement("details");
        details.append(
          element(
            "summary",
            item.excerpt || body.slice(0, 160) + "…",
            "thought-body",
          ),
          element("p", body, "thought-body"),
        );
        card.append(details);
      } else card.append(element("p", body, "thought-body"));
      if (item.tags.length) {
        const tags = element("div", "", "tag-list");
        for (const value of item.tags) {
          const button = element("button", `#${value}`, "thought-tag");
          button.type = "button";
          button.setAttribute("aria-label", `筛选标签：${value}`);
          button.addEventListener("click", () => {
            tag.value = value;
            updateFilters();
            tag.focus();
          });
          tags.append(button);
        }
        card.append(tags);
      }
      feed.append(card);
    }
    more.hidden = matches.length <= visible;
    more.textContent = "加载更多";
  }
  async function load() {
    more.disabled = true;
    feed.replaceChildren(element("p", "加载中……", "feed-message"));
    try {
      // Readers fetch exclusively from Gist. There is deliberately no API fallback.
      const url = new URL(feed.dataset.gist);
      url.searchParams.set("v", Math.floor(Date.now() / 60000));
      const response = await fetch(url, { cache: "no-store" });
      if (!response.ok) throw new Error("unavailable");
      const data = await response.json();
      if (!Array.isArray(data)) throw new Error("invalid format");
      records = data
        .filter(
          (item) =>
            item && !item.deleted_at && typeof item.content === "string",
        )
        .map((item) => ({
          ...item,
          tags: [...new Set((Array.isArray(item.tags) ? item.tags : [])
            .filter((value) => typeof value === "string")
            .map((value) => value.trim())
            .filter(Boolean))],
        }));
      loaded = true;
      populateFilters();
      render();
    } catch {
      feed.replaceChildren(
        element("p", "暂时无法加载想法，请稍后重试。", "feed-message"),
      );
      more.hidden = false;
      more.textContent = "重试";
    } finally {
      more.disabled = false;
    }
  }
  function updateFilters() {
    visible = 20;
    if (loaded) render();
  }
  for (const control of [tag, sort])
    control.addEventListener("change", updateFilters);
  reset.addEventListener("click", () => {
    datePicker.reset();
    tag.value = "";
    sort.value = "newest";
    updateFilters();
    document.querySelector("#thought-date-start").focus();
  });
  more.addEventListener("click", () => {
    if (!loaded) load();
    else {
      visible += 20;
      render();
    }
  });
  load();
})();
