import { useState } from 'react'
import { Outlet, useMatch, useParams } from 'react-router'
import { TrailPage } from '@shared/navigation/Trail'
import { useCourse, useCourseRole } from '@domain/courses/hooks'
import { useCourseModules } from '@domain/modules/hooks'
import { CourseViewContext } from '../../courseContext'

/**
 * Frame for every page of one course. Loads the course and its modules once and gives
 * them to the pages (useCourseView). The menus are drawn by the layouts inside it:
 * CourseMenuLayout (course pages) and ModuleLayout (one module).
 */
export default function CourseLayout() {
  const { courseId = '' } = useParams()
  const course = useCourse(courseId)
  const modules = useCourseModules(courseId)
  const role = useCourseRole(courseId)
  // Open by default; kept while moving around in the course, also into its modules.
  const [menuOpen, setMenuOpen] = useState(true)
  // ModuleLayout (course › module › …) and the course's content pages
  // (/courses/1/assignments/101: course › Assignments › item) draw their own breadcrumbs.
  const inModule = useMatch('/courses/:courseId/modules/:moduleId/*')
  const onCourseContent = useMatch('/courses/:courseId/:section/:contentId')
  const onSubmission = useMatch('/courses/:courseId/submissions/:contentId/:submissionId')
  const ownBreadcrumbs = inModule !== null || onCourseContent !== null || onSubmission !== null

  if (course.isPending || modules.isPending || role.isPending) return <p role="status">Loading course…</p>
  if (course.error) return <p role="alert">Could not load the course.</p>
  if (modules.error) return <p role="alert">Could not load the modules of this course.</p>

  const sortedModules = [...modules.data].sort((a, b) => a.position - b.position)

  return (
    <CourseViewContext
      value={{
        course: course.data,
        modules: sortedModules,
        isTeacher: role.role === 'TEACHER',
        menuOpen,
        toggleMenu: () => setMenuOpen(open => !open),
      }}
    >
      <TrailPage label={course.data.title} hideBreadcrumbs={ownBreadcrumbs}>
        <Outlet />
      </TrailPage>
    </CourseViewContext>
  )
}
