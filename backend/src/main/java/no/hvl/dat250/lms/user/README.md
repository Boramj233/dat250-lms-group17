# Local user profiles

The user feature stores application profiles and global access. Keycloak owns
accounts, credentials, password changes, and first-login requirements. There is
no local password column, password encoder, UserDetailsService, password-change
endpoint, or mustChangePassword flag.

`User(email, firstName, lastName)` requires a valid, non-blank email and non-blank
names, all at most 255 characters. The email must match the external identity;
it is unique and cannot be changed through profile updates. Only `userId` is
generated locally. It remains the Long primary key backed by `user_id_seq`.
`User` implements Principal for a mapped local identity, not UserDetails.

## Authentication integration still required

The auth feature must validate Keycloak OIDC/token identities and map them to
local profiles using the verified email claim. The current user controllers and
service expect the resulting local User as the authenticated principal. They do
not accept an unverified email from an HTTP header as proof of identity.
Direct OidcUser/Jwt principal mapping, OAuth login, bearer-token validation,
token issuance/revocation, and public/static asset rules are not implemented by
this cleanup. API tests inject local authenticated principals to test profile and
access policies; they do not prove Keycloak or bearer-token integration.

The exercise's optional local-profile creation on first verified OIDC login is
not implemented yet. The administrator operation below pre-provisions a local
profile only; it neither creates a Keycloak account nor changes its credentials.
The first local administrator still requires trusted setup.

## Profile endpoints

`GET /api/users/me` returns userId, email, firstName, lastName, and access.
`PUT /api/users/me` replaces both required names:

```json
{"firstName":"Sara","lastName":"Nordmann"}
```

Profile updates cannot change ID, email, or global access. Additional JSON fields
are ignored. Invalid names return 400; missing authentication returns 401; a
removed local profile returns 404. Name changes currently affect local display
names only; future auth integration must decide whether Keycloak or the local
profile is authoritative for display names.

## Administrator endpoints

All operations below require current stored Access.ADMIN, checked by the service.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/admin/users` | Create a local profile for an external identity |
| GET | `/api/admin/users?search=&page=0&size=20` | List/search local profiles |
| GET | `/api/admin/users/{userId}` | Read one profile |
| PATCH | `/api/admin/users/{userId}/access` | Set global application access |

Creation accepts firstName, lastName, and email. It returns 201, a Location
header, and the profile. It defaults to STANDARD and cannot accept a chosen
userId or access level. Duplicate emails return 409. No credentials are accepted
or stored. `UserService.createUser(firstName, lastName, email)` performs the
administrator check, validation, uniqueness check, and persistence.

Access updates accept {"access":"COURSE_MANAGER"}. Allowed values are STANDARD,
COURSE_MANAGER, and ADMIN. An administrator cannot remove their own ADMIN access.
Global access does not represent the student/teacher role in any course.

List responses contain users, page, size, totalElements, and totalPages. Pages
start at zero; size is 1–100, sorted by userId. Search is at most 255 characters,
matching name/email substrings without case sensitivity or an exact numeric ID.
Blank search lists all users; percent and underscore are literal characters.

Invalid requests return 400, missing authentication 401, insufficient access 403,
and missing target profiles 404. Access is reloaded from the database to reject
outdated privileges. Current session-based test scaffolding requires CSRF tokens
for writes; the future stateless bearer API needs its own security configuration.

## Restricted lookup for enrollment

`GET /api/users/search?search=Sara&page=0&size=20` requires COURSE_MANAGER or ADMIN
and non-blank search text. Pagination/search rules match the administrator list.
The response has pagination metadata and users containing only userId, firstName,
lastName, and email. No matches returns an empty page.

Course managers cannot create profiles or assign global access. Enrollment and
course-specific permissions remain the responsibility of course/enrollment.