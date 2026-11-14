import { Link } from 'react-router'
import { paths } from '../../../app/paths'
import { useCourseContent } from '../../../data/content/hooks'
import type { CourseModule } from '../../../data/modules/types'
import { useManageCourse } from '../manageCourseContext'
import AddModuleForm from './AddModuleForm'
import './CourseContentPage.css'

/** The Content tab: the course's modules, each with a button to manage it, and "Add a module". */
export default function CourseContentPage() {
  const { course, modules } = useManageCourse()
  // All modules' content at once, for the counts; the same cache as the module pages.
  const content = useCourseContent(course.id, modules.map(m => m.id))

  /** "Open from the start" or "Opens after module 1 and 2". */
  function unlockText(prerequisiteIds: CourseModule['id'][]): string {
    const numbers = modules
      .map((m, index) => ({ id: String(m.id), number: index + 1 }))
      .filter(m => prerequisiteIds.some(id => String(id) === m.id))
      .map(m => m.number)
    if (numbers.length === 0) return 'Open from the start'
    const list = numbers.length === 1
      ? `${numbers[0]}`
      : `${numbers.slice(0, -1).join(', ')} and ${numbers.at(-1)}`
    return `Opens after module ${list}`
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
                      {unlockText(module.prerequisiteModuleIds ?? [])}
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
