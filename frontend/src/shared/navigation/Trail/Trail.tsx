import { useContext, type ReactNode } from 'react'
import { Link, useLocation, type LinkProps } from 'react-router'
import { TrailContext } from '../trailContext'
import { type BreadcrumbItem, nextTrail, readTrail, trailItems } from '../trailState'
import './Breadcrumbs.css'

export type { BreadcrumbItem } from '../trailState'

/** A breadcrumb bar. The last item is the current page; earlier items are links. */
export function Breadcrumbs({ items }: { items: BreadcrumbItem[] }) {
  if (items.length < 2) return null
  return (
    <nav className="breadcrumbs" aria-label="Breadcrumb">
      <ol>
        {items.map((item, index) => {
          const isCurrent = index === items.length - 1
          return (
            <li key={`${item.to ?? 'current'}-${index}`} aria-current={isCurrent ? 'page' : undefined}>
              {isCurrent || !item.to ? (
                item.label
              ) : (
                <Link to={item.to} state={item.state}>
                  {item.label}
                </Link>
              )}
            </li>
          )
        })}
      </ol>
    </nav>
  )
}

type TrailPageProps = {
  /** This page's name in breadcrumbs, e.g. 'Dashboard' or the course title. */
  label: string
  /** Hide the breadcrumb bar when the page draws its own (the course layout does). */
  hideBreadcrumbs?: boolean
  children: ReactNode
}

/**
 * Wrap a page in this to show its breadcrumbs (when the user came through links in
 * other pages) and to let TrailLinks inside it carry the trail on to the next page.
 */
export function TrailPage({ label, hideBreadcrumbs = false, children }: TrailPageProps) {
  const location = useLocation()
  const trail = readTrail(location.state)
  const current = { label, to: location.pathname + location.search }

  return (
    <TrailContext.Provider value={{ trail, current }}>
      {!hideBreadcrumbs && <Breadcrumbs items={[...trailItems(trail), { label }]} />}
      {children}
    </TrailContext.Provider>
  )
}

type TrailLinkProps = Omit<LinkProps, 'to' | 'state'> & { to: string }

/**
 * A Link for use inside pages (cards, lists, "All courses"): the next page shows
 * breadcrumbs back to this one. Use plain Link/NavLink in the sidebar and menus.
 */
export function TrailLink({ to, ...props }: TrailLinkProps) {
  const context = useContext(TrailContext)
  const state = context ? { trail: nextTrail(context.trail, context.current, to) } : undefined
  return <Link to={to} state={state} {...props} />
}
