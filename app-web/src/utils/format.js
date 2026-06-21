export function timestampMs(value) {
  if (!value) {
    return 0;
  }
  if (typeof value === "number" || /^\d+$/.test(String(value))) {
    const number = Number(value);
    return number > 0 && number < 100000000000 ? number * 1000 : number;
  }
  const parsed = Date.parse(String(value).replace(/-/g, "/"));
  return Number.isFinite(parsed) ? parsed : 0;
}

export function dateTimeText(value) {
  if (!value) {
    return "-";
  }
  if (/^\d+$/.test(String(value))) {
    return new Date(timestampMs(value)).toLocaleString("zh-CN", { hour12: false }).replaceAll("/", "-");
  }
  return String(value).replace("T", " ").slice(0, 19);
}

export function moneyText(value) {
  return Number(value || 0).toFixed(2).replace(/\.00$/, "");
}
