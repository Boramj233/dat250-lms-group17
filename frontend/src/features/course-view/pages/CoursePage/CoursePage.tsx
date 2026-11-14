import { useParams } from 'react-router'
import { TrailLink, TrailPage } from '../../../../shared/navigation/Trail'
import { paths } from '../../../../shared/paths'
import PageHeader from '../../../../shared/ui/PageHeader'
import { contentTypeLabel } from '../../../content/contentTypes'
import { useCourseContent } from '../../../content/hooks'
import type { ContentSummary } from '../../../content/types'
import { formatTerm } from '../../../courses/courseFormat'
import { useCourse, useCourseRole } from '../../../courses/hooks'
import { useCourseModules } from '../../../modules/hooks'
import '../../../../shared/styles/forms.css'
import './CoursePage.css'

/**
 * A course as its members see it: the description, then every module with its content.
 * Teachers also get a link to manage the course, and see content hidden from students.
 */
export default function CoursePage() {
  const { courseId = '' } = useParams()
  const course = useCourse(courseId)
  const modules = useCourseModules(courseId)
  const { role } = useCourseRole(courseId)
  const sortedModules = [...(modules.data ?? [])].sort((a, b) => a.position - b.position)
  const content = useCourseContent(courseId, sortedModules.map(module => module.id))

  if (course.isPending || modules.isPending) return <p role="status">Loading course…</p>
  if (course.error) return <p role="alert">Could not load the course.</p>
  if (modules.error) return <p role="alert">Could not load the modules of this course.</p>

  const isTeacher = role === 'TEACHER'
  // Students only see active content; missing `active` means active.
  const visible = (items: ContentSummary[]) => isTeacher ? items : items.filter(item => item.active !== false)

  return (
    <TrailPage label={course.data.title}>
      <div className="course-page">
        <PageHeader title={course.data.title} kicker={formatTerm(course.data)}>
          {course.data.description && <p>{course.data.description}</p>}
          {isTeacher && (
            <TrailLink className="button-secondary course-page-manage" to={paths.manageCourse(course.data.id)}>
              Manage course
            </TrailLink>
          )}
        </PageHeader>

        <section aria-labelledby="course-modules-title">
          <h2 id="course-modules-title" className="section-title">Modules</h2>
          {sortedModules.length === 0 && <p>This course has no modules yet.</p>}
          {content.isError && <p role="alert">Could not load the content of some modules.</p>}
          <ol className="course-page-modules">
            {sortedModules.map((module, index) => {
              const items = content.byModule.get(String(module.id))
              return (
                <li key={module.id} className="course-page-module">
                  <h3>
                    <span className="course-page-module-number">Module {index + 1}</span>
                    {module.title}
                  </h3>
                  {items === undefined && !content.isError && <p className="course-page-note">Loading…</p>}
                  {items && visible(items).length === 0 && <p className="course-page-note">No content yet.</p>}
                  {items && visible(items).length > 0 && (
                    <ul className="course-page-content">
                      {visible(items).map(item => (
                        <li key={item.id}>
                          <span className="course-page-content-type">{contentTypeLabel(item.type)}</span>
                          <span className="course-page-content-title">{item.title}</span>
                          {item.active === false && <span className="course-page-hidden">Hidden from students</span>}
                        </li>
                      ))}
                    </ul>
                  )}
                </li>
              )
            })}
          </ol>
        </section>
      </div>
    </TrailPage>
  )
}
