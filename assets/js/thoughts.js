(() => {
  const feed = document.querySelector('#thought-feed');
  const search = document.querySelector('#thought-search');
  const more = document.querySelector('#load-more');
  let records = [], visible = 20, loaded = false;
  const element = (tag, text, className) => { const node = document.createElement(tag); node.textContent = text; if (className) node.className = className; return node; };
  function render() {
    const query = search.value.trim().toLowerCase();
    const matches = records.filter(item => `${item.excerpt || ''} ${item.content || ''} ${(item.tags || []).join(' ')}`.toLowerCase().includes(query));
    feed.replaceChildren();
    if (!matches.length) feed.append(element('p', query ? '没有找到相关想法。' : '还没有想法。', 'feed-message'));
    for (const item of matches.slice(0, visible)) {
      const card = element('article', '', 'thought-card');
      const date = element('time', new Date(item.created_at).toLocaleString('zh-CN', {year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit'}));
      date.dateTime = item.created_at;
      card.append(date);
      const body = [item.excerpt, item.content !== item.excerpt ? item.content : ''].filter(Boolean).join('\n\n');
      if (body.length > 400) {
        const details = document.createElement('details');
        details.append(element('summary', item.excerpt || body.slice(0, 160) + '…', 'thought-body'), element('p', body, 'thought-body'));
        card.append(details);
      } else card.append(element('p', body, 'thought-body'));
      card.append(element('div', (item.tags || []).map(tag => `#${tag}`).join('  '), 'tag-list'));
      feed.append(card);
    }
    more.hidden = matches.length <= visible;
    more.textContent = '加载更多';
  }
  async function load() {
    more.disabled = true;
    feed.replaceChildren(element('p', '加载中……', 'feed-message'));
    try {
      // Readers fetch exclusively from Gist. There is deliberately no API fallback.
      const url = new URL(feed.dataset.gist);
      url.searchParams.set('v', Math.floor(Date.now() / 60000));
      const response = await fetch(url, {cache:'no-store'});
      if (!response.ok) throw new Error('unavailable');
      const data = await response.json();
      if (!Array.isArray(data)) throw new Error('invalid format');
      records = data.filter(item => item && !item.deleted_at && typeof item.content === 'string').sort((a,b) => new Date(b.created_at) - new Date(a.created_at));
      loaded = true; render();
    } catch {
      feed.replaceChildren(element('p', '暂时无法从 Gist 加载想法，请稍后重试。', 'feed-message'));
      more.hidden = false; more.textContent = '重试';
    } finally { more.disabled = false; }
  }
  search.addEventListener('input', () => {visible = 20; if (loaded) render();});
  more.addEventListener('click', () => {if (!loaded) load(); else {visible += 20; render();}});
  load();
})();
