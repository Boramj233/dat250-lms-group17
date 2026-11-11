// Types shared by the mock backend (server.ts, Node) and the mock data and handlers
// (data.ts, handlers.ts). No imports here: server.ts must not depend on src/.

/** A test user you can log in as on the mock login page. */
export type MockUser = {
  userId: number
  email: string
  firstName: string
  lastName: string
  access: 'STANDARD' | 'COURSE_MANAGER' | 'ADMIN'
}

export type MockResult = {
  status: number
  body?: unknown
}

export type MockRequest = {
  /** The logged-in user (from the Bearer token). */
  user: MockUser
  /** Values from `:name` parts of the path. */
  params: Record<string, string>
  query: URLSearchParams
  /** Parsed JSON body, if any. */
  body: unknown
}

export type MockHandler = {
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  /** Path with `:name` parameters, e.g. '/api/users/:userId/courses'. */
  path: string
  handle: (request: MockRequest) => MockResult | Promise<MockResult>
}

export const ok = (body: unknown): MockResult => ({ status: 200, body })
export const created = (body: unknown): MockResult => ({ status: 201, body })
export const noContent = (): MockResult => ({ status: 204 })
export const forbidden = (): MockResult => ({ status: 403, body: { error: 'Forbidden' } })
export const notFound = (): MockResult => ({ status: 404, body: { error: 'Not found' } })
