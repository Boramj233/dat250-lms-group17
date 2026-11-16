# Deadlines API contract (proposed)

The dashboard shows the upcoming deadlines from all of the user's courses in
one list. The backend has `deadline` on assignments and coding exercises, but no
endpoint that collects them per user yet. Building the list in the frontend would
need one request per course, module and content item, so the backend should
collect it.

| Method | Endpoint                                 | Request body | Response             |
| ------ | ---------------------------------------- | ------------ | -------------------- |
| GET    | `/api/users/{userId}/upcoming-deadlines` | —            | `UpcomingDeadline[]` |

The response contains assignments and coding exercises that:

- are in a course the user is enrolled in (as student or teacher), and
- have a `deadline` that has not passed yet (content without a deadline is left out),

sorted by `deadline`, earliest first.

```json
[
  {
    "contentId": 12,
    "type": "ASSIGNMENT",
    "title": "Assignment 1",
    "deadline": "2026-11-14T22:59:00Z",
    "allowLateSubmission": false,
    "courseId": 1,
    "courseTitle": "DAT250",
    "moduleId": 3
  }
]
```

`type` is `ASSIGNMENT` or `CODING_EXERCISE`. Assignments also have `mandatory`
(boolean, see the content README); it is left out for coding exercises. `deadline` is an ISO 8601 instant
(Java `Instant`); the frontend shows it in the user's time zone.

```tsx
const deadlines = useUpcomingDeadlines(userId)
```

## TODO

- **Submitted status:** add `submitted: boolean` to `UpcomingDeadline`, so the
  dashboard can show whether the user has already handed in. The backend can take
  it from the submission package.

Each deadline links to its content page,
`/courses/{courseId}/modules/{moduleId}/content/{contentId}`, so `moduleId` is required.
