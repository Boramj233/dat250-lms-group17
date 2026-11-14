import { afterEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { clearToken, getAccessToken, isLoginRequired, login, LOGIN_URL, logout } from '../src/data/auth/auth.ts'
import { apiClient, AuthenticationRequiredError } from '../src/data/apiClient.ts'

const originalFetch = globalThis.fetch
const originalWindow = globalThis.window
const token = (value = 'application-token') => Response.json({
  accessToken: value,
  tokenType: 'Bearer',
  expiresAt: new Date(Date.now() + 900_000).toISOString(),
})

afterEach(() => {
  clearToken()
  globalThis.fetch = originalFetch
  globalThis.window = originalWindow
})

test('login is a full page navigation to the Spring Boot OAuth 2.0 login', () => {
  globalThis.window = { location: { href: 'http://localhost:5173/' } }
  globalThis.fetch = async () => assert.fail('login must not use fetch')
  login()
  assert.equal(globalThis.window.location.href, '/oauth2/authorization/keycloak')
  assert.equal(LOGIN_URL, '/oauth2/authorization/keycloak')
})

test('logout forgets the token and submits a form POST to /logout instead of using fetch', async () => {
  let tokenRequests = 0
  globalThis.fetch = async url => {
    assert.equal(url, '/token', 'logout must not use fetch')
    tokenRequests++
    return token(`token-${tokenRequests}`)
  }
  assert.equal(await getAccessToken(), 'token-1')

  const submitted = []
  const originalDocument = globalThis.document
  globalThis.document = {
    createElement: tag => ({ tag, submit() { submitted.push({ tag, method: this.method, action: this.action }) } }),
    body: { appendChild() {} },
  }
  try {
    logout()
  } finally {
    globalThis.document = originalDocument
  }
  assert.deepEqual(submitted, [{ tag: 'form', method: 'post', action: '/logout' }])
  // The old token is gone from memory: the next call has to ask GET /token again.
  assert.equal(await getAccessToken(), 'token-2')
})

test('concurrent requests share one token fetch and reuse the token in memory', async () => {
  let requests = 0
  globalThis.fetch = async (url, options) => {
    requests++
    assert.equal(url, '/token')
    assert.equal(options.credentials, 'same-origin')
    assert.equal(options.cache, 'no-store')
    return token()
  }
  const values = await Promise.all([getAccessToken(), getAccessToken()])
  assert.deepEqual(values, ['application-token', 'application-token'])
  assert.equal(await getAccessToken(), 'application-token')
  assert.equal(requests, 1)
})

test('API calls send the bearer token without session cookies and preserve other headers', async () => {
  globalThis.fetch = async (url, options) => {
    if (url === '/token') return token()
    assert.equal(url, '/api/users/me')
    assert.equal(options.credentials, 'omit')
    assert.equal(options.headers.get('Authorization'), 'Bearer application-token')
    assert.equal(options.headers.get('Accept'), 'application/json')
    return Response.json({ email: 'student1@example.com' })
  }
  const profile = await apiClient('/api/users/me', { headers: new Headers({ Accept: 'application/json' }) })
  assert.equal(profile.email, 'student1@example.com')
  assert.equal(isLoginRequired(), false)
})

test('an API 401 obtains a fresh token and retries once', async () => {
  let tokens = 0
  let calls = 0
  globalThis.fetch = async (url, options) => {
    if (url === '/token') return token(`token-${++tokens}`)
    calls++
    assert.equal(options.headers.get('Authorization'), `Bearer token-${calls}`)
    return calls === 1 ? new Response(null, { status: 401 }) : Response.json({ userId: 1 })
  }
  assert.deepEqual(await apiClient('/api/users/me'), { userId: 1 })
  assert.equal(calls, 2)
  assert.equal(tokens, 2)
})

test('without a session there is no token, the API is not called and login is required', async () => {
  globalThis.fetch = async url => {
    assert.equal(url, '/token')
    return new Response(null, { status: 401 })
  }
  await assert.rejects(apiClient('/api/users/me'), AuthenticationRequiredError)
  assert.equal(isLoginRequired(), true)
})

test('a second API 401 stops retrying and requires login', async () => {
  let apiCalls = 0
  globalThis.fetch = async url => {
    if (url === '/token') return token()
    apiCalls++
    return new Response(null, { status: 401 })
  }
  await assert.rejects(apiClient('/api/users/me'), AuthenticationRequiredError)
  assert.equal(apiCalls, 2)
  assert.equal(isLoginRequired(), true)
})

test('a forbidden operation is an ordinary error, not a login prompt', async () => {
  let tokenRequests = 0
  globalThis.fetch = async url => {
    if (url === '/token') { tokenRequests++; return token() }
    return new Response(null, { status: 403 })
  }
  await assert.rejects(apiClient('/api/admin/users'), error =>
    !(error instanceof AuthenticationRequiredError) && /403/.test(error.message))
  assert.equal(tokenRequests, 1)
  assert.equal(isLoginRequired(), false)
})
