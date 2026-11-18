# Users API contract

Profiles of the accounts in the login service (Keycloak). All endpoints below exist in
the backend (`UserController`, `AdminUserController`). `UserResponse` is
`{ userId, email, firstName, lastName, access }` (`UserAccount` in `types.ts`); `access`
is `STANDARD`, `COURSE_MANAGER` or `ADMIN` (see `@shared/auth/access.ts`).

## Everyone

| Method | Endpoint                          | Request body              | Response                                       |
| ------ | --------------------------------- | ------------------------- | ---------------------------------------------- |
| GET    | `/api/users/me`                   | —                         | `UserResponse` (`useCurrentUser`)              |
| PUT    | `/api/users/me`                   | `{ firstName, lastName }` | `UserResponse`; email and access stay the same |
| GET    | `/api/users/search?search=&size=` | —                         | `UserLookupPageResponse` (COURSE_MANAGER+)     |

The Profile page changes the names (`useUpdateMyProfile`), which also replaces the
logged-in user in the cache.

## Administrators (`/api/admin/users`, ADMIN only)

| Method | Endpoint                               | Request body                     | Response                                |
| ------ | -------------------------------------- | -------------------------------- | --------------------------------------- |
| GET    | `/api/admin/users?search=&page=&size=` | —                                | `UserPageResponse`, sorted by id        |
| GET    | `/api/admin/users/{userId}`            | —                                | `UserResponse`                          |
| POST   | `/api/admin/users`                     | `{ firstName, lastName, email }` | `201` `UserResponse`; `409` known email |
| POST   | `/api/admin/users/import`              | multipart: `file` (CSV)          | `UserImportResponse`, a report per row  |
| PATCH  | `/api/admin/users/{userId}/access`     | `{ access }`                     | `UserResponse`; `403` demoting yourself |

The CSV file has a header row with `email`, `firstName`, `lastName` and optionally
`access`, separated by commas or semicolons. Emails that already have a profile are
skipped, so the same file can be uploaded again after fixing the failed rows.

The Users page (`/admin/users`, in the sidebar for administrators) uses all of these:
the list with search and pages, "Change access" (loads the user with
`GET /api/admin/users/{userId}` first), "Add a user" and "Import users from a CSV file".
