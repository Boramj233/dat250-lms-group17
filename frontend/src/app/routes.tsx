import { Route, Routes } from 'react-router'
import { LoginPage } from '../features/auth'
import { CourseListPage, CoursePage } from '../features/course'
import AppLayout from './layout/AppLayout'
import HomePage from './pages/HomePage'
import NotFoundPage from './pages/NotFoundPage'
import { paths } from './paths'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path={paths.login} element={<LoginPage />} />

      <Route element={<AppLayout />}>
        <Route path={paths.home} element={<HomePage />} />
        <Route path={paths.courses} element={<CourseListPage />} />
        <Route path={paths.course} element={<CoursePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
