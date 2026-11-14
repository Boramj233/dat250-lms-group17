import { paths } from '../../../../../shared/paths'
import PageHeader from '../../../../../shared/ui/PageHeader'
import type { ContentType } from '../../../../content/types'
import ContentList from '../../../components/ContentList'
import { useCourseView } from '../../../courseContext'
import { useModuleView } from '../../moduleContext'

type ModuleContentTypePageProps = {
  type: ContentType
  /** "Pages", "Assignments", "Quizzes" or "Code Exercises". */
  title: string
}

/** The module's content of one type, in order. */
export default function ModuleContentTypePage({ type, title }: ModuleContentTypePageProps) {
  const { course } = useCourseView()
  const { module, items } = useModuleView()
  const ofType = items.filter(item => item.type === type)
  return (
    <>
      <PageHeader title={title} kicker={module.title} />
      {ofType.length === 0
        ? <p>No {title.toLowerCase()} in this module yet.</p>
        : <ContentList rows={ofType.map(item => ({
            item,
            to: paths.courseContent(course.id, module.id, item.id),
          }))} />}
    </>
  )
}
