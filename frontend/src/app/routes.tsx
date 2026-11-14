import { Navigate, Route, Routes } from 'react-router'
import AuthGate from './auth/AuthGate'
import RequireAccess from './auth/RequireAccess'
import CourseContentPage from '../features/course-management/pages/CourseContentPage'
import CourseDetailsPage from '../features/course-management/pages/CourseDetailsPage'
import CourseMembersPage from '../features/course-management/pages/CourseMembersPage'
import CourseTabsLayout from '../features/course-management/pages/CourseTabsLayout'
import ManageCourseLayout from '../features/course-management/pages/ManageCourseLayout'
import ManageCoursesPage from '../features/course-management/pages/ManageCoursesPage'
import ContentDetailsPage from '../features/course-management/module/pages/ContentDetailsPage'
import ManageModuleLayout from '../features/course-management/module/pages/ManageModuleLayout'
import ManageModulePage from '../features/course-management/module/pages/ManageModulePage'
import NewContentPage from '../features/course-management/module/pages/NewContentPage'
import NewCoursePage from '../features/course-management/pages/NewCoursePage'
import NotFoundPage from '../shared/ui/NotFoundPage'
import DashboardPage from '../features/dashboard/pages/DashboardPage'
import { paths } from '../shared/paths'

export default function AppRoutes() {
  return (
    <Routes>
      {/* Logging in happens from the landing page, which every page shows when needed. */}
      <Route path={paths.login} element={<Navigate to={paths.home} replace />} />

      {/* Logged in: topbar, sidebar and page content. Otherwise: the landing page. */}
      <Route element={<AuthGate />}>
        <Route path={paths.home} element={<DashboardPage />} />

        {/* Only course managers (and admins); others get the not-found page. */}
        <Route element={<RequireAccess level="COURSE_MANAGER" fallback={<NotFoundPage />} />}>
          <Route path={paths.manageCourses} element={<ManageCoursesPage />} />
          <Route path={paths.newCourse} element={<NewCoursePage />} />

          {/* One course (see paths.manage*): its tabs, then its modules and their content. */}
          <Route path={paths.manageCourseRoute} element={<ManageCourseLayout />}>
            <Route element={<CourseTabsLayout />}>
              <Route index element={<CourseContentPage />} />
              <Route path="members" element={<CourseMembersPage />} />
              <Route path="details" element={<CourseDetailsPage />} />
            </Route>

            <Route path="modules/:moduleId" element={<ManageModuleLayout />}>
              <Route index element={<ManageModulePage />} />
              <Route path="new/:type" element={<NewContentPage />} />
              <Route path="content/:contentId" element={<ContentDetailsPage />} />
            </Route>

            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
