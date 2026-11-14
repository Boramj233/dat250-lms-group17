import { Link } from 'react-router'
import { paths } from '../../../app/paths'
import { useCourseContent } from '../../../data/content/hooks'
import type { CourseModule } from '../../../data/modules/types'
import { formatDateTime } from '../../../shared/lib/dateFormat'
import { useManageCourse } from '../manageCourseContext'
import AddModuleForm from './AddModuleForm'
import './CourseContentPage.css'

/** The Content tab: the course's modules, each with a button to manage it, and "Add a module". */
export default function CourseContentPage() {
  const { course, modules } = useManageCourse()
  // All modules' content at once, for the counts; the same cache as the module pages.
  const content = useCourseContent(course.id, modules.map(m => m.id))

  /**
   * When students can open the module, e.g. "From 21 Nov 08:00 · After module 1 and 2 ·
   * For all from 5 Dec 08:00", or "Open from the start".
   */
  function availabilityText(module: CourseModule): string {
    const numbers = modules
      .map((m, index) => ({ id: String(m.id), number: index + 1 }))
      .filter(m => (module.prerequisiteModuleIds ?? []).some(id => String(id) === m.id))
      .map(m => m.number)
    const parts: string[] = []
    if (module.availableFrom) parts.push(`From ${formatDateTime(new Date(module.availableFrom))}`)
    if (numbers.length > 0) {
      const list = numbers.length === 1 ? `${numbers[0]}` : `${numbers.slice(0, -1).join(', ')} and ${numbers.at(-1)}`
      parts.push(`After module ${list}`)
    }
    if (module.openToAllFrom) parts.push(`For all from ${formatDateTime(new Date(module.openToAllFrom))}`)
    return parts.length > 0 ? parts.join(' · ') : 'Open from the start'
  }

  return (
    <>
      {modules.length === 0
        ? <p>This course has no modules yet. Add the first one below.</p>
        : (
          <ol className="manage-module-list">
            {modules.map((module, index) => {
              const items = content.byModule.get(String(module.id))
              return (
                <li key={module.id} className="manage-module-row">
                  <div className="manage-module-name">
                    <span className="manage-module-number">Module {index + 1}</span>
                    <span className="manage-module-title">{module.title}</span>
                    <span className="manage-module-count">
                      {items === undefined ? '…' : `${items.length} ${items.length === 1 ? 'item' : 'items'}`}
                      {' · '}
                      {availabilityText(module)}
                    </span>
                  </div>
                  <Link className="button-secondary" to={paths.manageModule(course.id, module.id)}>
                    Manage<span className="visually-hidden"> {module.title}</span>
                  </Link>
                </li>
              )
            })}
          </ol>
        )}
      <AddModuleForm courseId={course.id} position={modules.length} />
    </>
  )
}
