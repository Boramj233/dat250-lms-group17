import type { Content } from '../../../content/types'

/**
 * "Mandatory" (bold) or "Optional" for an assignment, for table cells and details.
 * "…" while the details load; "–" for other content or when the backend does not send
 * `mandatory` (proposed field).
 */
export default function MandatoryLabel({ content }: { content?: Content }) {
  if (!content) return '…'
  if (content.type !== 'ASSIGNMENT' || content.mandatory === undefined) return <span className="data-table-muted">–</span>
  return content.mandatory
    ? <span className="data-table-strong">Mandatory</span>
    : <span className="data-table-muted">Optional</span>
}
