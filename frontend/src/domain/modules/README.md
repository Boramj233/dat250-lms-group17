# Modules API contract

Modules organize content within a course. Content API functions and hooks belong to
`../content`, including module content lists. Shared identifiers are defined in
`../shared/types.ts`. All endpoints below exist in the backend (`ModuleController`).

| Method | Endpoint                                                               | Request body                            | Response                  |
| ------ | ---------------------------------------------------------------------- | --------------------------------------- | ------------------------- |
| GET    | `/api/courses/{courseId}/modules`                                      | —                                       | `ModuleResponse[]`        |
| GET    | `/api/courses/{courseId}/modules/{moduleId}`                           | —                                       | `ModuleResponse`          |
| POST   | `/api/courses/{courseId}/modules`                                      | `{ title, position }`                   | `ModuleResponse` (201)    |
| PUT    | `/api/courses/{courseId}/modules/{moduleId}`                           | `{ title, position }` (both required)   | `ModuleResponse`          |
| PUT    | `/api/courses/{courseId}/modules/{moduleId}/availability`              | `{ availableFrom, availableToAllFrom }` | `ModuleResponse`          |
| DELETE | `/api/courses/{courseId}/modules/{moduleId}`                           | —                                       | 204                       |
| GET    | `/api/courses/{courseId}/modules/{moduleId}/dependencies`              | —                                       | the required modules' ids |
| POST   | `/api/courses/{courseId}/modules/{moduleId}/dependencies/{requiredId}` | —                                       | `ModuleResponse`          |
| DELETE | `/api/courses/{courseId}/modules/{moduleId}/dependencies/{requiredId}` | —                                       | 204                       |

`ModuleResponse` is `{ id, courseId, title, position, dependencyIds, availableFrom,
availableToAllFrom }`. `api.ts` maps it to `CourseModule`: `dependencyIds` →
`prerequisiteModuleIds`, `availableToAllFrom` → `openToAllFrom`. The dates are Java
`LocalDateTime` (local time, no offset, e.g. `"2026-12-01T08:00:00"`); `api.ts` turns them
into ISO instants when reading and back into local time when writing, so the rest of the
frontend only sees ISO instants.

The frontend functions combine these endpoints:

- `createModule`: POST, then one POST per prerequisite.
- `updateModule({ courseId, moduleId, changes })`: reads the module, then PUT for
  `title`/`position` (the missing one is kept), PUT `/availability` for the dates (both are
  set; a missing one is kept), and POST/DELETE `/dependencies/{id}` for the difference in
  `prerequisiteModuleIds`.
- `reorderModules({ courseId, moduleIds })`: the backend has no endpoint for the whole
  order, so every module whose position changes gets a PUT with its new position.
- `deleteModule`: DELETE. The backend removes other modules' dependencies on it, but
  does not delete its content yet (TODO in `ModuleService.deleteModule`), so the manage
  page only offers it for an empty module.

## Module access (rules for the backend)

The backend records content completion per user and considers a module complete when
the user has completed its required content (`requiredForModuleCompletion`, see
`../content/README.md`). A module may require completion of all modules in
`prerequisiteModuleIds` before it becomes accessible. Prerequisites are explicit; module
position alone does not lock a module. The backend rejects self-dependencies and cycles
(400). `prerequisites.ts` has these rules for the frontend and the mock (`canRequire`,
`isAccessible`); the manage pages only offer prerequisites that `canRequire` allows.

Two optional dates per module:

- `availableFrom`: the module is closed to students before this date.
- `openToAllFrom` (backend `availableToAllFrom`): from this date the module is open to all
  students, also those who have not completed its prerequisites. It cannot be before
  `availableFrom` (400).

So a module is accessible when `availableFrom` has passed (or is null) and either its
prerequisites are completed or `openToAllFrom` has passed (`isAccessible`).

```tsx
const modules = useCourseModules(courseId)
const createModule = useCreateModule()
const updateModule = useUpdateModule()
const deleteModule = useDeleteModule()

createModule.mutate({ courseId, title: 'Introduction', position: 0 })
updateModule.mutate({ courseId, moduleId, changes: { title: 'Getting started' } })
updateModule.mutate({ courseId, moduleId, changes: { prerequisiteModuleIds: [introModuleId] } })
deleteModule.mutate({ courseId, moduleId })
```

## Order (rank) of the modules

`position` is the module's rank: every student sees the modules in this order, and the
frontend sorts by it everywhere. Teachers change it on the course's Content tab by
dragging a module or with ↑ / ↓, and save the whole order at once (`useReorderModules`).
