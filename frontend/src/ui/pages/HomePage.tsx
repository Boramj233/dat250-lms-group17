import { useCurrentUser, useLoginRequired } from '../../data/auth/useAuth'
import LoginButton from '../components/LoginButton'

export default function HomePage() {
  const loginRequired = useLoginRequired()
  const profile = useCurrentUser()
  // Spring Boot sends the browser to /?login_error when the login fails. If the LMS
  // refused the user, it first ends the Keycloak session, and Keycloak returns to
  // /?state=login_error (Keycloak does not allow a query on the return address itself).
  const query = new URLSearchParams(window.location.search)
  const loginFailed = query.has('login_error') || query.get('state') === 'login_error'
  return (
    <>
      <h1>Welcome to DAT250 LMS</h1>
      {loginFailed && <p role="alert">Login failed. Check your account and try again.</p>}
      {loginRequired ? (
        <p><LoginButton /> to view your courses.</p>
      ) : (
        <>
          {profile.isPending && <p>Loading profile...</p>}
          {profile.data && (
            <p>Logged in as {profile.data.firstName} {profile.data.lastName} ({profile.data.email})</p>
          )}
          {profile.error && <p role="alert">Could not load your profile. Try again later.</p>}
        </>
      )}
    </>
  )
}
