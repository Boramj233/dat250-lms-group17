# Frontend architecture

React and TypeScript, organized by feature: everything that belongs to one domain or
one part of the app (its API calls, hooks, types, components and pages) lives in the
same folder under `src/features/`.

```text
src/
├── app/                  entry point, routes, query client
│   ├── auth/             AuthGate, RequireAccess (route guards), LandingPage, LoginButton
│   └── layout/           the app shell: AppLayout, Topbar, Sidebar
├── shared/               knows nothing about courses, modules or content
│   ├── api/              apiClient (adds the Bearer token to every call)
│   ├── auth/             login session and current user: auth, access, useAuth
│   ├── lib/              dateFormat, safeUrl, useDebounced
│   ├── navigation/       breadcrumbs (Trail)
│   ├── styles/           tokens, global styles, forms.css
│   ├── ui/               QueryState, PageHeader, Pager, Tabs, ActionMenu (the "⋯" menu),
│   │                     NotFoundPage
│   ├── paths.ts          URL patterns and link builders
│   └── types.ts          id types (CourseId, ModuleId, ...)
└── features/
    ├── <feature>/        one folder per feature, with as much of this as it needs:
    │   ├── api.ts        API calls (no JSX)
    │   ├── hooks.ts      React Query hooks; mutations invalidate the cache here
    │   ├── queryKeys.ts
    │   ├── types.ts
    │   ├── README.md     API contract
    │   ├── components/   reusable components for this domain, props only
    │   └── pages/        routed pages and layouts
    │
    ├── dashboard/        DashboardPage (student and course manager variants)
    ├── courses/          data + courseFormat + CourseCard, CourseGrid, CourseForm,
    │                     CoursesPage (all my courses by term)
    ├── profile/          ProfilePage (the logged-in user)
    ├── course-view/      one course as its members see it: CourseLayout (collapsible course
    │                     menu, Manage for teachers) and Home, Modules, Assignments, People,
    │                     Groups, Quizzes, Grades, Files, Code Exercises
    │   └── module/       ModuleLayout (module menu: Files, Pages, Assignments, Quizzes,
    │                     Code Exercises), the module overview and one content item
    ├── deadlines/        data + DeadlineList
    ├── modules/          data + prerequisites
    ├── content/, quizzes/, assignments/, grades/, groups/, users/   data only so far
    └── course-management/  course managers: Manage courses, New course,
        │                 ManageCourseLayout (breadcrumbs) and CourseTabsLayout
        │                 (Content, Members, Details)
        └── module/       ManageModuleLayout, the module page ("+ Assignment" …, content
                          with a "⋯" menu), NewContentPage and ContentDetailsPage.
                          Only setup and configuration; grading belongs elsewhere.
```

The module and content pages for students have been removed for now. The
data for them (`features/courses`, `features/modules`, `features/content`, ...) is
still there. When they are rebuilt, give them their own feature (for example
`features/course-view/`, with `module/` inside), with a layout per level that loads
what its pages share and provides it through a React context.

### Rules

- **Import downwards only:** `app` → `features` → `shared`. `shared` never imports
  from `features` or `app`, and `features` never import from `app`.
- **Features may use each other's data**, meaning `api.ts`, `hooks.ts`,
  `queryKeys.ts`, `types.ts` and `components/` (for example `dashboard` uses
  `courses/components/CourseGrid`, and `assignments/cache.ts` invalidates
  `grades` and `modules` keys). Pages and the contexts in a feature are internal:
  only `app/routes.tsx` imports pages.
- **Pages orchestrate, components display.** Pages read route params, contexts and
  data hooks, and pass what the components need as props. Components in
  `features/*/components` and `shared/ui` never call data hooks, contexts or
  `useParams`. This keeps them reusable.
- **Loading, error and empty states** go through `QueryState`, page titles through
  `PageHeader`.
- **Cache invalidation belongs in a feature's `hooks.ts`**: a mutation hook refreshes
  everything it changes, so pages never call `queryClient` themselves.
- **Something used in two features moves down** to the feature that owns the domain
  (for example `courses/components`) or to `shared/` if it knows nothing about the
  domain. Extract it when it is used the third time, not before.
- **One folder per component**, with the component, its CSS and an `index.ts`
  that re-exports it. Import the folder, not the file inside it:

  ```text
  features/deadlines/components/DeadlineList/
  ├── DeadlineList.tsx
  ├── DeadlineList.css
  └── index.ts          export { default } from './DeadlineList'
  ```

  ```tsx
  // in features/dashboard/pages/DashboardPage/StudentDashboard.tsx
  import DeadlineList from '../../../deadlines/components/DeadlineList'
  ```

  Hooks, contexts and helpers without JSX (for example `courseFormat.ts`) are plain
  files at the feature root. Parts that only one page uses live in that page's
  folder (for example `DashboardPage/StudentDashboard.tsx`).
- Use existing style tokens where possible.

Folder placement organizes the source; `src/app/routes.tsx` defines routing.

## Running

Run `npm install` first, then from this directory:

- `npm run dev:mock`: frontend only, with test data and test users instead of the
  backend and Keycloak. No Docker needed. See [mock/README.md](mock/README.md).
- `npm run dev`: frontend against the real backend (start it with Docker first).

## Navigation

- `/` shows the landing page with a Log in button, or the dashboard after login.
- `/courses` lists all of the user's courses by term; `/courses/:courseId` shows one
  course with its course menu (`/modules`, `/assignments`, `/people`, `/groups`,
  `/quizzes`, `/grades`, `/files`, `/code-exercises`; the lists are tables, and Files is empty until the backend has files),
  and teachers get a Manage button in it. `/courses/:courseId/modules/:moduleId` shows
  one module with its own menu (`/files`, `/pages`, `/assignments`, `/quizzes`,
  `/code-exercises`) and `/content/:contentId` one item in it. `/profile` shows their account.
- `/manage-courses` (course managers and admins): create a course
  (`/manage-courses/new`) or pick one of the courses they teach.
- `/manage-courses/:courseId` manages one course: the Content tab (index, the
  modules), `/members` and `/details`. Only teachers of the course get in.
- `/manage-courses/:courseId/modules/:moduleId` manages one module;
  `/new/:type` (`assignment`, `quiz`, `coding-exercise`, `page`) creates content in it,
  and `/content/:contentId` shows and edits one item.
- `/login` redirects to `/`.
- Unknown URLs show the not-found page inside the shared layout.

Define URL patterns in `src/shared/paths.ts` and register pages in `src/app/routes.tsx`.
Use `Link` for navigation and `NavLink` for menu items that indicate the active route.
The global `AppLayout` provides the topbar, responsive sidebar, and main content
area, rendering the selected page through `Outlet`.

To add a page, create its folder in `src/features/<feature>/pages/`, import it in `src/app/routes.tsx`,
and add its path and route. `AuthGate` shows the landing page until the user is logged
in; `RequireAccess` hides pages from users without access (the backend checks every call).

When deploying, configure the web server to serve `index.html` for frontend routes
so that direct links and refreshing a page work with `BrowserRouter`.
