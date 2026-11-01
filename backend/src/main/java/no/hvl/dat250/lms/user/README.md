# User profiles

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

Successful requests return `userId`, `email`, `firstName`, `lastName`, and `access`, never password
hashes. Updates select the user exclusively from the authenticated principal.
Additional JSON fields, including email, are ignored and cannot change ID, access,
or credentials. The endpoint
uses the application's existing security filters, including CSRF for sessions.

Invalid profiles return 400, missing authentication 401, and deleted users 404.
The service validates input too, so
direct service calls cannot bypass profile validation.

Account registration, password hashing/change, login/logout, and session lifecycle
belong to the auth implementation. Global access-management policy is still to
be defined; this profile endpoint does not assign global access or course roles.
