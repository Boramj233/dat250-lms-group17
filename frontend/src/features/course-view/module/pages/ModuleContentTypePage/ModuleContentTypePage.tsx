import PageHeader from '@shared/ui/PageHeader'
import type { ContentType } from '@domain/content/types'
import { useCourseView } from '../../../courseContext'
import ContentTypeTable from '../../../content/ContentTypeTable'
import { useModuleView } from '../../moduleContext'

type ModuleContentTypePageProps = {
  type: ContentType
  /** "Pages", "Assignments", "Quizzes" or "Code Exercises". */
  title: string
}

/** The module's content of one type as a table, like the course's pages. */
export default function ModuleContentTypePage({ type, title }: ModuleContentTypePageProps) {
  const { course, isTeacher } = useCourseView()
  const { module, items } = useModuleView()
  return (
    <>
      <PageHeader title={title} kicker={module.title} />
      <ContentTypeTable
        courseId={course.id}
        type={type}
        title={title}
        place={module.title}
        rows={items.filter(item => item.type === type).map(item => ({ item, module }))}
        showModule={false}
        isTeacher={isTeacher}
      />
    </>
  )
}
