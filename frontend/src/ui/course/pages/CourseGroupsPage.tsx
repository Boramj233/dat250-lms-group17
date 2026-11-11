import './CoursePages.css'

/** The course's groups. */
export default function CourseGroupsPage() {
  // TODO: List the course's groups and their members. The backend endpoint
  // GET /api/courses/{courseId}/groups is on the Core/group-enrollment branch; add a
  // data/groups module (types, query, hook) and a mock handler when it is merged.
  return (
    <>
      <header className="course-header">
        <h1>Groups</h1>
      </header>
      <p>Groups are not available yet.</p>
    </>
  )
}
