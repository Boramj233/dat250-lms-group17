import { useState } from 'react'
import { Link, NavLink, Outlet, useMatch, useParams } from 'react-router'
import { Breadcrumbs, type BreadcrumbItem } from '@shared/navigation/Trail'
import { paths } from '@shared/paths'
import { useModuleContent } from '@domain/content/hooks'
import { formatTerm } from '@domain/courses/courseFormat'
import CourseMenuHeading from '../../../components/CourseMenuHeading'
import SideMenuLayout from '../../../components/SideMenuLayout'
import { CONTENT_SECTIONS } from '../../../contentSections'
import { courseMenuItems, courseMenuPath } from '../../../courseMenu'
import { useCourseView, visibleContent } from '../../../courseContext'
import { ModuleViewContext } from '../../moduleContext'
import { ChevronLeftIcon, ChevronRightIcon } from '@shared/ui/icons'

/**
 * One module of a course with the module menu in place of the course menu: the course
 * menu folded in (to unfold), back to the modules, the module's overview, Files and one
 * item per content type. Loads the module's content once and gives it to the pages
 * (useModuleView).
 */
export default function ModuleLayout() {
  const { moduleId = '' } = useParams()
  const { course, modules, isTeacher, menuOpen, toggleMenu } = useCourseView()
  const module = modules.find(m => String(m.id) === moduleId)
  const content = useModuleContent(course.id, module?.id)
  // A section (…/modules/2/quizzes) or a content item in it (…/modules/2/quizzes/13).
  const sectionMatch = useMatch('/courses/:courseId/modules/:moduleId/:section/*')
  const contentId = useMatch('/courses/:courseId/modules/:moduleId/:section/:contentId')?.params.contentId
  // Folded at first; kept while moving between the module's pages.
  const [courseMenuOpen, setCourseMenuOpen] = useState(false)

  if (!module) return <p role="alert">This module does not exist in {course.title}.</p>

  const items = content.data && visibleContent(content.data, isTeacher)
  const currentItem = items?.find(item => String(item.id) === contentId)

  // Course › Modules › module › the section › the item (on a content page). The last
  // crumb is the current page and is not a link.
  const crumbs: BreadcrumbItem[] = [
    { label: course.title, to: paths.course(course.id) },
    { label: 'Modules', to: paths.courseSection(course.id, 'modules') },
    { label: module.title, to: paths.courseModule(course.id, module.id) },
  ]
  const section = sectionMatch?.params.section
  if (section === 'files') {
    crumbs.push({ label: 'Files' })
  } else if (section) {
    const typeSection = CONTENT_SECTIONS.find(s => s.section === section)
    crumbs.push({
      label: typeSection?.label ?? section,
      to: typeSection && paths.courseModuleSection(course.id, module.id, typeSection.section),
    })
    if (contentId) crumbs.push({ label: currentItem?.title ?? '…' })
  }
  crumbs[crumbs.length - 1] = { label: crumbs[crumbs.length - 1].label }

  return (
    <SideMenuLayout
      label="Module menu"
      id="module-menu"
      open={menuOpen}
      onToggle={toggleMenu}
      heading={
        <CourseMenuHeading
          title={course.title}
          subtitle={`${formatTerm(course)} course`}
          to={paths.course(course.id)}
        />
      }
      menu={
        <>
          {/* The course menu, folded in here, so it can be seen without leaving the module. */}
          <div className="side-menu-disclosure">
            <button
              type="button"
              className="side-menu-disclosure-toggle"
              aria-expanded={courseMenuOpen}
              aria-controls="module-course-menu"
              onClick={() => setCourseMenuOpen(!courseMenuOpen)}
            >
              <ChevronRightIcon className="disclosure-chevron" />
              Course menu
            </button>
            <div id="module-course-menu" className="side-menu-disclosure-body" hidden={!courseMenuOpen}>
              {isTeacher && (
                <Link className="button-secondary side-menu-button" to={paths.manageCourse(course.id)}>
                  Manage
                </Link>
              )}
              <ul className="side-menu-list">
                {courseMenuItems(isTeacher).map(item => (
                  <li key={item.label}>
                    <NavLink to={courseMenuPath(course.id, item.section)} end>
                      {item.label}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          </div>
          <Link className="side-menu-back" to={paths.courseSection(course.id, 'modules')}>
            <ChevronLeftIcon />
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
                <NavLink to={paths.courseModuleSection(course.id, module.id, section)}>
                  {label}
                  {items && <span className="side-menu-count">{items.filter(item => item.type === type).length}</span>}
                </NavLink>
              </li>
            ))}
          </ul>
        </>
      }
    >
      <Breadcrumbs items={crumbs} />
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
