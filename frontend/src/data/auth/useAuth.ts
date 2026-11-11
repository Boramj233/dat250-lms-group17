import { useState, useSyncExternalStore } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, AuthenticationRequiredError } from '../shared/apiClient'
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
