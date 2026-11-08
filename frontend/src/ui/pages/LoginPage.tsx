import { Link } from 'react-router'
import { paths } from '../../app/paths'
import { login } from '../../data/auth/auth'

export default function LoginPage() {
  return (
    <>
      <h1>Log in</h1>
      {new URLSearchParams(window.location.search).has('error') && (
        <p role="alert">Login failed. Check your account and try again.</p>
      )}
      <button type="button" onClick={login}>Log in with Keycloak</button>
      <p><Link to={paths.home}>Go to home</Link></p>
    </>
  )
}
