import { Navigate, Route, Routes } from 'react-router'
import AuthGate from '../ui/layout/AuthGate'
import CoursePage from '../ui/pages/CoursePage'
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
        <Route path={paths.courseRoute} element={<CoursePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
