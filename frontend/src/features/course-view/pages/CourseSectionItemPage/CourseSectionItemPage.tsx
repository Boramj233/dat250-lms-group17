import { useParams } from 'react-router'
import { Breadcrumbs } from '@shared/navigation/Trail'
import { paths, type CourseSection } from '@shared/paths'
import { useCourseView } from '../../courseContext'
import { useCourseItem } from '../../useCourseItems'
import ContentView from '../../content/ContentView'

type CourseSectionItemPageProps = {
  /** The course page it was opened from, e.g. assignments or grades. */
  section: CourseSection
  /** That page's name, e.g. "Assignments", for the breadcrumbs. */
  label: string
}

/**
 * One content item opened from a course page, with the course menu:
 * /courses/1/assignments/101 shows Course › Assignments › module › item. The same item in its
 * module is /courses/1/modules/2/assignments/101 (ModuleContentPage).
 */
export default function CourseSectionItemPage({ section, label }: CourseSectionItemPageProps) {
  const { contentId = '' } = useParams()
  const { course, isTeacher } = useCourseView()
  // The same queries as the course's lists, so this is read from the cache.
  const content = useCourseItem(contentId)
  const { found } = content

  const crumbs = [
    { label: course.title, to: paths.course(course.id) },
    { label, to: paths.courseSection(course.id, section) },
    // The module the item is in, to go to it.
    ...(found ? [{ label: found.module.title, to: paths.courseModule(course.id, found.module.id) }] : []),
    { label: found?.item.title ?? '…' },
  ]

  return (
    <>
      <Breadcrumbs items={crumbs} />
      {found
        ? (
          <ContentView summary={found.item} isTeacher={isTeacher} module={found.module} />
        )
        : content.isPending
          ? <p role="status">Loading…</p>
          : <p role="alert">This content does not exist in {course.title}.</p>}
    </>
  )
}
