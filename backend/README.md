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
