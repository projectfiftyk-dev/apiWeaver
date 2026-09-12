import type {
  ApiErrorResponse,
  ChainRequest,
  ChainResponse,
  ChainRunResponse,
  HttpTemplateRequest,
  HttpTemplateResponse,
} from "./types";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...init,
  });

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;
    try {
      const body = (await response.json()) as ApiErrorResponse;
      message = body.message ?? body.detail ?? body.title ?? message;
      if (body.errors?.length) {
        message = body.errors
          .map((e) => [e.field, e.defaultMessage].filter(Boolean).join(": "))
          .join("; ");
      }
    } catch {
      // response had no JSON body; keep the generic message
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  templates: {
    list: () => request<HttpTemplateResponse[]>("/http-templates"),
    get: (id: number) => request<HttpTemplateResponse>(`/http-templates/${id}`),
    create: (body: HttpTemplateRequest) =>
      request<HttpTemplateResponse>("/http-templates", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    update: (id: number, body: HttpTemplateRequest) =>
      request<HttpTemplateResponse>(`/http-templates/${id}`, {
        method: "PUT",
        body: JSON.stringify(body),
      }),
    delete: (id: number) =>
      request<void>(`/http-templates/${id}`, { method: "DELETE" }),
  },
  chains: {
    list: () => request<ChainResponse[]>("/chains"),
    get: (id: number) => request<ChainResponse>(`/chains/${id}`),
    create: (body: ChainRequest) =>
      request<ChainResponse>("/chains", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    update: (id: number, body: ChainRequest) =>
      request<ChainResponse>(`/chains/${id}`, {
        method: "PUT",
        body: JSON.stringify(body),
      }),
    delete: (id: number) => request<void>(`/chains/${id}`, { method: "DELETE" }),
    run: (id: number) =>
      request<ChainRunResponse>(`/chains/${id}/run`, { method: "POST" }),
  },
};
