import { Link } from 'react-router'
import { paths } from '../../app/paths'

export default function LoginPage() {
  return (
    <>
      <h1>Log in</h1>
      <p>Login has not been implemented yet.</p>
      <Link to={paths.home}>Go to home (development)</Link>
    </>
  )
}
