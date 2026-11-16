import type { GroupId } from '@shared/types'

/**
 * Thrown by switchGroup when the student left their old group but could not join the
 * new one. `backInOldGroup`: they were put back in the old group.
 */
export class SwitchGroupError extends Error {
  readonly backInOldGroup: boolean

  constructor(cause: unknown, backInOldGroup: boolean) {
    super('Could not join the new group', { cause })
    this.backInOldGroup = backInOldGroup
  }
}

type SwitchGroupSteps<T> = {
  from: GroupId
  to: GroupId
  /** Takes the student out of a group. */
  leave: (groupId: GroupId) => Promise<unknown>
  /** Puts the student in a group. */
  join: (groupId: GroupId) => Promise<T>
}

/**
 * Moves a student from one group to another (one group at a time). The backend has no
 * single call for this, so it leaves `from` and then joins `to`:
 * - leaving fails: that error is thrown as it is, and nothing has changed;
 * - joining fails: it tries to rejoin `from`, and throws SwitchGroupError saying whether
 *   that worked.
 */
export async function switchGroup<T>({ from, to, leave, join }: SwitchGroupSteps<T>): Promise<T> {
  await leave(from)
  try {
    return await join(to)
  } catch (error) {
    const backInOldGroup = await join(from).then(
      () => true,
      () => false,
    )
    throw new SwitchGroupError(error, backInOldGroup)
  }
}
