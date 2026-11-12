import { Link } from 'react-router'
import { paths } from '../../app/paths'
import LoginButton from '../components/LoginButton'

export default function LoginPage() {
  return (
    <>
      <h1>Log in</h1>
      <p>You log in with your Keycloak account.</p>
      <LoginButton />
      <p><Link to={paths.home}>Go to home</Link></p>
    </>
  )
}
