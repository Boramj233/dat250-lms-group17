import { useMatch } from 'react-router'
import { paths } from '../../app/paths'
import { useModuleContent } from '../../data/content/hooks'
import type { Course } from '../../data/courses/types'
import type { CourseModule } from '../../data/modules/types'
import type { BreadcrumbItem } from '../navigation/Trail'
import { contentTypeBySlug } from './contentTypes'

/**
 * Breadcrumbs for the management pages, from the URL:
 * Overview › Manage courses › course › module › content (or "New quiz").
 * The last one is the current page.
 */
export function useManageBreadcrumbs(course: Course, modules: CourseModule[]): BreadcrumbItem[] {
  const moduleMatch = useMatch('/manage-courses/:courseId/modules/:moduleId/*')
  const contentMatch = useMatch('/manage-courses/:courseId/modules/:moduleId/content/:contentId/*')
  const newMatch = useMatch('/manage-courses/:courseId/modules/:moduleId/new/:type')
  const moduleIndex = modules.findIndex(m => String(m.id) === moduleMatch?.params.moduleId)
  const module = moduleIndex >= 0 ? modules[moduleIndex] : undefined
  // Same query as ManageModuleLayout, so this is read from the cache.
  const content = useModuleContent(course.id, module?.id)

  const crumbs: BreadcrumbItem[] = [
    { label: 'Overview', to: paths.home },
    { label: 'Manage courses', to: paths.manageCourses },
    { label: course.title, to: paths.manageCourse(course.id) },
  ]
  if (module) {
    crumbs.push({ label: `Module ${moduleIndex + 1}: ${module.title}`, to: paths.manageModule(course.id, module.id) })
    const contentId = contentMatch?.params.contentId
    const newType = contentTypeBySlug(newMatch?.params.type)
    if (contentId) {
      const item = content.data?.find(c => String(c.id) === contentId)
      crumbs.push({ label: item?.title ?? '…' })
    } else if (newType) {
      crumbs.push({ label: `New ${newType.label.toLowerCase()}` })
    }
  }
  // The last crumb is the current page and is not a link.
  crumbs[crumbs.length - 1] = { label: crumbs[crumbs.length - 1].label }
  return crumbs
}
