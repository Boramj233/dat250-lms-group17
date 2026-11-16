import { paths } from '@shared/paths'
import PageHeader from '@shared/ui/PageHeader'
import DataTable from '@shared/ui/DataTable'
import { contentSection } from '@domain/content/contentTypes'
import type { ContentSummary } from '@domain/content/types'
import type { CourseModule } from '@domain/modules/types'
import { titleColumn, typeColumn } from '../../../components/contentColumns'
import { useCourseView } from '../../../courseContext'
import { useModuleView } from '../../moduleContext'

type Row = { item: ContentSummary; module: CourseModule }

/** The module's start page: all its content in order, as a table with the type of each. */
export default function ModuleOverviewPage() {
  const { course } = useCourseView()
  const { module, items } = useModuleView()
  return (
    <>
      <PageHeader title={module.title} />
      <DataTable<Row>
        caption={`Content in ${module.title}`}
        rows={items.map(item => ({ item, module }))}
        rowKey={({ item }) => String(item.id)}
        empty="This module has no content yet."
        columns={[
          titleColumn(({ item }) => paths.courseContent(course.id, module.id, contentSection(item.type), item.id)),
          typeColumn(),
        ]}
      />
    </>
  )
}
