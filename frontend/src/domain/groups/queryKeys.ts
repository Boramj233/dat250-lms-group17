import type { CourseId } from '@shared/types'

export const groupKeys = {
  course: (courseId: CourseId) => ['groups', 'course', String(courseId)] as const,
}
