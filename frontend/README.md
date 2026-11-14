# Frontend architecture

React and TypeScript. The data layer is organized by entity; the UI is organized by
where it is used, with reusable building blocks separate from the pages that use them.

```text
src/
├── app/                  entry point, routes, paths, query client
├── shared/               knows nothing about courses, modules or content
│   ├── lib/              dateFormat, safeUrl, useDebounced
│   ├── styles/           tokens, global styles, forms.css
│   ├── ui/               QueryState, PageHeader, Pager, Tabs, ActionMenu (the "⋯" menu)
│   └── types.ts          id types (CourseId, ModuleId, ...)
├── data/                 API calls and React Query hooks, no JSX
│   ├── apiClient.ts
│   ├── auth/
│   └── <entity>/         api.ts, hooks.ts, queryKeys.ts, types.ts, README.md (API contract)
│                         courses, modules, content, submissions, quizzes, grades, deadlines, users
└── ui/
    ├── layout/           AppLayout, Topbar, Sidebar, AuthGate, RequireAccess
    ├── navigation/       breadcrumbs (Trail)
    ├── pages/            Overview, Landing, NotFound
    ├── manage/           course managers: Manage courses, New course, ManageCourseLayout
    │   │                 (breadcrumbs) and CourseTabsLayout (Content, Members, Details)
    │   └── module/       ManageModuleLayout, the module page ("+ Assignment" …, content
    │                     with a "⋯" menu), NewContentPage and ContentDetailsPage.
    │                     Only setup and configuration; grading belongs elsewhere.
    └── components/       reusable building blocks, by domain, props only
        ├── courses/      CourseCard, CourseGrid, CourseForm, courseFormat
        └── DeadlineList/
```

The course, module and content pages have been removed for now. The data layer for
them (`data/courses`, `data/modules`, `data/content`, ...) is still there. When they
are rebuilt, put them in `ui/course/` (and `ui/course/module/`), with a layout per
level that loads what its pages share and provides it through a React context.

### Rules

- **Import downwards only:** `app` → `ui/pages` → `ui/components` → `shared`.
  `ui/components` and `shared` never import from `ui/pages`, and `shared` never
  imports from `data`.
- **Pages orchestrate, components display.** Pages read route params, contexts and
  data hooks, and pass what the components need as props. Components in
  `ui/components` and `shared/ui` never call data hooks, contexts or `useParams`.
  This keeps them reusable.
- **Loading, error and empty states** go through `QueryState`, page titles through
  `PageHeader`.
- **Cache invalidation belongs in `data/`**: a mutation hook refreshes everything it
  changes, so the UI never calls `queryClient` itself.
- **Something used in two places moves down** to the nearest common folder
  (`ui/components/<domain>` or `shared/`). Extract it when it is used the third time,
  not before.
- **One folder per component**, with the component, its CSS and an `index.ts`
  that re-exports it. Import the folder, not the file inside it:

  ```text
  ui/components/deadlines/DeadlineList/
  ├── DeadlineList.tsx
  ├── DeadlineList.css
  └── index.ts          export { default } from './DeadlineList'
  ```

  ```tsx
  import DeadlineList from '../components/deadlines/DeadlineList'
  ```

  Hooks, contexts and helpers without JSX (for example `courseFormat.ts`) are plain
  files next to the component folders. Parts that only one page uses live in that
  page's folder (for example `OverviewPage/StudentOverview.tsx`).
- Use existing style tokens where possible.

Folder placement organizes the source; `src/app/routes.tsx` defines routing.

## Running

Run `npm install` first, then from this directory:

- `npm run dev:mock`: frontend only, with test data and test users instead of the
  backend and Keycloak. No Docker needed. See [mock/README.md](mock/README.md).
- `npm run dev`: frontend against the real backend (start it with Docker first).

## Navigation

- `/` shows the landing page with a Log in button, or the overview after login.
- `/manage-courses` (course managers and admins): create a course
  (`/manage-courses/new`) or pick one of the courses they teach.
- `/manage-courses/:courseId` manages one course: the Content tab (index, the
  modules), `/members` and `/details`. Only teachers of the course get in.
- `/manage-courses/:courseId/modules/:moduleId` manages one module;
  `/new/:type` (`assignment`, `quiz`, `coding-exercise`, `page`) creates content in it,
  and `/content/:contentId` shows and edits one item.
- `/login` redirects to `/`.
- Unknown URLs show the not-found page inside the shared layout.

Define URL patterns in `src/app/paths.ts` and register pages in `src/app/routes.tsx`.
Use `Link` for navigation and `NavLink` for menu items that indicate the active route.
The global `AppLayout` provides the topbar, responsive sidebar, and main content
area, rendering the selected page through `Outlet`.

To add a page, create its folder in `src/ui/pages/`, import it in `src/app/routes.tsx`,
and add its path and route. `AuthGate` shows the landing page until the user is logged
in; `RequireAccess` hides pages from users without access (the backend checks every call).

When deploying, configure the web server to serve `index.html` for frontend routes
so that direct links and refreshing a page work with `BrowserRouter`.
