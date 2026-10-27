import { Link } from 'react-router'
import { paths } from '../paths'

export default function NotFoundPage() {
  return (
    <>
      <h1>Siden finnes ikke</h1>
      <Link to={paths.courses}>Til mine kurs</Link>
    </>
  )
}
