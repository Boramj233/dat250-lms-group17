import { useState } from 'react'
import { Outlet, useMatch, useParams } from 'react-router'
import { TrailPage } from '@shared/navigation/Trail'
import { useCourseWithModules } from '@domain/courses/hooks'
import { CourseViewContext } from '../../courseContext'
import { routePatterns } from '@shared/paths'

/**
 * Frame for every page of one course. Loads the course and its modules once and gives
 * them to the pages (useCourseView). The menus are drawn by the layouts inside it:
 * CourseMenuLayout (course pages) and ModuleLayout (one module).
 */
export default function CourseLayout() {
  const { courseId = '' } = useParams()
  const loaded = useCourseWithModules(courseId)
  // Open by default; kept while moving around in the course, also into its modules.
  const [menuOpen, setMenuOpen] = useState(true)
  // ModuleLayout (course › module › …) and the course's content pages
  // (/courses/1/assignments/101: course › Assignments › item) draw their own breadcrumbs.
  const inModule = useMatch(routePatterns.inModule)
  const onCourseContent = useMatch(routePatterns.courseSectionItem)
  const onSubmission = useMatch(routePatterns.courseSubmission)
  const ownBreadcrumbs = inModule !== null || onCourseContent !== null || onSubmission !== null

  if (loaded.status === 'pending') return <p role="status">Loading course…</p>
  if (loaded.status === 'error') return <p role="alert">{loaded.message}</p>
  const { course, modules, isTeacher } = loaded

  return (
    <CourseViewContext
      value={{
        course,
        modules,
        isTeacher,
        menuOpen,
        toggleMenu: () => setMenuOpen(open => !open),
      }}
    >
      <TrailPage label={course.title} hideBreadcrumbs={ownBreadcrumbs}>
        <Outlet />
      </TrailPage>
    </CourseViewContext>
  )
}
