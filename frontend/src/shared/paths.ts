import type { ContentId, CourseId, ModuleId } from './types'

const id = (value: CourseId | ModuleId | ContentId) => encodeURIComponent(String(value))

/** The sections of a course's menu after Home, as URL parts: /courses/1/modules. */
export type CourseSection =
  | 'modules' | 'assignments' | 'people' | 'groups' | 'quizzes' | 'grades' | 'files' | 'code-exercises'

/** The sections of a module's menu: /courses/1/modules/2/quizzes. */
export type ModuleSection = 'files' | 'pages' | 'assignments' | 'quizzes' | 'code-exercises'

// URL patterns for the pages; register the pages in routes.tsx.
export const paths = {
  home: '/',
  login: '/login',
  courses: '/courses',
  // One course as its members see it (students and teachers): Home, then the sections
  // in its course menu. The sections are nested routes in routes.tsx.
  courseRoute: '/courses/:courseId',
  course: (courseId: CourseId) => `/courses/${id(courseId)}`,
  courseSection: (courseId: CourseId, section: CourseSection) => `/courses/${id(courseId)}/${section}`,
  // One module in a course, with the module menu: its overview, its sections and its content.
  courseModule: (courseId: CourseId, moduleId: ModuleId) =>
    `/courses/${id(courseId)}/modules/${id(moduleId)}`,
  courseModuleSection: (courseId: CourseId, moduleId: ModuleId, section: ModuleSection) =>
    `/courses/${id(courseId)}/modules/${id(moduleId)}/${section}`,
  courseContent: (courseId: CourseId, moduleId: ModuleId, contentId: ContentId) =>
    `/courses/${id(courseId)}/modules/${id(moduleId)}/content/${id(contentId)}`,
  profile: '/profile',

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
