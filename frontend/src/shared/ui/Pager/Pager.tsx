import type { ComponentType, ReactNode } from 'react'
import { Link } from 'react-router'
import './Pager.css'

type LinkComponent = ComponentType<{ to: string, className?: string, children: ReactNode }>

export type PagerItem = {
  to: string
  title: ReactNode
}

type PagerProps = {
  /** Name of the navigation for screen readers, e.g. "Modules". */
  label: string
  previous?: PagerItem
  next?: PagerItem
  /** Text above the previous item's title. Default: "Previous". */
  previousText?: string
  /** Text above the next item's title. Default: "Next". */
  nextText?: string
  /** Link component, e.g. CourseLink to keep the breadcrumbs. Default: Link. */
  link?: LinkComponent
}

/** Links to the previous and next item, e.g. at the bottom of module and content pages. */
export default function Pager(
  { label, previous, next, previousText = 'Previous', nextText = 'Next', link: LinkTo = Link }: PagerProps,
) {
  if (!previous && !next) return null
  return (
    <nav className="pager" aria-label={label}>
      {previous && (
        <LinkTo className="pager-link is-previous" to={previous.to}>
          <span className="pager-direction">{previousText}</span>
          <span className="pager-title">{previous.title}</span>
        </LinkTo>
      )}
      {next && (
        <LinkTo className="pager-link is-next" to={next.to}>
          <span className="pager-direction">{nextText}</span>
          <span className="pager-title">{next.title}</span>
        </LinkTo>
      )}
    </nav>
  )
}
