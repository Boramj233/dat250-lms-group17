import type { Content } from '@domain/content/types'

/**
 * "Mandatory" (bold) or "Optional" for an assignment (its `mandatoryForExam`), for table
 * cells and details. "…" while the details load; "–" for other content.
 */
export default function MandatoryLabel({ content }: { content?: Content }) {
  if (!content) return '…'
  if (content.type !== 'ASSIGNMENT') return <span className="text-meta">–</span>
  return content.mandatoryForExam ? (
    <span className="data-table-strong">Mandatory</span>
  ) : (
    <span className="text-meta">Optional</span>
  )
}
