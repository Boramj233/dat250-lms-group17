import { Link, NavLink, Outlet, useMatch } from 'react-router'
import { paths, type CourseSection } from '../../../../shared/paths'
import SideMenuLayout from '../../components/SideMenuLayout'
import { useCourseView } from '../../courseContext'

/** The course menu, in this order; Home has no section. */
const ITEMS: { label: string, section?: CourseSection }[] = [
  { label: 'Home' },
  { label: 'Modules', section: 'modules' },
  { label: 'Assignments', section: 'assignments' },
  { label: 'People', section: 'people' },
  { label: 'Groups', section: 'groups' },
  { label: 'Quizzes', section: 'quizzes' },
  { label: 'Grades', section: 'grades' },
  { label: 'Files', section: 'files' },
  { label: 'Code Exercises', section: 'code-exercises' },
]

/** The course's pages with the course menu: Manage (teachers), then the sections. */
export default function CourseMenuLayout() {
  const { course, modules, isTeacher, menuOpen, toggleMenu } = useCourseView()
  const onModulesPage = useMatch('/courses/:courseId/modules') !== null
  return (
    <SideMenuLayout
      label="Course menu"
      id="course-menu"
      open={menuOpen}
      onToggle={toggleMenu}
      menu={
        <>
          {isTeacher && (
            <Link className="button-secondary side-menu-button" to={paths.manageCourse(course.id)}>
              Manage
            </Link>
          )}
          <ul className="side-menu-list">
            {ITEMS.map(item => (
              <li key={item.label}>
                <NavLink
                  to={item.section ? paths.courseSection(course.id, item.section) : paths.course(course.id)}
                  end
                >
                  {item.label}
                </NavLink>
                {/* On the Modules page: a shortcut into each module. */}
                {item.section === 'modules' && onModulesPage && modules.length > 0 && (
                  <ul className="side-menu-sublist" aria-label="Modules in this course">
                    {modules.map(module => (
                      <li key={module.id}>
                        <Link to={paths.courseModule(course.id, module.id)}>{module.title}</Link>
                      </li>
                    ))}
                  </ul>
                )}
              </li>
            ))}
          </ul>
        </>
      }
    >
      <Outlet />
    </SideMenuLayout>
  )
}
