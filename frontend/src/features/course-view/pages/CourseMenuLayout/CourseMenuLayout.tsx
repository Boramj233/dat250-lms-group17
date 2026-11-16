import { Link, NavLink, Outlet, useMatch } from 'react-router'
import { paths, type CourseSection } from '@shared/paths'
import { isSubmittable } from '@domain/content/contentTypes'
import { formatTerm } from '@domain/courses/courseFormat'
import CourseMenuHeading from '../../components/CourseMenuHeading'
import SideMenuLayout from '../../components/SideMenuLayout'
import { CONTENT_SECTIONS } from '../../contentSections'
import { courseMenuItems, courseMenuPath } from '../../courseMenu'
import { useCourseView } from '../../courseContext'
import { useCourseItems } from '../../useCourseItems'

type SubItem = { key: string; label: string; to: string }

/**
 * The course's pages with the course menu: Manage (teachers), then the sections. The
 * current section unfolds a list of shortcuts: its modules, or its assignments,
 * quizzes or code exercises.
 */
export default function CourseMenuLayout() {
  const { course, modules, isTeacher, menuOpen, toggleMenu } = useCourseView()
  // The section, also on its content pages (/courses/1/assignments/101).
  const current = useMatch('/courses/:courseId/:section/*')?.params.section
  const contentSection = CONTENT_SECTIONS.find(s => s.section === current)
  const onSubmissions = current === 'submissions' && isTeacher
  // The same queries as the content pages, so this is read from the cache. Only
  // fetched on a content type's page and on Submissions.
  const { items } = useCourseItems({ enabled: contentSection !== undefined || onSubmissions })

  function subItems(section: CourseSection | undefined): SubItem[] {
    if (section !== current) return []
    if (section === 'modules') {
      return modules.map(module => ({
        key: String(module.id),
        label: module.title,
        to: paths.courseModule(course.id, module.id),
      }))
    }
    if (contentSection && section === contentSection.section) {
      return items
        .filter(({ item }) => item.type === contentSection.type)
        .map(({ item }) => ({
          key: String(item.id),
          label: item.title,
          to: paths.courseSectionContent(course.id, section, item.id),
        }))
    }
    if (section === 'submissions' && onSubmissions) {
      // Everything students hand in: assignments and code exercises.
      return items
        .filter(({ item }) => isSubmittable(item))
        .map(({ item }) => ({
          key: String(item.id),
          label: item.title,
          to: paths.courseSectionContent(course.id, 'submissions', item.id),
        }))
    }
    return []
  }

  return (
    <SideMenuLayout
      label="Course menu"
      id="course-menu"
      open={menuOpen}
      onToggle={toggleMenu}
      heading={
        <CourseMenuHeading
          title={course.title}
          subtitle={`${formatTerm(course)} course`}
          to={paths.course(course.id)}
        />
      }
      menu={
        <>
          {isTeacher && (
            <Link className="button-secondary side-menu-button" to={paths.manageCourse(course.id)}>
              Manage
            </Link>
          )}
          <ul className="side-menu-list">
            {courseMenuItems(isTeacher).map(item => {
              const sub = subItems(item.section)
              return (
                <li key={item.label}>
                  <NavLink to={courseMenuPath(course.id, item.section)} end={!item.section}>
                    {item.label}
                  </NavLink>
                  {/* On a section's page: a shortcut to each of its modules or items. */}
                  {sub.length > 0 && (
                    <ul className="side-menu-sublist" aria-label={`${item.label} in this course`}>
                      {sub.map(entry => (
                        <li key={entry.key}>
                          <NavLink to={entry.to}>{entry.label}</NavLink>
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
