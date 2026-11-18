import { Navigate, Route, Routes } from 'react-router'
import AuthGate from './auth/AuthGate'
import RequireAccess from './auth/RequireAccess'
import ModuleListPage from '@features/course-management/pages/ModuleListPage'
import CourseDetailsPage from '@features/course-management/pages/CourseDetailsPage'
import CourseMembersPage from '@features/course-management/pages/CourseMembersPage'
import CourseTabsLayout from '@features/course-management/pages/CourseTabsLayout'
import ManageCourseLayout from '@features/course-management/pages/ManageCourseLayout'
import ManageCoursesPage from '@features/course-management/pages/ManageCoursesPage'
import ContentDetailsPage from '@features/course-management/module/pages/ContentDetailsPage'
import ManageModuleLayout from '@features/course-management/module/pages/ManageModuleLayout'
import ManageModulePage from '@features/course-management/module/pages/ManageModulePage'
import NewContentPage from '@features/course-management/module/pages/NewContentPage'
import NewCoursePage from '@features/course-management/pages/NewCoursePage'
import NotFoundPage from '@shared/ui/NotFoundPage'
import DashboardPage from '@features/dashboard/pages/DashboardPage'
import CoursesPage from '@features/my-courses/pages/CoursesPage'
import CourseSectionItemPage from '@features/course-view/pages/CourseSectionItemPage'
import RequireCourseTeacher from '@features/course-view/pages/RequireCourseTeacher'
import CourseSubmissionsPage from '@features/course-view/submissions/pages/CourseSubmissionsPage'
import ContentSubmissionsPage from '@features/course-view/submissions/pages/ContentSubmissionsPage'
import SubmissionGradingPage from '@features/course-view/submissions/pages/SubmissionGradingPage'
import CourseContentTypePage from '@features/course-view/pages/CourseContentTypePage'
import CourseGradesPage from '@features/course-view/pages/CourseGradesPage'
import CourseGroupsPage from '@features/course-view/pages/CourseGroupsPage'
import CourseHomePage from '@features/course-view/pages/CourseHomePage'
import CourseLayout from '@features/course-view/pages/CourseLayout'
import CourseMenuLayout from '@features/course-view/pages/CourseMenuLayout'
import { CONTENT_SECTIONS } from '@features/course-view/contentSections'
import ModuleContentPage from '@features/course-view/module/pages/ModuleContentPage'
import ModuleContentTypePage from '@features/course-view/module/pages/ModuleContentTypePage'
import ModuleLayout from '@features/course-view/module/pages/ModuleLayout'
import ModuleOverviewPage from '@features/course-view/module/pages/ModuleOverviewPage'
import CourseModulesPage from '@features/course-view/pages/CourseModulesPage'
import CoursePeoplePage from '@features/course-view/pages/CoursePeoplePage'
import CourseFilesPage from '@features/course-view/pages/CourseFilesPage'
import ProfilePage from '@features/profile/pages/ProfilePage'
import AdminUsersPage from '@features/admin/pages/AdminUsersPage'
import { paths } from '@shared/paths'

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
            <Route
              path="assignments"
              element={<CourseContentTypePage type="ASSIGNMENT" title="Assignments" section="assignments" />}
            />
            <Route
              path="assignments/:contentId"
              element={<CourseSectionItemPage section="assignments" label="Assignments" />}
            />
            <Route path="people" element={<CoursePeoplePage />} />
            <Route path="groups" element={<CourseGroupsPage />} />
            <Route path="quizzes" element={<CourseContentTypePage type="QUIZ" title="Quizzes" section="quizzes" />} />
            <Route path="quizzes/:contentId" element={<CourseSectionItemPage section="quizzes" label="Quizzes" />} />
            <Route path="grades" element={<CourseGradesPage />} />
            <Route path="grades/:contentId" element={<CourseSectionItemPage section="grades" label="Grades" />} />
            {/* Teachers: what students hand in, per item, and grading one submission. */}
            <Route element={<RequireCourseTeacher />}>
              <Route path="submissions" element={<CourseSubmissionsPage />} />
              <Route path="submissions/:contentId" element={<ContentSubmissionsPage />} />
              <Route path="submissions/:contentId/:submissionId" element={<SubmissionGradingPage />} />
            </Route>
            <Route path="files" element={<CourseFilesPage />} />
            <Route
              path="code-exercises"
              element={<CourseContentTypePage type="CODING_EXERCISE" title="Code Exercises" section="code-exercises" />}
            />
            <Route
              path="code-exercises/:contentId"
              element={<CourseSectionItemPage section="code-exercises" label="Code Exercises" />}
            />
            <Route path="*" element={<NotFoundPage />} />
          </Route>

          <Route path="modules/:moduleId" element={<ModuleLayout />}>
            <Route index element={<ModuleOverviewPage />} />
            <Route path="files" element={<CourseFilesPage />} />
            {CONTENT_SECTIONS.flatMap(({ type, section, label }) => [
              <Route key={section} path={section} element={<ModuleContentTypePage type={type} title={label} />} />,
              <Route key={`${section}-item`} path={`${section}/:contentId`} element={<ModuleContentPage />} />,
            ])}
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Route>
        <Route path={paths.profile} element={<ProfilePage />} />

        {/* Only administrators; others get the not-found page. */}
        <Route element={<RequireAccess level="ADMIN" fallback={<NotFoundPage />} />}>
          <Route path={paths.adminUsers} element={<AdminUsersPage />} />
        </Route>

        {/* Only course managers (and admins); others get the not-found page. */}
        <Route element={<RequireAccess level="COURSE_MANAGER" fallback={<NotFoundPage />} />}>
          <Route path={paths.manageCourses} element={<ManageCoursesPage />} />
          <Route path={paths.newCourse} element={<NewCoursePage />} />

          {/* One course (see paths.manage*): its tabs, then its modules and their content. */}
          <Route path={paths.manageCourseRoute} element={<ManageCourseLayout />}>
            <Route element={<CourseTabsLayout />}>
              <Route index element={<ModuleListPage />} />
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
