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

// Name of the login popup window. The SPA that loads inside the popup after the login
// uses it to render a short message instead of the whole app (see isLoginPopup).
const LOGIN_POPUP_NAME = 'lms-login'
const LOGIN_POPUP_WIDTH = 500
const LOGIN_POPUP_HEIGHT = 650
const LOGIN_POPUP_POLL_MS = 250

/**
 * How the popup login ended:
 * - 'success': the popup came back to the SPA after a successful login.
 * - 'failed': Spring Boot sent the popup back with ?login_error / ?state=login_error.
 * - 'closed': the user closed the popup (or the browser cut the link to it).
 */
export type PopupLoginResult = 'success' | 'failed' | 'closed'

let pendingPopupLogin: { popup: Window; result: Promise<PopupLoginResult> } | undefined

/**
 * Popups are for desktop browsers. On phones and tablets (touch, narrow screen) the
 * login replaces the whole page instead, as with login().
 */
export function canUseLoginPopup(): boolean {
  return typeof window.matchMedia === 'function' && window.matchMedia('(pointer: fine) and (min-width: 768px)').matches
}

/** True in the popup window that loginWithPopup() opened. */
export function isLoginPopup(): boolean {
  return window.name === LOGIN_POPUP_NAME && window.opener != null
}

/**
 * Opens the Keycloak login in a centred popup window and closes it when the login is
 * done. The popup goes SPA -> Spring Boot -> Keycloak -> Spring Boot -> SPA ("/"); while
 * it is on Keycloak its address cannot be read (other origin), so we poll until it is
 * back on our own origin, then read the result from its address and close it.
 *
 * Returns undefined if the browser blocked the popup; the caller should fall back to
 * login(). Must be called directly from a click handler, or the popup is blocked.
 */
export function loginWithPopup(): Promise<PopupLoginResult> | undefined {
  if (pendingPopupLogin && !pendingPopupLogin.popup.closed) {
    pendingPopupLogin.popup.focus()
    return pendingPopupLogin.result
  }

  const left = Math.max(0, window.screenX + (window.outerWidth - LOGIN_POPUP_WIDTH) / 2)
  const top = Math.max(0, window.screenY + (window.outerHeight - LOGIN_POPUP_HEIGHT) / 2)
  const popup = window.open(
    LOGIN_URL,
    LOGIN_POPUP_NAME,
    `popup=yes,width=${LOGIN_POPUP_WIDTH},height=${LOGIN_POPUP_HEIGHT},left=${left},top=${top}`,
  )
  if (!popup) return undefined
  popup.focus()

  const result = new Promise<PopupLoginResult>(resolve => {
    const finish = (value: PopupLoginResult) => {
      window.clearInterval(timer)
      pendingPopupLogin = undefined
      resolve(value)
    }
    const timer = window.setInterval(() => {
      if (popup.closed) {
        finish('closed')
        return
      }
      let address: URL
      try {
        // Throws while the popup shows Keycloak (other origin).
        address = new URL(popup.location.href)
      } catch {
        return
      }
      // Still about:blank, or on the way through Spring Boot's OAuth2 endpoints.
      if (
        address.origin !== window.location.origin ||
        address.pathname.startsWith('/oauth2/') ||
        address.pathname.startsWith('/login/oauth2/')
      ) {
        return
      }
      popup.close()
      const failed = address.searchParams.has('login_error') || address.searchParams.get('state') === 'login_error'
      finish(failed ? 'failed' : 'success')
    }, LOGIN_POPUP_POLL_MS)
  })

  pendingPopupLogin = { popup, result }
  return result
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
        currentToken = (await response.json()) as TokenResponse
        setLoginRequired(false)
        return currentToken.accessToken
      })
      .finally(() => {
        pendingToken = undefined
      })
  }
  return pendingToken
}
