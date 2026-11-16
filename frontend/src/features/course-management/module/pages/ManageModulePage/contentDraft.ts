import type { ContentId, ModuleId } from '@shared/types'
import type { ContentSummary } from '@domain/content/types'

/**
 * Changes to the module's content that are not saved yet, by content id (as a string):
 * "required for module completion" switched, moves to another module, and deletions.
 */
export type ContentDraft = {
  required: Record<string, boolean>
  moves: Record<string, ModuleId>
  deletes: string[]
}

export const EMPTY_DRAFT: ContentDraft = { required: {}, moves: {}, deletes: [] }

/** How many changes the draft holds. */
export function contentChanges(draft: ContentDraft): number {
  return Object.keys(draft.required).length + Object.keys(draft.moves).length + draft.deletes.length
}

/** Switches "required"; switching back to the saved value is no longer a change. */
export function withRequired(draft: ContentDraft, item: ContentSummary, required: boolean): ContentDraft {
  const next = { ...draft.required }
  if (required === (item.requiredForCompletion ?? false)) delete next[String(item.id)]
  else next[String(item.id)] = required
  return { ...draft, required: next }
}

/** Moves the item to another module, or keeps it here (`null`). */
export function withMove(draft: ContentDraft, item: ContentSummary, target: ModuleId | null): ContentDraft {
  const next = { ...draft.moves }
  if (target === null) delete next[String(item.id)]
  else next[String(item.id)] = target
  return { ...draft, moves: next }
}

/** Marks the item for deletion, or undoes that. */
export function withDeleted(draft: ContentDraft, item: ContentSummary, deleted: boolean): ContentDraft {
  const id = String(item.id)
  return { ...draft, deletes: deleted ? [...draft.deletes, id] : draft.deletes.filter(other => other !== id) }
}

/** The draft without one change that has been saved, so a failed save can be tried again. */
export function withoutSaved(draft: ContentDraft, kind: keyof ContentDraft, id: ContentId): ContentDraft {
  return kind === 'deletes'
    ? { ...draft, deletes: draft.deletes.filter(other => other !== String(id)) }
    : { ...draft, [kind]: Object.fromEntries(Object.entries(draft[kind]).filter(([key]) => key !== String(id))) }
}

/** What the draft does to one item, for showing it in its row. */
export function itemDraft(draft: ContentDraft, item: ContentSummary) {
  const id = String(item.id)
  const requiredChanged = id in draft.required
  return {
    deleted: draft.deletes.includes(id),
    movedTo: draft.moves[id] as ModuleId | undefined,
    requiredChanged,
    required: requiredChanged ? draft.required[id] : (item.requiredForCompletion ?? false),
  }
}
