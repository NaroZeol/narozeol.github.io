(() => {
  const archive = document.querySelector('.archive');
  const index = archive?.querySelector('.taxonomy__index');
  const sections = [...(archive?.querySelectorAll('.taxonomy__section') || [])];
  if (!index || !sections.length) return;

  const ids = new Set(sections.map(section => section.id));
  const links = [...index.querySelectorAll('a')];
  const currentTab = archive.querySelector('.archive-navigation [aria-current="page"]');

  function targetURL(id) {
    const url = new URL(location.href);
    url.hash = '';
    if (id) url.searchParams.set('filter', id);
    else url.searchParams.delete('filter');
    return url.pathname + url.search;
  }

  // Preserve existing links from article metadata and bookmarks without
  // letting a fragment jump scroll the navigation off screen.
  let initialHash = '';
  try { initialHash = decodeURIComponent(location.hash.slice(1)); } catch {}
  if (!new URL(location.href).searchParams.has('filter') && ids.has(initialHash))
    history.replaceState(history.state, '', targetURL(initialHash));

  for (const link of links) {
    const id = decodeURIComponent(link.hash.slice(1));
    link.dataset.taxonomy = id;
    link.href = targetURL(id);
  }

  const all = document.createElement('a');
  all.textContent = `全部${currentTab.textContent.trim()}`;
  all.dataset.taxonomy = '';
  all.href = targetURL('');
  const item = document.createElement('li');
  item.append(all);
  index.prepend(item);
  links.unshift(all);

  function render() {
    const requested = new URL(location.href).searchParams.get('filter');
    const selected = ids.has(requested) ? requested : '';
    for (const section of sections) section.hidden = Boolean(selected && section.id !== selected);
    for (const link of links) {
      if (link.dataset.taxonomy === selected) link.setAttribute('aria-current', 'true');
      else link.removeAttribute('aria-current');
    }
    archive.dataset.taxonomyFiltered = String(Boolean(selected));
  }

  index.addEventListener('click', event => {
    const link = event.target.closest('a[data-taxonomy]');
    if (!link || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    event.stopPropagation();
    if (link.href !== location.href) history.pushState(null, '', link.href);
    render();
    window.scrollTo({ top: 0, behavior: 'instant' });
  });
  window.addEventListener('popstate', render);
  render();
  if (ids.has(initialHash)) window.scrollTo({ top: 0, behavior: 'instant' });
})();
