import { Link, useParams } from 'react-router'
import { paths } from '../../app/paths'
import { useCourse } from '../../data/courses/hooks/queries/useCourse'
import { formatTerm } from '../components/courseFormat'

/** Start page of one course. For now only the course details from GET /api/courses/{id}. */
export default function CoursePage() {
  const { courseId = '' } = useParams()
  const course = useCourse(courseId)

  return (
    <>
      <p><Link to={paths.home}>← Overview</Link></p>
      {course.isPending && <p role="status">Loading course…</p>}
      {course.error && <p role="alert">Could not load the course.</p>}
      {course.data && (
        <>
          <h1>{course.data.title}</h1>
          <p>{formatTerm(course.data)}</p>
          {course.data.description && <p>{course.data.description}</p>}
        </>
      )}
    </>
  )
}
