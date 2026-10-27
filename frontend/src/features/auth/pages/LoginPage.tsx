import { Link } from 'react-router'
import { paths } from '../../../app/paths'

export default function LoginPage() {
  return (
    <main className="page-content">
      <h1>Log in</h1>
      <p>Login has not been implemented yet.</p>
      <Link to={paths.courses}>Go to my courses (development)</Link>
    </main>
  )
}
