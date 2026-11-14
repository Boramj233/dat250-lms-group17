import { paths } from '../../../../../shared/paths'
import PageHeader from '../../../../../shared/ui/PageHeader'
import { contentTypeLabel } from '../../../../content/contentTypes'
import ContentList from '../../../components/ContentList'
import { useCourseView } from '../../../courseContext'
import { useModuleView } from '../../moduleContext'

/** The module's start page: all its content in order, with the type of each. */
export default function ModuleOverviewPage() {
  const { course } = useCourseView()
  const { module, items } = useModuleView()
  return (
    <>
      <PageHeader title={module.title} />
      {items.length === 0
        ? <p>This module has no content yet.</p>
        : <ContentList rows={items.map(item => ({
            item,
            to: paths.courseContent(course.id, module.id, item.id),
            meta: contentTypeLabel(item.type),
          }))} />}
    </>
  )
}
