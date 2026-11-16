import { errorMessage } from '@shared/api/errorMessage'
import { groupName, SwitchGroupError } from '@domain/groups/hooks'
import type { CourseGroup } from '@domain/groups/types'

// Messages for the group actions that failed, from the backend's status code.

export function joinError(error: Error): string {
  return errorMessage(
    error,
    {
      409: 'You are already in a group in this course. Leave it before you join another.',
      403: 'You are not allowed to join this group.',
      404: 'The group no longer exists.',
    },
    'You could not join the group. Try again.',
  )
}

export function switchError(error: Error, from: CourseGroup, to: CourseGroup): string {
  if (error instanceof SwitchGroupError) {
    const why = joinError(error.cause instanceof Error ? error.cause : error)
    return error.backInOldGroup
      ? `${why} You are still in ${groupName(from)}.`
      : `${why} You are no longer in ${groupName(from)} and not in ${groupName(to)}; join a group again.`
  }
  // Leaving the old group failed: nothing changed.
  return errorMessage(
    error,
    {
      409: `You were no longer in ${groupName(from)}, so nothing was changed. Try joining ${groupName(to)} again.`,
    },
    `${leaveError(error)} You are still in ${groupName(from)}.`,
  )
}

export function leaveError(error: Error): string {
  return errorMessage(
    error,
    {
      409: 'You are no longer in this group.',
      403: 'You are not allowed to leave this group.',
      404: 'The group no longer exists.',
    },
    'You could not leave the group. Try again.',
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
