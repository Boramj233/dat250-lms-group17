import { createContext, useContext } from 'react'
import type { Course } from '@domain/courses/types'
import type { CourseModule } from '@domain/modules/types'

/** What CourseLayout has loaded, for the pages of one course. */
export type CourseViewContextValue = {
  course: Course
  /** Sorted by position. */
  modules: CourseModule[]
  /** The user teaches this course: sees unpublished content and gets the Manage button. */
  isTeacher: boolean
  /** Whether the side menu (course or module menu) is open; shared by both menus. */
  menuOpen: boolean
  toggleMenu: () => void
}

export const CourseViewContext = createContext<CourseViewContextValue | null>(null)

/** The course being viewed. Only for pages inside CourseLayout. */
export function useCourseView(): CourseViewContextValue {
  const value = useContext(CourseViewContext)
  if (!value) throw new Error('useCourseView is used outside CourseLayout')
  return value
}
