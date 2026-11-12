import { login } from '../../data/auth/auth'

/** Starts the Keycloak login with a full page navigation (not fetch). */
export default function LoginButton() {
  return <button type="button" onClick={login}>Log in</button>
}
