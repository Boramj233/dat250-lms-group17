import { paths } from '../../../app/paths'
import { useCurrentUser } from '../../../data/auth/useAuth'
import { useMyCourses } from '../../../data/courses/hooks'
import type { EnrolledCourse } from '../../../data/courses/types'
import QueryState from '../../../shared/ui/QueryState'
import CourseGrid from '../../components/courses/CourseGrid'
import { byNewest } from '../../components/courses/courseFormat'
import { TrailLink } from '../../navigation/Trail'

const teaching = (courses: EnrolledCourse[]) =>
  courses.filter(course => course.role === 'TEACHER').sort(byNewest)

/**
 * Course managers: the courses they teach (newest first), and a button to the page
 * where they create courses or edit the ones they teach.
 */
export default function CourseManagerOverview() {
  const user = useCurrentUser().data!
  const courses = useMyCourses(user.userId)

  return (
    <section className="overview-section" aria-labelledby="overview-teaching">
      <header className="overview-section-header">
        <h2 id="overview-teaching">Courses I teach</h2>
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
