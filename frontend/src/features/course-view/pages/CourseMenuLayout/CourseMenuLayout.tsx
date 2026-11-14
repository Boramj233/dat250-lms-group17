import { Link, NavLink, Outlet, useMatch } from 'react-router'
import { paths, type CourseSection } from '../../../../shared/paths'
import { useCourseContent } from '../../../content/hooks'
import { formatTerm } from '../../../courses/courseFormat'
import CourseMenuHeading from '../../components/CourseMenuHeading'
import SideMenuLayout from '../../components/SideMenuLayout'
import { CONTENT_SECTIONS } from '../../contentSections'
import { useCourseView, visibleContent } from '../../courseContext'

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

type SubItem = { key: string, label: string, to: string }

/**
 * The course's pages with the course menu: Manage (teachers), then the sections. The
 * current section unfolds a list of shortcuts: its modules, or its assignments,
 * quizzes or code exercises.
 */
export default function CourseMenuLayout() {
  const { course, modules, isTeacher, menuOpen, toggleMenu } = useCourseView()
  const current = useMatch('/courses/:courseId/:section')?.params.section
  const contentSection = CONTENT_SECTIONS.find(s => s.section === current)
  // The same queries as the content pages, so this is read from the cache. Only
  // fetched on a content type's page.
  const content = useCourseContent(course.id, contentSection ? modules.map(m => m.id) : [])

  function subItems(section: CourseSection | undefined): SubItem[] {
    if (section !== current) return []
    if (section === 'modules') {
      return modules.map(module => ({
        key: String(module.id), label: module.title, to: paths.courseModule(course.id, module.id),
      }))
    }
    if (contentSection && section === contentSection.section) {
      return modules.flatMap(module =>
        visibleContent(content.byModule.get(String(module.id)) ?? [], isTeacher)
          .filter(item => item.type === contentSection.type)
          .map(item => ({
            key: String(item.id), label: item.title, to: paths.courseContent(course.id, module.id, item.id),
          })))
    }
    return []
  }

  return (
    <SideMenuLayout
      label="Course menu"
      id="course-menu"
      open={menuOpen}
      onToggle={toggleMenu}
      menu={
        <>
          <CourseMenuHeading
            title={course.title}
            subtitle={`${formatTerm(course)} course`}
            to={paths.course(course.id)}
          />
          {isTeacher && (
            <Link className="button-secondary side-menu-button" to={paths.manageCourse(course.id)}>
              Manage
            </Link>
          )}
          <ul className="side-menu-list">
            {ITEMS.map(item => {
              const sub = subItems(item.section)
              return (
                <li key={item.label}>
                  <NavLink
                    to={item.section ? paths.courseSection(course.id, item.section) : paths.course(course.id)}
                    end
                  >
                    {item.label}
                  </NavLink>
                  {/* On a section's page: a shortcut to each of its modules or items. */}
                  {sub.length > 0 && (
                    <ul className="side-menu-sublist" aria-label={`${item.label} in this course`}>
                      {sub.map(entry => (
                        <li key={entry.key}>
                          <Link to={entry.to}>{entry.label}</Link>
                        </li>
                      ))}
                    </ul>
                  )}
                </li>
              )
            })}
          </ul>
        </>
      }
    >
      <Outlet />
    </SideMenuLayout>
  )
}
