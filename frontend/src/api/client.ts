export type FieldErrors = Record<string, string>;

export class ApiError extends Error {
  status: number;
  code: string;
  fields: FieldErrors;

  constructor(status: number, code: string, message: string, fields: FieldErrors) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = fields;
  }
}

function readCookie(name: string): string {
  const prefix = `${name}=`;
  const found = document.cookie.split("; ").find((row) => row.startsWith(prefix));
  return found ? decodeURIComponent(found.slice(prefix.length)) : "";
}

export async function ensureCsrf(): Promise<void> {
  await fetch("/api/auth/csrf", { credentials: "include" });
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const csrf = readCookie("XSRF-TOKEN");
  if (csrf) {
    headers.set("X-XSRF-TOKEN", csrf);
  }
  let response: Response;
  try {
    response = await fetch(path, { ...options, headers, credentials: "include" });
  } catch {
    throw new ApiError(0, "NETWORK", "通信に失敗しました", {});
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  const data = text ? (JSON.parse(text) as { code?: string; message?: string; fields?: FieldErrors }) : null;
  if (!response.ok) {
    throw new ApiError(
      response.status,
      data?.code ?? "ERROR",
      data?.message ?? "処理に失敗しました",
      data?.fields ?? {},
    );
  }
  return data as T;
}
