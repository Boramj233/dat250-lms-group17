# Courses API contract (proposed)

`GET /api/courses/{courseId}` and `POST /api/courses` exist in the backend; the
others are not implemented yet (the home page uses `GET /api/users/{userId}/courses`). These endpoints and DTO shapes
are frontend assumptions to agree on when implementing the backend.

| Method | Endpoint | Request body | Response |
| --- | --- | --- | --- |
| GET | `/api/courses/{courseId}` | — | `Course` |
| GET | `/api/users/{userId}/courses` | — | `EnrolledCourse[]` |
| GET | `/api/courses/{courseId}/enrollments` | — | `CourseEnrollment[]` (every member of the course: the People page) |
| POST | `/api/courses` | `CreateCourseInput` | `Course` |
| PATCH | `/api/courses/{courseId}` | `UpdateCourseInput.changes` | `Course` |
| POST | `/api/courses/{courseId}/enrollments` | `{ userId, role? }` | `204 No Content` |
| DELETE | `/api/courses/{courseId}/enrollments/{userId}` | — | `204 No Content` |
| PATCH | `/api/courses/{courseId}/enrollments/{userId}` | `{ role }` | `204 No Content` |

Types are defined in `types.ts`. A course has `id`, `title`, `description`,
`term` (`SPRING` or `AUTUMN`) and `year`, as in the backend's `CourseResponse`. An enrolled course also includes the specified user's
`role`. A course enrollment has `userId`, `role`, `firstName`, `lastName` and
`email`, so the members page needs one request. New enrollments default to
`STUDENT`; `role` in the POST body sets another role directly.

**Who may do what** (the backend must enforce it):

- Users with global access `COURSE_MANAGER` (or `ADMIN`) may create courses
  (`POST /api/courses`). The creator must be enrolled as `TEACHER` of the new
  course; the backend does not do this yet (`CourseService.createCourse`).
- Only a `TEACHER` of the course may change it: enroll and remove members, change
  roles, add modules and content, see all submissions and grade them.
- Finding users to enroll uses `GET /api/users/search?search=` (exists, but only
  for `COURSE_MANAGER`/`ADMIN`; a teacher without that access cannot search yet). Authentication and
authorization must be enforced by the backend, including who may create courses
and assign teacher roles.

Query hooks take the relevant ID. Mutation hooks take their input via `mutate`
or `mutateAsync`:

```tsx
const course = useCourse(courseId);
const updateCourse = useUpdateCourse();
const enrollInCourse = useEnrollInCourse();

updateCourse.mutate({ courseId, changes: { description: "New description" } });
enrollInCourse.mutate({ courseId, userId });
```

Mutations invalidate affected queries after success. Query IDs are normalized to
strings in cache keys so `42` and `"42"` share the same cached resource.
