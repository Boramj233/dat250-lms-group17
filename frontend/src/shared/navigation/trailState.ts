// The breadcrumb trail: the pages the user went through by following links inside
// pages. It is kept in the history entry's state (react-router `location.state`), so it
// survives a reload and works with the browser's back button. Links in the sidebar and
// menus pass no state, so a page opened from there has no trail.

export type Crumb = {
  label: string
  /** Path and query of the page, e.g. '/courses?tab=old'. */
  to: string
}

/** Longest trail kept; older steps are dropped. */
export const MAX_TRAIL = 5

/** The trail in a location's state, or [] if there is none or it is malformed. */
export function readTrail(state: unknown): Crumb[] {
  const trail = (state as { trail?: unknown } | null)?.trail
  if (!Array.isArray(trail)) return []
  return trail.filter((crumb): crumb is Crumb => typeof crumb?.label === 'string' && typeof crumb?.to === 'string')
}

/**
 * The trail for a link from the current page to `to`: the current trail plus the
 * current page. If `to` is already in the trail (going back up), the trail is cut
 * there instead, so it never loops.
 */
export function nextTrail(trail: Crumb[], current: Crumb, to: string): Crumb[] {
  const full = [...trail, current]
  const index = full.findIndex(crumb => crumb.to === to)
  if (index >= 0) return full.slice(0, index)
  return full.slice(-MAX_TRAIL)
}

export type BreadcrumbItem = {
  label: string
  /** Leave out for the current page (the last item). */
  to?: string
  /** History state for the link, e.g. the trail that led to that page. */
  state?: unknown
}

/** Breadcrumb items for a trail; going back to a step keeps the trail that led to it. */
export function trailItems(trail: Crumb[]): BreadcrumbItem[] {
  return trail.map((crumb, index) => ({ ...crumb, state: { trail: trail.slice(0, index) } }))
}
