# User profiles

## Account lookup for enrollment

`GET /api/users/search?search=Sara&page=0&size=20` is available to users whose
current stored access is COURSE_MANAGER or ADMIN. Search text is required,
non-blank, and at most 255 characters. It uses the same case-insensitive name/email
and exact numeric ID search as the administrator list, with zero-based pages,
size 1–100, and ascending user ID order.

The response contains `users`, `page`, `size`, `totalElements`, and `totalPages`.
Each user contains only `userId`, `firstName`, `lastName`, and `email`. Global
access, password-change requirements, and password hashes are excluded, including
when an administrator uses this endpoint. No matches returns an empty page.

STANDARD users are rejected with 403 and anonymous requests with 401. Current
stored access is checked on every call, so deleted/demoted course managers cannot
retain lookup access through an old session. Invalid search/pagination returns 400.

This endpoint only finds existing accounts. Account creation and global access
changes remain ADMIN-only. Course enrollment and authorization to manage a
particular course must be implemented in the course/enrollment domains. Global
Access does not determine a user's student/teacher role within a course.

## Administrator API

Every operation below requires current stored `Access.ADMIN`, checked in the
service. Profile responses never include password hashes.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/admin/users?search=&page=0&size=20` | List/search users |
| GET | `/api/admin/users/{userId}` | Read one user |
| PATCH | `/api/admin/users/{userId}/access` | Set global access |

List responses contain `users`, `page`, `size`, `totalElements`, and `totalPages`.
Pages start at zero; size must be 1–100. Results are ordered by `userId`.
Search (max 255 characters) matches first name, last name, or email substrings
without case sensitivity, and also matches an exact numeric user ID. Blank search
lists all users. `%` and `_` are treated as literal search characters.

Access changes take `{"access":"COURSE_MANAGER"}`; allowed values are STANDARD,
COURSE_MANAGER, and ADMIN. Administrators cannot remove their own ADMIN access.
Other users' access can be assigned by an administrator; this does not change
course roles. Rejected operations do not change stored access.

Responses: 400 for invalid parameters/access, 401 for missing authentication,
403 for insufficient access or self-demotion, and 404 for unknown target users.
Session-based writes require CSRF tokens. Auth must still implement the separate
first-login password-change restriction described below. No admin frontend or
creation HTTP endpoint is included yet; creation still uses the service method.

`UserService.createUser(firstName, lastName, passwordHash)` validates and saves a
new user with STANDARD access and `mustChangePassword = true`, returning the user
with generated ID and email. The method requires an authenticated `User` in the
Spring Security context and reloads the actor from the database to require
`Access.ADMIN`. Missing authentication is rejected; ordinary users, course
managers, deleted users, and former admins are not allowed to create accounts.
Names and the encoded password must be non-blank and at most 255 characters.
The trusted caller must hash the password before calling this method; the user
service does not hash passwords or verify encoding formats. No creation HTTP
endpoint is exposed yet. Return a `UserResponse` rather than the entity from any
future registration endpoint to keep the stored hash out of API responses.

`GET /api/users/me` returns the authenticated user's stored profile.
`PUT /api/users/me` replaces the required first/last names:

```json
{"firstName": "Sara", "lastName": "Nordmann"}
```

First and last names are required, non-blank, and at most 255 characters each.
Omitted, null, empty, or whitespace-only names are rejected with 400. Both name
columns are non-null, and entity validation enforces the same name constraints
when persisting users. Account creation must supply both names.
Email is automatically generated as `{userId}@epost.com` when the user is first
persisted, for example `42@epost.com`. It cannot be supplied by the caller or
changed later. It is the unique login identifier.
`userId` is the automatically generated Long primary key
(database column `user_id`); it cannot be supplied or changed through the profile API.
There is no separate username field. `UserDetails.getUsername()` returns email
because Spring Security requires that method for the login identity.
JPA excludes email from updates (`updatable = false`).
ID generation uses the `user_id_seq` database sequence, so the ID is available
in the `@PrePersist` callback that sets email before the first INSERT. New Java
objects have neither ID nor email until persisted. Account creation supplies
only the encoded password, first name, and last name to the constructor.

Successful requests return `userId`, `email`, `firstName`, `lastName`, `access`,
and `mustChangePassword`, never password
hashes. Updates select the user exclusively from the authenticated principal.
Additional JSON fields, including email, are ignored and cannot change ID, access,
or credentials. The endpoint
uses the application's existing security filters, including CSRF for sessions.

Invalid profiles return 400, missing authentication 401, and deleted users 404.
The service validates input too, so
direct service calls cannot bypass profile validation.

Account registration, password hashing/change, login/logout, and session lifecycle
belong to the auth implementation. Ordinary profile updates do not assign global
access or course roles; only the administrator access endpoint assigns global access.

The user model persists the first-login requirement and exposes it in profile
responses. Profile updates cannot clear it. `User.changePassword(encodedHash)`
replaces the hash and clears the flag together; it is an internal domain operation,
not an HTTP endpoint. The auth service must first verify the current plaintext
password, reject password reuse, and encode the new password. It then calls
`UserService.changePassword(verifiedCurrentPasswordHash, newPasswordHash)` with
the stored hash it verified and the newly encoded hash. The service selects the
account from the authenticated identity, validates the new hash as non-blank and
at most 255 characters, and updates the hash and first-login flag together in
one transaction. It accepts no target user ID. If the verified hash is no longer
current (or the account was deleted), the update is rejected without overwriting
credentials. Identical encoded hashes are rejected as well; checking plaintext
password reuse remains auth's responsibility.

This is an internal persistence operation, not a password-change HTTP endpoint;
neither hash argument should come directly from an HTTP request. Hashing and
verification are not performed here. The conditional database update clears the
JPA persistence context, but detached session principals still contain old values.
Auth must invalidate or refresh sessions after success and enforce the first-login
restriction against current persisted state.

Auth/security must still restrict accounts with `mustChangePassword = true` to
password change and logout. That restriction is not implemented by this feature;
the flag does not prevent authentication. The first administrator must be
provisioned through trusted setup, not an unauthenticated creation endpoint.
