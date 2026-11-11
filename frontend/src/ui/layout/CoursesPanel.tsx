import { NavLink } from 'react-router'
import { paths } from '../../app/paths'
import { useCurrentUser } from '../../data/auth/useAuth'
import { useMyCourses } from '../../data/courses/hooks/queries/useMyCourses'
import { byNewest, formatTerm } from '../components/courseFormat'
import './CoursesPanel.css'

export const COURSES_PANEL_ID = 'courses-panel'

type CoursesPanelProps = {
  onClose: () => void
}

/** Second panel that slides out to the right of the sidebar with the user's courses. */
export default function CoursesPanel({ onClose }: CoursesPanelProps) {
  const user = useCurrentUser().data!
  const courses = useMyCourses(user.userId)

  return (
    <section id={COURSES_PANEL_ID} className="courses-panel" aria-labelledby="courses-panel-title">
      <header className="courses-panel-header">
        <h2 id="courses-panel-title">Courses</h2>
        <button type="button" className="courses-panel-close" aria-label="Close courses" onClick={onClose}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2" strokeLinecap="round" aria-hidden="true" focusable="false">
            <path d="M6 6l12 12M6 18L18 6" />
          </svg>
        </button>
      </header>

      {courses.isPending && <p role="status">Loading courses…</p>}
      {courses.error && <p role="alert">Could not load your courses.</p>}
      {courses.data?.length === 0 && <p>You are not enrolled in any courses yet.</p>}
      {courses.data && courses.data.length > 0 && (
        <ul className="courses-panel-list">
          {[...courses.data].sort(byNewest).map(course => (
            <li key={course.id}>
              <NavLink className="courses-panel-item" to={paths.course(course.id)} onClick={onClose}>
                <span className="courses-panel-item-title">{course.title}</span>
                <span className="courses-panel-item-term">{formatTerm(course)}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
