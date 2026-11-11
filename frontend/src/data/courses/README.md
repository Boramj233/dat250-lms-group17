# Courses API contract (proposed)

`GET /api/courses/{courseId}` and `POST /api/courses` exist in the backend; the
others are not implemented yet (the home page uses `GET /api/users/{userId}/courses`). These endpoints and DTO shapes
are frontend assumptions to agree on when implementing the backend.

| Method | Endpoint | Request body | Response |
| --- | --- | --- | --- |
| GET | `/api/courses` | — | `Course[]` |
| GET | `/api/courses/{courseId}` | — | `Course` |
| GET | `/api/users/{userId}/courses` | — | `EnrolledCourse[]` |
| GET | `/api/courses/{courseId}/enrollments` | — | `CourseEnrollment[]` |
| POST | `/api/courses` | `CreateCourseInput` | `Course` |
| PATCH | `/api/courses/{courseId}` | `UpdateCourseInput.changes` | `Course` |
| DELETE | `/api/courses/{courseId}` | — | `204 No Content` |
| POST | `/api/courses/{courseId}/enrollments` | `{ userId }` | `204 No Content` |
| DELETE | `/api/courses/{courseId}/enrollments/{userId}` | — | `204 No Content` |
| PATCH | `/api/courses/{courseId}/enrollments/{userId}` | `{ role }` | `204 No Content` |

Types are defined in `types.ts`. A course has `id`, `title`, `description`,
`term` (`SPRING` or `AUTUMN`) and `year`, as in the backend's `CourseResponse`. An enrolled course also includes the specified user's
`role`. A course enrollment has `userId` and `role`. New enrollments default to
`STUDENT` on the backend; promotion uses the role endpoint. Authentication and
authorization must be enforced by the backend, including who may create courses
and assign teacher roles.

Query hooks take the relevant ID. Mutation hooks take their input via `mutate`
or `mutateAsync`:

```tsx
const course = useCourse(courseId);
const updateCourse = useUpdateCourse();
const enrollInCourse = useEnrollInCourse();
const deleteCourse = useDeleteCourse();

updateCourse.mutate({ courseId, changes: { description: "New description" } });
enrollInCourse.mutate({ courseId, userId });
deleteCourse.mutate(courseId);
```

Mutations invalidate affected queries after success. Deleting a course also
removes its details and enrollments from the cache. Query IDs are normalized to
strings in cache keys so `42` and `"42"` share the same cached resource.
