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

// Keep API tokens in memory. On reload, the HttpOnly login session can obtain a new one.
let currentToken: TokenResponse | undefined
let pendingToken: Promise<string | undefined> | undefined

export function clearToken() {
  currentToken = undefined
}

export function login() {
  window.location.assign('/oauth2/authorization/keycloak')
}

export function getAccessToken(): Promise<string | undefined> {
  if (currentToken && Date.parse(currentToken.expiresAt) > Date.now() + 5000) {
    return Promise.resolve(currentToken.accessToken)
  }
  if (!pendingToken) {
    pendingToken = fetch('/token', { credentials: 'same-origin', cache: 'no-store' })
      .then(async response => {
        if (response.status === 401) {
          clearToken()
          return undefined
        }
        if (!response.ok) throw new Error(`Token request failed: ${response.status}`)
        currentToken = await response.json() as TokenResponse
        return currentToken.accessToken
      })
      .finally(() => { pendingToken = undefined })
  }
  return pendingToken
}
