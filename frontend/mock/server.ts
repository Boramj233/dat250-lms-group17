// Mock backend for `npm run dev:mock`: a Vite plugin that answers the requests that
// normally go to Spring Boot and Keycloak, so the frontend runs without Docker.
//
//   GET  /oauth2/authorization/keycloak  login page: pick a test user (instead of Keycloak)
//   POST /mock/login                     sets the session cookie, back to /
//   GET  /token                          token for the session (401 without one)
//   POST /logout                         clears the session, back to /
//   *    /api/...                        handlers.ts (401 without a valid Bearer token)
//
// data.ts and handlers.ts are loaded through Vite on every request, so edits to them
// apply on the next request without restarting the dev server.
import type { IncomingMessage, ServerResponse } from 'node:http'
import type { Plugin, ViteDevServer } from 'vite'
import type { MockHandler, MockResult, MockUser } from './types.ts'

const SESSION_COOKIE = 'mock_session'
const TOKEN_PREFIX = 'mock-token-'
const TOKEN_LIFETIME_MS = 5 * 60_000

type DataModule = { users: MockUser[], latencyMs: number }
type HandlersModule = { handlers: MockHandler[] }

export function mockBackend(): Plugin {
  return {
    name: 'lms-mock-backend',
    configureServer(server) {
      server.config.logger.info('\n  Mock backend on: log in as a test user from mock/data.ts\n')
      server.middlewares.use((req, res, next) => {
        handle(server, req, res).then(handled => { if (!handled) next() }, next)
      })
    },
  }
}

/** Returns false for requests the mock does not handle (Vite serves those). */
async function handle(server: ViteDevServer, req: IncomingMessage, res: ServerResponse): Promise<boolean> {
  const url = new URL(req.url ?? '/', 'http://localhost')
  const method = req.method ?? 'GET'
  const path = url.pathname

  if (path === '/oauth2/authorization/keycloak' && method === 'GET') {
    const { users } = await load<DataModule>(server, '/mock/data.ts')
    sendHtml(res, loginPage(users))
    return true
  }

  if (path === '/mock/login' && method === 'POST') {
    const form = new URLSearchParams(await readBody(req))
    const { users } = await load<DataModule>(server, '/mock/data.ts')
    const user = users.find(u => String(u.userId) === form.get('userId'))
    if (!user) return redirect(res, '/?login_error')
    res.setHeader('Set-Cookie', `${SESSION_COOKIE}=${user.userId}; Path=/; HttpOnly; SameSite=Lax`)
    return redirect(res, '/')
  }

  if (path === '/logout' && method === 'POST') {
    res.setHeader('Set-Cookie', `${SESSION_COOKIE}=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0`)
    return redirect(res, '/')
  }

  if (path === '/token' && method === 'GET') {
    const user = await userFromSession(server, req)
    if (!user) return sendJson(res, { status: 401, body: { error: 'No session' } })
    return sendJson(res, {
      status: 200,
      body: {
        accessToken: TOKEN_PREFIX + user.userId,
        tokenType: 'Bearer',
        expiresAt: new Date(Date.now() + TOKEN_LIFETIME_MS).toISOString(),
      },
    })
  }

  if (path.startsWith('/api/')) {
    const data = await load<DataModule>(server, '/mock/data.ts')
    if (data.latencyMs > 0) await new Promise(resolve => setTimeout(resolve, data.latencyMs))

    const user = userFromToken(data.users, req.headers.authorization)
    if (!user) return sendJson(res, { status: 401, body: { error: 'Missing or invalid token' } })

    const { handlers } = await load<HandlersModule>(server, '/mock/handlers.ts')
    for (const handler of handlers) {
      const params = matchPath(handler.path, path)
      if (handler.method !== method || !params) continue
      const raw = await readBody(req)
      const result = await handler.handle({
        user,
        params,
        query: url.searchParams,
        body: raw ? JSON.parse(raw) : undefined,
      })
      return sendJson(res, result)
    }
    server.config.logger.warn(`[mock] No handler for ${method} ${path}: add one in mock/handlers.ts`)
    return sendJson(res, { status: 404, body: { error: `No mock handler for ${method} ${path}` } })
  }

  return false
}

/** Loads a module through Vite, so it is TypeScript and reloads when the file changes. */
function load<T>(server: ViteDevServer, path: string): Promise<T> {
  return server.ssrLoadModule(path) as Promise<T>
}

async function userFromSession(server: ViteDevServer, req: IncomingMessage): Promise<MockUser | undefined> {
  const cookie = req.headers.cookie?.split(';')
    .map(part => part.trim().split('='))
    .find(([name]) => name === SESSION_COOKIE)
  if (!cookie?.[1]) return undefined
  const { users } = await load<DataModule>(server, '/mock/data.ts')
  return users.find(u => String(u.userId) === cookie[1])
}

function userFromToken(users: MockUser[], authorization: string | undefined): MockUser | undefined {
  const token = authorization?.match(/^Bearer (.+)$/)?.[1]
  if (!token?.startsWith(TOKEN_PREFIX)) return undefined
  return users.find(u => String(u.userId) === token.slice(TOKEN_PREFIX.length))
}

/** '/api/users/:userId/courses' against '/api/users/1/courses' gives { userId: '1' }. */
function matchPath(pattern: string, path: string): Record<string, string> | undefined {
  const patternParts = pattern.split('/')
  const pathParts = path.split('/')
  if (patternParts.length !== pathParts.length) return undefined
  const params: Record<string, string> = {}
  for (let i = 0; i < patternParts.length; i++) {
    if (patternParts[i].startsWith(':')) params[patternParts[i].slice(1)] = decodeURIComponent(pathParts[i])
    else if (patternParts[i] !== pathParts[i]) return undefined
  }
  return params
}

function readBody(req: IncomingMessage): Promise<string> {
  return new Promise((resolve, reject) => {
    let body = ''
    req.setEncoding('utf8')
    req.on('data', chunk => { body += chunk })
    req.on('end', () => resolve(body))
    req.on('error', reject)
  })
}

function sendJson(res: ServerResponse, result: MockResult): true {
  res.statusCode = result.status
  if (result.body === undefined) {
    res.end()
  } else {
    res.setHeader('Content-Type', 'application/json')
    res.end(JSON.stringify(result.body))
  }
  return true
}

function sendHtml(res: ServerResponse, html: string): true {
  res.statusCode = 200
  res.setHeader('Content-Type', 'text/html; charset=utf-8')
  res.end(html)
  return true
}

function redirect(res: ServerResponse, location: string): true {
  res.statusCode = 302
  res.setHeader('Location', location)
  res.end()
  return true
}

function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, ch => `&#${ch.charCodeAt(0)};`)
}

/** Stands in for Keycloak's login page: one button per test user. */
function loginPage(users: MockUser[]): string {
  const buttons = users.map(user => `
    <button name="userId" value="${user.userId}">
      <strong>${escapeHtml(user.firstName)} ${escapeHtml(user.lastName)}</strong>
      <span>${escapeHtml(user.email)} · ${user.access}</span>
    </button>`).join('')
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Mock login</title>
<style>
  body { margin: 0; min-height: 100svh; display: grid; place-items: center; font-family: system-ui, sans-serif; background: #f2f4f7; color: #1b2a41; }
  main { width: min(26rem, 100% - 2rem); padding: 1.5rem; background: #fff; border-radius: 0.5rem; box-shadow: 0 2px 12px rgb(0 0 0 / 0.1); }
  h1 { margin: 0 0 0.25rem; font-size: 1.25rem; }
  p { margin: 0 0 1rem; font-size: 0.875rem; color: #555; }
  form { display: flex; flex-direction: column; gap: 0.5rem; }
  button { display: flex; flex-direction: column; align-items: flex-start; gap: 0.125rem; padding: 0.75rem 1rem; font: inherit; text-align: left; background: #fff; border: 1px solid #ccd; border-radius: 0.375rem; cursor: pointer; }
  button:hover { background: #e8eef7; }
  button span { font-size: 0.8rem; color: #555; }
  a { display: inline-block; margin-top: 1rem; font-size: 0.875rem; color: #1b2a41; }
</style>
</head>
<body>
<main>
  <h1>Mock login</h1>
  <p>No Keycloak in mock mode. Pick a test user from mock/data.ts.</p>
  <form method="post" action="/mock/login">${buttons}</form>
  <a href="/?login_error">Cancel (simulates a failed login)</a>
</main>
</body>
</html>`
}
