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

## Content required to complete a module (backend: ContentCompletionController)

`ContentSummary.requiredForModuleCompletion` (boolean) marks an item the user must
complete to complete its module, for every content type. The module content list
(`GET .../modules/{moduleId}/contents`) sends it; the detail responses do not.

| Method | Endpoint                                        | Request body                                         | Response                                                              |
| ------ | ----------------------------------------------- | ---------------------------------------------------- | --------------------------------------------------------------------- |
| PATCH  | `/api/contents/{contentId}/completion-settings` | `{ requiredForModuleCompletion, passingPercentage }` | `204` (teachers; `passingPercentage` only for quizzes, null keeps it) |
| GET    | `/api/contents/{contentId}/completion`          | —                                                    | `{ contentId, requiredForModuleCompletion, completed }` (students)    |
| PUT    | `/api/contents/{contentId}/completion`          | `{ completed }`                                      | the same; pages only (marked as read by the student)                  |

The frontend uses all three:

- `useUpdateCompletionSettings`: the module's content table (teachers).
- `useContentCompletion` / `useSetPageCompletion`: "Mark as done" under a page
  (students; `PageCompletion` in the course view).
- `useContentCompletions`: the Status column of the Pages table (students).

Only active students of the course may call the two student endpoints (403 for
teachers), so the frontend never asks for teachers. A page is completed when the
student marks it, a quiz when an attempt reaches its passing percentage, an assignment
when a released grade is passed; a coding exercise is never completed yet.

It is separate from an assignment's `mandatoryForExam`, which means the assignment must
be passed to sit the exam.

The backend's module progress (`data/modules/README.md`) should then count a module
as completed when the user has completed all its required items. How a module with
no required items is treated is up to the backend; the mock then requires all its
assignments, coding exercises and quizzes, as before.

## Visible from (proposed)

Every content item can get a date students see it from, so a module's content does not
all appear at once; it is also how a teacher prepares content before it is shown (there
is no separate "active" flag). The `Content` entity already has `visibleFrom`; the API
does not use it yet.

- `visibleFrom` (ISO instant or null) on every create request (pages, assignments,
  coding exercises, quizzes), on `PATCH /api/contents/{id}`, and on
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

- `PATCH /api/contents/{contentId}` takes any of the fields the item was created with:
  `title`, `visibleFrom`, and per type `body` (pages); `instructions`, `deadline`,
  `allowLateSubmission`, `groupAssignment`, `acceptedFileTypes` (assignments; not
  `mandatoryForExam`, see "Mandatory for the exam"); `instructions`, `deadline`, `allowLateSubmission`,
  `starterRepositoryUrl`, `starterCode` (coding exercises); `maxAttempts`,
  `passingPercentage` (quizzes).
- `PUT /api/quizzes/{quizId}/questions` replaces all of a quiz's questions.
- Once published, both answer **409**. Only `position` can still be changed with PATCH
  (and the completion settings, which have their own endpoint).
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
`{ answers: [{ questionId, selectedOptionIds, textAnswer, orderedItemIds, matches }] }`
returns `QuizAttemptResponse` `{ id, quizId, submittedAt, correctCount, totalQuestions,
hints }` (hints for the questions answered wrong), and refuses (409) when the user has
used `maxAttempts`.

Each question has a `type` (backend `QuestionType`; default `MULTIPLE_CHOICE`):

| Type              | Shown with                | Answered with                                 | In the UI        |
| ----------------- | ------------------------- | --------------------------------------------- | ---------------- |
| `MULTIPLE_CHOICE` | `options`                 | `selectedOptionIds`: exactly the correct ones | yes (checkboxes) |
| `TRUE_FALSE`      | two `options`             | `selectedOptionIds`                           | yes              |
| `FILL_IN_BLANK`   | —                         | `textAnswer` (compared without case)          | not yet          |
| `ORDERING`        | `items` (UUIDs)           | `orderedItemIds`: every item once             | not yet          |
| `MATCHING`        | `leftItems`, `rightItems` | `matches: [{ leftItemId, rightItemId }]`, all | not yet          |

The backend answers **400** for the whole attempt when a fill-in, ordering or matching
answer is missing, so the quiz form does not let a student submit a quiz with those
types until it can answer them. Proposed, for the quiz page:

- `GET /api/quizzes/{quizId}/attempts/me`: the user's earlier attempts
  (`QuizAttemptResponse[]`), for "attempts used" and the best score.
- On `QuizDetailResponse`: `description` (text shown before the quiz is started) and
  `deadline` (ISO 8601 instant or null; no attempts after it).

### Looking at an earlier attempt (proposed)

Students can see how they answered each earlier attempt, with every question marked
correct or wrong. The backend already stores this per question (`QuizAnswer`:
selected options and `correct`); it only needs an endpoint to read it:

- `GET /api/quizzes/{quizId}/attempts/{attemptId}` → `QuizAttemptResponse` plus
  `answers: [{ questionId, selectedOptionIds, correct }]`. Only the user's own attempt;
  404 for anyone else's.
- It does not include which options are correct, so the review does not give the
  answers away while the student has attempts left.

### Passing percentage (backend)

Every quiz has a `passingPercentage` (1–100, default 60) in `QuizDetailResponse`. An
attempt passes when `correctCount * 100 >= passingPercentage * totalQuestions`, and the
quiz is completed once any attempt passed. `CreateQuizRequest` has no field for it, so
`createContent` sets it right after creating the quiz with
`PATCH /api/contents/{id}/completion-settings`.

The backend does not send `passed` on an attempt; the frontend works it out with the
same rule (`domain/quizzes/passRule.ts`).

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

## Mandatory for the exam (backend)

`AssignmentContent.mandatoryForExam` (boolean) says whether the assignment must be
passed to sit the exam. It is sent on `CreateAssignmentRequest` and returned in
`AssignmentResponse`; the backend changes it with
`PATCH /api/assignments/{assignmentId}/exam-requirement` (`{ mandatoryForExam }`, 204),
also after the assignment is published.

The frontend uses that endpoint (`updateExamRequirement`, `useUpdateExamRequirement`).
When the edit form saves an assignment, `editContent` sends `mandatoryForExam` there
first and the other fields with `PATCH /api/contents/{id}`, so a change to the exam
requirement is kept even if the item is published and the PATCH answers 409.

## Other endpoints

Proposed (not in the backend yet):

| Method | Endpoint                          | Request body                                                          | Response         |
| ------ | --------------------------------- | --------------------------------------------------------------------- | ---------------- |
| PATCH  | `/api/contents/{contentId}`       | Any of the item's fields (see "Editing until published")              | `Content`        |
| PUT    | `/api/quizzes/{quizId}/questions` | `CreateQuizQuestionRequest[]`: all of an unpublished quiz's questions | the questions    |
| DELETE | `/api/contents/{contentId}`       | —                                                                     | `204 No Content` |
| PATCH  | `/api/contents/{contentId}/move`  | `{ moduleId, position }`                                              | `Content`        |

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
