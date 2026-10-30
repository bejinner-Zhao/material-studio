export function formatDateTime(value: string | null): string {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(date);
}

const MIME_LABELS: Record<string, string> = {
  "image/jpeg": "JPEG",
  "image/png": "PNG",
  "image/webp": "WebP",
};

export function formatMime(mime: string): string {
  return MIME_LABELS[mime] ?? mime;
}

export function formatMegabytes(bytes: number): string {
  const value = bytes / 1024 / 1024;
  return `${Number.isInteger(value) ? value : value.toFixed(1)} MB`;
}
