import type { ReactNode } from 'react'
import { formatDateTime, formatTimeLeft } from '@shared/lib/dateFormat'
import './DetailLayout.css'

type DetailLayoutProps = {
  /** The details panel (deadline, score, …), one or more `<dl className="submittable-facts">`. */
  panel: ReactNode
  /** For screen readers, e.g. "Quiz details". */
  panelLabel: string
  /** The cards. */
  children: ReactNode
}

/**
 * The layout of an item's page (an assignment, a quiz, a submission to grade): cards on
 * the left, a details panel on the right, or on top on narrow screens so the deadline is
 * seen at once.
 */
export default function DetailLayout({ panel, panelLabel, children }: DetailLayoutProps) {
  return (
    <div className="submittable">
      <div className="submittable-main">{children}</div>
      <aside className="submittable-card submittable-panel" aria-label={panelLabel}>{panel}</aside>
    </div>
  )
}

type CardProps = {
  /** The heading; leave out for a box without one. */
  title?: ReactNode
  /** The heading's id; needed with a title. */
  id?: string
  className?: string
  role?: string
  children: ReactNode
}

/** A bordered box with a heading, e.g. the task, the submission or the grade. */
export function Card({ title, id, className, role, children }: CardProps) {
  return (
    <section
      className={className ? `submittable-card ${className}` : 'submittable-card'}
      aria-labelledby={title !== undefined ? id : undefined}
      role={role}
    >
      {title !== undefined && <h2 id={id} className="submittable-card-title">{title}</h2>}
      {children}
    </section>
  )
}

type DeadlineFactProps = {
  deadline: Date | null
  now: Date
  /** Also say whether late submissions are accepted. Leave out to say nothing about it. */
  allowLate?: boolean
}

/** "Deadline" in a details panel, with the time left; for inside a `<dl className="submittable-facts">`. */
export function DeadlineFact({ deadline, now, allowLate }: DeadlineFactProps) {
  return (
    <>
      <dt>Deadline</dt>
      <dd>
        {deadline
          ? <>
              {formatDateTime(deadline)}
              <span className="submittable-muted">
                {' · '}{deadline < now ? 'passed' : formatTimeLeft(deadline, now)}
              </span>
              {allowLate !== undefined && (
                <>
                  <br />
                  <span className="submittable-muted">
                    {allowLate ? 'Late submissions are accepted' : 'Late submissions are not accepted'}
                  </span>
                </>
              )}
            </>
          : 'No deadline'}
      </dd>
    </>
  )
}
