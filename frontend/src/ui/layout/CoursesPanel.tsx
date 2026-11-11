import { useState } from 'react'
import { Link, NavLink } from 'react-router'
import { paths } from '../../app/paths'
import { useCurrentUser } from '../../data/auth/useAuth'
import { useMyCourses } from '../../data/courses/hooks/queries/useMyCourses'
import type { EnrolledCourse } from '../../data/courses/types'
import { splitByCurrentTerm, type TermGroup } from '../components/courseFormat'
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
        <h2 id="courses-panel-title">
          <Link className="courses-panel-title-link" to={paths.courses} onClick={onClose}>
            Courses
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
              <path d="M9 6l6 6-6 6" />
            </svg>
          </Link>
        </h2>
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
        <CourseTerms courses={courses.data} onNavigate={onClose} />
      )}
    </section>
  )
}

type CourseTermsProps = {
  courses: EnrolledCourse[]
  onNavigate: () => void
}

/** This term's (and later) courses on top; previous terms behind a show/hide button. */
function CourseTerms({ courses, onNavigate }: CourseTermsProps) {
  const [showPrevious, setShowPrevious] = useState(false)
  const { current, previous } = splitByCurrentTerm(courses)
  const previousCount = previous.reduce((sum, group) => sum + group.courses.length, 0)

  return (
    <>
      {current.length === 0 && <p className="courses-panel-empty">No courses this term.</p>}
      {current.map(group => <TermList key={group.key} group={group} onNavigate={onNavigate} />)}

      {previous.length > 0 && (
        <div className="courses-panel-previous">
          <button
            type="button"
            className="courses-panel-previous-toggle"
            aria-expanded={showPrevious}
            aria-controls="courses-panel-previous"
            onClick={() => setShowPrevious(!showPrevious)}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
              <path d="M9 6l6 6-6 6" />
            </svg>
            Previous terms ({previousCount})
          </button>
          <div id="courses-panel-previous" hidden={!showPrevious}>
            {previous.map(group => <TermList key={group.key} group={group} onNavigate={onNavigate} />)}
          </div>
        </div>
      )}
    </>
  )
}

type TermListProps = {
  group: TermGroup<EnrolledCourse>
  onNavigate: () => void
}

/** The courses of one term under a small term heading. */
function TermList({ group, onNavigate }: TermListProps) {
  return (
    <div className="courses-panel-term">
      <h3 id={`panel-term-${group.key}`}>{group.label}</h3>
      <ul className="courses-panel-list" aria-labelledby={`panel-term-${group.key}`}>
        {group.courses.map(course => (
          <li key={course.id}>
            <NavLink className="courses-panel-item" to={paths.course(course.id)} onClick={onNavigate}>
              {course.title}
            </NavLink>
          </li>
        ))}
      </ul>
    </div>
  )
}
