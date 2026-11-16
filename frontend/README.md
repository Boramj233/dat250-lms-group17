# Frontend architecture

React and TypeScript. `src/` has four layers, and each folder answers one question:

- **`domain/`**: what is an X, and how do we fetch, change and interpret it? One folder per
  data domain (its API calls, React Query hooks, types and rules).
- **`features/`**: what does the user see? One folder per part of the app they navigate
  in (its pages and the parts only those pages use).
- **`shared/`**: what knows nothing about courses, modules or content?
- **`app/`**: what ties it together (entry point, routes, app shell)?

```text
src/
├── app/                  entry point, routes, query client
│   ├── auth/             AuthGate, RequireAccess (route guards), LandingPage, LoginButton
│   └── layout/           the app shell: AppLayout, Topbar, Sidebar, CoursesPanel
├── shared/               knows nothing about courses, modules or content
│   ├── api/              apiClient (adds the Bearer token to every call)
│   ├── auth/             login session and current user: auth, access, useAuth
│   ├── lib/              dateFormat, fullName, safeUrl, useDebounced, moveItem
│   ├── navigation/       breadcrumbs (Trail)
│   ├── styles/           tokens, global styles, forms.css
│   ├── ui/               QueryState, PageHeader, DataTable, Tabs, ActionMenu (the "⋯" menu), icons,
│   │                     ConfirmDialog, SaveBar, RequiredMark, NotFoundPage
│   ├── paths.ts          URL patterns and link builders
│   └── types.ts          id types (CourseId, ModuleId, ...)
├── domain/
│   ├── <domain>/         one folder per data domain, with as much of this as it needs:
│   │   ├── api.ts        API calls (no JSX)
│   │   ├── hooks.ts      React Query hooks; mutations invalidate the cache here
│   │   ├── queryKeys.ts
│   │   ├── types.ts
│   │   ├── *.ts          rules and formatting without JSX (passRule, visibility, ...)
│   │   ├── README.md     API contract
│   │   └── components/   small display components for the domain, props only
│   │
│   ├── courses/          + courseFormat, members (role labels, sorting), CourseCard, CourseGrid, CourseForm
│   ├── deadlines/        + DeadlineList
│   ├── modules/          + prerequisites, moduleFormat
│   ├── content/          + contentTypes, visibility
│   ├── quizzes/          + passRule
│   ├── assignments/      + scoreFormat, submissionStats
│   └── grades/, groups/, users/
└── features/
    ├── dashboard/        DashboardPage (student and course manager variants)
    ├── my-courses/       CoursesPage (all my courses by term)
    ├── profile/          ProfilePage (the logged-in user)
    ├── course-view/      one course as its members see it (/courses/:courseId)
    │   ├── courseContext.ts, courseMenu.ts, contentSections.ts, useCourseItems.ts, usePeopleNames.ts
    │   ├── pages/        CourseLayout (collapsible course menu, Manage for teachers),
    │   │                 CourseMenuLayout and the menu's pages: Home, Modules, Assignments,
    │   │                 Quizzes, Code Exercises, People, Groups, Grades, Files, and
    │   │                 CourseSectionItemPage (one item opened from those lists)
    │   ├── content/      one content item, wherever it is opened (not routed):
    │   │                 ContentView picks QuizView or SubmittableContent (assignments and
    │   │                 coding exercises); ContentTypeTable lists the items of one type
    │   ├── submissions/  teachers: pages/ CourseSubmissions, ContentSubmissions, SubmissionGrading
    │   ├── module/       ModuleLayout (module menu: Files, Pages, Assignments, Quizzes,
    │   │                 Code Exercises), the module overview and one content item
    │   └── components/   DetailLayout (cards + details panel of an item's page), SideMenuLayout,
    │                     PassedBadge, VisibleFromTag, SubmissionAnswer, ...
    └── course-management/  course managers (/manage-courses)
        ├── manageCourseContext.ts, useManageBreadcrumbs.ts
        ├── pages/        ManageCourses, NewCourse, ManageCourseLayout (breadcrumbs),
        │                 CourseTabsLayout and its tabs: ModuleListPage (Content), CourseMembers,
        │                 CourseDetails
        └── module/       one module: manageModuleContext.ts, useManagedContent.ts
            ├── pages/    ManageModuleLayout, ManageModulePage (create buttons, content table,
            │             availability; contentDraft.ts holds its unsaved changes), NewContentPage,
            │             ContentDetailsPage
            └── components/  ContentForm (create and edit every content type; the values, checks
                             and what is sent are in contentFormValues.ts), VisibleFromField
```

Each layout level loads what its pages share and provides it through a React context
(`course-view/courseContext.ts`, `course-management/manageCourseContext.ts`, ...).

### Imports and path aliases

Every layer has an alias: `@app/…`, `@shared/…`, `@domain/…` and `@features/…` (defined
in `tsconfig.app.json` under `paths` and in `vite.config.ts` under `resolve.alias`; keep
the two in sync).

- **Relative imports only inside one slice** (one domain, one feature, or one folder in
  `shared/`/`app/`): `./types`, `../../manageCourseContext`.
- **Everything else goes through an alias**, also from one domain to another:
  `@shared/ui/QueryState`, `@domain/courses/hooks`, `@domain/grades/queryKeys`.
- `test/*.test.mjs` runs the source with Node. `test/setup/resolve-hooks.mjs` teaches it
  the aliases and imports without an extension, so a test can import any `.ts` file in
  `src/` (not `.tsx`: Node does not read JSX). Add a new alias there too.
- `mock/` imports `src/` with relative paths (`../src/domain/content/types`).

### Rules

- **Import downwards only:** `app` → `features` → `domain` → `shared`. `shared` never
  imports from the other layers, `domain` never imports from `features` or `app`, and
  `features` never import from `app`.
- **Features never import each other.** What two features need moves down to the domain
  that owns it (for example `domain/courses/components/CourseGrid`, used by `dashboard`
  and `my-courses`) or to `shared/` if it knows nothing about the domain. Extract it
  when it is used the third time, not before.
- **Domains may use each other's types and query keys** (for example `content/hooks.ts`
  invalidates `quizzes` keys), not each other's components.
- **Pages and contexts are internal to their feature:** only `app/routes.tsx` imports
  pages.
- **Pages orchestrate, components display.** Pages read route params, contexts and
  data hooks, and pass what the components need as props. Components in
  `domain/*/components`, `features/*/components` and `shared/ui` never call data hooks,
  contexts or `useParams`. This keeps them reusable.
- **Loading, error and empty states** go through `QueryState`, page titles through
  `PageHeader`.
- **Cache invalidation belongs in a domain's `hooks.ts`**: a mutation hook refreshes
  everything it changes, so pages never call `queryClient` themselves.
- **One folder per component**, with the component, its CSS and an `index.ts`
  that re-exports it. Import the folder, not the file inside it:

  ```text
  domain/deadlines/components/DeadlineList/
  ├── DeadlineList.tsx
  ├── DeadlineList.css
  └── index.ts          export { default } from './DeadlineList'
  ```

  ```tsx
  // in features/dashboard/pages/DashboardPage/StudentDashboard.tsx
  import DeadlineList from '@domain/deadlines/components/DeadlineList'
  ```

  Hooks, contexts and helpers without JSX (for example `courseFormat.ts`) are plain
  files at the root of their domain or feature. Parts that only one page uses live in
  that page's folder (for example `DashboardPage/StudentDashboard.tsx`).

- Use existing style tokens where possible.

Folder placement organizes the source; `src/app/routes.tsx` defines routing.

`npm run lint` checks the import rules above (`.oxlintrc.json`): layers only import
downwards, features never import each other through `@features/…`, and there are no
import cycles. It does not see a relative import from one feature into another
(`../../../dashboard/…`), so use the aliases between folders.

## Running

Run `npm install` first, then from this directory:

- `npm run dev:mock`: frontend only, with test data and test users instead of the
  backend and Keycloak. No Docker needed. See [mock/README.md](mock/README.md).
- `npm run dev`: frontend against the real backend (start it with Docker first).

Before you push:

- `npm run format`: formats all files with Prettier (`.prettierrc.json`); `npm run format:check`
  only reports files that are not formatted. Set your editor to format with Prettier on save.
- `npm run lint`: oxlint, including the import rules (see Rules above).
- `npm test`: the unit tests in `test/`.
- `npm run build`: type checks everything (also `mock/`) and builds.

## Navigation

- `/` shows the landing page with a Log in button, or the dashboard after login.
- `/courses` lists all of the user's courses by term; `/courses/:courseId` shows one
  course with its course menu (`/modules`, `/assignments`, `/people`, `/groups`,
  `/quizzes`, `/grades`, `/files`, `/code-exercises`; the lists are tables, and Files is empty until the backend has files),
  and teachers get a Manage button in it. Teachers also get Submissions
  (`/submissions`: every assignment and code exercise with answered, submitted and
  graded counts; `/submissions/:contentId` its submissions; `/submissions/:contentId/:submissionId`
  one submission to grade, see src/domain/assignments/README.md). `/courses/:courseId/modules/:moduleId` shows
  one module with its own menu (`/files`, `/pages`, `/assignments`, `/quizzes`,
  `/code-exercises`) and `/<section>/:contentId` (e.g. `/assignments/101`) one item in it.
  The course's lists open their items under themselves instead, with the course menu:
  `/courses/:courseId/assignments/:contentId` (also `/quizzes`, `/code-exercises`,
  `/grades`). `/profile` shows their account.
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
