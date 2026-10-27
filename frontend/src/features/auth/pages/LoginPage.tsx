import { Link } from 'react-router'
import { paths } from '../../../app/paths'

export default function LoginPage() {
  return (
    <main className="page-content">
      <h1>Logg inn</h1>
      <p>Innlogging er ikke implementert ennå.</p>
      <Link to={paths.courses}>Gå til mine kurs (utvikling)</Link>
    </main>
  )
}
