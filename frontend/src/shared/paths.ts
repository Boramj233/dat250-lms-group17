import type { ContentId, CourseId, ModuleId } from './types'

const id = (value: CourseId | ModuleId | ContentId) => encodeURIComponent(String(value))

// URL patterns for the pages; register the pages in routes.tsx.
export const paths = {
  home: '/',
  login: '/login',

  // Course managers: create courses and manage the courses they teach.
  manageCourses: '/manage-courses',
  newCourse: '/manage-courses/new',
  // Route pattern for one course's management pages; they are nested routes in routes.tsx.
  manageCourseRoute: '/manage-courses/:courseId',
  manageCourse: (courseId: CourseId) => `/manage-courses/${id(courseId)}`,
  manageCourseMembers: (courseId: CourseId) => `/manage-courses/${id(courseId)}/members`,
  manageCourseDetails: (courseId: CourseId) => `/manage-courses/${id(courseId)}/details`,
  manageModule: (courseId: CourseId, moduleId: ModuleId) =>
    `/manage-courses/${id(courseId)}/modules/${id(moduleId)}`,
  /** `typeSlug` from CONTENT_TYPES in ui/manage/contentTypes.ts, e.g. "quiz". */
  manageNewContent: (courseId: CourseId, moduleId: ModuleId, typeSlug: string) =>
    `/manage-courses/${id(courseId)}/modules/${id(moduleId)}/new/${typeSlug}`,
  manageContent: (courseId: CourseId, moduleId: ModuleId, contentId: ContentId) =>
    `/manage-courses/${id(courseId)}/modules/${id(moduleId)}/content/${id(contentId)}`,} as const
