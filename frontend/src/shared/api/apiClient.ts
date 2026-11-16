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

/** The API answered with an error status (not 401, which means "log in again"). */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number) {
    super(`HTTP error! Status: ${status}`);
    this.status = status;
  }
}

/** JSON from the API, or undefined for 204 No Content. */
export async function apiClient<T>(
  endpoint: string,
  options?: RequestInit
): Promise<T> {
  const response = await apiFetch(endpoint, options);
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

/**
 * Like apiClient, but null when the API answers 404: for things that may not exist
 * yet, e.g. a grade that is not released.
 */
export async function apiClientOrNull<T>(endpoint: string, options?: RequestInit): Promise<T | null> {
  try {
    return await apiClient<T>(endpoint, options);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

/**
 * Downloads a file from the API (which needs the token, so a plain link cannot) and
 * lets the browser save it, named as the API says or `fallbackName`.
 */
export async function apiDownload(endpoint: string, fallbackName: string): Promise<void> {
  const response = await apiFetch(endpoint);
  const blob = await response.blob();
  const disposition = response.headers.get('Content-Disposition') ?? '';
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  const plain = disposition.match(/filename="?([^";]+)"?/i)?.[1];
  const name = encoded ? decodeURIComponent(encoded) : plain ?? fallbackName;

  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}

/** Sends the request with the token; tries once more with a fresh token on 401. */
async function apiFetch(endpoint: string, options?: RequestInit): Promise<Response> {
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
    throw new ApiError(response.status);
  }
  return response;
}
