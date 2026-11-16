import type { EnrolledCourse } from '../../types'
import CourseCard from '../CourseCard'
import './CourseGrid.css'

/** Course cards in a responsive grid: several columns on wide screens, one on phones. */
export default function CourseGrid({ courses }: { courses: EnrolledCourse[] }) {
  return (
    <ul className="course-grid">
      {courses.map(course => (
        <li key={course.id}>
          <CourseCard course={course} />
        </li>
      ))}
    </ul>
  )
}
