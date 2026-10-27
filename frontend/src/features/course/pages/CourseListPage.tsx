import { Link } from 'react-router'
import { coursePath } from '../../../app/paths'
import { useCoursePreferences } from '../preferences/useCoursePreferences'
import './CourseListPage.css'

export default function CourseListPage() {
  const { courses, setVisible, reorder, savedLocally } = useCoursePreferences()

  return (
    <>
      <h1>My courses</h1>
      <p>Choose which courses appear in the sidebar and change their order. Hiding a course does not unenroll you.</p>
      <p role="status">
        {savedLocally ? 'Changes are saved automatically in this browser.' : 'Changes apply now, but could not be saved in this browser.'}
      </p>
      {courses.length > 0 ? (
        <ol className="course-preferences">
          {courses.map((course, index) => (
            <li className="course-preference" key={course.id}>
              <Link className="course-preference-name" to={coursePath(course.id)}>{course.name}</Link>
              <label className="course-visibility">
                <input
                  type="checkbox"
                  checked={course.visible}
                  onChange={event => setVisible(course.id, event.target.checked)}
                  aria-label={`Show ${course.name} in sidebar`}
                />
                Show in sidebar
              </label>
              <div className="course-order">
                <button
                  type="button"
                  disabled={index === 0}
                  aria-label={`Move ${course.name} up`}
                  onClick={() => reorder(course.id, -1)}
                >Move up</button>
                <button
                  type="button"
                  disabled={index === courses.length - 1}
                  aria-label={`Move ${course.name} down`}
                  onClick={() => reorder(course.id, 1)}
                >Move down</button>
              </div>
            </li>
          ))}
        </ol>
      ) : (
        <p>You are not enrolled in any courses yet.</p>
      )}
    </>
  )
}
