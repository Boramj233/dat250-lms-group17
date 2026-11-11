import { Navigate, Route, Routes } from 'react-router'
import AuthGate from '../ui/layout/AuthGate'
import HomePage from '../ui/pages/HomePage'
import NotFoundPage from '../ui/pages/NotFoundPage'
import { paths } from './paths'

export default function AppRoutes() {
  return (
    <Routes>
      {/* Logging in happens from the landing page, which every page shows when needed. */}
      <Route path={paths.login} element={<Navigate to={paths.home} replace />} />

      {/* Logged in: topbar, sidebar and page content. Otherwise: the landing page. */}
      <Route element={<AuthGate />}>
        <Route path={paths.home} element={<HomePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
