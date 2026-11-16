import { TrailLink } from '@shared/navigation/Trail'
import { paths } from '@shared/paths'
import PageHeader from '@shared/ui/PageHeader'
import { contentSection, contentTypeLabel } from '@domain/content/contentTypes'
import type { ContentSummary } from '@domain/content/types'
import DataTable from '../../../components/DataTable'
import VisibleFromTag from '../../../components/VisibleFromTag'
import { useCourseView } from '../../../courseContext'
import { useModuleView } from '../../moduleContext'

/** The module's start page: all its content in order, as a table with the type of each. */
export default function ModuleOverviewPage() {
  const { course } = useCourseView()
  const { module, items } = useModuleView()
  return (
    <>
      <PageHeader title={module.title} />
      <DataTable<ContentSummary>
        caption={`Content in ${module.title}`}
        rows={items}
        rowKey={item => String(item.id)}
        empty="This module has no content yet."
        columns={[
          {
            header: 'Title',
            cell: item => (
              <>
                <TrailLink to={paths.courseContent(course.id, module.id, contentSection(item.type), item.id)}>{item.title}</TrailLink>
                {item.active === false && <span className="data-table-tag">Hidden from students</span>}
                <VisibleFromTag item={item} module={module} className="data-table-tag" />
              </>
            ),
          },
          { header: 'Type', cell: item => <span className="data-table-muted">{contentTypeLabel(item.type)}</span> },
        ]}
      />
    </>
  )
}
