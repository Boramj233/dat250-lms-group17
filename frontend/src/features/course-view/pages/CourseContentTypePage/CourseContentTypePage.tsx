import { paths } from '../../../../shared/paths'
import PageHeader from '../../../../shared/ui/PageHeader'
import { useCourseContent } from '../../../content/hooks'
import type { ContentType } from '../../../content/types'
import ContentList from '../../components/ContentList'
import { useCourseView, visibleContent } from '../../courseContext'

type CourseContentTypePageProps = {
  type: ContentType
  /** "Assignments", "Quizzes" or "Code Exercises". */
  title: string
}

/** All content of one type in the course, in module order, with the module it is in. */
export default function CourseContentTypePage({ type, title }: CourseContentTypePageProps) {
  const { course, modules, isTeacher } = useCourseView()
  const content = useCourseContent(course.id, modules.map(module => module.id))

  const rows = modules.flatMap(module =>
    visibleContent(content.byModule.get(String(module.id)) ?? [], isTeacher)
      .filter(item => item.type === type)
      .map(item => ({ item, to: paths.courseContent(course.id, module.id, item.id), meta: module.title })))

  return (
    <>
      <PageHeader title={title} />
      {content.isError && <p role="alert">Could not load the content of some modules.</p>}
      {content.isPending && rows.length === 0 && <p role="status">Loading…</p>}
      {!content.isPending && rows.length === 0 && <p>No {title.toLowerCase()} in this course yet.</p>}
      {rows.length > 0 && <ContentList rows={rows} />}
    </>
  )
}
