# Backend architecture

Java and Spring Boot, organized by domain under `src/main/java/no/hvl/dat250/lms/`.

- Domain packages: `course`, `module`, `content`, `submission`, `enrollment`, `group`, and `user`. Each follows this structure:
  - `api/controllers/`: HTTP endpoints and request handling.
  - `api/dto/`: request and response objects.
  - `service/`: business logic.
  - `repository/`: database access.
  - `domain/`: entities and domain types.
- `auth/`: authentication controllers, services, and DTOs; currently uses a flatter structure.
- `config/`: application-wide configuration, including security.
- `placeholder/`: temporary connectivity test endpoint.
- `BackendApplication.java`: application entry point.

Configuration files belong in `src/main/resources/`; tests belong in `src/test/java/`.

Keep HTTP handling in controllers, business logic in services, and database access in repositories. Empty folders guide placement; only add files when needed.

## Tests

Group tests by domain under `src/test/java/no/hvl/dat250/lms/`.

- `CourseServiceTest`: unit tests with Mockito, without starting Spring.
- `CourseIntegrationTest`: Spring integration tests using the `test` profile and H2. Add MockMvc for API tests. Same-thread database changes roll back after each test; real HTTP server tests need separate cleanup.
- `ArchitectureTest`: checks that controllers do not depend directly on repositories.
- `BackendApplicationTests`: verifies that the Spring application context starts.

Course classes are templates without test methods. Add one `@Test` per scenario when implementing functionality. Follow Arrange, Act, Assert and keep tests independent.

Run from the project root (Windows):

```powershell
.\gradlew.bat :backend:test -x :backend:buildFrontend
```

Report: `backend/build/reports/tests/test/index.html`.

## Mandatory Assignment API

Backend integration note: `Assignment.mandatoryForExam` defaults to `false` and is
returned by the existing Assignment detail and creation responses. It is independent
of `Content.requiredForModuleCompletion`, Grade Pass/Fail and `isCompleted()`.
This flag records the teacher's requirement only; exam eligibility is not calculated.

An active teacher in the Assignment's course can update it:

```http
PATCH /api/assignments/{assignmentId}/exam-requirement
Authorization: Bearer <access-token>
Content-Type: application/json

{"mandatoryForExam": true}
```

Success returns 204. Set `false` to clear the requirement. Missing/null values return
400; missing authentication returns 401; students, inactive teachers and teachers
of other courses receive 403. Unknown Assignment IDs and other Content types return
404. The caller's identity comes from the verified principal, never from the request
body. Assignment creation also requires an active course teacher; its request body
is unchanged, and new Assignments start with `mandatoryForExam=false`.

For an existing H2 schema, back up the database and stop the application before
manually applying `src/main/resources/db/manual/h2-mandatory-assignments.sql`.
It adds a non-null Boolean column defaulting to false without changing existing
completion or grading data. The script is not executed automatically; H2 DDL is
not atomic. No frontend integration or exam eligibility endpoint is included.

## Quiz 3A API

Backend integration note: questions support `MULTIPLE_CHOICE`, `TRUE_FALSE` and
`FILL_IN_BLANK`. Existing questions and requests without a type remain multiple
choice. Every question is worth one correct/incorrect result; Passing Percentage,
QuizAttempt counts and Content Completion keep their existing meaning.

Quiz creation and question authoring require an active course TEACHER, using the
existing course-role authorization. Quiz detail requires an active course enrollment.
Attempts remain restricted to the verified active STUDENT, not a body-supplied user.
Internal QuizService lookups contain grading data and are for trusted server callers.

Create a true/false question through `POST /api/quizzes/{quizId}/questions`:

```json
{
  "questionText": "Java supports interfaces.",
  "position": 0,
  "type": "TRUE_FALSE",
  "hint": "Review Java type declarations.",
  "options": [
    {"optionText": "True", "position": 0, "correct": true},
    {"optionText": "False", "position": 1, "correct": false}
  ]
}
```

True/false questions require exactly two options and one correct option. The student
must select exactly one. Multiple choice preserves exact-set grading, including
multiple correct options; an empty selection is an incorrect answer.

Create a single-blank text question through the same endpoint:

```json
{
  "questionText": "Name the language used in this course.",
  "position": 1,
  "type": "FILL_IN_BLANK",
  "hint": "Consider JVM languages.",
  "acceptedAnswers": ["Java", "Java language"]
}
```

Fill-in questions accept 1–20 nonblank answer keys of at most 255 characters and no
options (omit options or send an empty list). Choice questions reject text answer
keys. Quiz detail includes question types and choice options, but no answer keys,
correct flags or hints. The teacher-only creation response still includes the hint.

Submit all questions once via `POST /api/quizzes/{quizId}/attempts`:

```json
{
  "answers": [
    {"questionId": 10, "selectedOptionIds": [21]},
    {"questionId": 11, "textAnswer": "  JAVA  "}
  ]
}
```

Types come from the stored question, not the client. Choice answers must provide
selectedOptionIds and must not include textAnswer. Fill-in answers must provide
textAnswer (up to 255 characters) and no selected options; omit selectedOptionIds
or send an empty list. Empty/blank text is accepted as an incorrect answer, while
missing/null text is invalid. Each question must appear exactly once. Invalid
submissions do not create an attempt or consume its limit.

Text matching strips leading/trailing whitespace and ignores case; it preserves
internal spaces and uses no fuzzy matching, regex or numeric tolerance. Original
student text is persisted unchanged.

A successful submission retains the existing attempt/score fields and adds `hints`:

```json
{
  "id": 100,
  "quizId": 7,
  "submittedAt": "2026-11-16T12:00:00Z",
  "correctCount": 1,
  "totalQuestions": 2,
  "hints": [{"questionId": 11, "hint": "Consider JVM languages."}]
}
```

Only incorrect questions with a configured nonblank hint appear. Hints do not carry
answer keys or selected-option correctness. Teachers should write hints as guidance,
not put the answer itself in the hint text. Correct answers produce no hint. This
is feedback after a whole-quiz submission, usable for a later attempt, not an
in-progress answer/hint API. No historical hint endpoint or usage counter is added.

After the first persisted attempt, adding questions returns 409. This is a simple
transactional existence check, not a concurrent exam-versioning guarantee. The
existing attempt-limit implementation is retained without adding locks. Quiz-wide
visibility windows, deadlines, result release policy and browser integration are
not expanded in this change; Ordering and Matching remain deferred.

For existing H2 databases, back up and stop the application before manually applying
`src/main/resources/db/manual/h2-quiz-question-types.sql`. It adds the type column,
text answers and the accepted-answer table, preserving old questions/answers.
The script is not automatic; H2 DDL is not atomic. Tests use disposable databases,
not the developer database.
