---
permalink: /thoughts/
layout: single
title: 想法
---
<div class="thoughts">
  <div id="thought-filters" class="thought-filters" role="group" aria-label="筛选和排序想法" hidden>
    <div class="thought-filter thought-filter--date">
      <label for="thought-date-start">发布日期</label>
      <div id="thought-date-root" class="thought-date-scope"></div>
    </div>
    <div id="thought-tag-filter" class="thought-filter" hidden>
      <label for="thought-tag">标签</label>
      <select id="thought-tag" aria-controls="thought-feed"><option value="">全部标签</option></select>
    </div>
    <div class="thought-filter">
      <label for="thought-sort">排序</label>
      <select id="thought-sort" aria-controls="thought-feed">
        <option value="newest">最新发布</option>
        <option value="oldest">最早发布</option>
        <option value="updated">最近更新</option>
      </select>
    </div>
    <button id="thought-reset" class="thought-reset" type="button" hidden>重置</button>
  </div>
  <p id="thought-count" class="thought-count" role="status" hidden></p>
  <div id="thought-feed" class="thought-feed" data-gist="{{ site.thoughts_gist_url | escape }}" aria-live="polite"><p class="feed-message">加载中……</p></div>
  <button id="load-more" class="btn btn--primary load-more" type="button" hidden>加载更多</button>
  <noscript><p>请启用 JavaScript 以浏览想法。</p></noscript>
</div>
<script src="{{ '/assets/js/thoughts.js' | relative_url }}" type="module"></script>
