import type { ReactNode } from 'react'
import { Outlet } from 'react-router'
import { useCourseContext } from './courseContext'

/**
 * Shows its content only to teachers of the current course (inside CourseLayout).
 * This only decides what the UI shows; the backend must check every change itself.
 */
export function TeacherOnly({ children, fallback = null }: { children: ReactNode, fallback?: ReactNode }) {
  const { isTeacher } = useCourseContext()
  return <>{isTeacher ? children : fallback}</>
}

/** Shows its content only to teachers who have switched on edit mode. */
export function EditModeOnly({ children }: { children: ReactNode }) {
  const { editMode } = useCourseContext()
  return <>{editMode ? children : null}</>
}

/** Route guard for teacher pages in a course (members, new content). */
export function RequireTeacher() {
  const context = useCourseContext()
  if (!context.isTeacher) {
    return <p role="alert">Only teachers of {context.course.title} can open this page.</p>
  }
  // Pass the course context on to the teacher pages.
  return <Outlet context={context} />
}
