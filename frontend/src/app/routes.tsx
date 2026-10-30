import { Route, Routes } from 'react-router'
import LoginPage from '../ui/pages/LoginPage'
import AppLayout from '../ui/layout/AppLayout'
import HomePage from '../ui/pages/HomePage'
import NotFoundPage from '../ui/pages/NotFoundPage'
import { paths } from './paths'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path={paths.login} element={<LoginPage />} />

      <Route element={<AppLayout />}>
        <Route path={paths.home} element={<HomePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
