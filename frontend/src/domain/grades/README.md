# Grades API contract

The grades pages (all courses at `/grades`, one course at `/courses/{id}/grades`)
list every assignment and coding exercise the user has submitted, split into
**Graded** and **Awaiting grade**. Both use one backend endpoint
(`UserController`, `UserGradeResponse`):

| Method | Endpoint               | Request body | Response       |
| ------ | ---------------------- | ------------ | -------------- |
| GET    | `/api/users/me/grades` | —            | `GradeEntry[]` |

One entry per assignment / coding exercise the user handed in (alone or as a member
of a group submission), in courses where the user has an active enrollment, newest
first. It describes the latest submission:

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
  "grade": { "score": 85, "feedback": "Good work!", "passed": true, "releasedAt": "2026-11-10T08:30:00Z" }
}
```

`grade` is the released grade, the same as the backend's `ReleasedGradeResponse`
(`GET /api/submissions/{id}/grade`) without `submissionId`: a score from 0 to 100,
optional feedback, and `passed` (`null` when not decided). It is `null` while the
submission is not graded or the grade is not released; unreleased grades are never
sent to students.

```tsx
const grades = useMyGrades()
```
