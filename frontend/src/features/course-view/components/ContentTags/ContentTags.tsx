import { useState } from 'react'
import { formatDateTime } from '@shared/lib/dateFormat'
import { effectiveVisibleFrom, isNotYetVisible } from '@domain/content/visibility'
import type { ContentSummary } from '@domain/content/types'
import type { CourseModule } from '@domain/modules/types'

type ContentTagsProps = {
  item: ContentSummary
  /** The item's module, for its start date; leave out where it is not known. */
  module?: CourseModule
  /** The tags' class, so they match the list, table or header they are in. */
  className: string
  /** Each tag on its own line (a <p>), e.g. under a page title; otherwise inline (a <span>). */
  block?: boolean
}

/**
 * "Unpublished · visible to students from …" for an item students cannot see yet (before
 * its own visible date, or its module's start date). Only teachers get such items from
 * the API, so only they see this.
 */
export default function ContentTags({ item, module, className, block }: ContentTagsProps) {
  const [now] = useState(() => Date.now())
  const Tag = block ? 'p' : 'span'
  const from = module && isNotYetVisible(item, module, now) ? effectiveVisibleFrom(item, module) : null
  return (
    from && <Tag className={className}>Unpublished · visible to students from {formatDateTime(new Date(from))}</Tag>
  )
}
