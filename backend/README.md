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
