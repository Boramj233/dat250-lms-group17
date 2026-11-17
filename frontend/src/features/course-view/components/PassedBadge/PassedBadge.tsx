import { passedLabel } from '@domain/assignments/scoreFormat'
import './PassedBadge.css'
import { CheckIcon, CloseIcon } from '@shared/ui/icons'

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
      {passed ? <CheckIcon size={14} strokeWidth={3} /> : <CloseIcon size={14} strokeWidth={3} />}
      {label}
    </span>
  )
}
