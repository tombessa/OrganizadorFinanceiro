const defaultApiUrl = "https://organizador-financeiro-backend.vercel.app";

export const apiBaseUrl = (import.meta.env.VITE_API_URL || defaultApiUrl).replace(/\/$/, "");

interface ApiErrorBody {
  message?: string;
  details?: string[];
  error?: string;
}

export class ApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
    this.name = "ApiError";
  }
}

export async function apiRequest<T>(accessToken: string, path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Authorization", `Bearer ${accessToken}`);
  if (init.body && !(init.body instanceof FormData)) headers.set("Content-Type", "application/json");

  const response = await fetch(`${apiBaseUrl}${path}`, { ...init, headers });
  if (!response.ok) {
    let body: ApiErrorBody = {};
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      // Respostas sem JSON usam a mensagem HTTP abaixo.
    }
    const details = body.details?.length ? ` (${body.details.join("; ")})` : "";
    throw new ApiError(`${body.message || body.error || `Erro HTTP ${response.status}`}${details}`, response.status);
  }
  return (await response.json()) as T;
}

export async function apiHealth(): Promise<boolean> {
  try {
    const response = await fetch(`${apiBaseUrl}/api/health`);
    return response.ok;
  } catch {
    return false;
  }
}
