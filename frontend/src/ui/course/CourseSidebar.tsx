import { useState } from 'react'
import { paths } from '../../app/paths'
import { useModuleContent } from '../../data/content/hooks/queries/useModuleContent'
import type { Course } from '../../data/courses/types'
import type { CourseModule } from '../../data/module/types'
import { formatTerm } from '../components/courseFormat'
import '../components/forms.css'
import CompletedMark from './CompletedMark'
import { CourseNavLink } from './CourseLink'
import ContentTypeIcon from './ContentTypeIcon'
import { CONTENT_TYPES } from './contentTypes'

type CourseSidebarProps = {
  id: string
  course: Course
  /** All modules, sorted by position. */
  modules: CourseModule[]
  /** The module being viewed, if any. */
  currentModuleId?: CourseModule['id']
  isOpen: boolean
  /** Completed content ids, for check marks. */
  completed?: Set<string>
  /** Completed module ids, for check marks. */
  completedModules?: Set<string>
  /** Show the teacher tools (members). */
  isTeacher?: boolean
}

/**
 * The course menu: all modules, then the course-wide lists per content type and the
 * groups. Each module can be unfolded to show its content (grouped by type) without
 * leaving the page; the module being viewed is unfolded by default.
 */
export default function CourseSidebar(
  { id, course, modules, currentModuleId, isOpen, completed, completedModules, isTeacher = false }: CourseSidebarProps,
) {
  const currentId = currentModuleId === undefined ? undefined : String(currentModuleId)
  // Modules the user folded in or out by hand. They apply while the same module is
  // viewed; going to another module starts from the default again.
  const [folding, setFolding] = useState<{ forModule?: string, open: Record<string, boolean> }>({ open: {} })
  const isUnfolded = (moduleId: string) =>
    (folding.forModule === currentId ? folding.open[moduleId] : undefined) ?? moduleId === currentId

  function toggle(moduleId: string) {
    setFolding(previous => ({
      forModule: currentId,
      open: { ...(previous.forModule === currentId ? previous.open : {}), [moduleId]: !isUnfolded(moduleId) },
    }))
  }

  return (
    <nav id={id} className={`course-nav${isOpen ? ' is-open' : ''}`} aria-label="Course navigation">
      <div className="course-nav-heading">
        <span className="course-nav-title">{course.title}</span>
        <span className="course-nav-term">{formatTerm(course)}</span>
        {isTeacher && <span className="teacher-tag course-nav-role">Teacher</span>}
      </div>

      <ul className="course-nav-list">
        <li>
          <CourseNavLink to={paths.course(course.id)} end>Modules</CourseNavLink>
          {modules.length > 0 && (
            <ol className="course-nav-modules">
              {modules.map(module => {
                const moduleId = String(module.id)
                const isCurrent = moduleId === currentId
                const unfolded = isUnfolded(moduleId)
                const contentId = `${id}-module-${moduleId}`
                return (
                  <li key={module.id} className={isCurrent ? 'is-current' : undefined}>
                    <div className="course-nav-module-row">
                      <button
                        type="button"
                        className="course-nav-fold"
                        aria-expanded={unfolded}
                        aria-controls={contentId}
                        aria-label={`${unfolded ? 'Hide' : 'Show'} content of ${module.title}`}
                        onClick={() => toggle(moduleId)}
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                          strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                          aria-hidden="true" focusable="false">
                          <path d="M9 6l6 6-6 6" />
                        </svg>
                      </button>
                      <CourseNavLink className="course-nav-module-link" end
                        to={paths.courseModule(course.id, module.id)}>
                        <span className="course-nav-item-title">{module.title}</span>
                        {completedModules?.has(moduleId) && <CompletedMark />}
                      </CourseNavLink>
                    </div>
                    {unfolded && (
                      <ModuleContentNav id={contentId} courseId={course.id} moduleId={module.id} completed={completed} />
                    )}
                  </li>
                )
              })}
            </ol>
          )}
        </li>
      </ul>

      <ul className="course-nav-list course-nav-sections">
        {CONTENT_TYPES.map(info => (
          <li key={info.slug}>
            <CourseNavLink to={paths.courseContentType(course.id, info.slug)}>
              <ContentTypeIcon type={info.type} />
              All {info.plural.toLowerCase()}
            </CourseNavLink>
          </li>
        ))}
        <li>
          <CourseNavLink to={paths.courseGrades(course.id)}>Grades</CourseNavLink>
        </li>
        <li>
          <CourseNavLink to={paths.courseGroups(course.id)}>Groups</CourseNavLink>
        </li>
        {isTeacher && (
          <li>
            <CourseNavLink to={paths.courseMembers(course.id)}>Members</CourseNavLink>
          </li>
        )}
      </ul>
    </nav>
  )
}

type ModuleContentNavProps = {
  id: string
  courseId: Course['id']
  moduleId: CourseModule['id']
  completed?: Set<string>
}

/** A module's content, grouped by type, under the module in the menu. */
function ModuleContentNav({ id, courseId, moduleId, completed }: ModuleContentNavProps) {
  // Fetched when unfolded; shared with the module pages through the query cache.
  const { data: content, isError } = useModuleContent(courseId, moduleId)
  if (isError) return <p id={id} className="course-nav-note">Could not load the content.</p>
  if (content === undefined) return <p id={id} className="course-nav-note">Loading…</p>
  if (content.length === 0) return <p id={id} className="course-nav-note">No content yet.</p>
  return (
    <div id={id} className="course-nav-module-content">
      {CONTENT_TYPES.map(info => {
        const items = content.filter(item => item.type === info.type)
        if (items.length === 0) return null
        return (
          <div key={info.type} className="course-nav-group">
            {/* All items of this type in the module; the items below open one each. */}
            <CourseNavLink className="course-nav-group-title"
              to={paths.courseModuleContentType(courseId, moduleId, info.slug)}>
              <ContentTypeIcon type={info.type} />
              {info.plural}
              <span className="course-nav-count">{items.length}</span>
            </CourseNavLink>
            <ul className="course-nav-list">
              {items.map(item => (
                <li key={item.id}>
                  <CourseNavLink to={paths.courseContent(courseId, moduleId, item.id)}>
                    <span className="course-nav-item-title">{item.title}</span>
                    {completed?.has(String(item.id)) && <CompletedMark />}
                  </CourseNavLink>
                </li>
              ))}
            </ul>
          </div>
        )
      })}
    </div>
  )
}
