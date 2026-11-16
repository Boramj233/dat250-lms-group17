# Courses API contract

All endpoints below exist in the backend except `PATCH /api/courses/{courseId}`,
which is still a frontend proposal. The user's courses come from the enrollment
feature's `UserCoursesController`, the members of a course from its
`EnrollmentController`.

| Method | Endpoint                                       | Request body                | Response                                                   |
| ------ | ---------------------------------------------- | --------------------------- | ---------------------------------------------------------- |
| GET    | `/api/courses/{courseId}`                      | —                           | `Course`                                                   |
| GET    | `/api/users/{userId}/courses`                  | —                           | `EnrolledCourse[]` (active enrollments, newest term first) |
| GET    | `/api/courses/{courseId}/enrollments`          | —                           | `CourseEnrollment[]` (every active member, sorted by name) |
| POST   | `/api/courses`                                 | `CreateCourseInput`         | `Course`                                                   |
| PATCH  | `/api/courses/{courseId}`                      | `UpdateCourseInput.changes` | `Course` (proposed)                                        |
| POST   | `/api/courses/{courseId}/enrollments`          | `{ userId, role? }`         | `201` `CourseEnrollment`; `404` unknown user, `409` member |
| PATCH  | `/api/courses/{courseId}/enrollments/{userId}` | `{ role }`                  | `CourseEnrollment`                                         |
| DELETE | `/api/courses/{courseId}/enrollments/{userId}` | —                           | `204 No Content` (submissions and grades are kept)         |

Types are defined in `types.ts`. A course has `id`, `title`, `description`,
`term` (`SPRING` or `AUTUMN`) and `year`, as in the backend's `CourseResponse`. An
enrolled course (`EnrolledCourseResponse`) also includes the user's `role`. A course
enrollment (`EnrollmentResponse`) has `userId`, `role`, `firstName`, `lastName` and
`email`, so the members page needs one request. New enrollments default to
`STUDENT`; `role` in the POST body sets another role directly.

**Who may do what** (enforced by the backend):

- `GET /api/users/{userId}/courses`: only the user themselves or an `ADMIN` (else 403).
- `GET /api/courses/{courseId}/enrollments`: members of the course, and `ADMIN`.
- Enrolling, changing roles and removing members: a `TEACHER` of that course, or `ADMIN`.
- `COURSE_MANAGER` is a global access that every teacher has. It gives no rights in a
  course the user does not teach; there the course role decides.
- Users with `COURSE_MANAGER` (or `ADMIN`) may create courses (`POST /api/courses`)
  and find users to enroll (`GET /api/users/search?search=`).

Query hooks take the relevant ID. Mutation hooks take their input via `mutate`
or `mutateAsync`:

```tsx
const course = useCourse(courseId)
const updateCourse = useUpdateCourse()
const enrollInCourse = useEnrollInCourse()

updateCourse.mutate({ courseId, changes: { description: 'New description' } })
enrollInCourse.mutate({ courseId, userId })
```

Mutations invalidate affected queries after success. Query IDs are normalized to
strings in cache keys so `42` and `"42"` share the same cached resource.
