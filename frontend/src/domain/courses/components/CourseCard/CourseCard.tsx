import type { EnrolledCourse } from '../../types'
import { TrailLink } from '@shared/navigation/Trail'
import { paths } from '@shared/paths'
import { formatTerm } from '../../courseFormat'
import './CourseCard.css'

/** Short overview of one course the user is enrolled in; the whole card opens the course. */
export default function CourseCard({ course }: { course: EnrolledCourse }) {
  return (
    <article className="course-card">
      <h3 className="course-card-title">
        {/* Stretched over the card (CSS), so the whole card is one link. */}
        <TrailLink className="course-card-link" to={paths.course(course.id)}>
          {course.title}
        </TrailLink>
      </h3>
      <p className="course-card-term">{formatTerm(course)}</p>
      {course.description && <p className="course-card-description">{course.description}</p>}
    </article>
  )
}
