import { paths } from '@shared/paths'
import { useCurrentUser } from '@shared/auth/useAuth'
import { useMyCourses } from '@domain/courses/hooks'
import type { EnrolledCourse } from '@domain/courses/types'
import QueryState from '@shared/ui/QueryState'
import CourseGrid from '@domain/courses/components/CourseGrid'
import { byNewest } from '@domain/courses/courseFormat'
import { TrailLink } from '@shared/navigation/Trail'

const teaching = (courses: EnrolledCourse[]) =>
  courses.filter(course => course.role === 'TEACHER').sort(byNewest)

/**
 * Course managers: the courses they teach (newest first), and a button to the page
 * where they create courses or edit the ones they teach.
 */
export default function CourseManagerDashboard() {
  const user = useCurrentUser().data!
  const courses = useMyCourses(user.userId)

  return (
    <section className="dashboard-section" aria-labelledby="dashboard-teaching">
      <header className="dashboard-section-header">
        <h2 id="dashboard-teaching">Courses I teach</h2>
        <TrailLink className="button-primary" to={paths.manageCourses}>Manage courses</TrailLink>
      </header>
      <QueryState
        query={courses}
        loading="Loading courses…"
        error="Could not load your courses. Try again later."
        empty="You do not teach any courses yet. Create one under Manage courses."
        isEmpty={all => teaching(all).length === 0}
      >
        {all => <CourseGrid courses={teaching(all)} />}
      </QueryState>
    </section>
  )
}
