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
  /** Send this file instead of JSON (a download), like the backend's file endpoints. */
  download?: { name: string; mediaType: string; content: Uint8Array }
}

/** A file part of a multipart request. */
export type MockUpload = { field: string; name: string; mediaType: string; content: Uint8Array }

/** The body of a multipart/form-data request (e.g. a PDF submission). */
export type MockMultipart = { fields: Record<string, string>; files: MockUpload[] }

export type MockRequest = {
  /** The logged-in user (from the Bearer token). */
  user: MockUser
  /** Values from `:name` parts of the path. */
  params: Record<string, string>
  query: URLSearchParams
  /** Parsed JSON body, or MockMultipart for multipart/form-data; undefined without a body. */
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
export const conflict = (detail: string): MockResult => ({ status: 409, body: { detail } })
