//shared api client for all api calls, using fetch and returning the response as json
import { getAccessToken } from '../auth/auth.ts';

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
  // Refreshes the Keycloak access token first if it is about to expire.
  const token = await getAccessToken();
  if (!token) throw new AuthenticationRequiredError();

  const headers = new Headers(options?.headers);
  headers.set('Authorization', `Bearer ${token}`);
  // The token in the header is the only credential; never send cookies to the API.
  const response = await fetch(`${BASE_URL}${endpoint}`, { ...options, credentials: 'omit', headers });

  if (response.status === 401) {
    throw new AuthenticationRequiredError();
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
