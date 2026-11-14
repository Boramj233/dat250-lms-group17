import { Navigate, Route, Routes } from 'react-router'
import AuthGate from '../ui/layout/AuthGate'
import RequireAccess from '../ui/layout/RequireAccess'
import CourseContentPage from '../ui/manage/CourseContentPage'
import CourseDetailsPage from '../ui/manage/CourseDetailsPage'
import CourseMembersPage from '../ui/manage/CourseMembersPage'
import CourseTabsLayout from '../ui/manage/CourseTabsLayout'
import ManageCourseLayout from '../ui/manage/ManageCourseLayout'
import ManageCoursesPage from '../ui/manage/ManageCoursesPage'
import ContentDetailsPage from '../ui/manage/module/ContentDetailsPage'
import ManageModuleLayout from '../ui/manage/module/ManageModuleLayout'
import ManageModulePage from '../ui/manage/module/ManageModulePage'
import NewContentPage from '../ui/manage/module/NewContentPage'
import NewCoursePage from '../ui/manage/NewCoursePage'
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
