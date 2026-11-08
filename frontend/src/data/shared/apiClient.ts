//shared api client for all api calls, using fetch and returning the response as json
import { clearToken, getAccessToken } from '../auth/auth.ts';

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
    return fetch(`${BASE_URL}${endpoint}`, { ...options, credentials: 'omit', headers });
  };
  let token = await getAccessToken();
  if (!token) throw new AuthenticationRequiredError();
  let response = await send(token);
  if (response.status === 401) {
    clearToken();
    token = await getAccessToken();
    if (!token) throw new AuthenticationRequiredError();
    response = await send(token);
    if (response.status === 401) {
      clearToken();
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
