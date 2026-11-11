import { Link, NavLink, type LinkProps, type NavLinkProps } from 'react-router'
import { useCourseLinkState } from './courseContext'

/** Link between pages in a course; keeps the breadcrumbs from outside the course. */
export function CourseLink(props: Omit<LinkProps, 'state'>) {
  return <Link state={useCourseLinkState()} {...props} />
}

/** NavLink for the course sidebar; keeps the breadcrumbs from outside the course. */
export function CourseNavLink(props: Omit<NavLinkProps, 'state'>) {
  return <NavLink state={useCourseLinkState()} {...props} />
}
