# Modules API contract (proposed)

These are frontend assumptions; agree on the endpoints and DTOs when implementing
the backend. Modules organize content within a course. Content API functions and
hooks belong to `../content`, including module content lists and moving content.
Shared identifiers are defined in `../shared/types.ts`.

| Method | Endpoint                                     | Request body                                                                                 | Response                          |
| ------ | -------------------------------------------- | -------------------------------------------------------------------------------------------- | --------------------------------- |
| GET    | `/api/courses/{courseId}/modules`            | —                                                                                            | `CourseModule[]`                  |
| POST   | `/api/courses/{courseId}/modules`            | `{ title, position, prerequisiteModuleIds? }`                                                | `CourseModule`                    |
| PATCH  | `/api/courses/{courseId}/modules/{moduleId}` | Updated `title`, `position`, `prerequisiteModuleIds`, `availableFrom` and/or `openToAllFrom` | `CourseModule`                    |
| PUT    | `/api/courses/{courseId}/modules/order`      | `{ moduleIds }`: every module of the course once, first to last (proposed)                   | `CourseModule[]` in the new order |

A `CourseModule` has `id`, `courseId`, `title`, zero-based `position`, and
`prerequisiteModuleIds` (an empty array for modules without prerequisites).
GET returns modules sorted by position. The backend maintains a consistent order
when modules are inserted, reordered, or deleted. Updating position reorders a
module within its current course; moving modules between courses is not supported.

## Module access (rules for the backend)

The backend records content completion per user and considers a module complete
when all its content is complete for that user. Proposed: only the content marked
`requiredForCompletion` counts (see `data/content/README.md`). It must define how empty modules
are treated. A module may require completion of all modules in
`prerequisiteModuleIds` before it becomes accessible. Prerequisites are explicit;
module position alone does not lock a module. The backend must validate that
prerequisites exist in the same course and reject self-dependencies and cycles.
`prerequisites.ts` has these rules for the frontend and the mock (`canRequire`,
`isAccessible`); the manage pages only offer prerequisites that `canRequire` allows.

Two optional dates per module (proposed, ISO 8601 instants or null, set with the
PATCH above):

- `availableFrom`: the module is closed to students before this date.
- `openToAllFrom`: from this date the module is open to all students, also those
  who have not completed its prerequisites. It cannot be before `availableFrom`.

So a module is accessible when `availableFrom` has passed (or is null) and either
its prerequisites are completed or `openToAllFrom` has passed (`isAccessible`).

The backend It must check enrollment and module access
when returning content and accepting completion requests. Client-supplied IDs or
completion flags do not establish authorization. A direct API call must not bypass
a locked module. These backend rules are not implemented by the frontend hooks.

```tsx
const modules = useCourseModules(courseId)
const createModule = useCreateModule()
const updateModule = useUpdateModule()

createModule.mutate({ courseId, title: 'Introduction', position: 0 })
updateModule.mutate({ courseId, moduleId, changes: { title: 'Getting started' } })
updateModule.mutate({ courseId, moduleId, changes: { prerequisiteModuleIds: [introModuleId] } })
```

The backend must authenticate the actor and check teacher access to the course
for each mutation. Query hooks require defined IDs. Clear user-dependent caches
when logging out or switching users, as with course queries.

## Order (rank) of the modules (proposed)

`position` is the module's rank: every student sees the modules in this order, and the
frontend sorts by it everywhere. Teachers change it on the course's Content tab by
dragging a module or with ↑ / ↓, and save the whole order at once with
`PUT /api/courses/{courseId}/modules/order` (`{ moduleIds }`). The backend sets the
positions 0, 1, 2, … from the list and answers with the modules in the new order. A list
that is not exactly the course's modules (missing, extra or repeated ids): 400.
Teachers of the course only.
