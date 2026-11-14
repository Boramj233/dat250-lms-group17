import PageHeader from '../../../../shared/ui/PageHeader'
import { useCourseView } from '../../courseContext'

/** A course or module section that has no backend support yet (Groups, Files). */
export default function CoursePlaceholderPage({ title }: { title: string }) {
  const { course } = useCourseView()
  return (
    <>
      <PageHeader title={title} kicker={course.title} />
      <p>This part of the course is not available yet.</p>
    </>
  )
}
