import { useState } from 'react'
import { formatDateTime } from '../../../../shared/lib/dateFormat'
import { effectiveVisibleFrom, isNotYetVisible } from '../../../content/visibility'
import type { ContentSummary } from '../../../content/types'
import type { CourseModule } from '../../../modules/types'

type VisibleFromTagProps = {
  item: ContentSummary
  module: CourseModule
  /** The tag's class, so it matches the list or table it is in. */
  className: string
}

/**
 * "Unpublished · visible to students from …" while students cannot see the item yet (its own visible
 * date, or its module's start date). Only teachers get such items from the API.
 */
export default function VisibleFromTag({ item, module, className }: VisibleFromTagProps) {
  const [now] = useState(() => Date.now())
  if (!isNotYetVisible(item, module, now)) return null
  const from = effectiveVisibleFrom(item, module)!
  return <span className={className}>Unpublished · visible to students from {formatDateTime(new Date(from))}</span>
}
