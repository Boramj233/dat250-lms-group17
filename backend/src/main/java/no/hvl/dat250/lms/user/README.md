# Local user profiles

The user feature stores application profiles and global access. Keycloak owns
accounts, credentials, password changes, and first-login requirements. There is
no local password column, password encoder, UserDetailsService, password-change
endpoint, or mustChangePassword flag.

`User(email, firstName, lastName)` requires a valid, non-blank email and non-blank
names, all at most 255 characters. The email must match the external identity;
it is unique, stored in lower case (as Keycloak reports it), and cannot be changed
through profile updates. Only `userId` is generated locally. It remains the Long
primary key backed by `user_id_seq`.

## Division of work with the security feature

The security feature (`no.hvl.dat250.lms.security`) authenticates every request
(Keycloak access token, verified email -> local profile) and decides who may call
what: `@IsAdmin` and `@IsCourseManager` on the controllers, with the caller's access
read from the database on every request. It also decides who may log in at all
(invite-only) and bootstraps the first admin.

This feature only owns the profiles: their data, validation, unique emails, the
class-list import, and the rule that an admin cannot demote themselves. The
service does not read the security context; controllers pass the caller's ID
from `@AuthenticationPrincipal AuthenticatedUser` when a rule needs it.

Profiles come from an admin (single profile or CSV import) before the first login.
They never create Keycloak accounts or credentials: users register in Keycloak
themselves with the same email.

## Profile endpoints

`GET /api/users/me` returns userId, email, firstName, lastName, and access.
`PUT /api/users/me` replaces both required names:

```json
{"firstName":"Sara","lastName":"Nordmann"}
```

Profile updates cannot change ID, email, or global access. Additional JSON fields
are ignored. Invalid names return 400; missing authentication returns 401; a
removed local profile returns 404. Name changes currently affect local display
names only; future auth integration must decide whether Keycloak or the local
profile is authoritative for display names.

## Dashboard endpoints (across all of a user's courses)

Only courses where the user has an active enrollment count.

| Method | Endpoint | Who | Response |
| --- | --- | --- | --- |
| GET | `/api/users/me/grades` | the caller | `[{contentId, type, title, courseId, courseTitle, moduleId, submissionId, submittedAt, late, grade}]` |
| GET | `/api/users/{userId}/upcoming-deadlines` | that user, or ADMIN (else 403) | `[{contentId, type, title, deadline, allowLateSubmission, mandatoryForExam, courseId, courseTitle, moduleId}]` |

- `type` is `ASSIGNMENT` or `CODING_EXERCISE`.
- Grades: one row per assignment or coding exercise the user handed in, alone or as a
  member of a group submission, for the latest submission, newest first. `grade` is
  `{score, feedback, passed, releasedAt}` once the grade is released, else null.
- Deadlines: deadline after now, soonest first. Students do not get content outside its
  `visibleFrom`/`visibleUntil` window; teachers of the course do. `mandatoryForExam`
  is null for coding exercises.

Which courses count, and the user's role in each, comes from the enrollment feature
(`EnrollmentService.activeCourseRoles`). The content, submission and grade services
have no per-user queries and their repositories are private to them (ArchitectureTest),
so `UserDashboardRepository` reads those entities for the given course ids with
read-only JPQL; it adds no tables. Module dates (`availableFrom`) and grade
`visibleFrom` are not taken into account yet, since those columns do not exist.

## Administrator endpoints

All operations below require ADMIN access (`@IsAdmin`).

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/admin/users` | Create a local profile for an external identity |
| POST | `/api/admin/users/import` | Create profiles from a CSV class list (multipart field `file`) |
| GET | `/api/admin/users?search=&page=0&size=20` | List/search local profiles |
| GET | `/api/admin/users/{userId}` | Read one profile |
| PATCH | `/api/admin/users/{userId}/access` | Set global application access |

Creation accepts firstName, lastName, and email. It returns 201, a Location
header, and the profile. It defaults to STANDARD and cannot accept a chosen
userId or access level. Duplicate emails return 409. No credentials are accepted
or stored. `UserService.createUser(firstName, lastName, email)` performs
validation, the uniqueness check, and persistence.

The CSV import takes a header row `email,firstName,lastName,access` (access is
optional, blank = STANDARD; commas or semicolons; UTF-8). Each row is validated
like a single profile; existing emails are skipped, so a file can be uploaded
again. The response reports every row as CREATED, ALREADY_EXISTS,
DUPLICATE_IN_FILE, or INVALID (with the errors); an unreadable file returns 400.

Access updates accept {"access":"COURSE_MANAGER"}. Allowed values are STANDARD,
COURSE_MANAGER, and ADMIN. An administrator cannot remove their own ADMIN access.
Global access does not represent the student/teacher role in any course.

List responses contain users, page, size, totalElements, and totalPages. Pages
start at zero; size is 1–100, sorted by userId. Search is at most 255 characters,
matching name/email substrings without case sensitivity or an exact numeric ID.
Blank search lists all users; percent and underscore are literal characters.

Invalid requests return 400, missing authentication 401, insufficient access 403,
and missing target profiles 404. The security feature reads access from the
database on every request, so outdated privileges are rejected.

## Restricted lookup for enrollment

`GET /api/users/search?search=Sara&page=0&size=20` requires COURSE_MANAGER or ADMIN
and non-blank search text. Pagination/search rules match the administrator list.
The response has pagination metadata and users containing only userId, firstName,
lastName, and email. No matches returns an empty page.

Course managers cannot create profiles or assign global access. Enrollment and
course-specific permissions remain the responsibility of course/enrollment.