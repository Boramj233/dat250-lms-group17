import { Navigate, Route, Routes } from 'react-router'
import CourseLayout from '../ui/course/CourseLayout'
import ContentPage from '../ui/course/pages/ContentPage'
import ContentTypePage from '../ui/course/pages/ContentTypePage'
import CourseGradesPage from '../ui/course/pages/CourseGradesPage'
import CourseGroupsPage from '../ui/course/pages/CourseGroupsPage'
import CourseHomePage from '../ui/course/pages/CourseHomePage'
import ModuleContentTypePage from '../ui/course/pages/ModuleContentTypePage'
import ModulePage from '../ui/course/pages/ModulePage'
import AuthGate from '../ui/layout/AuthGate'
import CoursesPage from '../ui/pages/CoursesPage'
import GradesPage from '../ui/pages/GradesPage'
import NotFoundPage from '../ui/pages/NotFoundPage'
import OverviewPage from '../ui/pages/OverviewPage'
import { paths } from './paths'

export default function AppRoutes() {
  return (
    <Routes>
      {/* Logging in happens from the landing page, which every page shows when needed. */}
      <Route path={paths.login} element={<Navigate to={paths.home} replace />} />

      {/* Logged in: topbar, sidebar and page content. Otherwise: the landing page. */}
      <Route element={<AuthGate />}>
        <Route path={paths.home} element={<OverviewPage />} />
        <Route path={paths.courses} element={<CoursesPage />} />
        <Route path={paths.grades} element={<GradesPage />} />

        {/* A course: course menu + one of these pages (see paths.course*). */}
        <Route path={paths.courseRoute} element={<CourseLayout />}>
          <Route index element={<CourseHomePage />} />
          <Route path="modules/:moduleId" element={<ModulePage />} />
          <Route path="modules/:moduleId/content/:contentId" element={<ContentPage />} />
          {/* One content type in one module, e.g. modules/2/assignments. */}
          <Route path="modules/:moduleId/:section" element={<ModuleContentTypePage />} />
          <Route path="groups" element={<CourseGroupsPage />} />
          <Route path="grades" element={<CourseGradesPage />} />
          {/* pages, quizzes, assignments, coding-exercises; anything else is not found. */}
          <Route path=":section" element={<ContentTypePage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
