import { loadAuth } from './storage';

export type HttpError = {
  status: number;
  message: string;
  details?: unknown;
};

const API_ORIGIN = (import.meta.env.VITE_API_ORIGIN as string | undefined) ?? '';

function buildUrl(path: string) {
  if (!path.startsWith('/')) return `${API_ORIGIN}/${path}`;
  return `${API_ORIGIN}${path}`;
}

export async function httpJson<TResponse>(
  path: string,
  options: RequestInit & { json?: unknown; auth?: boolean } = {}
): Promise<TResponse> {
  const headers = new Headers(options.headers);
  headers.set('Accept', 'application/json');

  if (options.json !== undefined) {
    headers.set('Content-Type', 'application/json');
  }

  if (options.auth !== false) {
    const auth = loadAuth();
    if (auth?.token) {
      headers.set('Authorization', `Bearer ${auth.token}`);
    }
  }

  const res = await fetch(buildUrl(path), {
    ...options,
    headers,
    body: options.json !== undefined ? JSON.stringify(options.json) : options.body
  });

  const contentType = res.headers.get('content-type') ?? '';
  const isJson = contentType.includes('application/json');

  if (!res.ok) {
    let details: unknown = undefined;
    let message = res.statusText || 'Request failed';
    if (isJson) {
      try {
        details = await res.json();
        if (
          details &&
          typeof details === 'object' &&
          'message' in (details as Record<string, unknown>) &&
          typeof (details as Record<string, unknown>).message === 'string'
        ) {
          message = String((details as Record<string, unknown>).message);
        }
      } catch {
        // ignore
      }
    } else {
      try {
        message = await res.text();
      } catch {
        // ignore
      }
    }
    const err: HttpError = { status: res.status, message, details };
    throw err;
  }

  if (res.status === 204) {
    return undefined as TResponse;
  }

  if (!isJson) {
    const text = await res.text();
    return text as unknown as TResponse;
  }

  return (await res.json()) as TResponse;
}

