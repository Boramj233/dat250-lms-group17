import { formatDateTime } from '@shared/lib/dateFormat'
import { TrailLink } from '@shared/navigation/Trail'
import { paths } from '@shared/paths'
import type { CourseId } from '@shared/types'
import type { Column } from '@shared/ui/DataTable'
import { contentTypeLabel, isSubmittable } from '@domain/content/contentTypes'
import type { Content, ContentSummary } from '@domain/content/types'
import type { CourseModule } from '@domain/modules/types'
import ContentTags from './ContentTags'

// Columns that several tables of content items share. A row has the item and its module,
// and for the deadline its details (undefined while they load).

type ItemRow = { item: ContentSummary; module: CourseModule }

/** The item's title as a link (to `to(row)`), with what students cannot see of it. */
export function titleColumn<R extends ItemRow>(to: (row: R) => string): Column<R> {
  return {
    header: 'Title',
    cell: row => (
      <>
        <TrailLink to={to(row)}>{row.item.title}</TrailLink>
        <ContentTags item={row.item} module={row.module} className="data-table-tag" />
      </>
    ),
  }
}

/** The module the item is in, as a link to it. */
export function moduleColumn<R extends ItemRow>(courseId: CourseId): Column<R> {
  return {
    header: 'Module',
    cell: ({ module }) => <TrailLink to={paths.courseModule(courseId, module.id)}>{module.title}</TrailLink>,
  }
}

/** "Assignment", "Quiz", … */
export function typeColumn<R extends ItemRow>(): Column<R> {
  return { header: 'Type', cell: ({ item }) => <span className="text-meta">{contentTypeLabel(item.type)}</span> }
}

/** The deadline of an assignment or coding exercise: "…" while loading, "–" for other types. */
export function deadlineColumn<R extends { details?: Content }>(): Column<R> {
  return { header: 'Deadline', cell: ({ details }) => deadlineText(details) }
}

function deadlineText(content: Content | undefined): string {
  if (!content) return '…'
  if (!isSubmittable(content)) return '–'
  return content.deadline ? formatDateTime(new Date(content.deadline)) : 'No deadline'
}
