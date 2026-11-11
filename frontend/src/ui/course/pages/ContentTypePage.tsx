import { useParams } from 'react-router'
import { paths } from '../../../app/paths'
import { useCourseContent } from '../../../data/content/hooks/queries/useCourseContent'
import type { ContentSummary } from '../../../data/content/types'
import type { CourseModule } from '../../../data/module/types'
import NotFoundPage from '../../pages/NotFoundPage'
import ContentList from '../ContentList'
import { CourseLink } from '../CourseLink'
import { useCourseContext } from '../courseContext'
import CompletionSections from '../CompletionSections'
import { contentTypeBySlug, isCompletable } from '../contentTypes'
import './CoursePages.css'

/** All content of one type in the course (e.g. all quizzes), grouped by module. */
export default function ContentTypePage() {
  const { section } = useParams()
  const { course, modules, completed } = useCourseContext()
  const content = useCourseContent(course.id, modules.map(m => m.id))
  const info = contentTypeBySlug(section)
  if (!info) return <NotFoundPage />

  // All items of this type in the course, in module order, each with its module.
  const items: ItemInModule[] = modules.flatMap((module, index) =>
    (content.byModule.get(String(module.id)) ?? [])
      .filter(item => item.type === info.type)
      .map(item => ({ ...item, module, moduleNumber: index + 1 })))

  const byModule = (part: ItemInModule[]) => modules
    .map(module => ({ module, items: part.filter(item => item.module.id === module.id) }))
    .filter(group => group.items.length > 0)
    .map(group => (
      <section key={group.module.id} className="type-group" aria-label={group.module.title}>
        <h3 className="type-group-title">
          <span className="type-group-number">Module {group.items[0].moduleNumber}</span>
          <CourseLink to={paths.courseModule(course.id, group.module.id)}>{group.module.title}</CourseLink>
        </h3>
        <ContentList courseId={course.id} moduleId={group.module.id} items={group.items} />
      </section>
    ))

  return (
    <>
      <header className="course-header">
        <h1>{info.plural}</h1>
      </header>

      {content.isPending && <p role="status">Loading…</p>}
      {content.isError && <p role="alert">Could not load the content of some modules.</p>}
      {!content.isPending && items.length === 0 && (
        <p>There are no {info.plural.toLowerCase()} in this course yet.</p>
      )}
      {items.length > 0 && (
        isCompletable(info.type)
          ? <CompletionSections items={items} completed={completed} noun={info.plural.toLowerCase()} render={byModule} />
          : byModule(items)
      )}
    </>
  )
}

type ItemInModule = ContentSummary & { module: CourseModule, moduleNumber: number }
