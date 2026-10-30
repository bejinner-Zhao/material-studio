/**
 * 同源请求：/api/** 与 /files/** 由 next.config.ts 的 rewrites 转发到 Java 后端。
 * Cookie 由浏览器自动携带，前端不接触身份信息。
 */

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

interface ErrorBody {
  code?: string;
  message?: string;
}

async function toApiError(response: Response): Promise<ApiError> {
  let body: ErrorBody | null = null;
  try {
    body = (await response.json()) as ErrorBody;
  } catch {
    body = null;
  }
  return new ApiError(
    response.status,
    body?.code ?? `HTTP_${response.status}`,
    body?.message ?? `请求失败（${response.status}）`,
  );
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, { ...init, credentials: "same-origin" });
  if (!response.ok) {
    throw await toApiError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  get<T>(path: string): Promise<T> {
    return request<T>(path);
  },

  post<T>(path: string, body: unknown): Promise<T> {
    return request<T>(path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
  },

  postForm<T>(path: string, form: FormData): Promise<T> {
    return request<T>(path, { method: "POST", body: form });
  },

  delete(path: string): Promise<void> {
    return request<void>(path, { method: "DELETE" });
  },
};
