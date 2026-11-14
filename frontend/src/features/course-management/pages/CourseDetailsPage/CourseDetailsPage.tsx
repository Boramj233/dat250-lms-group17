import { useState } from 'react'
import { useUpdateCourse } from '../../../courses/hooks'
import CourseForm from '../../../courses/components/CourseForm'
import { useManageCourse } from '../../manageCourseContext'

/** The Details tab: change the course's title, description, term and year. */
export default function CourseDetailsPage() {
  const { course } = useManageCourse()
  const updateCourse = useUpdateCourse()
  const [saved, setSaved] = useState(false)
  const { title, description, term, year } = course

  return (
    <>
      <CourseForm
        initial={{ title, description, term, year }}
        submitLabel="Save changes"
        saving={updateCourse.isPending}
        error={updateCourse.error && 'Could not save the course. Try again.'}
        onSubmit={changes => {
          setSaved(false)
          updateCourse.mutate({ courseId: course.id, changes }, { onSuccess: () => setSaved(true) })
        }}
      />
      {saved && <p className="form-success" role="status">The course was saved.</p>}
    </>
  )
}
