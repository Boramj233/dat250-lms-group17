import { afterEach, beforeEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { getAccessToken, initAuth, logout } from '../src/data/auth/auth.ts'
import { apiClient, AuthenticationRequiredError } from '../src/data/shared/apiClient.ts'

const originalFetch = globalThis.fetch
const originalWindow = globalThis.window

// Stands in for keycloak-js: records calls and lets each test choose the outcome.
function fakeKeycloak({ authenticated = true, token = 'keycloak-token', refreshFails = false } = {}) {
  return {
    authenticated,
    token,
    calls: [],
    async init(options) { this.calls.push(['init', options]); return this.authenticated },
    async updateToken(minValidity) {
      this.calls.push(['updateToken', minValidity])
      if (refreshFails) throw new Error('session ended')
      return false
    },
    async login(options) { this.calls.push(['login', options]) },
    async logout(options) { this.calls.push(['logout', options]) },
  }
}

beforeEach(() => {
  globalThis.window = { location: { origin: 'http://localhost:8080' } }
})

afterEach(() => {
  globalThis.fetch = originalFetch
  globalThis.window = originalWindow
})

test('init uses check-sso with PKCE S256', async () => {
  const keycloak = fakeKeycloak()
  await initAuth(keycloak)
  const [, options] = keycloak.calls.find(([name]) => name === 'init')
  assert.equal(options.onLoad, 'check-sso')
  assert.equal(options.pkceMethod, 'S256')
})

test('API calls refresh the token first and send it as a bearer header without cookies', async () => {
  const keycloak = fakeKeycloak()
  await initAuth(keycloak)
  globalThis.fetch = async (url, options) => {
    assert.deepEqual(keycloak.calls.at(-1), ['updateToken', 30])
    assert.equal(url, '/api/users/me')
    assert.equal(options.credentials, 'omit')
    assert.equal(options.headers.get('Authorization'), 'Bearer keycloak-token')
    assert.equal(options.headers.get('Accept'), 'application/json')
    return Response.json({ email: 'student1@example.com' })
  }
  const profile = await apiClient('/api/users/me', { headers: new Headers({ Accept: 'application/json' }) })
  assert.equal(profile.email, 'student1@example.com')
})

test('a logged-out user gets no token and the API is not called', async () => {
  await initAuth(fakeKeycloak({ authenticated: false }))
  globalThis.fetch = async () => assert.fail('the API must not be called without a token')
  assert.equal(await getAccessToken(), undefined)
  await assert.rejects(apiClient('/api/users/me'), AuthenticationRequiredError)
})

test('a failed token refresh (ended session) asks for login', async () => {
  await initAuth(fakeKeycloak({ refreshFails: true }))
  globalThis.fetch = async () => assert.fail('the API must not be called with an expired token')
  await assert.rejects(apiClient('/api/users/me'), AuthenticationRequiredError)
})

test('an API 401 asks for login', async () => {
  await initAuth(fakeKeycloak())
  globalThis.fetch = async () => new Response(null, { status: 401 })
  await assert.rejects(apiClient('/api/users/me'), AuthenticationRequiredError)
})

test('a forbidden operation is an ordinary error, not a login prompt', async () => {
  await initAuth(fakeKeycloak())
  globalThis.fetch = async () => new Response(null, { status: 403 })
  await assert.rejects(apiClient('/api/admin/users'), error =>
    !(error instanceof AuthenticationRequiredError) && /403/.test(error.message))
})

test('logout ends the Keycloak session and returns to the app', async () => {
  const keycloak = fakeKeycloak()
  await initAuth(keycloak)
  logout()
  assert.deepEqual(keycloak.calls.at(-1), ['logout', { redirectUri: 'http://localhost:8080' }])
})
