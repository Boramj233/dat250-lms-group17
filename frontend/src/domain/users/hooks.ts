import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useInvalidatingMutation } from '@shared/api/useInvalidatingMutation'
import type { UserId } from '@shared/types'
import { createUser, getUser, importUsers, listUsers, searchUsers, updateMyProfile, updateUserAccess } from './api'
import type { ListUsersInput } from './types'

export const userKeys = {
  all: ['users'] as const,
  search: (text: string) => ['users', 'search', text] as const,
  adminList: (input: ListUsersInput) => ['users', 'admin', 'list', input] as const,
  admin: (userId: UserId) => ['users', 'admin', String(userId)] as const,
}

/** The same key as useCurrentUser in shared/auth/useAuth.ts. */
const currentUserKey = ['currentUser'] as const

// Accounts matching a search text, for adding members to a course. Waits until the
// text has at least two characters.
export function useUserSearch(search: string) {
  const text = search.trim()
  return useQuery({
    queryKey: userKeys.search(text),
    queryFn: () => searchUsers(text),
    enabled: text.length >= 2,
    staleTime: 30_000,
  })
}

// Changes the logged-in user's names; the user shown everywhere is replaced at once.
export function useUpdateMyProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateMyProfile,
    onSuccess: user => {
      queryClient.setQueryData(currentUserKey, user)
      // Names appear in member lists and search results too.
      void queryClient.invalidateQueries({ queryKey: userKeys.all })
    },
  })
}

// --- Administrators -------------------------------------------------------------------

// One page of profiles; the previous page stays on screen while the next one loads.
export function useAdminUsers(input: ListUsersInput) {
  return useQuery({
    queryKey: userKeys.adminList(input),
    queryFn: () => listUsers(input),
    placeholderData: keepPreviousData,
  })
}

export function useAdminUser(userId: UserId | null) {
  return useQuery({
    queryKey: userKeys.admin(userId ?? ''),
    queryFn: () => getUser(userId!),
    enabled: userId !== null,
  })
}

export function useCreateUser() {
  return useInvalidatingMutation(createUser, () => [userKeys.all])
}

export function useImportUsers() {
  return useInvalidatingMutation(importUsers, () => [userKeys.all])
}

// Also reloads the logged-in user, in case it was their own access.
export function useUpdateUserAccess() {
  return useInvalidatingMutation(updateUserAccess, () => [userKeys.all, currentUserKey])
}
