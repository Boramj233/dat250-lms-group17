// Shared fetch helper for all API calls: adds `Authorization: Bearer <token>` so the
// components and query functions never handle the token themselves.
import { clearToken, getAccessToken, requireLogin } from '../auth/auth.ts';

const BASE_URL = import.meta.env?.VITE_API_URL ?? '';

export class AuthenticationRequiredError extends Error {
  constructor() {
    super('Authentication required');
  }
}

/**
 * Builds an API path and URL-encodes every interpolated value, so ids can never
 * break the path: apiPath`/api/courses/${courseId}/modules/${moduleId}`.
 * Not for query strings; build those with URLSearchParams.
 */
export function apiPath(strings: TemplateStringsArray, ...values: (string | number)[]): string {
  return strings.reduce((path, part, i) => path + encodeURIComponent(String(values[i - 1])) + part);
}

/** Request options for sending `body` as JSON: apiClient(path, jsonBody('PATCH', changes)). */
export function jsonBody(method: 'POST' | 'PUT' | 'PATCH', body: unknown): RequestInit {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}

export async function apiClient<T>(
  endpoint: string,
  options?: RequestInit
): Promise<T> {
  const send = (token: string) => {
    const headers = new Headers(options?.headers);
    headers.set('Authorization', `Bearer ${token}`);
    // The token is the only credential for the API; the session cookie is not sent.
    return fetch(`${BASE_URL}${endpoint}`, { ...options, credentials: 'omit', headers });
  };

  let token = await getAccessToken();
  if (!token) throw new AuthenticationRequiredError();
  let response = await send(token);

  if (response.status === 401) {
    // The token expired or the backend restarted: try once with a fresh token.
    clearToken();
    token = await getAccessToken();
    if (!token) throw new AuthenticationRequiredError();
    response = await send(token);
    if (response.status === 401) {
      requireLogin();
      throw new AuthenticationRequiredError();
    }
  }

  if (!response.ok) {
    throw new Error(
      `HTTP error! Status: ${response.status}`
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
