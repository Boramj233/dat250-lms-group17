import { Outlet, useParams } from 'react-router'
import { useCourse, useCourseRole } from '@domain/courses/hooks'
import { useCourseModules } from '@domain/modules/hooks'
import { Breadcrumbs } from '@shared/navigation/Trail'
import { ManageCourseContext, useManageCourse } from '../../manageCourseContext'
import { useManageBreadcrumbs } from '../../useManageBreadcrumbs'

/**
 * Frame for managing one course and its modules and content. Loads the course and
 * its modules once and gives them to the pages inside (useManageCourse), and shows
 * the breadcrumbs. Only teachers of the course may manage it; the backend checks
 * every change itself.
 */
export default function ManageCourseLayout() {
  const { courseId = '' } = useParams()
  const course = useCourse(courseId)
  const modules = useCourseModules(courseId)
  const role = useCourseRole(courseId)

  if (course.isPending || modules.isPending || role.isPending) return <p role="status">Loading course…</p>
  if (course.error) return <p role="alert">Could not load the course.</p>
  if (modules.error) return <p role="alert">Could not load the modules of this course.</p>
  if (role.role !== 'TEACHER') {
    return <p role="alert">Only teachers of {course.data.title} can manage it.</p>
  }

  const sortedModules = [...modules.data].sort((a, b) => a.position - b.position)
  return (
    <ManageCourseContext value={{ course: course.data, modules: sortedModules }}>
      <ManageBreadcrumbs />
      <Outlet />
    </ManageCourseContext>
  )
}

function ManageBreadcrumbs() {
  const { course, modules } = useManageCourse()
  return <Breadcrumbs items={useManageBreadcrumbs(course, modules)} />
}
