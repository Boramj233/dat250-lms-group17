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

| Method | Endpoint                                              | Request body | Response                                 |
| ------ | ----------------------------------------------------- | ------------ | ---------------------------------------- |
| GET    | `/api/courses/{courseId}/modules/{moduleId}/contents` | —            | `ContentSummary[]`                       |
| GET    | `/api/contents/{contentId}`                           | —            | `StaticPageContent` (without `type`)     |
| GET    | `/api/quizzes/{quizId}`                               | —            | `QuizContent` (without `type`)           |
| GET    | `/api/assignments/{assignmentId}`                     | —            | `AssignmentContent` (without `type`)     |
| GET    | `/api/coding-exercises/{exerciseId}`                  | —            | `CodingExerciseContent` (without `type`) |

## Content required to complete a module (proposed)

`ContentSummary.requiredForCompletion` (boolean) marks an item the user must
complete to complete its module, for every content type. Teachers switch it on and
off with `PATCH /api/content/{contentId}` (`{ requiredForCompletion }`). The module
content list (`GET .../modules/{moduleId}/contents`) should include it; missing
means not required.

It is separate from an assignment's `mandatory`, which means the assignment must be
passed for the course (for example to take the exam).

The backend's module progress (`data/modules/README.md`) should then count a module
as completed when the user has completed all its required items. How a module with
no required items is treated is up to the backend; the mock then requires all its
assignments, coding exercises and quizzes, as before.

## Active (visible to students) content (proposed)

`ContentSummary.active` (boolean) says whether students can see an item. Teachers
switch it off to prepare content before it is shown, or to hide it again, with
`PATCH /api/content/{contentId}` (`{ active }`). New content is active. The module
content list includes it; missing means active.

The UI no longer switches `active`; it shows items with `active: false` as "Hidden from
students". Published / Unpublished in the UI is decided by the visible date alone (below).

For students the backend must leave inactive content out everywhere: the module
content list, the detail endpoints (404), upcoming deadlines and completion. Teachers
of the course still get it, with `active: false`.

## Visible from (proposed)

Every content item can get a date students see it from, so a module's content does not
all appear at once. The `Content` entity already has `visibleFrom`; the API does not
use it yet.

- `visibleFrom` (ISO instant or null) on every create request (pages, assignments,
  coding exercises, quizzes), on `PATCH /api/content/{id}`, and on
  `ContentSummaryResponse` and the detail responses.
- Null: the item **inherits its module's start date** (`availableFrom`), or is visible
  at once if the module has none.
- It **cannot be before the module's start date**: 400. Likewise a module's
  `availableFrom` cannot be moved after the visible date of content in it: 400.
- Before that date students do not get the item (not in the module's list, 404 on its
  page, not among upcoming deadlines); teachers of the course do, and see when it opens.

The rules are in `visibility.ts` (`effectiveVisibleFrom`, `checkContentVisibleFrom`,
`checkModuleStartAgainstContent`); the mock backend uses the same functions.

## Editing until published (proposed)

An item is **published** once its visible date has passed (its own `visibleFrom`, or its
module's start date); that alone decides it, and the module's table shows it as
Published / Unpublished. A teacher can change **everything** about an item until then:

- `PATCH /api/content/{contentId}` takes any of the fields the item was created with:
  `title`, `visibleFrom`, and per type `body` (pages); `instructions`, `deadline`,
  `allowLateSubmission`, `mandatory`, `groupAssignment`, `acceptedFileTypes`
  (assignments); `instructions`, `deadline`, `allowLateSubmission`,
  `starterRepositoryUrl`, `starterCode` (coding exercises); `maxAttempts`,
  `requiredCorrectAnswers` (quizzes).
- `PUT /api/quizzes/{quizId}/questions` replaces all of a quiz's questions.
- Once published, both answer **409**. Only `position` and `requiredForCompletion` (and
  `active`) can still be changed with PATCH.
- `POST /api/quizzes/{quizId}/questions` stays open, since a new quiz's questions are
  added right after it is created.

The frontend uses the same form for creating and editing (`course-management/module/ContentForm`).
It does **not** check whether an item may be changed: it always shows the form and shows
the backend's 409 if it refuses. The rule above is the backend's to decide (the mock
backend follows it).

## Quiz questions and correct answers (proposed)

`GET /api/quizzes/{quizId}` (`QuizDetailResponse`) returns the questions and their
options, but not which options are correct or the hints, so students cannot see
them. Teachers need both to check a quiz they made. Proposed: when the user is a
teacher of the course, the backend adds `hint` to each question and `correct` to each
option (`QuizContentQuestion`, `QuizContentOption` in `types.ts`); for everyone else
the response stays as it is. Until then the manage pages show the questions and
options, and say that the correct answers are not available.

## Taking a quiz (partly proposed)

The backend grades a quiz in one request: `POST /api/quizzes/{quizId}/attempts` with
`{ answers: [{ questionId, selectedOptionIds }] }` returns `QuizAttemptResponse`
`{ id, quizId, submittedAt, correctCount, totalQuestions }`, and refuses when the user
has used `maxAttempts`. A question is correct when the chosen options are exactly
its correct options. Proposed, for the quiz page:

- `GET /api/quizzes/{quizId}/attempts/me`: the user's earlier attempts
  (`QuizAttemptResponse[]`), for "attempts used" and the best score.
- On `QuizDetailResponse`: `description` (text shown before the quiz is started) and
  `deadline` (ISO 8601 instant or null; no attempts after it).
- On each question: `questionType`, one of `SINGLE_CHOICE` (one answer),
  `MULTIPLE_CHOICE` (every correct answer) or `TRUE_FALSE` (two options). The page
  shows each type differently; missing means `SINGLE_CHOICE`.

### Looking at an earlier attempt (proposed)

Students can see how they answered each earlier attempt, with every question marked
correct or wrong. The backend already stores this per question (`QuizAnswer`:
selected options and `correct`); it only needs an endpoint to read it:

- `GET /api/quizzes/{quizId}/attempts/{attemptId}` → `QuizAttemptResponse` plus
  `answers: [{ questionId, selectedOptionIds, correct }]`. Only the user's own attempt;
  404 for anyone else's.
- It does not include which options are correct, so the review does not give the
  answers away while the student has attempts left.

### Pass requirement (proposed)

The teacher sets how many questions must be correct for an attempt to pass; the
backend checks it when it grades the attempt.

- `requiredCorrectAnswers` (Integer, nullable) on `CreateQuizRequest` and
  `QuizDetailResponse`. Null: no pass requirement. Otherwise at least 1 and at most the
  number of questions (400 otherwise).
- `passed` (Boolean, nullable) on `QuizAttemptResponse`: `correctCount >=
requiredCorrectAnswers`, null when the quiz has no requirement. The quiz counts as
  passed once any attempt passed.
- A quiz with a requirement is only completed (e.g. for `requiredForCompletion` and
  module prerequisites) once it is passed, not after any attempt. The frontend shows it
  that way: "Not passed yet" while attempts are left, never "Completed".

Until the backend sends `passed`, the frontend works it out from
`requiredCorrectAnswers` (`domain/quizzes/passRule.ts`).

## Starter code for coding exercises (proposed)

`CodingExerciseContent.starterCode` (string or null) is code the students start from,
shown with the exercise, as an alternative or addition to `starterRepositoryUrl`. A
coding exercise has at least one of them, so `starterRepositoryUrl` may be null when
there is starter code. The backend should add `starterCode` to the entity,
`CreateCodingExerciseRequest` and `CodingExerciseResponse`, and accept a missing
`starterRepositoryUrl`. Until then the field is missing and the UI shows only the
repository.

## Group assignments and accepted file types (proposed)

Two more fields on `AssignmentContent`, set when the assignment is created:

- `groupAssignment` (boolean): students hand in as a group, one submission per
  group (the submission's `groupId`, see `domain/assignments`). A member hands in
  with `groupId` in `POST /api/contents/{contentId}/submissions` (or `…/submissions/file`),
  and every member then sees it in `…/submissions/me` with `groupId` and the `members`
  at that time. The frontend shows the group's name from the course's groups.
- `acceptedFileTypes` (string array): the file name extensions that may be handed
  in, lower case with a dot, e.g. `[".pdf", ".zip"]`. Empty means any file type.
  The backend must check uploaded files against it.

The backend should add both to the `Assignment` entity, `CreateAssignmentRequest`
and `AssignmentResponse`. Until then they are missing and the UI does not show them.

## Mandatory assignments (proposed)

`AssignmentContent.mandatory` (boolean) says whether the assignment must be passed.
It is not in the backend's `AssignmentResponse` (or the `Assignment` entity) yet; the
backend should add it to the entity, `CreateAssignmentRequest` and
`AssignmentResponse`. Until then the field is missing and the UI does not show it.

## Other endpoints

Proposed (not in the backend yet):

| Method | Endpoint                          | Request body                                                          | Response         |
| ------ | --------------------------------- | --------------------------------------------------------------------- | ---------------- |
| PATCH  | `/api/content/{contentId}`        | Any of the item's fields (see "Editing until published")              | `Content`        |
| PUT    | `/api/quizzes/{quizId}/questions` | `CreateQuizQuestionRequest[]`: all of an unpublished quiz's questions | the questions    |
| DELETE | `/api/content/{contentId}`        | —                                                                     | `204 No Content` |
| PATCH  | `/api/content/{contentId}/move`   | `{ moduleId, position }`                                              | `Content`        |

Content is created with the backend's endpoint for its type
(`POST /api/courses/{courseId}/modules/{moduleId}/contents/{pages|assignments|coding-exercises|quizzes}`)
through `useCreateContent`; a quiz is created first and its questions are then added
with `POST /api/quizzes/{quizId}/questions`. Taking a quiz is described under
"Taking a quiz" above.

The backend returns module content sorted by position and maintains ordering after
creation, deletion, position changes, and moves. Moves are within one course and
may reorder content in the same module. Moving content preserves its ID.

```tsx
const items = useModuleContent(courseId, moduleId)
const content = useContent(summary.type, contentId)
const all = useCourseContent(courseId, moduleIds) // several modules at once
const createContent = useCreateContent()
const updateContent = useUpdateContent()
const moveContent = useMoveContent()
const deleteContent = useDeleteContent()

createContent.mutate({ moduleId, type: 'STATIC_PAGE', title: 'Welcome', position: 0, body: 'Hello' })
updateContent.mutate({ contentId, moduleId, changes: { body: 'Updated text' } })
moveContent.mutate({ contentId, sourceModuleId: moduleId, targetModuleId, position: 0 })
deleteContent.mutate({ contentId, moduleId })
```

Mutation inputs include parent IDs used for cache invalidation even when those
IDs are not sent to the API. Supply the current module ID when updating/deleting
and both module IDs when moving content.
Queries require defined IDs.

Mutations invalidate the affected lists/details. Moving content refreshes both
module lists; deletion removes the cached details. Cache IDs are normalized to strings.

The backend must enforce enrollment and course teacher permissions, validate
source/destination modules and enforce module prerequisites when content is
requested. A frontend
`userId` does not identify the authenticated actor. Authentication is still a TODO
in `apiClient`. Clear caches when logging out or switching users.
