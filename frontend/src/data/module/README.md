# Modules API contract (proposed)

These are frontend assumptions; agree on the endpoints and DTOs when implementing
the backend. Modules organize content within a course. Content API functions and
hooks belong to `../content`, including module content lists and moving content.
Shared identifiers are defined in `../shared/types.ts`.

| Method | Endpoint | Request body | Response |
| --- | --- | --- | --- |
| GET | `/api/courses/{courseId}/modules` | — | `CourseModule[]` |
| POST | `/api/courses/{courseId}/modules` | `{ title, position, prerequisiteModuleIds? }` | `CourseModule` |
| PATCH | `/api/courses/{courseId}/modules/{moduleId}` | Updated `title`, `position`, and/or `prerequisiteModuleIds` | `CourseModule` |
| DELETE | `/api/courses/{courseId}/modules/{moduleId}` | — | `204 No Content` |

A `CourseModule` has `id`, `courseId`, `title`, zero-based `position`, and
`prerequisiteModuleIds` (an empty array for modules without prerequisites).
GET returns modules sorted by position. The backend maintains a consistent order
when modules are inserted, reordered, or deleted. Updating position reorders a
module within its current course; moving modules between courses is not supported.

The proposed delete behavior is to delete the module and its content and completion
records. The delete hook removes the module's content list from cache and
invalidates content queries and the course's module list. Agree on cascade deletion
with the backend before connecting this operation to a UI.

## User progress and module access

`GET /api/users/{userId}/courses/{courseId}/modules/progress` returns
`UserModuleProgress[]`. Each entry contains `moduleId`, `completed`, and
`accessible`. Use `useUserModuleProgress(courseId, userId)` alongside
`useCourseModules(courseId)` and match entries by module ID. Progress is cached
separately for each course/user pair; it is not part of the shared module data.
While progress is loading or failed, do not assume access is granted.

The backend records content completion per user and considers a module complete
when all its content is complete for that user. It must define how empty modules
are treated. A module may require completion of all modules in
`prerequisiteModuleIds` before it becomes accessible. Prerequisites are explicit;
module position alone does not lock a module. The backend must validate that
prerequisites exist in the same course and reject self-dependencies and cycles.

The backend calculates completion and access before returning progress after a
successful content-completion write. It must check enrollment and module access
when returning content and accepting completion requests. Client-supplied IDs or
completion flags do not establish authorization. A direct API call must not bypass
a locked module. These backend rules are not implemented by the frontend hooks.

Module mutations refresh progress for all cached users of that course. Content
mutations refresh all cached module progress because their inputs lack course ID.
Completion changes refresh the specific course/user pair, including dependent
modules that may become accessible or locked again.

```tsx
const modules = useCourseModules(courseId);
const progress = useUserModuleProgress(courseId, userId);
const createModule = useCreateModule();
const updateModule = useUpdateModule();
const deleteModule = useDeleteModule();

createModule.mutate({ courseId, title: "Introduction", position: 0 });
updateModule.mutate({ courseId, moduleId, changes: { title: "Getting started" } });
updateModule.mutate({ courseId, moduleId, changes: { prerequisiteModuleIds: [introModuleId] } });
deleteModule.mutate({ courseId, moduleId });
```

The backend must authenticate the actor and check teacher access to the course
for each mutation. Query hooks require defined IDs. Clear user-dependent caches
when logging out or switching users, as with course queries.
