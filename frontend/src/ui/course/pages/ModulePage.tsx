import { useParams } from 'react-router'
import { paths } from '../../../app/paths'
import { useModuleContent } from '../../../data/content/hooks/queries/useModuleContent'
import ContentList from '../ContentList'
import CompletedMark from '../CompletedMark'
import { CourseLink } from '../CourseLink'
import { useCourseContext } from '../courseContext'
import { summarizeTypes } from '../contentTypes'
import { EditModeOnly } from '../TeacherOnly'
import '../../components/forms.css'
import './CoursePages.css'

/** One module: its content in order, and links to the previous and next module. */
export default function ModulePage() {
  const { moduleId } = useParams()
  const { course, modules, completedModules } = useCourseContext()
  const content = useModuleContent(course.id, moduleId)

  const index = modules.findIndex(m => String(m.id) === moduleId)
  if (index < 0) return <p role="alert">This module does not exist in {course.title}.</p>
  const module = modules[index]
  const previous = modules[index - 1]
  const next = modules[index + 1]

  return (
    <>
      <header className="course-header">
        <p className="course-header-kicker content-kicker">
          Module {index + 1} of {modules.length}
          {completedModules?.has(String(module.id)) && <CompletedMark label />}
        </p>
        <h1>{module.title}</h1>
        {content.data && content.data.length > 0 && <p>{summarizeTypes(content.data)}</p>}
      </header>

      {content.isPending && <p role="status">Loading content…</p>}
      {content.error && <p role="alert">Could not load the content of this module.</p>}
      {content.data?.length === 0 && <p>This module has no content yet.</p>}
      {content.data && content.data.length > 0 && (
        <ContentList courseId={course.id} moduleId={module.id} items={content.data} />
      )}

      <EditModeOnly>
        <div className="teacher-panel">
          <h2 className="teacher-panel-title">Add to this module</h2>
          <div className="form-actions">
            <CourseLink className="button-primary" to={paths.courseNewContent(course.id, module.id)}>
              Add content
            </CourseLink>
          </div>
        </div>
      </EditModeOnly>

      <nav className="pager" aria-label="Modules">
        {previous && (
          <CourseLink className="pager-link is-previous" to={paths.courseModule(course.id, previous.id)}>
            <span className="pager-direction">Previous module</span>
            <span className="pager-title">{previous.title}</span>
          </CourseLink>
        )}
        {next && (
          <CourseLink className="pager-link is-next" to={paths.courseModule(course.id, next.id)}>
            <span className="pager-direction">Next module</span>
            <span className="pager-title">{next.title}</span>
          </CourseLink>
        )}
      </nav>
    </>
  )
}
