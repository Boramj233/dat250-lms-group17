# Deadlines API contract

The dashboard shows the upcoming deadlines from all of the user's courses in
one list. The backend collects them (`UserController`, `UpcomingDeadlineResponse`),
so the frontend needs a single request.

| Method | Endpoint                                 | Request body | Response             |
| ------ | ---------------------------------------- | ------------ | -------------------- |
| GET    | `/api/users/{userId}/upcoming-deadlines` | —            | `UpcomingDeadline[]` |

Only the user themselves or an `ADMIN` may call it (else 403). The response contains
assignments and coding exercises that:

- are in a course the user has an active enrollment in (as student or teacher),
- have a `deadline` that has not passed yet (content without a deadline is left out), and
- for students: are inside their `visibleFrom`/`visibleUntil` window (teachers of the
  course get them regardless),

sorted by `deadline`, earliest first.

```json
[
  {
    "contentId": 12,
    "type": "ASSIGNMENT",
    "title": "Assignment 1",
    "deadline": "2026-11-14T22:59:00Z",
    "allowLateSubmission": false,
    "mandatoryForExam": true,
    "courseId": 1,
    "courseTitle": "DAT250",
    "moduleId": 3
  }
]
```

`type` is `ASSIGNMENT` or `CODING_EXERCISE`. `mandatoryForExam` is the assignment's
flag (see the content README) and `null` for coding exercises. `deadline` is an
ISO 8601 instant (Java `Instant`); the frontend shows it in the user's time zone.

```tsx
const deadlines = useUpcomingDeadlines(userId)
```

## TODO

- **Submitted status:** add `submitted: boolean` to `UpcomingDeadline`, so the
  dashboard can show whether the user has already handed in. The backend can take
  it from the submission package.

Each deadline links to its content page,
`/courses/{courseId}/modules/{moduleId}/content/{contentId}`, so `moduleId` is required.
