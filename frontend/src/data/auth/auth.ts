import type Keycloak from 'keycloak-js'

export type CurrentUser = {
  userId: number
  email: string
  firstName: string
  lastName: string
  access: 'STANDARD' | 'COURSE_MANAGER' | 'ADMIN'
}

// Baked in at build time. Defaults match the local Docker Compose / Vite setup.
const keycloakConfig = {
  url: import.meta.env?.VITE_KEYCLOAK_URL ?? 'http://localhost:8081',
  realm: import.meta.env?.VITE_KEYCLOAK_REALM ?? 'lms',
  clientId: import.meta.env?.VITE_KEYCLOAK_CLIENT_ID ?? 'lms-frontend',
}

// The parts of keycloak-js this app uses; tests pass in a fake with the same shape.
export type AuthClient = Pick<Keycloak, 'init' | 'authenticated' | 'token' | 'updateToken' | 'login' | 'logout'>

// keycloak-js keeps the access and refresh tokens in memory only (no localStorage).
let keycloak: AuthClient | undefined

/**
 * Checks for an existing Keycloak session (Authorization Code flow + PKCE) before the
 * app renders. Not logged in is fine here: protected data shows a login link instead.
 */
export async function initAuth(client?: AuthClient): Promise<boolean> {
  if (!client) {
    const { default: KeycloakClient } = await import('keycloak-js')
    client = new KeycloakClient(keycloakConfig)
  }
  keycloak = client
  return client.init({
    onLoad: 'check-sso',
    pkceMethod: 'S256',
    // Checks the Keycloak session in a hidden iframe instead of a full-page redirect.
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    checkLoginIframe: false,
  })
}

export function isAuthenticated(): boolean {
  return keycloak?.authenticated === true
}

/** Redirects to the Keycloak login page and back to the app's start page. */
export function login() {
  void keycloak?.login({ redirectUri: `${window.location.origin}/` })
}

/** Ends the Keycloak session and returns to the app's start page. */
export function logout() {
  void keycloak?.logout({ redirectUri: window.location.origin })
}

/**
 * Returns a valid access token, refreshing it first if it expires within 30 seconds,
 * or undefined if the user is not logged in (or the Keycloak session has ended).
 */
export async function getAccessToken(): Promise<string | undefined> {
  if (!keycloak?.authenticated) return undefined
  try {
    await keycloak.updateToken(30)
  } catch {
    return undefined
  }
  return keycloak.token
}
