# Local Keycloak and RabbitMQ

Run commands from the project root (`dat250-lms-group17`). Docker Desktop must be
running with Linux containers. This setup starts Keycloak, Mailpit and RabbitMQ;
event publishing/consumption still needs implementation.

## Start and stop

The initial setup creates an ignored `.env` with random local credentials. On a
fresh checkout, copy `.env.example` to `.env` and replace its example secrets.
Do not commit `.env`.

```powershell
docker compose up -d --wait --wait-timeout 180
docker compose ps
docker compose logs --tail 50 keycloak rabbitmq
docker compose stop
docker compose start
```

`docker compose down` removes the containers/network while retaining named data
volumes. Avoid `down -v` unless you intentionally want to erase realms, users,
queues, and messages. Changing `.env` or the realm import does not overwrite
existing persisted users/credentials: imports/bootstrap defaults apply on first
initialization. Manage existing settings through the admin consoles afterwards.

## Keycloak

- Admin console: http://localhost:8081/admin/ (master realm).
- Admin username/password: `KEYCLOAK_ADMIN_USERNAME` / `KEYCLOAK_ADMIN_PASSWORD` in `.env`.
- Application realm: `lms`; confidential OIDC client: `spring` (the backend's web login).
- Public client for third-party API clients: `lms-cli` (authorization code + PKCE,
  or device flow). Access tokens of both clients carry the audience `lms-api`, which
  `/api/**` requires, and live 5 minutes.
- Issuer: `http://localhost:8081/realms/lms`.
- Discovery: http://localhost:8081/realms/lms/.well-known/openid-configuration.
- Client secret: `KEYCLOAK_CLIENT_SECRET` in `.env`.
- Account console: http://localhost:8081/realms/lms/account/.
- Demo users: `admin`, `teacher`, `student1`, `student2`, or their `@example.com` emails.
- Demo password: `KEYCLOAK_DEMO_PASSWORD` in `.env` (same for all four).
- `admin@example.com` is the backend's bootstrap admin (`lms.security.initial-admin-emails`):
  its local profile gets ADMIN when it is created at the first login.

The realm is imported on first start. Password grant is disabled; authorization-code
flow is enabled. Demo passwords are non-temporary, as requested by the exercise.
Usernames do not assign local Access or course roles. Keycloak's master
administrator is also not automatically an LMS ADMIN.

## Who can log in (invite-only)

Keycloak owns accounts and passwords; the LMS owns the class list. Nobody sets
passwords for other people:

1. An LMS admin uploads a CSV file with the users (email, names, access) to
   `POST /api/admin/users/import`. This creates LMS profiles only, no accounts.
2. Each user registers in Keycloak themselves ("Register" on the login page) with
   the same email, chooses a password, and confirms the email via the link Keycloak
   sends. Forgotten passwords are reset the same way.
3. At the first login the backend finds the profile by the verified email.

Anyone can register in Keycloak, but only emails on the class list (plus the
bootstrap admin) get into the LMS (`lms.security.require-invitation=true`); others
are refused at login and no profile is created.

For the demo users, log in as `admin` and import `infra/demo-users.csv`
(see `backend/http/content.http`).

## Mailpit (development email)

Keycloak sends its emails (verify email, reset password) to Mailpit, which keeps
them instead of delivering them: http://localhost:8025/.

When implementing backend OAuth, use these application settings and pass the
client secret as an environment variable rather than copying it into Git:

```properties
spring.security.oauth2.client.registration.keycloak.client-id=spring
spring.security.oauth2.client.registration.keycloak.client-secret=${KEYCLOAK_CLIENT_SECRET}
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:8081/realms/lms
```

Compose reads `.env` for containers; it does not automatically load variables
into a separately launched Gradle/Java process.

## RabbitMQ

- Management UI: http://localhost:15672/.
- Username/password: `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` in `.env`.
- AMQP host/port: `localhost:5672`; virtual host: `lms`.

Future Spring AMQP settings:

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=${RABBITMQ_USERNAME}
spring.rabbitmq.password=${RABBITMQ_PASSWORD}
spring.rabbitmq.virtual-host=lms
```

Data persists in named Docker volumes. Published ports bind only to loopback.
The Keycloak development server and local credentials are for development only.
