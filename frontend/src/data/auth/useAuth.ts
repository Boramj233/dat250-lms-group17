import { useSyncExternalStore } from 'react'
import { useQuery } from '@tanstack/react-query'
import { apiClient, AuthenticationRequiredError } from '../shared/apiClient'
import { isLoginRequired, subscribeToAuth, type CurrentUser } from './auth'

export const currentUserQueryKey = ['currentUser'] as const

/** The logged-in user from GET /api/users/me. */
export function useCurrentUser() {
  return useQuery({
    queryKey: currentUserQueryKey,
    queryFn: () => apiClient<CurrentUser>('/api/users/me'),
    retry: false,
  })
}

/**
 * True when the Login button should be shown: there is no token (no session), or an API
 * call was answered with 401.
 */
export function useLoginRequired(): boolean {
  const loginRequired = useSyncExternalStore(subscribeToAuth, isLoginRequired)
  const currentUser = useCurrentUser()
  return loginRequired || currentUser.error instanceof AuthenticationRequiredError
}
