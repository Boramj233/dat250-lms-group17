import { passedLabel } from '@domain/assignments/scoreFormat'
import './PassedBadge.css'

type PassedBadgeProps = {
  /** True: passed, false: not passed. Null or missing: nothing is shown. */
  passed: boolean | null | undefined
}

/**
 * "✓ Passed" (green) or "✗ Not passed" (coral). The icon and the text carry the meaning,
 * so it does not depend on colour alone.
 */
export default function PassedBadge({ passed }: PassedBadgeProps) {
  const label = passedLabel(passed)
  if (!label) return null
  return (
    <span className={`passed-badge ${passed ? 'is-passed' : 'is-not-passed'}`}>
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3"
        strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
        {passed ? <path d="M5 12.5l4.5 4.5L19 7.5" /> : <path d="M6 6l12 12M18 6L6 18" />}
      </svg>
      {label}
    </span>
  )
}
