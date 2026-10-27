import { Link } from 'react-router'
import { coursePath } from '../../../app/paths'

export default function CourseListPage() {
  // Temporary example until courses are loaded from the backend.
  return (
    <>
      <h1>Mine kurs</h1>
      <Link to={coursePath('dat250')}>Åpne DAT250</Link>
    </>
  )
}
