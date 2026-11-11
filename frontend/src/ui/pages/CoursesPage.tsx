import { useCurrentUser } from '../../data/auth/useAuth'
import { useMyCourses } from '../../data/courses/hooks/queries/useMyCourses'
import CourseGrid from '../components/CourseGrid'
import { groupByTerm } from '../components/courseFormat'
import { TrailPage } from '../navigation/Trail'
import './CoursesPage.css'

/** All courses the user is enrolled in, grouped by term (newest first). */
export default function CoursesPage() {
  const user = useCurrentUser().data!
  const courses = useMyCourses(user.userId)

  return (
    <TrailPage label="Courses">
      <div className="courses-page">
        <h1>Courses</h1>
        {courses.isPending && <p role="status">Loading courses…</p>}
        {courses.error && <p role="alert">Could not load your courses. Try again later.</p>}
        {courses.data?.length === 0 && <p>You are not enrolled in any courses yet.</p>}
        {courses.data && groupByTerm(courses.data).map(group => (
          <section key={group.key} className="courses-term" aria-labelledby={`term-${group.key}`}>
            <h2 id={`term-${group.key}`}>
              {group.label}
              <span className="courses-term-count">
                {group.courses.length} {group.courses.length === 1 ? 'course' : 'courses'}
              </span>
            </h2>
            <CourseGrid courses={group.courses} />
          </section>
        ))}
      </div>
    </TrailPage>
  )
}
