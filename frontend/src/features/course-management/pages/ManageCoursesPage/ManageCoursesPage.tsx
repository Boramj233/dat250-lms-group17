import { paths } from '../../../../shared/paths'
import { useCurrentUser } from '../../../../shared/auth/useAuth'
import { useMyCourses } from '../../../courses/hooks'
import QueryState from '../../../../shared/ui/QueryState'
import { byNewest, formatTerm } from '../../../courses/courseFormat'
import { TrailLink, TrailPage } from '../../../../shared/navigation/Trail'
import './ManageCoursesPage.css'

/**
 * Course managers: create a course, or pick one of the courses they teach to manage
 * its content, members and details. Only teachers of a course may change it, so
 * other courses are not listed.
 */
export default function ManageCoursesPage() {
  const user = useCurrentUser().data!
  const courses = useMyCourses(user.userId)

  return (
    <TrailPage label="Manage courses">
      <div className="manage-courses">
        <header className="manage-courses-header">
          <h1>Manage courses</h1>
          <TrailLink className="button-primary" to={paths.newCourse}>Create new course</TrailLink>
        </header>

        <section aria-labelledby="manage-courses-list">
          <h2 id="manage-courses-list" className="section-title">Your courses</h2>
          <QueryState
            query={courses}
            loading="Loading courses…"
            error="Could not load your courses. Try again later."
            empty="You do not teach any courses yet."
            isEmpty={all => !all.some(course => course.role === 'TEACHER')}
          >
            {all => (
              <ul className="manage-courses-list">
                {all.filter(course => course.role === 'TEACHER').sort(byNewest).map(course => (
                  <li key={course.id} className="manage-courses-item">
                    <div className="manage-courses-name">
                      <span className="manage-courses-title">{course.title}</span>
                      <span className="manage-courses-term">{formatTerm(course)}</span>
                    </div>
                    <TrailLink className="button-secondary" to={paths.manageCourse(course.id)}>
                      Manage<span className="visually-hidden"> {course.title}</span>
                    </TrailLink>
                  </li>
                ))}
              </ul>
            )}
          </QueryState>
        </section>
      </div>
    </TrailPage>
  )
}
