---
permalink: /thoughts/
author_profile: true
---

本页面记录一些个人的想法，有些可以展开查看更多内容。

<ol id="thought-list">
  <li>加载中……</li>
</ol>

<style>
  .thought-content {
    margin-top: 0.25em;
  }

  li {
    margin-bottom: 1em;
  }
</style>

<script>
const dataUrl = "https://gist.githubusercontent.com/NaroZeol/a783c548bd3c7b22578a4bd3748bc9d5/raw/thoughts.json";

fetch(dataUrl)
  .then(response => response.json())
  .then(data => {
    const thoughtList = document.getElementById("thought-list");
    thoughtList.innerHTML = "";

    data
      .filter(item => item.deleted_at === null)
      .forEach(item => {
        const li = document.createElement("li");

        // 显示 excerpt（始终可见）
        const excerptSpan = document.createElement("span");
        excerptSpan.textContent = item.excerpt || item.content;

        // 展开部分（初始隐藏）
        const fullContentDiv = document.createElement("div");
        fullContentDiv.className = "thought-content";
        fullContentDiv.style.display = "none";
        // 处理内容中的换行符，每一处换行作为独立的段落
        var lastParagraph = null;
        paragraphs = item.content.split('\n');
        paragraphs.forEach(paragraph => {
          const p = document.createElement("p");
          p.textContent = paragraph;
          fullContentDiv.appendChild(p);
          lastParagraph = p;
        });

        // 悬浮时显示创建时间和更新时间
        // 点击摘要展开全文或收起全文
        // 如果正文没有内容，则不显示全文展开功能
        excerptSpan.title = 
            `创建时间：${new Date(item.created_at).toLocaleString()}\n` +
            `更新时间：${new Date(item.updated_at).toLocaleString()}`;
        if (item.content && item.content.length > 0) {
            excerptSpan.style.cursor = "pointer";
            excerptSpan.onclick = () => {
                if (fullContentDiv.style.display === "block") {
                    fullContentDiv.style.display = "none";
                } else {
                    fullContentDiv.style.display = "block";
                }
            };
            excerptSpan.title += "\n点击查看更多";
        } else {
            excerptSpan.style.cursor = "text";
        }

        li.appendChild(excerptSpan);
        li.appendChild(fullContentDiv);
        thoughtList.appendChild(li);
      });
  })
  .catch(err => {
    document.getElementById("thought-list").innerHTML = `<li>加载失败：${err.message}</li>`;
  });
</script>
