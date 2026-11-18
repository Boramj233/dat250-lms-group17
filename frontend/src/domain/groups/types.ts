import type { GroupId, UserId } from '@shared/types'

// Matches the backend's GroupDTO (GET /api/courses/{courseId}/groups). Members' names
// come from the course's members (GET /api/courses/{courseId}/enrollments).
export interface CourseGroup {
  id: GroupId
  groupNumber: number
  memberIds: UserId[]
}
