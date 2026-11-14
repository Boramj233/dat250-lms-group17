import { createContext, useContext } from 'react'
import type { Course } from '../../data/courses/types'
import type { CourseModule } from '../../data/modules/types'

/** What ManageCourseLayout has loaded, for the management pages of one course. */
export type ManageCourseContextValue = {
  course: Course
  /** Sorted by position. */
  modules: CourseModule[]
}

export const ManageCourseContext = createContext<ManageCourseContextValue | null>(null)

/** The course being managed. Only for pages inside ManageCourseLayout. */
export function useManageCourse(): ManageCourseContextValue {
  const value = useContext(ManageCourseContext)
  if (!value) throw new Error('useManageCourse is used outside ManageCourseLayout')
  return value
}
