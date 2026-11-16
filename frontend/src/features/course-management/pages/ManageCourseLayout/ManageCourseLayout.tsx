import { Outlet, useParams } from 'react-router'
import { useCourseWithModules } from '@domain/courses/hooks'
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
  const loaded = useCourseWithModules(courseId)

  if (loaded.status === 'pending') return <p role="status">Loading course…</p>
  if (loaded.status === 'error') return <p role="alert">{loaded.message}</p>
  const { course, modules, isTeacher } = loaded
  if (!isTeacher) return <p role="alert">Only teachers of {course.title} can manage it.</p>

  return (
    <ManageCourseContext value={{ course, modules }}>
      <ManageBreadcrumbs />
      <Outlet />
    </ManageCourseContext>
  )
}

function ManageBreadcrumbs() {
  const { course, modules } = useManageCourse()
  return <Breadcrumbs items={useManageBreadcrumbs(course, modules)} />
}
