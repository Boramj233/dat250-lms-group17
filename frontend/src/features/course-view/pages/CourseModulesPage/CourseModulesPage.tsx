import { useState } from 'react'
import { Link } from 'react-router'
import { paths } from '../../../../shared/paths'
import type { CourseId } from '../../../../shared/types'
import PageHeader from '../../../../shared/ui/PageHeader'
import { contentSection, contentTypeLabel } from '../../../content/contentTypes'
import { useCourseContent } from '../../../content/hooks'
import type { ContentSummary } from '../../../content/types'
import type { CourseModule } from '../../../modules/types'
import ContentList from '../../components/ContentList'
import { useCourseView, visibleContent } from '../../courseContext'
import './CourseModulesPage.css'

/**
 * Every module of the course. The arrow folds a module's content in and out; the title
 * opens the module with its own menu.
 */
export default function CourseModulesPage() {
  const { course, modules, isTeacher } = useCourseView()
  const content = useCourseContent(course.id, modules.map(module => module.id))
  // Ids of the folded (closed) modules; all are open at first.
  const [closed, setClosed] = useState<Set<string>>(() => new Set())

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
      <PageHeader title="Modules" kicker={course.title} />
      {modules.length === 0 && <p>This course has no modules yet.</p>}
      {content.isError && <p role="alert">Could not load the content of some modules.</p>}
      <ol className="course-modules">
        {modules.map(module => {
          const loaded = content.byModule.get(String(module.id))
          return (
            <li key={module.id}>
              <ModuleBox
                courseId={course.id}
                module={module}
                items={loaded && visibleContent(loaded, isTeacher)}
                loading={loaded === undefined && !content.isError}
                open={!closed.has(String(module.id))}
                onToggle={() => toggle(String(module.id))}
              />
            </li>
          )
        })}
      </ol>
    </>
  )
}

type ModuleBoxProps = {
  courseId: CourseId
  module: CourseModule
  /** Undefined while loading. */
  items?: ContentSummary[]
  loading: boolean
  open: boolean
  onToggle: () => void
}

/** One module: an arrow that shows or hides the content, and the title as a link. */
function ModuleBox({ courseId, module, items, loading, open, onToggle }: ModuleBoxProps) {
  const panelId = `course-module-${module.id}`
  return (
    <section className={`course-module${open ? ' is-open' : ''}`}>
      <h2 className="course-module-header">
        <button
          type="button"
          className="course-module-toggle"
          aria-expanded={open}
          aria-controls={panelId}
          aria-label={`${open ? 'Hide' : 'Show'} the content of ${module.title}`}
          onClick={onToggle}
        >
          <svg className="course-module-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
            aria-hidden="true" focusable="false">
            <path d="M9 6l6 6-6 6" />
          </svg>
        </button>
        <Link className="course-module-title" to={paths.courseModule(courseId, module.id)}>
          {module.title}
        </Link>
      </h2>

      <div id={panelId} className="course-module-body" hidden={!open}>
        {loading && <p className="content-list-note">Loading…</p>}
        {items?.length === 0 && <p className="content-list-note">No content yet.</p>}
        {items && items.length > 0 && (
          <ContentList rows={items.map(item => ({
            item,
            to: paths.courseContent(courseId, module.id, contentSection(item.type), item.id),
            meta: contentTypeLabel(item.type),
          }))} />
        )}
      </div>
    </section>
  )
}
