import { useState } from 'react'
import { Outlet, useLocation, useMatch, useParams } from 'react-router'
import { paths } from '../../app/paths'
import { useCurrentUser } from '../../data/auth/useAuth'
import { useCourseCompletions } from '../../data/content/hooks/queries/useCourseCompletions'
import { useModuleContent } from '../../data/content/hooks/queries/useModuleContent'
import { useCourse } from '../../data/courses/hooks/queries/useCourse'
import { useCourseModules } from '../../data/module/hooks/queries/useCourseModules'
import { useUserModuleProgress } from '../../data/module/hooks/queries/useUserModuleProgress'
import { Breadcrumbs, type BreadcrumbItem } from '../navigation/Trail'
import { readTrail, trailItems } from '../navigation/trailState'
import type { CourseContextValue } from './courseContext'
import CourseSidebar from './CourseSidebar'
import { contentTypeBySlug } from './contentTypes'
import './CourseLayout.css'

const COURSE_NAV_ID = 'course-navigation'

/**
 * Frame for all pages in a course: breadcrumbs (course › module › content), the course
 * menu on the left and the page in <Outlet />. Loads the course and its modules once
 * and hands them to the pages (useCourseContext).
 */
export default function CourseLayout() {
  const { courseId = '' } = useParams()
  const location = useLocation()
  const course = useCourse(courseId)
  const modules = useCourseModules(courseId)
  const completions = useCourseCompletions(courseId)
  const user = useCurrentUser().data!
  const progress = useUserModuleProgress(courseId, user.userId)

  // Which module / content / section is open. useMatch reads the whole URL, so the
  // layout knows about its child routes.
  const moduleMatch = useMatch('/courses/:courseId/modules/:moduleId/*')
  const contentMatch = useMatch('/courses/:courseId/modules/:moduleId/content/:contentId')
  const sectionMatch = useMatch('/courses/:courseId/:section')
  const moduleSectionMatch = useMatch('/courses/:courseId/modules/:moduleId/:section')
  const moduleId = moduleMatch?.params.moduleId
  const contentId = contentMatch?.params.contentId
  const section = sectionMatch?.params.section
  const moduleSection = contentTypeBySlug(moduleSectionMatch?.params.section)

  const moduleContent = useModuleContent(courseId, moduleId)

  // On narrow screens the course menu is a toggle; it closes again on every navigation.
  const [menuOpenAt, setMenuOpenAt] = useState<string | null>(null)
  const menuOpen = menuOpenAt === location.key

  if (course.isPending || modules.isPending) return <p role="status">Loading course…</p>
  if (course.error) return <p role="alert">Could not load the course.</p>
  if (modules.error) return <p role="alert">Could not load the modules of this course.</p>

  const sortedModules = [...modules.data].sort((a, b) => a.position - b.position)
  const moduleIndex = sortedModules.findIndex(m => String(m.id) === moduleId)
  const currentModule = moduleIndex >= 0 ? sortedModules[moduleIndex] : undefined
  const currentContent = moduleContent.data?.find(item => String(item.id) === contentId)

  // Breadcrumbs: how the user got to the course (if through links), then the course path.
  const linkState = { trail: readTrail(location.state) }
  const crumbs: BreadcrumbItem[] = [
    ...trailItems(linkState.trail),
    { label: course.data.title, to: paths.course(course.data.id), state: linkState },
  ]
  if (currentModule) {
    crumbs.push({
      label: currentModule.title,
      to: paths.courseModule(course.data.id, currentModule.id),
      state: linkState,
    })
    if (contentId) crumbs.push({ label: currentContent?.title ?? '…' })
    else if (moduleSection) crumbs.push({ label: moduleSection.plural })
  } else if (section === 'groups') {
    crumbs.push({ label: 'Groups' })
  } else if (section === 'grades') {
    crumbs.push({ label: 'Grades' })
  } else if (contentTypeBySlug(section)) {
    crumbs.push({ label: contentTypeBySlug(section)!.plural })
  }
  // The last crumb is the current page and is not a link.
  crumbs[crumbs.length - 1] = { label: crumbs[crumbs.length - 1].label }

  const context: CourseContextValue = {
    course: course.data,
    modules: sortedModules,
    completed: completions.data,
    completedModules: progress.data && new Set(
      progress.data.filter(entry => entry.completed).map(entry => String(entry.moduleId))),
  }

  return (
    <div className="course-layout">
      <Breadcrumbs items={crumbs} />

      <button
        type="button"
        className="course-nav-toggle"
        aria-expanded={menuOpen}
        aria-controls={COURSE_NAV_ID}
        onClick={() => setMenuOpenAt(menuOpen ? null : location.key)}
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          strokeWidth="2" strokeLinecap="round" aria-hidden="true" focusable="false">
          <path d="M4 6h16M4 12h16M4 18h16" />
        </svg>
        Course menu
      </button>

      <div className="course-body">
        <CourseSidebar
          id={COURSE_NAV_ID}
          course={course.data}
          modules={sortedModules}
          currentModuleId={currentModule?.id}
          isOpen={menuOpen}
          completed={completions.data}
          completedModules={context.completedModules}
        />
        <div className="course-page">
          <Outlet context={context} />
        </div>
      </div>
    </div>
  )
}
