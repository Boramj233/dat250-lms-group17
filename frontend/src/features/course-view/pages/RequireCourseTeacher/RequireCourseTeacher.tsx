import { Outlet } from 'react-router'
import { useCourseView } from '../../courseContext'

/**
 * Route guard for the course's teacher pages (Submissions, grading): teachers of the course
 * get the page, everyone else a message, and the page is not even loaded for them. Only
 * what the UI shows; the backend checks every call itself.
 *
 *   <Route element={<RequireCourseTeacher />}>
 *     <Route path="submissions" element={<CourseSubmissionsPage />} />
 *   </Route>
 */
export default function RequireCourseTeacher() {
  const { course, isTeacher } = useCourseView()
  if (!isTeacher) return <p role="alert">Only teachers of {course.title} can see this page.</p>
  return <Outlet />
}
