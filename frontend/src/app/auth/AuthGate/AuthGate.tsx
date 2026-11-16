import { useCurrentUser, useLoginRequired } from '@shared/auth/useAuth'
import LandingPage from '../LandingPage'
import AppLayout from '../../layout/AppLayout'

/**
 * Chooses the page frame: visitors who are not logged in get the landing page, logged-in
 * users get the app with topbar, sidebar and the page content. The address stays the
 * same, so after a popup login the user is on the page they opened.
 */
export default function AuthGate() {
  const loginRequired = useLoginRequired()
  const currentUser = useCurrentUser()

  if (loginRequired) return <LandingPage />
  if (currentUser.isPending)
    return (
      <p className="auth-status" role="status">
        Loading…
      </p>
    )
  if (currentUser.error) {
    return (
      <p className="auth-status" role="alert">
        Could not load your profile. Try again later.{' '}
        <button type="button" onClick={() => currentUser.refetch()}>
          Try again
        </button>
      </p>
    )
  }
  return <AppLayout />
}
