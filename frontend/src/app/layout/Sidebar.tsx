import { NavLink } from 'react-router'
import { useCoursePreferences } from '../../features/course'
import { coursePath, paths } from '../paths'
import './Sidebar.css'

type SidebarProps = {
  isOpen: boolean
  onNavigate: () => void
}

export default function Sidebar({ isOpen, onNavigate }: SidebarProps) {
  const { courses } = useCoursePreferences()
  const visibleCourses = courses.filter(course => course.visible)

  return (
    <nav
      id="sidebar-navigation"
      className={`sidebar${isOpen ? ' is-open' : ''}`}
      aria-label="Sidebar navigation"
    >
      <section aria-labelledby="sidebar-courses-heading">
        <h2 className="sidebar-heading" id="sidebar-courses-heading">
          <NavLink to={paths.courses} end onClick={onNavigate}>My courses</NavLink>
        </h2>
        {visibleCourses.length > 0 ? (
          <ul className="sidebar-courses">
            {visibleCourses.map(course => (
              <li key={course.id}>
                <NavLink to={coursePath(course.id)} onClick={onNavigate}>
                  {course.name}
                </NavLink>
              </li>
            ))}
          </ul>
        ) : (
          <p className="sidebar-empty">
            {courses.length > 0 ? 'No courses shown. Choose courses in My courses.' : 'You are not enrolled in any courses yet.'}
          </p>
        )}
      </section>
    </nav>
  )
}
