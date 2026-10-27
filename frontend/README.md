# Frontend architecture

React and TypeScript, organized by domain to align with the backend.

- `src/app/`: application entry point, root component, routing, and shared layout.
- `src/features/`: domain-specific code (`auth`, `course`, `module`, `content`, `submission`). Each feature contains:
  - `pages/`: full pages.
  - `components/`: components used within the feature.
  - `api/`: backend calls for the feature.
- `src/shared/`: components and API utilities used across features.
- `src/styles/`: primitive tokens, semantic tokens, and global styles.
- `src/assets/`: images and other imported assets.

Keep component CSS next to its component. Use existing style tokens where possible. Empty folders guide placement; only add files when needed.

## Navigation

Run `npm install` and `npm run dev` from this directory to start the frontend.

- `/` redirects to `/courses`.
- `/courses` shows the course list with a temporary example course.
- `/courses/:courseId` shows a placeholder for the selected course.
- `/login` shows a placeholder for login, outside the shared layout.
- Unknown URLs show the not-found page inside the shared layout.

Define URL patterns in `src/app/paths.ts` and register pages in `src/app/routes.tsx`.
Use `Link` for navigation, `NavLink` for menu items that indicate the active route,
and `coursePath(courseId)` when linking to a specific course. The shared layout
renders the selected page through `Outlet`.

To add a page, create it under the relevant feature, export it from the feature's
`index.ts`, add its path and route, and add a navigation link if needed.
There are currently no authentication checks; all course URLs are accessible.

When deploying, configure the web server to serve `index.html` for frontend routes
so that direct links and refreshing a course page work with `BrowserRouter`.
