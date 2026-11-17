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
body. Assignment creation also requires an active course teacher and accepts an
optional `mandatoryForExam` field in its POST body. `true` creates a mandatory
Assignment; `false`, null or an omitted field creates a nonmandatory Assignment.
The 201 response includes the persisted value. PATCH still requires an explicit
nonnull true/false value; its behavior is unchanged. This API addition requires
no new database migration and does not affect Content Completion or exam eligibility.

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

## CodingExercise 4A API

Creating a CodingExercise requires an active TEACHER enrollment in the course:
`POST /api/courses/{courseId}/modules/{moduleId}/contents/coding-exercises`.
The teacher's local User ID comes from the verified principal. The module must
belong to the specified course. Reading `GET /api/coding-exercises/{exerciseId}`
requires an active enrollment (student or teacher) in the exercise's course.
Missing authentication returns 401; insufficient course access returns 403;
a missing exercise, another Content type or a mismatched course/module returns 404.

The creation body accepts optional `starterCode` alongside the existing required
`starterRepositoryUrl`:

```json
{
  "title": "Implement greeting",
  "position": 0,
  "instructions": "Complete the greet method.",
  "starterRepositoryUrl": "https://example.com/course/starter",
  "starterCode": "public String greet() {\n    // TODO\n    return null;\n}\n",
  "allowLateSubmission": false
}
```

Creation and detail responses include `starterCode`. It is plain display text,
limited to 10,000 characters; omitted/null values remain null, empty strings are
allowed, and whitespace/indentation/newlines are preserved. No language selection,
execution, repository fetching or automatic grading is performed. A frontend
should render it as text in a code block, not as HTML. This field is independent
of instructions and the repository URL; it is not a student's submission.

For existing H2 databases, back up and stop the application before manually
applying `src/main/resources/db/manual/h2-coding-starter-code.sql`. It adds a
nullable column; existing exercises retain null. The script is not run
automatically and H2 DDL is not atomic. No development database was migrated.

The 4A changes cover creation/read authorization and starter code only.
Manual review/grading and Completion are described below; global content
visibility and frontend integration remain outside these changes.

## CodingExercise 4B: manual review and grading

Teachers can use a CodingExercise as a manually assessed coding task. Assignment
and CodingExercise submissions now share the existing teacher review and Grade
APIs; there is no separate CodingGrade or new grading endpoint:

- `GET /api/contents/{contentId}/submissions/review`: latest submission per individual/group sequence.
- `GET /api/submissions/{submissionId}/review`: submission detail.
- `GET /api/submissions/{submissionId}/review/history`: that individual/group sequence's history.
- `PUT /api/submissions/{submissionId}/grade`: save score, feedback, optional passed and version.
- `GET /api/submissions/{submissionId}/grade/review`: teacher Grade view.
- `POST /api/submissions/{submissionId}/grade/release`: release using the current version.
- `GET /api/submissions/{submissionId}/grade`: the owner's released Grade.

Teacher operations require an active TEACHER enrollment in the Content's course.
Scores remain 0–100 with at most two decimal places; feedback is limited to 10,000
characters. A draft may have passed=null, but first release requires an explicit
Pass/Fail decision independent of the numeric score. Students cannot read drafts.
Only the latest submission in its sequence can be graded or released for the first
time; released grades cannot be edited, and repeated release is idempotent.
Resubmission creates a new ungraded version and preserves historical released grades.

CodingExercise supports existing TEXT/GIT_URL individual and group submissions.
A Group Grade belongs to the concrete Submission and its immutable member snapshot:
former members retain access to their old released grades, and new members do not
inherit them. Individual and group sequences remain independent. FILE upload remains
Assignment-only; reviewing a coding submission does not fetch or execute its repository.

The official description allows assignments to be coding tasks and also describes
automatically graded coding exercises. This phase supplies the manual teacher workflow
only. Assignment and CodingExercise remain separate Content types; no assignment/exercise
mode or automatic/manual grading selector is introduced. Future automatic practice
requires a separate design, including isolated execution and how automated results
relate to Grade's required teacher attribution; it must not execute student code in
the LMS JVM. Manual grading does not fulfill the automatic-grading capability.

The later Completion integration below also supports manually graded CodingExercises.
No Entity/schema migration, frontend, automatic grading or hidden tests are added in
4B. Existing Assignment behavior and transaction/locking rules are retained.

## Content access control

The following existing APIs use course-scoped authorization:

- `POST /api/courses/{courseId}/modules/{moduleId}/contents/pages`: active course TEACHER.
- `GET /api/courses/{courseId}/modules/{moduleId}/contents`: active course enrollment.
- `GET /api/contents/{contentId}`: active enrollment in the StaticPage's actual course.
- `GET /api/assignments/{assignmentId}`: active enrollment in the Assignment's actual course.

Reads allow both active students and teachers. Global access levels do not bypass
course enrollment. The server derives the detail resource's course from stored data;
clients cannot supply a user/enrollment ID or substitute a course to gain access.
StaticPage detail remains page-only, not a generic detail endpoint for every Content type.

Missing authentication returns 401; insufficient/inactive course enrollment returns
403. Missing/wrong-type detail resources return 404. For course/module routes,
course authorization is checked first; an authorized caller gets 404 if the module
is missing or belongs to another course. An enrolled user can read an empty module
as an empty list. Request and response shapes remain unchanged.

These HTTP checks reuse CourseAccess and existing course-role annotations. Internal
Content/Assignment lookup services remain for trusted server callers; callers must
apply the authorization appropriate to their workflow (including historical submission
ownership). This change does not add ordering, editing, visibility or Module unlocking,
and does not require a database migration.

## Content Completion: Service contract for Module integration

```java
public boolean isCompleted(Long contentId, Long studentUserId)
```

Call `ContentService.isCompleted` through the injected Spring service. The second
argument is the trusted local User ID, not Enrollment ID or a client-supplied identity.
The student must have an active STUDENT enrollment in the Content's actual course.
The method is read-only and checks persisted completion evidence:

| Content | Completed when |
|---|---|
| StaticPage | A manual completion record exists for this page and student Enrollment. |
| Quiz | Any attempt passes the current passingPercentage (default 60); exact comparison, no rounding. |
| Assignment | Any historically owned Submission has a released Grade with passed=true. |
| CodingExercise | Same released-pass rule as Assignment, for individual or Group submissions. |

Numeric scores alone do not establish Assignment/Coding completion. Unsubmitted,
ungraded, undecided/Passed drafts and released Failed grades do not count.
A historical released pass continues to count after an ungraded or failed resubmission.
Individual ownership uses the submitter; Group ownership uses the immutable submission
member snapshot. Former members retain old evidence, new members do not inherit it.
Withdrawal retains history but prevents current Completion queries until the student
again has an active STUDENT enrollment. Quiz threshold changes reinterpret old attempts.

### Required configuration and Module responsibilities

All four types can be configured through the existing teacher API:

```http
PATCH /api/contents/{contentId}/completion-settings
Authorization: Bearer <access-token>
Content-Type: application/json

{"requiredForModuleCompletion":true}
```

This requires an active teacher in the Content's course. The flag defaults to false
and is independent of the student's completed status. passingPercentage is optional
and Quiz-only. CodingExercise cannot be marked complete manually through PUT completion.
The existing student GET completion API reports the same rule for the caller only.

Module code obtains the module's Content through ContentService (for example,
`findContentsByModule(courseId, moduleId)`), filters `isRequiredForModuleCompletion()`,
and requires every selected Content to satisfy `isCompleted`. No required Content
means the completion condition is satisfied. The following illustrates aggregation;
it is not an implemented Module endpoint:

```java
// Module caller validates its own authorization and the target student's
// active course enrollment, including when the required set is empty.
boolean requirementsSatisfied = contentService.findContentsByModule(courseId, moduleId)
    .stream()
    .filter(Content::isRequiredForModuleCompletion)
    .allMatch(content -> contentService.isCompleted(content.getId(), studentUserId));
```

Module remains responsible for its completion aggregation, prerequisites, dependency
graph validation and access/unlock decisions. Do not use mandatoryForExam as the
required filter. isCompleted neither checks prerequisites nor enforces content
visibility/deadlines. Do not inject ContentService back into the existing ModuleService:
ContentService already depends on ModuleService; use a separate Module dependency
orchestration service to avoid a circular dependency. Cross-feature repository access
continues to go through owning services.

### Errors and implementation boundary

- Null/nonpositive IDs: IllegalArgumentException.
- Missing Content: NoSuchElementException.
- Absent, inactive or non-STUDENT course enrollment: AccessDeniedException.
- Valid active student without completion evidence: false.

These errors are not interchangeable with incomplete progress; the caller should not
catch every exception and silently return false. Required and Completion status are
separate, so optional Content is not automatically completed.

Coding Completion reuses GradeCompletionService and the existing Grade query; no new
table, grading model or migration is required. This phase does not implement Module
Dependency, Frontend, ordering, automatic execution or GradingMode.

## Content Ordering

An active TEACHER in the course can replace the order of all Content in one Module:

```http
PUT /api/courses/{courseId}/modules/{moduleId}/contents/order
Authorization: Bearer <access-token>
Content-Type: application/json

{"contentIds":[25,11,18]}
```

The list must contain every Content ID in that Module exactly once, across all four
Content types. Its order becomes positions 0, 1, 2, ... . Success returns 204; use the
existing Module Content GET to read the result. Repeating the same order is idempotent.
An empty Module accepts an empty list. Missing/null/nonpositive/duplicate IDs, missing
items, unknown IDs and items from another Module return 400 without changing order.
Missing authentication returns 401; students, inactive teachers and other-course
teachers receive 403. A teacher authorized for the Course receives 404 for a missing
Module or one belonging to another Course.

Creation APIs keep their existing position parameter. Nonnegative unused positions
(including gaps) are accepted; an occupied position returns 409 for every Content type.
The database enforces UNIQUE(module_id, content_position) and content_position >= 0.
The availability check supplies a clear conflict before save; the unique constraint
also protects writes that bypass it or conflict after the check.

Reordering uses one transaction and two flushes: first move items to unused temporary
nonnegative positions outside the current and final positions, then write the final
0..n-1 positions. This permits swaps without violating immediate unique constraints.
No new locks, retries or ordering version model are introduced; concurrent teacher
edits are not given a stale-order detection guarantee. Content IDs, Module membership,
Required settings, quiz questions, completion records, submissions and Grades are retained.
The endpoint does not implement cross-Module moves, editing or visibility.

For an existing H2 schema, back up and stop the application before manually applying
`src/main/resources/db/manual/h2-content-ordering.sql`. The script normalizes each Module
by old position then Content ID, resolving duplicate/negative/gapped positions, and adds
the named constraints. Only position values change. It uses a temporary table and briefly
drops the named unique constraint when rerun; H2 DDL is not atomic, so run it with the
application stopped. It is not run automatically. Migration/constraint/restart tests use
only disposable H2 databases; no actual development database is accessed or reset.

## Basic Content Editing

Active course teachers can replace the basic editable fields of an existing Content:

| PUT endpoint | Request fields |
|---|---|
| `/api/contents/{contentId}` (StaticPage only) | title, body |
| `/api/assignments/{assignmentId}` | title, instructions |
| `/api/coding-exercises/{exerciseId}` | title, instructions, starterRepositoryUrl, optional starterCode |
| `/api/quizzes/{quizId}` | title |

Success returns 200 with the existing type-specific response DTO (QuizResponse for
quiz updates). Each PUT supplies the complete editable field set, not a partial patch.
Missing/null/blank required text returns 400. title is at most 255 characters; body,
instructions and starterCode are at most 10,000; starterRepositoryUrl remains required
and at most 2,048. starterCode may be null or omitted to clear it, or empty; its original
whitespace/newlines are preserved. Other text fields are also stored without trimming.

```http
PUT /api/coding-exercises/42
Authorization: Bearer <access-token>
Content-Type: application/json

{"title":"Greeting","instructions":"Complete greet().","starterRepositoryUrl":"https://example.com/starter","starterCode":"  // TODO\n"}
```

Authorization uses the resource's actual course and existing CourseAccess checks.
Missing authentication returns 401; students, inactive teachers and teachers of other
courses receive 403. Missing IDs and wrong Content types return 404. No global role
bypasses course enrollment. Services use ordinary transactions and JPA dirty checking;
internal callers are trusted and must apply their workflow's authorization.

Editing remains allowed after submissions or quiz attempts. It updates the current
content only: no previous text/instruction/starter-code versions are retained. Existing
Submission payloads, Grade feedback/Pass/Fail/release, attempts, answer keys and Completion
are preserved. Quiz title editing does not allow question changes after an attempt.

These requests do not edit position, Module, Required, mandatoryForExam, passingPercentage,
maxAttempts, deadlines, late-submission rules or visibility. Existing ordering and settings
endpoints retain their separate paths. No schema migration, new locks or versions are added.

Frontend integration: current proposed `PATCH /api/contents/{id}` and
`PUT /api/quizzes/{id}/questions` are not implemented by this phase. Use the type-specific
PUT endpoints above for basic editing; no frontend code is changed here.
