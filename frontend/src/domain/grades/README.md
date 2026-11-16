# Grades API contract (proposed)

The grades pages (all courses at `/grades`, one course at `/courses/{id}/grades`)
list every assignment and coding exercise the user has submitted, split into
**Graded** and **Awaiting grade**. The backend has no grades yet, so this endpoint
is a proposal:

| Method | Endpoint               | Request body | Response       |
| ------ | ---------------------- | ------------ | -------------- |
| GET    | `/api/users/me/grades` | —            | `GradeEntry[]` |

One entry per assignment / coding exercise with at least one submission by the user
(or their group), in courses the user is enrolled in. It describes the latest
submission:

```json
{
  "contentId": 104,
  "type": "ASSIGNMENT",
  "title": "Assignment 1",
  "courseId": 1,
  "courseTitle": "DAT250",
  "moduleId": 1,
  "submissionId": 1,
  "submittedAt": "2026-11-07T19:00:00Z",
  "late": false,
  "grade": { "score": 85, "feedback": "Good work!", "releasedAt": "2026-11-10T08:30:00Z" }
}
```

`grade` is the released grade, the same as the backend's `ReleasedGradeResponse`
(`GET /api/submissions/{id}/grade`) without `submissionId`: a score from 0 to 100.
It is `null` while the submission is not graded or the grade is not released.
Unreleased grades must not be sent to students. Without this endpoint the frontend
would need one request per submission.

```tsx
const grades = useMyGrades()
```
