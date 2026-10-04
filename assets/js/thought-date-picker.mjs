const dayFormat = new Intl.DateTimeFormat("en-CA", {
  timeZone: "Asia/Shanghai", year: "numeric", month: "2-digit", day: "2-digit",
});

export function publicationDay(value) {
  const timestamp = Date.parse(value);
  if (!Number.isFinite(timestamp)) return "";
  const parts = Object.fromEntries(dayFormat.formatToParts(timestamp).map(({ type, value }) => [type, value]));
  return `${parts.year}-${parts.month}-${parts.day}`;
}

export function withinDateRange(value, { start, end }) {
  if (!start && !end) return true;
  const day = publicationDay(value);
  return Boolean(day) && (!start || day >= start) && (!end || day <= end);
}
