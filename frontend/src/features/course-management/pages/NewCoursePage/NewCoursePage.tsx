import { useState } from 'react'
import { useNavigate } from 'react-router'
import { paths } from '../../../../shared/paths'
import { useCreateCourse } from '../../../courses/hooks'
import CourseForm from '../../../courses/components/CourseForm'
import { termOf } from '../../../courses/courseFormat'
import { TrailLink, TrailPage } from '../../../../shared/navigation/Trail'

/**
 * Course managers create a course here. The backend must make the creator a teacher
 * of the new course, so it shows up under "Manage courses" and can be edited.
 */
export default function NewCoursePage() {
  const navigate = useNavigate()
  const createCourse = useCreateCourse()
  const [thisTerm] = useState(() => termOf(new Date()))

  return (
    <TrailPage label="New course">
      <h1>New course</h1>
      <CourseForm
        initial={{ title: '', description: '', ...thisTerm }}
        submitLabel="Create course"
        saving={createCourse.isPending}
        error={createCourse.error && 'Could not create the course. Try again.'}
        onSubmit={values => createCourse.mutate(values, { onSuccess: () => navigate(paths.manageCourses) })}
        actions={<TrailLink className="button-secondary" to={paths.manageCourses}>Cancel</TrailLink>}
      />
    </TrailPage>
  )
}
