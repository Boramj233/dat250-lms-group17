# Content API contract

The read endpoints below exist in the backend, and the types in `types.ts` match its
DTOs. The write and completion endpoints further down are still frontend proposals.

The content types are `STATIC_PAGE`, `QUIZ`, `ASSIGNMENT` and `CODING_EXERCISE`.
The backend's content list sends `"Assignment"` for assignments; `getModuleContent`
normalizes it to `ASSIGNMENT` (the backend should send `ASSIGNMENT` too).

`ContentSummary` (backend `ContentSummaryResponse`) contains `id`, `type`, `title`
and zero-based `position`. The detail responses have no `type` field, and there is
one endpoint per type, so `getContent(type, contentId)` takes the type from the
summary and adds it to the result. `StaticPageContent.body` is plain text: render it
as text in React; do not insert it as raw HTML.

| Method | Endpoint | Request body | Response |
| --- | --- | --- | --- |
| GET | `/api/courses/{courseId}/modules/{moduleId}/contents` | — | `ContentSummary[]` |
| GET | `/api/contents/{contentId}` | — | `StaticPageContent` (without `type`) |
| GET | `/api/quizzes/{quizId}` | — | `QuizContent` (without `type`) |
| GET | `/api/assignments/{assignmentId}` | — | `AssignmentContent` (without `type`) |
| GET | `/api/coding-exercises/{exerciseId}` | — | `CodingExerciseContent` (without `type`) |

## Mandatory assignments (proposed)

`AssignmentContent.mandatory` (boolean) says whether the assignment must be passed.
It is not in the backend's `AssignmentResponse` (or the `Assignment` entity) yet; the
backend should add it to the entity, `CreateAssignmentRequest` and
`AssignmentResponse`. Until then the field is missing and the UI does not show it.

## Completions (proposed)

Assignments, coding exercises and quizzes are shown as "To do" or "Completed". One
request per course gives the status of all of them:

| Method | Endpoint | Request body | Response |
| --- | --- | --- | --- |
| GET | `/api/courses/{courseId}/completions/me` | — | `CompletedContent[]` |

`CompletedContent` is `{ contentId, completedAt }`. The list contains, for the
logged-in user:

- assignments and coding exercises with at least one submission (by the user, or
  by their group), `completedAt` = first submission;
- quizzes with at least one submitted attempt, `completedAt` = first attempt.

Static pages are not included. The backend has `GET /api/contents/{id}/submissions/me`,
but that would be one request per item.

```tsx
const completed = useCourseCompletions(courseId); // Set of content ids (strings)
completed.data?.has(String(item.id));
```

## Other endpoints

Proposed (not in the backend yet):

| Method | Endpoint | Request body | Response |
| --- | --- | --- | --- |
| POST | `/api/modules/{moduleId}/content` | Text or quiz fields from `CreateContentInput` | `Content` |
| PATCH | `/api/content/{contentId}` | Updated `title`, `position`, and/or `body` | `Content` |
| DELETE | `/api/content/{contentId}` | — | `204 No Content` |
| PATCH | `/api/content/{contentId}/move` | `{ moduleId, position }` | `Content` |
| GET | `/api/users/{userId}/content/{contentId}/completion` | — | `ContentCompletion` |
| PUT | `/api/users/{userId}/content/{contentId}/completion` | `{ completed }` | `ContentCompletion` |

Create quizzes through `useCreateContent` with `type: "QUIZ"` and teacher-only
question definitions, including correct answers. Use `useUpdateQuiz` to edit
questions and settings, and `useQuizDefinition` for the teacher's editor. The
student-facing `useQuiz`, `useContent`, and attempt responses must exclude answer
keys and grading rules; the backend must enforce this distinction.

Quiz attempts, answers, hints, and submission use the API functions in
`../quizzes/api`. Quiz submission registers the interaction and refreshes module
progress; students do not manually mark quizzes complete using the text-completion
operation. Await pending answer saves before submitting an attempt. Released
results are part of `QuizAttempt`; a missing result means unavailable or unreleased.

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
const items = useModuleContent(courseId, moduleId);
const content = useContent(summary.type, contentId);
const all = useCourseContent(courseId, moduleIds); // several modules at once
const completion = useContentCompletion(contentId, userId);
const moduleProgress = useUserModuleProgress(courseId, userId);
const createContent = useCreateContent();
const updateContent = useUpdateContent();
const moveContent = useMoveContent();
const deleteContent = useDeleteContent();
const setCompletion = useSetContentCompletion();

createContent.mutate({ moduleId, type: "STATIC_PAGE", title: "Welcome", position: 0, body: "Hello" });
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
