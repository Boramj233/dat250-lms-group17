import type { ContentId, CourseId, ModuleId } from '../data/shared/types'

const id = (value: CourseId | ModuleId | ContentId) => encodeURIComponent(String(value))

/** URL part for the course pages that list one content type, e.g. /courses/1/quizzes. */
export type ContentTypeSlug = 'pages' | 'quizzes' | 'assignments' | 'coding-exercises'

export const paths = {
  home: '/',
  login: '/login',
  courses: '/courses',
  grades: '/grades',

  // Route pattern for the course layout; the course pages are nested routes in routes.tsx.
  courseRoute: '/courses/:courseId',
  course: (courseId: CourseId) => `/courses/${id(courseId)}`,
  courseModule: (courseId: CourseId, moduleId: ModuleId) =>
    `/courses/${id(courseId)}/modules/${id(moduleId)}`,
  courseContent: (courseId: CourseId, moduleId: ModuleId, contentId: ContentId) =>
    `/courses/${id(courseId)}/modules/${id(moduleId)}/content/${id(contentId)}`,
  courseModuleContentType: (courseId: CourseId, moduleId: ModuleId, slug: ContentTypeSlug) =>
    `/courses/${id(courseId)}/modules/${id(moduleId)}/${slug}`,
  courseContentType: (courseId: CourseId, slug: ContentTypeSlug) =>
    `/courses/${id(courseId)}/${slug}`,
  courseGroups: (courseId: CourseId) => `/courses/${id(courseId)}/groups`,
  courseGrades: (courseId: CourseId) => `/courses/${id(courseId)}/grades`,
} as const
