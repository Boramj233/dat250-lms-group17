# Content API contract (proposed)

These endpoints and DTOs are frontend assumptions until the backend is implemented.
The first supported content type is `TEXT`. `Content` currently aliases
`TextContent`; extend it into a discriminated union when assignment, quiz, and
coding exercise fields and behavior are defined. They are not implemented here.

`ContentSummary` contains `id`, `moduleId`, `title`, zero-based `position`, and
`type`. `TextContent` also contains `body`, treated as plain text. Render it as text
in React; do not insert it as raw HTML. Module lists return summaries without body.

| Method | Endpoint | Request body | Response |
| --- | --- | --- | --- |
| GET | `/api/modules/{moduleId}/content` | — | `ContentSummary[]` |
| GET | `/api/content/{contentId}` | — | `Content` |
| POST | `/api/modules/{moduleId}/content` | `{ type: "TEXT", title, position, body }` | `Content` |
| PATCH | `/api/content/{contentId}` | Updated `title`, `position`, and/or `body` | `Content` |
| DELETE | `/api/content/{contentId}` | — | `204 No Content` |
| PATCH | `/api/content/{contentId}/move` | `{ moduleId, position }` | `Content` |
| GET | `/api/users/{userId}/content/{contentId}/completion` | — | `ContentCompletion` |
| PUT | `/api/users/{userId}/content/{contentId}/completion` | `{ completed }` | `ContentCompletion` |

The backend returns module content sorted by position and maintains ordering after
creation, deletion, position changes, and moves. Moves are within one course and
may reorder content in the same module. Moving content preserves its ID and user
completion records. Deleting content deletes its completion records.

Completion is separate from shared content. A `ContentCompletion` contains
`contentId`, `userId`, and `completed`. GET returns `completed: false` for a valid
content/user pair with no completion record. PUT sets the requested state, so
repeating the same request does not toggle it. This is a manual text-completion
feature; completion rules for graded content must be defined separately. The
backend uses these records to compute module completion and prerequisite-based
access to other modules. It must persist and recalculate progress before returning
a successful completion response; the frontend does not calculate or grant access.

```tsx
const items = useModuleContent(moduleId);
const content = useContent(contentId);
const completion = useContentCompletion(contentId, userId);
const moduleProgress = useUserModuleProgress(courseId, userId);
const createContent = useCreateContent();
const updateContent = useUpdateContent();
const moveContent = useMoveContent();
const deleteContent = useDeleteContent();
const setCompletion = useSetContentCompletion();

createContent.mutate({ moduleId, type: "TEXT", title: "Welcome", position: 0, body: "Hello" });
updateContent.mutate({ contentId, moduleId, changes: { body: "Updated text" } });
moveContent.mutate({ contentId, sourceModuleId: moduleId, targetModuleId, position: 0 });
deleteContent.mutate({ contentId, moduleId });
setCompletion.mutate({ courseId, contentId, userId, completed: true });
```

Mutation inputs include parent IDs used for cache invalidation even when those
IDs are not sent to the API. Supply the current module ID when updating/deleting
and both module IDs when moving content. Completion inputs also require `courseId`
for refreshing that user's module progress; it is not sent in the completion body.
Queries require defined IDs.

Mutations invalidate the affected lists/details. Moving content refreshes both
module lists; deletion removes cached details and all users' completion records.
Setting completion invalidates that content/user pair and the user's module
progress for the course, so newly unlocked or relocked modules can be reflected
without reloading the page. Creating, editing, deleting, and moving content also
invalidate module progress because the set of content required for module
completion may have changed. Cache IDs are normalized to strings and completion
keys include user ID to separate progress.

The backend must enforce enrollment and course teacher permissions, validate
source/destination modules, enforce module prerequisites when content is requested,
and restrict access to user progress. A frontend
`userId` does not identify the authenticated actor. Authentication is still a TODO
in `apiClient`. Clear caches when logging out or switching users.
