import { Link, NavLink, Outlet, useMatch, useParams } from 'react-router'
import { paths } from '../../../../../shared/paths'
import { useModuleContent } from '../../../../content/hooks'
import SideMenuLayout from '../../../components/SideMenuLayout'
import { CONTENT_SECTIONS } from '../../../contentSections'
import { useCourseView, visibleContent } from '../../../courseContext'
import { ModuleViewContext } from '../../moduleContext'

/**
 * One module of a course with the module menu in place of the course menu: back to
 * the modules, the module's overview, Files and one item per content type. Loads the
 * module's content once and gives it to the pages (useModuleView).
 */
export default function ModuleLayout() {
  const { moduleId = '' } = useParams()
  const { course, modules, isTeacher, menuOpen, toggleMenu } = useCourseView()
  const module = modules.find(m => String(m.id) === moduleId)
  const content = useModuleContent(course.id, module?.id)
  // On a content page, its type's menu item is the current one.
  const contentMatch = useMatch('/courses/:courseId/modules/:moduleId/content/:contentId')

  if (!module) return <p role="alert">This module does not exist in {course.title}.</p>

  const items = content.data && visibleContent(content.data, isTeacher)
  const currentType = items?.find(item => String(item.id) === contentMatch?.params.contentId)?.type

  return (
    <SideMenuLayout
      label="Module menu"
      id="module-menu"
      open={menuOpen}
      onToggle={toggleMenu}
      menu={
        <>
          <Link className="side-menu-back" to={paths.courseSection(course.id, 'modules')}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
              <path d="M15 6l-6 6 6 6" />
            </svg>
            All modules
          </Link>
          <NavLink className="side-menu-heading" to={paths.courseModule(course.id, module.id)} end>
            {module.title}
          </NavLink>
          <ul className="side-menu-list">
            <li>
              <NavLink to={paths.courseModuleSection(course.id, module.id, 'files')}>Files</NavLink>
            </li>
            {CONTENT_SECTIONS.map(({ type, section, label }) => (
              <li key={section}>
                <NavLink
                  to={paths.courseModuleSection(course.id, module.id, section)}
                  className={({ isActive }) => (isActive || type === currentType ? 'active' : undefined)}
                >
                  {label}
                  {items && <span className="side-menu-count">{items.filter(item => item.type === type).length}</span>}
                </NavLink>
              </li>
            ))}
          </ul>
        </>
      }
    >
      {content.isPending && <p role="status">Loading module…</p>}
      {content.error && <p role="alert">Could not load the content of this module.</p>}
      {items && (
        <ModuleViewContext value={{ module, items }}>
          <Outlet />
        </ModuleViewContext>
      )}
    </SideMenuLayout>
  )
}
