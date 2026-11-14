import PageHeader from '../../../../shared/ui/PageHeader'
import { formatTerm } from '../../../courses/courseFormat'
import { useCourseView } from '../../courseContext'

/** The course's start page: title, term and description. */
export default function CourseHomePage() {
  const { course } = useCourseView()
  return (
    <PageHeader title={course.title} kicker={`${formatTerm(course)} course`}>
      <p>{course.description || 'This course has no description yet.'}</p>
    </PageHeader>
  )
}
