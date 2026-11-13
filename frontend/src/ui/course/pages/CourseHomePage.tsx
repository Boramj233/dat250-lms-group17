import { useState } from 'react'
import { paths } from '../../../app/paths'
import { useCourseContent } from '../../../data/content/hooks/queries/useCourseContent'
import type { ContentSummary } from '../../../data/content/types'
import type { CourseModule } from '../../../data/module/types'
import type { CourseId } from '../../../data/shared/types'
import { formatTerm } from '../../components/courseFormat'
import AddModuleForm from '../AddModuleForm'
import '../../components/forms.css'
import ContentList from '../ContentList'
import CompletedMark from '../CompletedMark'
import { CourseLink } from '../CourseLink'
import { useCourseContext } from '../courseContext'
import { summarizeTypes } from '../contentTypes'
import { EditModeOnly } from '../TeacherOnly'
import './CoursePages.css'

/**
 * Start page of a course: the modules below each other, each with all its content.
 * Every module can be folded in and out; all are open at first.
 */
export default function CourseHomePage() {
  const { course, modules, completedModules } = useCourseContext()
  const content = useCourseContent(course.id, modules.map(m => m.id))
  // Ids of the folded (closed) modules.
  const [closed, setClosed] = useState<Set<string>>(() => new Set())
  const allClosed = modules.length > 0 && closed.size === modules.length

  function toggle(moduleId: string) {
    setClosed(previous => {
      const next = new Set(previous)
      if (next.has(moduleId)) next.delete(moduleId)
      else next.add(moduleId)
      return next
    })
  }

  return (
    <>
      <header className="course-header">
        <h1>{course.title}</h1>
        <p className="course-header-term">{formatTerm(course)}</p>
        {course.description && <p>{course.description}</p>}
      </header>

      <div className="module-list-header">
        <h2 className="course-section-title">Modules</h2>
        {modules.length > 1 && (
          <button
            type="button"
            className="module-toggle-all"
            onClick={() => setClosed(allClosed ? new Set() : new Set(modules.map(m => String(m.id))))}
          >
            {allClosed ? 'Expand all' : 'Collapse all'}
          </button>
        )}
      </div>

      {modules.length === 0 && <p>This course has no modules yet.</p>}
      {content.isError && <p role="alert">Could not load the content of some modules.</p>}
      <ol className="module-list">
        {modules.map((module, index) => (
          <li key={module.id}>
            <ModuleSection
              courseId={course.id}
              module={module}
              number={index + 1}
              items={content.byModule.get(String(module.id))}
              open={!closed.has(String(module.id))}
              completed={completedModules?.has(String(module.id)) ?? false}
              onToggle={() => toggle(String(module.id))}
            />
          </li>
        ))}
      </ol>

      <EditModeOnly>
        <AddModuleForm />
      </EditModeOnly>
    </>
  )
}

type ModuleSectionProps = {
  courseId: CourseId
  module: CourseModule
  number: number
  /** Undefined while loading. */
  items?: ContentSummary[]
  open: boolean
  /** The user has completed the module (from the backend's module progress). */
  completed: boolean
  onToggle: () => void
}

/** One module: a header that folds the content in and out, and the full content list. */
function ModuleSection({ courseId, module, number, items, open, completed, onToggle }: ModuleSectionProps) {
  const panelId = `module-panel-${module.id}`
  return (
    <section className={`module-section${open ? ' is-open' : ''}`} aria-labelledby={`module-title-${module.id}`}>
      <header className="module-section-header">
        <button
          type="button"
          className="module-section-toggle"
          aria-expanded={open}
          aria-controls={panelId}
          onClick={onToggle}
        >
          <svg className="module-section-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
            aria-hidden="true" focusable="false">
            <path d="M9 6l6 6-6 6" />
          </svg>
          <span className="module-section-heading">
            <span className="module-section-number">
              Module {number}
              {completed && <CompletedMark label />}
            </span>
            <span id={`module-title-${module.id}`} className="module-section-title">{module.title}</span>
          </span>
          {items && items.length > 0 && <span className="module-section-summary">{summarizeTypes(items)}</span>}
        </button>
        <EditModeOnly>
          <CourseLink className="button-secondary" to={paths.courseNewContent(courseId, module.id)}>
            Add content<span className="visually-hidden"> to {module.title}</span>
          </CourseLink>
        </EditModeOnly>
        <CourseLink className="module-section-open" to={paths.courseModule(courseId, module.id)}>
          Open module<span className="visually-hidden"> {module.title}</span>
        </CourseLink>
      </header>

      <div id={panelId} className="module-section-body" hidden={!open}>
        {items === undefined && <p className="module-section-note">Loading…</p>}
        {items?.length === 0 && <p className="module-section-note">No content yet.</p>}
        {items && items.length > 0 && <ContentList courseId={courseId} moduleId={module.id} items={items} />}
      </div>
    </section>
  )
}
