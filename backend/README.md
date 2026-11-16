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
