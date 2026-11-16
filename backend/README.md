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

## Quiz 3B-1 Ordering

Ordering extends the existing question, attempt, Hint and completion flow. It does
not add Matching, partial credit, frontend components or new concurrency locks.
Each ordering question has 2–20 distinct display items and one complete correct
sequence. Creation still requires an active teacher in the Quiz's course:

```http
POST /api/quizzes/{quizId}/questions
Content-Type: application/json
Authorization: Bearer <access-token>
```

```json
{
  "questionText": "Arrange these steps.",
  "position": 0,
  "type": "ORDERING",
  "hint": "Review the process.",
  "ordering": {
    "items": [
      {"key": "a", "text": "Step A"},
      {"key": "b", "text": "Step B"},
      {"key": "c", "text": "Step C"}
    ],
    "correctOrder": ["b", "c", "a"]
  }
}
```

Keys are temporary teacher-request references (nonblank, unique, at most 64
characters). Text is nonblank, at most 255 characters, with no identical texts
within a question after stripping their edges. correctOrder must contain every
key exactly once. Ordering questions reject options and acceptedAnswers; other
types reject a non-null ordering definition. Existing 3A authoring requests are
unchanged, including the default MULTIPLE_CHOICE type.

The server generates random UUIDs for QuizItem; it does not expose the teacher
keys, an insertion index or the correct position. Quiz detail adds an `items`
array of `{id, text}` (empty for other types). Ordering items are displayed in
stable UUID-string order, independent of correctOrder. The display can coincidentally
match the answer; it is not deliberately forced to be wrong, and no per-attempt
shuffle state is introduced. Existing student detail still omits hints and keys.

Students submit through the existing whole-quiz attempt endpoint:

```json
{
  "answers": [
    {
      "questionId": 10,
      "orderedItemIds": ["<uuid-for-b>", "<uuid-for-c>", "<uuid-for-a>"]
    }
  ]
}
```

The UUIDs must come from this question's displayed items. Missing/null/empty,
incomplete, duplicate, extra, invalid or foreign UUIDs return 400 and create no
attempt. textAnswer and nonempty selectedOptionIds are rejected for Ordering.
Other types reject nonempty orderedItemIds. Complete but wrongly ordered answers
are valid submissions and count as incorrect; only exact list equality is correct.
Original MC empty-selection and FILL blank-text behavior stays unchanged.

Correct sequences and submitted sequences use separate relational join tables
with `@OrderColumn`; submitted order is never sorted or converted to a Set before
storage. QuizItem belongs to its question, and no cascade delete is added. Each
question remains worth one correct/incorrect result, reusing Passing Percentage,
Content Completion and the existing post-submission wrong-answer Hint behavior.
Permissions, attempt limits and the post-first-attempt question freeze remain.
Reading detail also works with OSIV disabled, without relying on a controller
transaction.

Apply `src/main/resources/db/manual/h2-quiz-ordering.sql` manually only after
backup and stopping the application, on a schema that already has Quiz 3A.
It adds quiz_items, quiz_question_correct_order and quiz_answer_ordered_items.
It also expands question_type to the four-value H2 ENUM: databases created by
Hibernate can have a native ENUM restricted to the old three types, while the
older manual 3A script used VARCHAR. The migration handles both and preserves
existing questions and answers. H2 DDL is not atomic; this is not an automatic
migration system. Only disposable H2 databases are used in upgrade tests.

## Quiz 3B-2 Matching

Matching supports complete one-to-one associations, with 2–20 items on each side
and equal side sizes. It reuses QuizItem's random UUIDs and question ownership.
QuizItemKind distinguishes ORDERING, MATCHING_LEFT and MATCHING_RIGHT; existing
Ordering items retain the ORDERING kind. No partial credit, distractors, many-to-one
matching, frontend work or new locking is included.

Create through the existing teacher-only question endpoint:

```json
{
  "questionText": "Match each term to its description.",
  "position": 0,
  "type": "MATCHING",
  "hint": "Review these concepts.",
  "matching": {
    "leftItems": [
      {"key": "l1", "text": "Java"},
      {"key": "l2", "text": "React"}
    ],
    "rightItems": [
      {"key": "r1", "text": "UI library"},
      {"key": "r2", "text": "Programming language"}
    ],
    "correctMatches": [
      {"leftKey": "l1", "rightKey": "r2"},
      {"leftKey": "l2", "rightKey": "r1"}
    ]
  }
}
```

Each side requires nonblank unique temporary keys (up to 64 characters) and
nonblank display texts (up to 255 characters). Texts must be distinct within the
same side after stripping their edges; keys are scoped to their side. Every left
and right key must occur exactly once in correctMatches. Matching rejects options,
acceptedAnswers and an Ordering definition. Other types reject a non-null Matching
definition. The server replaces temporary keys with random UUID references.

Quiz detail adds `leftItems` and `rightItems`, each containing only `{id, text}`.
Each side is independently sorted by its UUID string; neither side is generated
by traversing correctMatches. Teacher keys, paired item IDs, correct pairs and
hints are absent from student detail. As with Ordering, a display can coincide
with the correct alignment by chance; it is not forced to be incorrect. Other
question types have empty leftItems/rightItems; Matching has an empty Ordering
items array.

Submit through the existing whole-quiz attempt endpoint:

```json
{
  "answers": [
    {
      "questionId": 10,
      "matches": [
        {"leftItemId": "<uuid-for-l1>", "rightItemId": "<uuid-for-r2>"},
        {"leftItemId": "<uuid-for-l2>", "rightItemId": "<uuid-for-r1>"}
      ]
    }
  ]
}
```

Missing/null/empty/incomplete matches, reused left or right items, invalid UUIDs,
foreign-question items and reversed sides return 400 without creating an attempt.
Matching rejects textAnswer and nonempty selectedOptionIds/orderedItemIds; other
types reject nonempty matches. Empty unused lists remain acceptable.

Complete valid pairs are compared as a left-to-right map. Pair-array order does
not matter. A complete but incorrect association is a valid answer that counts
as wrong and receives the configured nonblank Hint, without revealing which pair
was wrong. A correct answer receives no Hint. Question weights, attempt limits,
Passing Percentage, Content Completion and first-attempt question freeze retain
their existing behavior.

QuizMatch is an embeddable pair of QuizItem references. Correct matches and each
student's submitted matches are stored in separate element-collection tables,
with foreign keys and per-owner uniqueness on both sides. Pair order is not
persisted as meaningful state. No cascade remove is added. Student detail and
submission work with OSIV disabled and without an outer test transaction.

For an existing Ordering schema, manually apply
`src/main/resources/db/manual/h2-quiz-matching.sql` after backup and stopping the
application. It expands the question-type ENUM, defaults existing items to
ORDERING, and adds quiz_question_correct_matches and quiz_answer_matches. Tests
cover old native H2 ENUM and VARCHAR question-type columns, preserved Ordering
history, and distinct correct/submitted Matching pairs after restart. Prior
upgrade tests apply the incremental chain before validating today's mappings.
Do not rerun the older Ordering script on a database already containing Matching
questions: that older script enumerates only the first four types. Scripts are
not an automatic migration system; H2 DDL is non-atomic. No developer database
has been accessed or migrated by these tests.
