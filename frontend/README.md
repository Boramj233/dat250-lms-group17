# Frontend architecture

React and TypeScript, organized by the frontend route hierarchy.

- `src/app/`: application entry point, root component, routing, and global layout.
- `src/pages/`: pages grouped by their routes, with supporting code next to the pages that use it.
- `src/shared/`: components and API utilities used across route areas.
- `src/styles/`: primitive tokens, semantic tokens, and global styles.
- `src/assets/`: images and other imported assets.

Keep component CSS next to its component and use existing style tokens where possible.

```text
src/pages/
  home/HomePage.tsx
  login/LoginPage.tsx
  not-found/NotFoundPage.tsx
```

Place components used by one page next to that page in a `components/` folder.
Use `src/shared/components/` for components shared across unrelated route areas.
Folder placement organizes the source; `src/app/routes.tsx` defines routing.

## Running

Run `npm install` first, then from this directory:

- `npm run dev:mock`: frontend only, with test data and test users instead of the
  backend and Keycloak. No Docker needed. See [mock/README.md](mock/README.md).
- `npm run dev`: frontend against the real backend (start it with Docker first).

## Navigation

- `/` shows the landing page with a Log in button, or the overview after login.
- `/courses/:courseId` shows a course.
- `/login` redirects to `/`.
- Unknown URLs show the not-found page inside the shared layout.

Define URL patterns in `src/app/paths.ts` and register pages in `src/app/routes.tsx`.
Use `Link` for navigation and `NavLink` for menu items that indicate the active route.
The global `AppLayout` provides the topbar, responsive sidebar, and main content
area, rendering the selected page through `Outlet`.

To add a page, create it under the relevant route folder in `src/pages/`,
import it in `src/app/routes.tsx`, and add its path and route.
There are currently no authentication checks.

When deploying, configure the web server to serve `index.html` for frontend routes
so that direct links and refreshing a page work with `BrowserRouter`.
