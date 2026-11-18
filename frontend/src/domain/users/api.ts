import { apiClient, apiPath, jsonBody } from '@shared/api/apiClient'
import type { UserId } from '@shared/types'
import type {
  CreateUserInput,
  ListUsersInput,
  UpdateAccessInput,
  UpdateProfileInput,
  UserAccount,
  UserAccountPage,
  UserImportResult,
  UserLookupPage,
} from './types'

/** Finds accounts by name or email (backend: COURSE_MANAGER and ADMIN only). */
export function searchUsers(search: string, size = 10): Promise<UserLookupPage> {
  const query = new URLSearchParams({ search, size: String(size) })
  return apiClient<UserLookupPage>(`/api/users/search?${query}`)
}

/** PUT /api/users/me: changes the logged-in user's names. */
export function updateMyProfile(input: UpdateProfileInput): Promise<UserAccount> {
  return apiClient<UserAccount>('/api/users/me', jsonBody('PUT', input))
}

// --- Administrators (AdminUserController) -------------------------------------------

/** GET /api/admin/users: one page of profiles, sorted by id. */
export function listUsers({ search, page, size = 20 }: ListUsersInput): Promise<UserAccountPage> {
  const query = new URLSearchParams({ search, page: String(page), size: String(size) })
  return apiClient<UserAccountPage>(`/api/admin/users?${query}`)
}

/** GET /api/admin/users/{userId}. */
export function getUser(userId: UserId): Promise<UserAccount> {
  return apiClient<UserAccount>(apiPath`/api/admin/users/${userId}`)
}

/** POST /api/admin/users (201). 409 when the email already has a profile. */
export function createUser(input: CreateUserInput): Promise<UserAccount> {
  return apiClient<UserAccount>('/api/admin/users', jsonBody('POST', input))
}

/** POST /api/admin/users/import: a CSV file (part "file"); a report per row. */
export function importUsers(file: File): Promise<UserImportResult> {
  const body = new FormData()
  body.append('file', file)
  // Let fetch set the multipart Content-Type with its boundary.
  return apiClient<UserImportResult>('/api/admin/users/import', { method: 'POST', body })
}

/** PATCH /api/admin/users/{userId}/access. */
export function updateUserAccess({ userId, access }: UpdateAccessInput): Promise<UserAccount> {
  return apiClient<UserAccount>(apiPath`/api/admin/users/${userId}/access`, jsonBody('PATCH', { access }))
}
