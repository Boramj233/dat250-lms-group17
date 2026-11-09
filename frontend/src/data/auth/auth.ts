// Login state for the SPA. Spring Boot does the OAuth 2.0 login with Keycloak and keeps
// a session (HttpOnly cookie). The SPA exchanges that session for an API token at
// GET /token and sends the token as `Authorization: Bearer <token>` on API calls.

export type CurrentUser = {
  userId: number
  email: string
  firstName: string
  lastName: string
  access: 'STANDARD' | 'COURSE_MANAGER' | 'ADMIN'
}

type TokenResponse = {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: string
}

// Starts the authorization code flow in Spring Boot, which redirects to Keycloak.
export const LOGIN_URL = '/oauth2/authorization/keycloak'

// Fetch a new token this long before the old one expires.
const EXPIRY_MARGIN_MS = 5_000

// The token is kept in memory only (never localStorage): after a reload the SPA fetches
// a new one with the session cookie.
let currentToken: TokenResponse | undefined
let pendingToken: Promise<string | undefined> | undefined

// True once we know that the user must log in (no session, or the API answered 401).
let loginRequired = false
const listeners = new Set<() => void>()

function setLoginRequired(value: boolean) {
  if (loginRequired === value) return
  loginRequired = value
  listeners.forEach(listener => listener())
}

/** For React's useSyncExternalStore: is a Login button needed? */
export function isLoginRequired(): boolean {
  return loginRequired
}

export function subscribeToAuth(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/** Forgets the token; the next API call fetches a new one. */
export function clearToken() {
  currentToken = undefined
}

/** Marks that the user has to log in again (shows the Login button). */
export function requireLogin() {
  clearToken()
  setLoginRequired(true)
}

/**
 * Full page navigation (not fetch) to the login: the browser must follow the redirects
 * to Keycloak's login page and back.
 */
export function login() {
  window.location.href = LOGIN_URL
}

/** Ends the session in Spring Boot and Keycloak (POST /logout, full page navigation). */
export function logout() {
  clearToken()
  const form = document.createElement('form')
  form.method = 'post'
  form.action = '/logout'
  document.body.appendChild(form)
  form.submit()
}

/**
 * Returns the API token, fetching it from GET /token (with the session cookie) when there
 * is none or it is about to expire. Concurrent callers share one request. Resolves to
 * undefined if there is no login session.
 */
export function getAccessToken(): Promise<string | undefined> {
  if (currentToken && Date.parse(currentToken.expiresAt) > Date.now() + EXPIRY_MARGIN_MS) {
    return Promise.resolve(currentToken.accessToken)
  }
  if (!pendingToken) {
    pendingToken = fetch('/token', { credentials: 'same-origin', cache: 'no-store' })
      .then(async response => {
        if (response.status === 401) {
          requireLogin()
          return undefined
        }
        if (!response.ok) throw new Error(`Token request failed: ${response.status}`)
        currentToken = await response.json() as TokenResponse
        setLoginRequired(false)
        return currentToken.accessToken
      })
      .finally(() => { pendingToken = undefined })
  }
  return pendingToken
}
