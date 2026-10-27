import { Link, useParams } from 'react-router'
import { paths } from '../../../app/paths'

export default function CoursePage() {
  const { courseId } = useParams()

  return (
    <>
      <Link to={paths.courses}>Tilbake til mine kurs</Link>
      <h1>Kurs: {courseId}</h1>
      <p>Her kommer innholdet i kurset.</p>
    </>
  )
}
