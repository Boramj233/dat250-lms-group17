import { afterEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { clearToken, getAccessToken } from '../src/data/auth/auth.ts'
import { apiClient, AuthenticationRequiredError } from '../src/data/shared/apiClient.ts'

const originalFetch = globalThis.fetch
const token = (value = 'application-token') => Response.json({
  accessToken: value,
  tokenType: 'Bearer',
  expiresAt: new Date(Date.now() + 900_000).toISOString(),
})

afterEach(() => {
  clearToken()
  globalThis.fetch = originalFetch
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

test('an anonymous token request asks for login without calling the API', async () => {
  globalThis.fetch = async url => {
    assert.equal(url, '/token')
    return new Response(null, { status: 401 })
  }
  await assert.rejects(apiClient('/api/users/me'), AuthenticationRequiredError)
})

test('a second API 401 stops retrying and asks for login', async () => {
  let apiCalls = 0
  globalThis.fetch = async url => {
    if (url === '/token') return token()
    apiCalls++
    return new Response(null, { status: 401 })
  }
  await assert.rejects(apiClient('/api/users/me'), AuthenticationRequiredError)
  assert.equal(apiCalls, 2)
})

test('a forbidden operation does not trigger another token request', async () => {
  let tokenRequests = 0
  globalThis.fetch = async url => {
    if (url === '/token') { tokenRequests++; return token() }
    return new Response(null, { status: 403 })
  }
  await assert.rejects(apiClient('/api/admin/users'), /403/)
  assert.equal(tokenRequests, 1)
})
