import type { CourseId } from '../../data/shared/types'
import type { EnrolledCourse } from '../../data/courses/types'
import CourseCard from './CourseCard'
import './CourseGrid.css'

type CourseGridProps = {
  /** Shown in the given order. */
  courses: EnrolledCourse[]
  /** Upcoming deadlines per course for the cards; leave out to hide the count. */
  deadlineCount?: (courseId: CourseId) => number | undefined
}

/** Course cards in a responsive grid: several columns on wide screens, one on phones. */
export default function CourseGrid({ courses, deadlineCount }: CourseGridProps) {
  return (
    <ul className="course-grid">
      {courses.map(course => (
        <li key={course.id}>
          <CourseCard course={course} upcomingDeadlines={deadlineCount?.(course.id)} />
        </li>
      ))}
    </ul>
  )
}
