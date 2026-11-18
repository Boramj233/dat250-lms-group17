import type { Access } from '@shared/auth/access'
import type { UserId } from '@shared/types'

// Matches the backend's UserLookupResponse.
export interface UserLookup {
  userId: UserId
  firstName: string
  lastName: string
  email: string
}

// Matches the backend's UserLookupPageResponse.
export interface UserLookupPage {
  users: UserLookup[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// Matches the backend's UserResponse (GET/PUT /api/users/me and the admin endpoints).
export interface UserAccount {
  userId: UserId
  email: string
  firstName: string
  lastName: string
  access: Access
}

// Matches the backend's UpdateUserRequest: PUT /api/users/me. Email and access cannot
// be changed here.
export interface UpdateProfileInput {
  firstName: string
  lastName: string
}

// --- Administrators (AdminUserController, /api/admin/users) ------------------------

// Matches the backend's UserPageResponse.
export interface UserAccountPage {
  users: UserAccount[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ListUsersInput {
  /** Name or email; empty for everyone. */
  search: string
  /** From 0. */
  page: number
  /** 1–100. */
  size?: number
}

// Matches the backend's CreateUserRequest: no password, the account is in Keycloak.
export interface CreateUserInput {
  firstName: string
  lastName: string
  email: string
}

// Matches the backend's UpdateAccessRequest. 403 when an admin removes their own ADMIN.
export interface UpdateAccessInput {
  userId: UserId
  access: Access
}

// Matches the backend's UserImportResponse: one row per data line of the CSV file.
export type UserImportStatus = 'CREATED' | 'ALREADY_EXISTS' | 'DUPLICATE_IN_FILE' | 'INVALID'

export interface UserImportRow {
  line: number
  email: string
  status: UserImportStatus
  /** The new or existing profile's id; null for failed rows. */
  userId: UserId | null
  /** Why the row was rejected; empty unless INVALID. */
  errors: string[]
}

export interface UserImportResult {
  created: number
  skipped: number
  failed: number
  rows: UserImportRow[]
}
