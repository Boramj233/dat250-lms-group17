import type { ReactNode } from 'react'
import { Outlet } from 'react-router'
import type { Access } from '../../../data/auth/access'
import { useHasAccess } from '../../../data/auth/useAuth'

type RequireAccessProps = {
  /** Lowest global access that may see the content; higher levels are included. */
  level: Access
  /** Shown instead when the user lacks access. Default: nothing. */
  fallback?: ReactNode
  /** The protected content. Leave out to use it as a route element (renders <Outlet />). */
  children?: ReactNode
}

/**
 * Shows its content only to users with at least `level` global access.
 *
 * Part of a page:
 *   <RequireAccess level="ADMIN"><NavLink to="/users">Users</NavLink></RequireAccess>
 *
 * Whole pages (route guard, also stops users who type the address):
 *   <Route element={<RequireAccess level="COURSE_MANAGER" fallback={<NotFoundPage />} />}>
 *     <Route path="/courses/new" element={<NewCoursePage />} />
 *   </Route>
 *
 * This only decides what the UI shows; the backend checks every API call itself.
 */
export default function RequireAccess({ level, fallback = null, children }: RequireAccessProps) {
  const allowed = useHasAccess(level)
  if (!allowed) return <>{fallback}</>
  return <>{children ?? <Outlet />}</>
}
