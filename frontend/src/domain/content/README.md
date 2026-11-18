# Content API contract

The read, write, visibility, completion and delete endpoints below exist in the
backend, and the types in `types.ts` match its DTOs. Sections marked "proposed" are
still frontend proposals.

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

## Visible from / until (backend)

Every content item has `visibleFrom` and `visibleUntil` (ISO instants or null) in
`ContentSummaryResponse` and the detail responses. Students only get the item between
the two (not in the module's list, 403/404 on its page); teachers of the course always do.

- `PUT /api/contents/{contentId}/visibility` with `{ visibleFrom, visibleUntil? }` →
  `{ contentId, visibleFrom, visibleUntil }` (teachers). Leaving `visibleUntil` out keeps
  the saved one; null removes it. Frontend: `updateVisibility`, `useUpdateVisibility`.
- The create requests have no `visibleFrom`, so `createContent` sets it with this
  endpoint right after creating the item, and `editContent` sends it last.
- Null `visibleFrom` means visible at once; the module's own `availableFrom` still keeps
  students out until the module opens, so in practice it **inherits the module's start
  date**. The frontend does not allow a date before the module's start date
  (`visibility.ts`: `effectiveVisibleFrom`, `checkContentVisibleFrom`,
  `checkModuleStartAgainstContent`); the backend does not check this.

## Editing (backend)

Each type has its own update endpoints; `editContent` calls the ones for the item's type:

| Type            | Endpoints                                                                                                                                                                      |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Page            | `PUT /api/contents/{id}` `{ title, body }`                                                                                                                                     |
| Assignment      | `PATCH /api/assignments/{id}/exam-requirement`, `PUT /api/assignments/{id}` `{ title, instructions }`, `PUT …/submission-settings` `{ deadline, allowLateSubmission }`         |
| Coding exercise | `PUT /api/coding-exercises/{id}` `{ title, instructions, starterRepositoryUrl, starterCode }`, `PUT …/submission-settings`                                                     |
| Quiz            | `PUT /api/quizzes/{id}` `{ title, maxAttempts }`, `PUT /api/quizzes/{id}/questions` `{ questions }` (only when changed), passing percentage with `PATCH …/completion-settings` |

and then `PUT /api/contents/{id}/visibility` for every type. All fields of these requests
are required except `starterCode` and `deadline`.

A quiz's questions are replaced all at once, and only when they differ from the saved ones
(`questionsChanged`), since the backend refuses (409) once a student has made an attempt;
`maxAttempts` can always be changed. Students hand in a text or one PDF file (the
backend accepts only PDF), so there is no choice of file types.

An item is shown as **published** once its visible date has passed (its own
`visibleFrom`, or its module's start date). The backend does not lock published content.

## Quiz answer key (teachers)

`GET /api/quizzes/{quizId}` (`QuizDetailResponse`) returns the questions and their
options, items and sides, but never the correct answers or hints. Teachers of the
course get them from `GET /api/quizzes/{quizId}/answer-key` (`QuizAnswerKeyResponse`,
`QuizAnswerKey` in `../quizzes/types.ts`): per question the question itself, `hint`,
`correctOptionIds`, `acceptedAnswers`, `correctOrder` and `correctMatches`. The quiz page
shows it to teachers with "Show answer key" (`useQuizAnswerKey`), and the edit form on
the manage pages fills it into the questions (`questionsFromContent(questions, answerKey)`).

## Taking a quiz

The backend grades a quiz in one request: `POST /api/quizzes/{quizId}/attempts` with
`{ answers: [{ questionId, selectedOptionIds, textAnswer, orderedItemIds, matches }] }`
returns `QuizAttemptResponse` `{ id, quizId, submittedAt, correctCount, totalQuestions,
hints }` (hints for the questions answered wrong), and refuses (409) when the user has
used `maxAttempts`.

Each question has a `type` (backend `QuestionType`; default `MULTIPLE_CHOICE`):

| Type              | Shown with                | Answered with                                 |
| ----------------- | ------------------------- | --------------------------------------------- |
| `MULTIPLE_CHOICE` | `options`                 | `selectedOptionIds`: exactly the correct ones |
| `TRUE_FALSE`      | two `options`             | `selectedOptionIds`                           |
| `FILL_IN_BLANK`   | —                         | `textAnswer` (compared without case)          |
| `ORDERING`        | `items` (UUIDs)           | `orderedItemIds`: every item once             |
| `MATCHING`        | `leftItems`, `rightItems` | `matches: [{ leftItemId, rightItemId }]`, all |

The backend answers **400** for the whole attempt when a fill-in, ordering or matching
answer is missing. Still proposed on `QuizDetailResponse`: `description` (text shown
before the quiz is started) and `deadline` (ISO 8601 instant or null).

### Attempts (`QuizAttemptController`, `../quizzes/api.ts`)

| Endpoint                                                | Who                | Response                                  |
| ------------------------------------------------------- | ------------------ | ----------------------------------------- |
| `GET /api/quizzes/{quizId}/attempts/me`                 | the student        | `QuizAttemptResponse[]` (best score)      |
| `GET /api/quizzes/{quizId}/attempts/me/count`           | the student        | `{ usedAttempts }` ("attempts used")      |
| `GET /api/quizzes/{quizId}/attempts/{attemptId}`        | the student        | `QuizAttemptDetailResponse`, own only     |
| `GET /api/quizzes/{quizId}/attempts/students/{userId}`  | teachers of course | one student's `QuizAttemptResponse[]`     |
| `GET /api/quizzes/{quizId}/attempts/{attemptId}/review` | teachers of course | any student's `QuizAttemptDetailResponse` |

`QuizAttemptDetailResponse` is the attempt with `answers: [{ questionId, question,
selectedOptionIds, textAnswer, orderedItemIds, matches, correct }]`. It never includes
the answer key, so a student's review does not give the answers away. Teachers pick a
student under "Student attempts" on the quiz page.

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

## Handing in as a group (backend)

An assignment has no "group assignment" setting. On **every** assignment and coding
exercise, a student who is in a group in the course may hand in for the group: the
submission form asks "Only me" or "Group N". That sends `groupId` with
`POST /api/contents/{contentId}/submissions` (or `…/submissions/file`); the backend
checks that the student is a member (403 otherwise) and stores the members at that
time. Every one of them then sees it in `…/submissions/me` (with `groupId` and
`members`), and its released grade counts for each of them. Group answers and the
student's own answers are separate sequences; teachers grade the latest of each.

## Mandatory for the exam (backend)

`AssignmentContent.mandatoryForExam` (boolean) says whether the assignment must be
passed to sit the exam. It is sent on `CreateAssignmentRequest` and returned in
`AssignmentResponse`; the backend changes it with
`PATCH /api/assignments/{assignmentId}/exam-requirement` (`{ mandatoryForExam }`, 204),
also after the assignment is published.

The frontend uses that endpoint (`updateExamRequirement`, `useUpdateExamRequirement`).
When the edit form saves an assignment, `editContent` sends `mandatoryForExam` there
first, so a change to the exam requirement is kept even if a later request fails.

## Deleting and ordering (backend)

| Method | Endpoint                                                    | Request body     | Response                                   |
| ------ | ----------------------------------------------------------- | ---------------- | ------------------------------------------ |
| DELETE | `/api/contents/{contentId}`                                 | —                | 204; **409** when students have work on it |
| PUT    | `/api/courses/{courseId}/modules/{moduleId}/contents/order` | `{ contentIds }` | 204; every item of the module once         |

Content with learning records (submissions, quiz attempts, a page marked as done) is
never deleted: the module page then tells the teacher to hide it with "Visible from"
instead. Frontend: `deleteContent` / `useDeleteContent`, `reorderContent` /
`useReorderContent`.

Still proposed (not in the backend): `PATCH /api/contents/{contentId}/move`
(`{ moduleId, position }`), used by "Move to" on the module page; it fails until the
backend has it.

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
const updateVisibility = useUpdateVisibility()
const deleteContent = useDeleteContent()

createContent.mutate({
  courseId,
  moduleId,
  type: 'STATIC_PAGE',
  title: 'Welcome',
  position: 0,
  body: 'Hello',
  visibleFrom: null,
})
updateVisibility.mutate({ contentId, moduleId, visibleFrom: '2026-12-01T08:00:00.000Z' })
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
