import { useState } from 'react'
import { useUpdateCourse } from '@domain/courses/hooks'
import CourseForm from '@domain/courses/components/CourseForm'
import type { CreateCourseInput } from '@domain/courses/types'
import { useLeaveGuard } from '@shared/ui/SaveBar'
import { useManageCourse } from '../../manageCourseContext'

/**
 * The Details tab: change the course's title, description, term and year. Save changes
 * saves them; Cancel goes back to what is saved. Leaving with unsaved changes asks first.
 */
export default function CourseDetailsPage() {
  const { course } = useManageCourse()
  const updateCourse = useUpdateCourse()
  const [saved, setSaved] = useState(false)
  const [dirty, setDirty] = useState(false)
  // A new key starts the form again from the saved course (Cancel, or after saving).
  const [version, setVersion] = useState(0)
  const leaveDialog = useLeaveGuard(dirty)
  const { title, description, term, year } = course
  const initial = { title, description, term, year }

  function changed(values: CreateCourseInput) {
    setSaved(false)
    setDirty(
      values.title !== title || values.description !== description || values.term !== term || values.year !== year,
    )
  }

  function cancel() {
    setDirty(false)
    updateCourse.reset()
    setVersion(v => v + 1)
  }

  return (
    <>
      <CourseForm
        key={version}
        initial={initial}
        submitLabel="Save changes"
        saving={updateCourse.isPending}
        error={updateCourse.error && 'Could not save the course. Try again.'}
        onChange={changed}
        actions={
          <>
            <button
              type="button"
              className="button-secondary"
              disabled={!dirty || updateCourse.isPending}
              onClick={cancel}
            >
              Cancel
            </button>
            {dirty && !updateCourse.isPending && <span className="form-hint">Unsaved changes</span>}
          </>
        }
        onSubmit={changes => {
          setSaved(false)
          updateCourse.mutate(
            { courseId: course.id, changes },
            {
              onSuccess: () => {
                setSaved(true)
                setDirty(false)
              },
            },
          )
        }}
      />
      {saved && (
        <p className="form-success" role="status">
          The course was saved.
        </p>
      )}
      {leaveDialog}
    </>
  )
}
