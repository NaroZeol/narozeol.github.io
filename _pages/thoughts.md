---
permalink: /thoughts/
layout: default
title: 想法
---
<header class="page-heading"><p class="eyebrow">NOTES</p><h1>想法<span class="brand-dot">.</span></h1><p>本页面记录一些个人的想法，有些可以展开查看更多内容。</p></header>
<div class="thoughts-toolbar"><input id="thought-search" type="search" aria-label="搜索想法" placeholder="搜索内容或标签"><a href="{{ site.thoughts_manage_url }}">管理想法 ↗</a></div>
<div id="thought-feed" class="thought-feed" data-gist="{{ site.thoughts_gist_url }}" aria-live="polite"><p class="feed-message">加载中……</p></div>
<button id="load-more" class="load-more" hidden>加载更多</button>
<script src="{{ '/assets/js/thoughts.js' | relative_url }}" defer></script>
