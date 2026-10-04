# 个人博客

主题为[minimal-mistakes](https://github.com/mmistakes/minimal-mistakes)

沿用 `177cbd4` 时期的 Minimal Mistakes 默认皮肤和原生布局：首页文章列表、作者侧栏、文章目录及各类归档均由主题提供，不在 `_layouts/` 中另写一套同名模板。

主题固定为 `4.27.3`，采用上游修复解决 `4.26.2` 的归档页脚本空节点异常及 Font Awesome 7 下 `.sr-only` 文本外露问题。

`_config.yml` 管理作者信息和每页 8 篇的首页分页；所有页面通过 `_data/navigation.yml` 统一使用“文章 / 想法 / 关于”三个顶部入口。文章列表上方通过 `_includes/archive-navigation.html` 切换“全部 / 分类 / 标签 / 年份”，首页分页和归档页共用这组入口。分类、标签和年份沿用主题原生列表与数量索引，通过 `assets/js/archive-filter.js` 原地筛选对应文章，导航保持可见；支持清除筛选、分享筛选链接及浏览器前进后退，兼容原有锚点链接，无 JavaScript 时仍可按主题默认方式浏览。完整文章列表位于 `/posts/`，原有文章地址不变。

不提供 RSS：不加载 `jekyll-feed`，并设置 `atom_feed.hide: true`，关闭订阅入口及页面中的 feed 发现链接。构建结果不应包含 `feed.xml`。

阅读时长仍关闭：GitHub Pages 的 Jekyll 3 按空格计词，不能准确估算中文长文。面包屑也未开启，以免主题把现有文章路径中的年月日误识别为分类。评论服务需要单独配置提供方，目前未接入。

自定义样式入口为 `assets/css/main.scss`：导入主题前设置 1440px 的页面上限、220px 的桌面侧栏和 20px 的大屏基础字号，再导入 `_sass/_blog.scss`。后者负责紧凑的文章列表、归档切换和想法筛选。文章保留目录列；首页、归档和普通页面使用无目录的可用宽度。导航和页面文案使用中文。

“想法”从公开 Gist 读取，日期和标签可组合筛选，并可按最新发布、最早发布、最近更新排序；保留展开、加载更多和失败重试，不提供全文搜索。没有标签时隐藏标签筛选；时间显示和日期归属统一使用北京时间。条目显示发布时间及更新时间，内部 ID、版本号和删除标记不进入读者界面。

日期选择直接使用 [Arco Design RangePicker](https://arco.design/react/components/date-picker)（2.66.16，MIT），支持输入或粘贴 `YYYY-MM-DD`、双月日历、年月面板切换、单日范围及最近 7 天 / 最近 30 天 / 本月 / 今年快捷项。手动输入起止日期后按 Enter 确认；日历选完两端或点击快捷项即生效。起止当天均包含在内，快捷范围截止北京时间当天。清除恢复全部日期，Escape 或点击外部放弃未完成的选择。手机端纵向排列日历，浮层内滚动，快捷项固定在底部。

组件仅在想法页加载，样式限定在日期组件内部。源码和锁定依赖位于 `tools/date-picker/`；本地生成的 JS、CSS 与许可证保存在 `assets/vendor/date-picker/`，访问博客不依赖组件 CDN，也不要求 Jekyll 构建时运行 npm。修改组件后更新产物：

```sh
cd tools/date-picker
npm ci
npm run build
# 先启动博客预览；首次运行需 npx playwright install chromium
BLOG_PREVIEW_URL=http://localhost:4000 npm test
```


本地检查：

```sh
bundle install
bundle exec jekyll serve
node --check assets/js/thoughts.js
node --check assets/js/thought-date-picker.mjs
node --test tests/*.test.mjs
```
