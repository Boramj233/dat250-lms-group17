import { Link } from 'react-router'
import type { ContentSummary } from '@domain/content/types'
import type { CourseModule } from '@domain/modules/types'
import VisibleFromTag from '../VisibleFromTag'
import './ContentList.css'

export type ContentRow = {
  item: ContentSummary
  /** The item's page. */
  to: string
  /** Shown after the title, e.g. the type or the module it is in. */
  meta?: string
  /** The item's module, for "Visible to students from …" before its visible date. */
  module?: CourseModule
}

/**
 * Content items as links, one per row, with "Hidden from students" for inactive ones and
 * "Visible to students from …" for those students cannot see yet.
 */
export default function ContentList({ rows }: { rows: ContentRow[] }) {
  return (
    <ul className="content-list">
      {rows.map(({ item, to, meta, module }) => (
        <li key={item.id}>
          <Link className="content-list-title" to={to}>{item.title}</Link>
          {item.active === false && <span className="content-list-hidden">Hidden from students</span>}
          {module && <VisibleFromTag item={item} module={module} className="content-list-hidden" />}
          {meta && <span className="content-list-meta">{meta}</span>}
        </li>
      ))}
    </ul>
  )
}
