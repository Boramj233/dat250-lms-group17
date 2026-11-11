# Mock backend

Run the frontend without Docker, Spring Boot or Keycloak:

```sh
npm run dev:mock
```

Open http://localhost:5173. Changes in `src/` show up at once (Vite hot reload), so
there is no container to rebuild.

## How it works

`mock/server.ts` is a Vite plugin. In mock mode it answers the requests that
normally go to the backend, so the frontend code runs unchanged, including the
login popup:

| Request | Mock behaviour |
| --- | --- |
| `GET /oauth2/authorization/keycloak` | Login page with one button per test user (instead of Keycloak). "Cancel" simulates a failed login. |
| `GET /token` | Token for the logged-in test user, `401` without a session. |
| `POST /logout` | Ends the session. |
| `/api/...` | `mock/handlers.ts`. `401` without a valid token, `404` (and a warning in the terminal) for paths without a handler. |

## Files you edit

- **`data.ts`**: test users, courses, enrollments, deadlines. Log in as different
  users to test `STANDARD`, `COURSE_MANAGER` and `ADMIN`, and a user with no courses.
  Deadlines are set relative to today, so they stay upcoming.
- **`handlers.ts`**: one handler per API endpoint. When the frontend starts using a
  new endpoint, add a handler here:

  ```ts
  {
    method: 'GET',
    path: '/api/courses/:courseId/modules',
    handle: ({ user, params }) => ok(modules.filter(m => String(m.courseId) === params.courseId)),
  },
  ```

Both files are reloaded on the next request after you save them: refresh the
browser, no restart. They use the types from `src/data`, so `npm run build` fails
when the test data no longer matches what the frontend expects.

Changes made through POST/PATCH/DELETE handlers only live in memory and are lost
when you edit these files or restart the dev server.

Mock data is for development only. Before merging, still test against the real
backend (`npm run dev` with Docker running), since the mock only matches the API as
far as the handlers are written.
