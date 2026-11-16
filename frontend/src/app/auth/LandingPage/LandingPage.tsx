import { useLogin } from '@shared/auth/useAuth'
import LoginButton from '../LoginButton'
import './LandingPage.css'

/** What a visitor who is not logged in sees: only a centred Log in button for now. */
export default function LandingPage() {
  const { startLogin, popupOpen, popupFailed } = useLogin()
  // After a full page login (mobile), Spring Boot sends the browser to /?login_error when
  // the login fails. If the LMS refused the user, it first ends the Keycloak session, and
  // Keycloak returns to /?state=login_error (Keycloak does not allow a query on the
  // return address itself).
  const query = new URLSearchParams(window.location.search)
  const redirectFailed = query.has('login_error') || query.get('state') === 'login_error'

  return (
    <main className="landing-page">
      <h1>DAT250 LMS</h1>
      {(popupFailed || (redirectFailed && !popupOpen)) && (
        <p role="alert">Login failed. Check your account and try again.</p>
      )}
      <LoginButton onClick={startLogin} waiting={popupOpen} />
    </main>
  )
}
