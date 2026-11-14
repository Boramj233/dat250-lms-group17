import { Link } from 'react-router'
import type { ContentSummary } from '../../../content/types'
import './ContentList.css'

export type ContentRow = {
  item: ContentSummary
  /** The item's page. */
  to: string
  /** Shown after the title, e.g. the type or the module it is in. */
  meta?: string
}

/** Content items as links, one per row, with "Hidden from students" for inactive ones. */
export default function ContentList({ rows }: { rows: ContentRow[] }) {
  return (
    <ul className="content-list">
      {rows.map(({ item, to, meta }) => (
        <li key={item.id}>
          <Link className="content-list-title" to={to}>{item.title}</Link>
          {item.active === false && <span className="content-list-hidden">Hidden from students</span>}
          {meta && <span className="content-list-meta">{meta}</span>}
        </li>
      ))}
    </ul>
  )
}
