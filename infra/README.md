# Local Keycloak and RabbitMQ

Run commands from the project root (`dat250-lms-group17`). Docker Desktop must be
running with Linux containers. This setup starts infrastructure only; backend
OAuth/token handling and event publishing/consumption still need implementation.

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
- Demo users: `teacher`, `student1`, `student2`, or their `@example.com` emails.
- Demo password: `KEYCLOAK_DEMO_PASSWORD` in `.env` (same for all three).

The realm is imported on first start. Self-registration and password grant are
disabled; authorization-code flow is enabled. Demo passwords are non-temporary,
as requested by the exercise. Usernames do not assign local Access or course
roles. Keycloak's master administrator is also not automatically an LMS ADMIN.
No local backend profiles are created by this infrastructure setup.

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
