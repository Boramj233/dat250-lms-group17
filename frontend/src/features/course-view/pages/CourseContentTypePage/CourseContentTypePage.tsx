import type { CourseSection } from '@shared/paths'
import PageHeader from '@shared/ui/PageHeader'
import type { ContentType } from '@domain/content/types'
import { useCourseView } from '../../courseContext'
import { useCourseItems } from '../../useCourseItems'
import ContentTypeTable from '../ContentTypeTable'

type CourseContentTypePageProps = {
  type: ContentType
  /** "Assignments", "Quizzes" or "Code Exercises". */
  title: string
  /** This page's part of the URL; its items open under it (/courses/1/assignments/101). */
  section: CourseSection
}

/** All content of one type in the course as a table, in module order. */
export default function CourseContentTypePage({ type, title, section }: CourseContentTypePageProps) {
  const { course, isTeacher } = useCourseView()
  const content = useCourseItems({ filter: item => item.type === type })

  return (
    <>
      <PageHeader title={title} kicker={course.title} />
      {content.isError && <p role="alert">Could not load the content of some modules.</p>}
      {content.isPending
        ? <p role="status">Loading…</p>
        : (
          <ContentTypeTable
            courseId={course.id}
            type={type}
            title={title}
            place={course.title}
            rows={content.items}
            showModule
            isTeacher={isTeacher}
            courseSection={section}
          />
        )}
    </>
  )
}
