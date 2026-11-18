import { errorMessage } from '@shared/api/errorMessage'
import { groupName } from '@domain/groups/hooks'
import type { CourseGroup } from '@domain/groups/types'

// Messages for the group actions that failed, from the backend's status code.

export function addError(error: Error, name: string, group: CourseGroup): string {
  return errorMessage(
    error,
    {
      409: `${name} is already in a group in this course. Remove them from it first.`,
      403: `${name} could not be added: only active students of the course can be in a group.`,
      404: `${groupName(group)} no longer exists.`,
    },
    `${name} could not be added to ${groupName(group)}. Try again.`,
  )
}

export function removeError(error: Error, name: string, group: CourseGroup): string {
  return errorMessage(
    error,
    {
      409: `${name} is no longer in ${groupName(group)}.`,
      404: `${groupName(group)} no longer exists.`,
    },
    `${name} could not be removed from ${groupName(group)}. Try again.`,
  )
}

export function deleteError(error: Error, group: CourseGroup): string {
  return errorMessage(
    error,
    {
      409: `${groupName(group)} has submissions or other history and cannot be deleted.`,
    },
    `${groupName(group)} could not be deleted. Try again.`,
  )
}
