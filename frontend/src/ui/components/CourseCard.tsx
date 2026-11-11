import { paths } from '../../app/paths'
import type { CourseRole, EnrolledCourse } from '../../data/courses/types'
import { TrailLink } from '../navigation/Trail'
import { formatTerm } from './courseFormat'
import './CourseCard.css'

const ROLE_LABEL: Record<CourseRole, string> = {
  STUDENT: 'Student',
  TEACHER: 'Teacher',
}

type CourseCardProps = {
  course: EnrolledCourse
  /** Number of upcoming deadlines in the course; left out while unknown. */
  upcomingDeadlines?: number
}

/** Short overview of one course the user is enrolled in, with a link to the course. */
export default function CourseCard({ course, upcomingDeadlines }: CourseCardProps) {
  return (
    <article className="course-card">
      <header className="course-card-header">
        <h3 className="course-card-title">{course.title}</h3>
        <span className="course-card-role">{ROLE_LABEL[course.role]}</span>
      </header>
      <p className="course-card-term">{formatTerm(course)}</p>
      {course.description && <p className="course-card-description">{course.description}</p>}
      <footer className="course-card-footer">
        {upcomingDeadlines !== undefined && (
          <span className="course-card-deadlines">
            {upcomingDeadlines === 0
              ? 'No upcoming deadlines'
              : `${upcomingDeadlines} upcoming deadline${upcomingDeadlines === 1 ? '' : 's'}`}
          </span>
        )}
        <TrailLink className="course-card-link" to={paths.course(course.id)}>
          Go to course<span className="visually-hidden"> {course.title}</span>
        </TrailLink>
      </footer>
    </article>
  )
}
