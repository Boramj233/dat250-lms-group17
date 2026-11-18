import { apiClient, apiPath, jsonBody } from '@shared/api/apiClient'
import type { CourseId, GroupId, UserId } from '@shared/types'
import type { CourseGroup } from './types'

/** All groups in a course (backend). */
export function getCourseGroups(courseId: CourseId): Promise<CourseGroup[]> {
  return apiClient<CourseGroup[]>(apiPath`/api/courses/${courseId}/groups`)
}

/** Teachers: a new, empty group. 409 when the number is taken, 400 when it is below 1. */
export function createGroup(courseId: CourseId, groupNumber: number): Promise<CourseGroup> {
  return apiClient<CourseGroup>(apiPath`/api/courses/${courseId}/groups`, jsonBody('POST', { groupNumber }))
}

/** Teachers: removes a group. 409 when it has history (e.g. submissions). */
export function deleteGroup(courseId: CourseId, groupId: GroupId): Promise<void> {
  return apiClient<void>(apiPath`/api/courses/${courseId}/groups/${groupId}`, { method: 'DELETE' })
}

/**
 * Teachers: adds a student of the course to a group. The body is the bare user id. 409
 * when the student is already in a group in this course.
 */
export function addGroupMember(courseId: CourseId, groupId: GroupId, userId: UserId): Promise<CourseGroup> {
  return apiClient<CourseGroup>(
    apiPath`/api/courses/${courseId}/groups/${groupId}/members`,
    jsonBody('POST', Number(userId)),
  )
}

/** Teachers: removes a member from a group. */
export function removeGroupMember(courseId: CourseId, groupId: GroupId, userId: UserId): Promise<void> {
  return apiClient<void>(apiPath`/api/courses/${courseId}/groups/${groupId}/members/${userId}`, { method: 'DELETE' })
}
