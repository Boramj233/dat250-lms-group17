import { useState, useSyncExternalStore } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, AuthenticationRequiredError } from '../api/apiClient'
import { hasAccess, type Access } from './access'
import {
  canUseLoginPopup,
  clearToken,
  getAccessToken,
  isLoginRequired,
  login,
  loginWithPopup,
  subscribeToAuth,
  type CurrentUser,
} from './auth'

const currentUserQueryKey = ['currentUser'] as const

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

/**
 * True if the logged-in user has `required` global access or higher (ADMIN includes
 * COURSE_MANAGER, which includes STANDARD). False while the user is loading or missing.
 */
export function useHasAccess(required: Access): boolean {
  return hasAccess(useCurrentUser().data?.access, required)
}

/**
 * Starts the login. On desktop the Keycloak login opens in a popup and this page stays;
 * when the popup is done we fetch a token with the new session and reload the user. On
 * mobile (or if the popup is blocked) the whole page navigates to the login instead.
 */
export function useLogin() {
  const queryClient = useQueryClient()
  const [popupOpen, setPopupOpen] = useState(false)
  const [popupFailed, setPopupFailed] = useState(false)

  function startLogin() {
    const popupLogin = canUseLoginPopup() ? loginWithPopup() : undefined
    if (!popupLogin) {
      login()
      return
    }
    setPopupOpen(true)
    setPopupFailed(false)
    popupLogin
      .then(async result => {
        // Check the session even when the popup was just closed: the browser may have
        // cut the link to it after a successful login.
        clearToken()
        const token = await getAccessToken()
        await queryClient.resetQueries({ queryKey: currentUserQueryKey })
        setPopupFailed(result === 'failed' || (result === 'success' && !token))
      })
      .catch(() => setPopupFailed(true))
      .finally(() => setPopupOpen(false))
  }

  return { startLogin, popupOpen, popupFailed }
}
