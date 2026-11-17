import { Outlet, useParams } from 'react-router'
import { useModuleContent } from '@domain/content/hooks'
import QueryState from '@shared/ui/QueryState'
import { useManageCourse } from '../../../manageCourseContext'
import { ManageModuleContext } from '../../manageModuleContext'

/**
 * Frame for managing one module (/manage-courses/:courseId/modules/:moduleId/...).
 * Loads the module's content once and gives it to the pages inside (useManageModule).
 */
export default function ManageModuleLayout() {
  const { moduleId } = useParams()
  const { course, modules } = useManageCourse()
  const index = modules.findIndex(m => String(m.id) === moduleId)
  const module = index >= 0 ? modules[index] : undefined
  const content = useModuleContent(course.id, module?.id)

  if (!module) return <p role="alert">This module does not exist in {course.title}.</p>

  return (
    <QueryState query={content} loading="Loading the module…" error="Could not load the content of this module.">
      {items => (
        <ManageModuleContext value={{ module, number: index + 1, content: items }}>
          <Outlet />
        </ManageModuleContext>
      )}
    </QueryState>
  )
}
