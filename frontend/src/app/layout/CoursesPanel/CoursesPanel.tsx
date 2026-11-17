import { useState } from 'react'
import { Link, NavLink } from 'react-router'
import { useCurrentUser } from '@shared/auth/useAuth'
import { paths } from '@shared/paths'
import { byNewest, formatTerm, termOf } from '@domain/courses/courseFormat'
import { useMyCourses } from '@domain/courses/hooks'
import type { CourseRole, EnrolledCourse } from '@domain/courses/types'
import RequireAccess from '../../auth/RequireAccess'
import './CoursesPanel.css'
import { ChevronRightIcon, CloseIcon } from '@shared/ui/icons'

export const COURSES_PANEL_ID = 'courses-panel'

const ROLE_HEADING: Record<CourseRole, string> = {
  TEACHER: 'As teacher',
  STUDENT: 'As student',
}
const ROLE_ORDER: CourseRole[] = ['TEACHER', 'STUDENT']

type CoursesPanelProps = {
  /** Closes the panel; called after a link is picked and by the close button. */
  onClose: () => void
}

/**
 * Opens to the right of the sidebar: a link to all courses and this term's courses.
 * Teachers see the courses grouped by their role in each course.
 */
export default function CoursesPanel({ onClose }: CoursesPanelProps) {
  const user = useCurrentUser().data!
  const courses = useMyCourses(user.userId)
  const [thisTerm] = useState(() => termOf(new Date()))

  const termCourses = (courses.data ?? [])
    .filter(course => course.term === thisTerm.term && course.year === thisTerm.year)
    .sort(byNewest)
  const isTeacher = termCourses.some(course => course.role === 'TEACHER')

  return (
    <section id={COURSES_PANEL_ID} className="courses-panel" aria-labelledby="courses-panel-title">
      <header className="courses-panel-header">
        <h2 id="courses-panel-title">Courses</h2>
        <button type="button" className="courses-panel-close" aria-label="Close courses" onClick={onClose}>
          <CloseIcon size={20} />
        </button>
      </header>

      <Link className="courses-panel-all" to={paths.courses} onClick={onClose}>
        All courses
        <ChevronRightIcon />
      </Link>
      {/* Course managers (and admins) only. */}
      <RequireAccess level="COURSE_MANAGER">
        <Link className="courses-panel-all" to={paths.manageCourses} onClick={onClose}>
          Manage courses
          <ChevronRightIcon />
        </Link>
      </RequireAccess>

      <h3 className="courses-panel-term">{formatTerm(thisTerm)}</h3>
      {courses.isPending && <p role="status">Loading courses…</p>}
      {courses.error && <p role="alert">Could not load your courses.</p>}
      {courses.data && termCourses.length === 0 && <p>No courses this term.</p>}
      {termCourses.length > 0 &&
        (isTeacher ? (
          ROLE_ORDER.map(role => {
            const inRole = termCourses.filter(course => course.role === role)
            if (inRole.length === 0) return null
            return (
              <div key={role} className="courses-panel-group">
                <h4 id={`courses-panel-${role}`}>{ROLE_HEADING[role]}</h4>
                <CourseList courses={inRole} labelledBy={`courses-panel-${role}`} onNavigate={onClose} />
              </div>
            )
          })
        ) : (
          <CourseList courses={termCourses} onNavigate={onClose} />
        ))}

      <p className="courses-panel-note">
        If you want to view previous courses, go to{' '}
        <Link to={paths.courses} onClick={onClose}>
          All courses
        </Link>
        .
      </p>
    </section>
  )
}

type CourseListProps = {
  courses: EnrolledCourse[]
  labelledBy?: string
  onNavigate: () => void
}

function CourseList({ courses, labelledBy, onNavigate }: CourseListProps) {
  return (
    <ul className="courses-panel-list" aria-labelledby={labelledBy}>
      {courses.map(course => (
        <li key={course.id}>
          <NavLink className="courses-panel-item" to={paths.course(course.id)} onClick={onNavigate}>
            {course.title}
          </NavLink>
        </li>
      ))}
    </ul>
  )
}
