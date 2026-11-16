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
