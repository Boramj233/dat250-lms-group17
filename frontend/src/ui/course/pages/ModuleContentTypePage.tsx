import { useParams } from 'react-router'
import { paths } from '../../../app/paths'
import { useModuleContent } from '../../../data/content/hooks/queries/useModuleContent'
import NotFoundPage from '../../pages/NotFoundPage'
import ContentList from '../ContentList'
import { CourseLink } from '../CourseLink'
import ContentTypeIcon from '../ContentTypeIcon'
import { useCourseContext } from '../courseContext'
import CompletionSections from '../CompletionSections'
import { contentTypeBySlug, isCompletable } from '../contentTypes'
import './CoursePages.css'

/**
 * All content of one type in one module, e.g. the assignments in module 2. Each item
 * opens its own page (where the quiz is taken or the assignment handed in).
 */
export default function ModuleContentTypePage() {
  const { moduleId, section } = useParams()
  const { course, modules, completed } = useCourseContext()
  const content = useModuleContent(course.id, moduleId)
  const info = contentTypeBySlug(section)

  if (!info) return <NotFoundPage />
  const index = modules.findIndex(m => String(m.id) === moduleId)
  if (index < 0) return <p role="alert">This module does not exist in {course.title}.</p>
  const module = modules[index]
  const items = content.data?.filter(item => item.type === info.type)

  return (
    <>
      <header className="course-header">
        <p className="course-header-kicker content-kicker">
          <ContentTypeIcon type={info.type} />
          Module {index + 1}: {module.title}
        </p>
        <h1>{info.plural}</h1>
      </header>

      {content.isPending && <p role="status">Loading…</p>}
      {content.error && <p role="alert">Could not load the content of this module.</p>}
      {items?.length === 0 && (
        <p>
          There are no {info.plural.toLowerCase()} in this module.{' '}
          <CourseLink to={paths.courseModule(course.id, module.id)}>Back to the module</CourseLink>
        </p>
      )}
      {items && items.length > 0 && (
        isCompletable(info.type)
          ? (
            <CompletionSections
              items={items}
              completed={completed}
              noun={info.plural.toLowerCase()}
              render={part => <ContentList courseId={course.id} moduleId={module.id} items={part} />}
            />
          )
          : <ContentList courseId={course.id} moduleId={module.id} items={items} />
      )}
    </>
  )
}
