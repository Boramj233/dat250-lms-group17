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
import CoursesPage from '../features/courses/pages/CoursesPage'
import CourseContentTypePage from '../features/course-view/pages/CourseContentTypePage'
import CourseGradesPage from '../features/course-view/pages/CourseGradesPage'
import CourseGroupsPage from '../features/course-view/pages/CourseGroupsPage'
import CourseHomePage from '../features/course-view/pages/CourseHomePage'
import CourseLayout from '../features/course-view/pages/CourseLayout'
import CourseMenuLayout from '../features/course-view/pages/CourseMenuLayout'
import { CONTENT_SECTIONS } from '../features/course-view/contentSections'
import ModuleContentPage from '../features/course-view/module/pages/ModuleContentPage'
import ModuleContentTypePage from '../features/course-view/module/pages/ModuleContentTypePage'
import ModuleLayout from '../features/course-view/module/pages/ModuleLayout'
import ModuleOverviewPage from '../features/course-view/module/pages/ModuleOverviewPage'
import CourseModulesPage from '../features/course-view/pages/CourseModulesPage'
import CoursePeoplePage from '../features/course-view/pages/CoursePeoplePage'
import CoursePlaceholderPage from '../features/course-view/pages/CoursePlaceholderPage'
import ProfilePage from '../features/profile/pages/ProfilePage'
import { paths } from '../shared/paths'

export default function AppRoutes() {
  return (
    <Routes>
      {/* Logging in happens from the landing page, which every page shows when needed. */}
      <Route path={paths.login} element={<Navigate to={paths.home} replace />} />

      {/* Logged in: topbar, sidebar and page content. Otherwise: the landing page. */}
      <Route element={<AuthGate />}>
        <Route path={paths.home} element={<DashboardPage />} />
        <Route path={paths.courses} element={<CoursesPage />} />
        {/* One course (see paths.course*): its pages with the course menu, or one module
            with the module menu. */}
        <Route path={paths.courseRoute} element={<CourseLayout />}>
          <Route element={<CourseMenuLayout />}>
            <Route index element={<CourseHomePage />} />
            <Route path="modules" element={<CourseModulesPage />} />
            <Route path="assignments" element={<CourseContentTypePage type="ASSIGNMENT" title="Assignments" />} />
            <Route path="people" element={<CoursePeoplePage />} />
            <Route path="groups" element={<CourseGroupsPage />} />
            <Route path="quizzes" element={<CourseContentTypePage type="QUIZ" title="Quizzes" />} />
            <Route path="grades" element={<CourseGradesPage />} />
            <Route path="files" element={<CoursePlaceholderPage title="Files" />} />
            <Route path="code-exercises" element={<CourseContentTypePage type="CODING_EXERCISE" title="Code Exercises" />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>

          <Route path="modules/:moduleId" element={<ModuleLayout />}>
            <Route index element={<ModuleOverviewPage />} />
            <Route path="files" element={<CoursePlaceholderPage title="Files" />} />
            {CONTENT_SECTIONS.map(({ type, section, label }) => (
              <Route key={section} path={section} element={<ModuleContentTypePage type={type} title={label} />} />
            ))}
            <Route path="content/:contentId" element={<ModuleContentPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Route>
        <Route path={paths.profile} element={<ProfilePage />} />

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
