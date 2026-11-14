import { Link } from 'react-router'
import './CourseMenuHeading.css'

type CourseMenuHeadingProps = {
  title: string
  /** e.g. "Autumn 2026 course". */
  subtitle: string
  /** The course's home page. */
  to: string
}

/** The course the menu belongs to, at the top of the course and module menus. */
export default function CourseMenuHeading({ title, subtitle, to }: CourseMenuHeadingProps) {
  return (
    <div className="course-menu-heading">
      <Link className="course-menu-heading-title" to={to}>{title}</Link>
      <span className="course-menu-heading-subtitle">{subtitle}</span>
    </div>
  )
}
