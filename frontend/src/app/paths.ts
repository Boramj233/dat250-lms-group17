import { generatePath } from 'react-router'

export const paths = {
  home: '/',
  login: '/login',
  courses: '/courses',
  course: '/courses/:courseId',
} as const

export function coursePath(courseId: string) {
  return generatePath(paths.course, { courseId })
}
