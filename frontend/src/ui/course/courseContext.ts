import { useLocation, useOutletContext } from 'react-router'
import type { Course } from '../../data/courses/types'
import type { CourseModule } from '../../data/module/types'
import { readTrail } from '../navigation/trailState'

/** What CourseLayout has loaded, for the course pages rendered in its <Outlet />. */
export type CourseContextValue = {
  course: Course
  /** Sorted by position. */
  modules: CourseModule[]
  /**
   * Ids (strings) of the assignments, coding exercises and quizzes the user has
   * completed; undefined while loading or if it could not be loaded.
   */
  completed?: Set<string>
  /**
   * Ids (strings) of the modules the user has completed, calculated by the backend
   * (GET /api/users/{userId}/courses/{courseId}/modules/progress, see data/module).
   * TODO: Not implemented in the backend yet; until then this is undefined and no
   * module shows a check mark.
   */
  completedModules?: Set<string>
}

export function useCourseContext(): CourseContextValue {
  return useOutletContext<CourseContextValue>()
}

/**
 * History state for links inside a course: keeps the trail from outside the course
 * (e.g. Overview › Courses), so the breadcrumbs keep it while moving around in the course.
 */
export function useCourseLinkState(): { trail: ReturnType<typeof readTrail> } {
  return { trail: readTrail(useLocation().state) }
}
