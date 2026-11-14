import type { EnrolledCourse } from '../../types'
import { formatTerm } from '../../courseFormat'
import './CourseCard.css'

/** Short overview of one course the user is enrolled in. */
export default function CourseCard({ course }: { course: EnrolledCourse }) {
  return (
    <article className="course-card">
      <h3 className="course-card-title">{course.title}</h3>
      <p className="course-card-term">{formatTerm(course)}</p>
      {course.description && <p className="course-card-description">{course.description}</p>}
    </article>
  )
}
