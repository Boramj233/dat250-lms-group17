// Shared fetch helper for all API calls: adds `Authorization: Bearer <token>` so the
// components and query functions never handle the token themselves.
import { clearToken, getAccessToken, requireLogin } from './auth/auth.ts';

const BASE_URL = import.meta.env?.VITE_API_URL ?? '';

export class AuthenticationRequiredError extends Error {
  constructor() {
    super('Authentication required');
  }
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
