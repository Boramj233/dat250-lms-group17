import { Link, useParams } from 'react-router'
import { paths } from '../../../app/paths'

export default function CoursePage() {
  const { courseId } = useParams()

  return (
    <>
      <Link to={paths.courses}>Back to my courses</Link>
      <h1>Course: {courseId}</h1>
      <p>Course content will appear here.</p>
    </>
  )
}
