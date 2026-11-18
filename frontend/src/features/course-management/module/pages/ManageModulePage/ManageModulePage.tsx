import { useState } from 'react'
import { useNavigate } from 'react-router'
import { ApiError } from '@shared/api/apiClient'
import { fromDateTimeLocal } from '@shared/lib/dateFormat'
import { moveItem } from '@shared/lib/moveItem'
import { paths } from '@shared/paths'
import type { ContentId, ModuleId } from '@shared/types'
import {
  useCourseContent,
  useDeleteContent,
  useMoveContent,
  useReorderContent,
  useUpdateCompletionSettings,
} from '@domain/content/hooks'
import type { ContentSummary } from '@domain/content/types'
import type { ActionMenuItem } from '@shared/ui/ActionMenu'
import { useConfirm } from '@shared/ui/ConfirmDialog'
import PageHeader from '@shared/ui/PageHeader'
import SaveBar from '@shared/ui/SaveBar'
import { checkModuleStartAgainstContent } from '@domain/content/visibility'
import { useDeleteModule, useUpdateModule } from '@domain/modules/hooks'
import { checkAvailabilityDates } from '@domain/modules/prerequisites'
import { useManageCourse } from '../../../manageCourseContext'
import { useManageModule } from '../../manageModuleContext'
import { availabilityChanges, savedAvailability, type AvailabilityDraft } from './availabilityDraft'
import {
  contentChanges,
  EMPTY_DRAFT,
  withDeleted,
  withMove,
  withoutSaved,
  withRequired,
  type ContentDraft,
} from './contentDraft'
import ModuleAvailability from './ModuleAvailability'
import ModuleContentTable from './ModuleContentTable'
import NewContentLinks from './NewContentLinks'
import './ManageModulePage.css'

/**
 * One module: a button per content type to create new content, and a table of the
 * module's content (visible from, published or unpublished by that date, required for
 * module completion, ↑ / ↓ for its place in the module, and a "⋯" menu to edit, move or
 * delete it), and below it when students can open the module. Every change on the page is collected and marked first;
 * one Save changes applies them all (deleting asks once more), Cancel throws them away.
 */
export default function ManageModulePage() {
  const { course, modules } = useManageCourse()
  const { module, number, content } = useManageModule()
  const navigate = useNavigate()
  const moveContent = useMoveContent()
  const deleteContent = useDeleteContent()
  const updateCompletionSettings = useUpdateCompletionSettings()
  const updateModule = useUpdateModule()
  const reorderContent = useReorderContent()
  const deleteModule = useDeleteModule()
  // Content counts of all modules, so moved content goes to the end of its new module.
  const allContent = useCourseContent(
    course.id,
    modules.map(m => m.id),
  )
  const [confirm, confirmDialog] = useConfirm()
  const [now] = useState(() => Date.now())
  const [draft, setDraft] = useState<ContentDraft>(EMPTY_DRAFT)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState<string | null>(null)
  // The availability fields; they start from the module, which is new after a save.
  const savedDates = savedAvailability(module)
  const [availability, setAvailability] = useState<AvailabilityDraft>(savedDates)
  // A new order of the content (ids as strings) that is not saved yet; null: the saved order.
  const [order, setOrder] = useState<string[] | null>(null)

  const savedIds = content.map(item => String(item.id))
  // The unsaved order, without items that are gone and with new ones at the end.
  const shownIds = order
    ? [...order.filter(id => savedIds.includes(id)), ...savedIds.filter(id => !order.includes(id))]
    : savedIds
  const shown = shownIds.map(id => content.find(item => String(item.id) === id)!)
  const orderChanges = order ? shownIds.filter((id, index) => savedIds[index] !== id).length : 0

  const changes = contentChanges(draft) + availabilityChanges(availability, savedDates) + orderChanges
  const moduleLabel = (id: ModuleId) => {
    const index = modules.findIndex(m => String(m.id) === String(id))
    return index >= 0 ? `Module ${index + 1}: ${modules[index].title}` : 'another module'
  }

  /** Changes the draft, and clears the last save's message. */
  function change(update: (draft: ContentDraft) => ContentDraft) {
    setDraft(update)
    setSaved(null)
    setError(null)
  }

  function changeAvailability(next: AvailabilityDraft) {
    setAvailability(next)
    setSaved(null)
    setError(null)
  }

  function cancel() {
    change(() => EMPTY_DRAFT)
    setAvailability(savedDates)
    setOrder(null)
  }

  /** ↑ / ↓: moves the item one place in the module (saved with the other changes). */
  function moveRow(item: ContentSummary, by: -1 | 1) {
    const from = shownIds.indexOf(String(item.id))
    const to = from + by
    if (from < 0 || to < 0 || to >= shownIds.length) return
    const next = moveItem(shownIds, from, to)
    setOrder(next.join() === savedIds.join() ? null : next)
    setSaved(null)
    setError(null)
  }

  /** Removes one applied change from the draft, so a failed save can be tried again. */
  function done(kind: keyof ContentDraft, id: ContentId) {
    setDraft(d => withoutSaved(d, kind, id))
  }

  async function save() {
    // Check the dates before anything is saved.
    const availabilityChanged = availabilityChanges(availability, savedDates) > 0
    const moduleChanges = {
      availableFrom: fromDateTimeLocal(availability.availableFrom),
      openToAllFrom: fromDateTimeLocal(availability.openToAllFrom),
      // The ids as the modules have them.
      prerequisiteModuleIds: modules.filter(m => availability.prerequisites.includes(String(m.id))).map(m => m.id),
    }
    if (availabilityChanged) {
      const dateProblem =
        checkAvailabilityDates(moduleChanges.availableFrom, moduleChanges.openToAllFrom) ??
        checkModuleStartAgainstContent(moduleChanges.availableFrom, content)
      if (dateProblem) {
        setError(dateProblem)
        return
      }
    }
    const deleting = content.filter(item => draft.deletes.includes(String(item.id)))
    if (deleting.length > 0) {
      const confirmed = await confirm({
        title: `Delete ${deleting.length === 1 ? `"${deleting[0].title}"` : `${deleting.length} items`}?`,
        message: (
          <>
            <p>Saving also deletes {deleting.length === 1 ? 'this item' : 'these items'}. This cannot be undone.</p>
            {deleting.length > 1 && (
              <ul>
                {deleting.map(item => (
                  <li key={item.id}>{item.title}</li>
                ))}
              </ul>
            )}
          </>
        ),
        confirmLabel: deleting.length === 1 ? 'Delete and save' : `Delete ${deleting.length} and save`,
        danger: true,
      })
      if (!confirmed) return
    }
    setSaving(true)
    setError(null)
    try {
      if (availabilityChanged) {
        await updateModule.mutateAsync({ courseId: course.id, moduleId: module.id, changes: moduleChanges })
      }
      // The order first, while every item is still in the module (the backend needs each once).
      if (orderChanges > 0) {
        await reorderContent.mutateAsync({ courseId: course.id, moduleId: module.id, contentIds: shownIds })
        setOrder(null)
      }
      for (const [id, required] of Object.entries(draft.required)) {
        if (draft.deletes.includes(id)) {
          done('required', id)
          continue
        }
        // A quiz's passing percentage is left out, so it is kept.
        await updateCompletionSettings.mutateAsync({
          contentId: id,
          moduleId: module.id,
          requiredForModuleCompletion: required,
        })
        done('required', id)
      }
      // Moved items go to the end of their new module, in the order they are moved.
      const added = new Map<string, number>()
      for (const [id, target] of Object.entries(draft.moves)) {
        if (draft.deletes.includes(id)) {
          done('moves', id)
          continue
        }
        const already = added.get(String(target)) ?? 0
        await moveContent.mutateAsync({
          contentId: id,
          sourceModuleId: module.id,
          targetModuleId: target,
          position: (allContent.byModule.get(String(target))?.length ?? 0) + already,
        })
        added.set(String(target), already + 1)
        done('moves', id)
      }
      // Content students have worked on (submissions, attempts, pages marked as done) is
      // never deleted: the backend answers 409 for it.
      const kept: string[] = []
      for (const id of draft.deletes) {
        try {
          await deleteContent.mutateAsync({ contentId: id, moduleId: module.id })
        } catch (deleteError) {
          if (!(deleteError instanceof ApiError && deleteError.status === 409)) throw deleteError
          kept.push(content.find(item => String(item.id) === id)?.title ?? 'An item')
        }
        done('deletes', id)
      }
      if (kept.length > 0) {
        setError(
          `${kept.map(title => `“${title}”`).join(', ')} could not be deleted, since students have already ` +
            'worked on it. Hide it with “Visible from” instead.',
        )
        return
      }
      setSaved('All changes are saved.')
    } catch {
      setError('Some changes could not be saved. Those still marked are not saved; try again.')
    } finally {
      setSaving(false)
    }
  }

  /**
   * Deletes the whole module (DELETE /modules/{id}). Only an empty module: the backend
   * does not delete a module's content yet, and content students have worked on is kept.
   */
  async function removeModule() {
    const confirmed = await confirm({
      title: `Delete "${module.title}"?`,
      message: (
        <p>The module is removed from the course, and other modules no longer require it. This cannot be undone.</p>
      ),
      confirmLabel: 'Delete module',
      danger: true,
    })
    if (!confirmed) return
    setError(null)
    deleteModule.mutate(
      { courseId: course.id, moduleId: module.id },
      { onSuccess: () => navigate(paths.manageCourse(course.id)) },
    )
  }

  function actionsFor(item: ContentSummary): ActionMenuItem[] {
    const id = String(item.id)
    const movedTo = draft.moves[id]
    const deleted = draft.deletes.includes(id)
    const otherModules = modules
      .map((m, index) => ({ module: m, number: index + 1 }))
      .filter(({ module: m }) => m.id !== module.id)
    return [
      {
        label: 'Edit',
        onSelect: async () => {
          // Editing opens another page, so unsaved changes here would be lost.
          if (
            changes > 0 &&
            !(await confirm({
              title: 'Discard unsaved changes?',
              message: 'You have changes in this module that are not saved. If you open the item, they are lost.',
              confirmLabel: 'Discard and edit',
              cancelLabel: 'Stay on this page',
              danger: true,
            }))
          )
            return
          navigate(paths.manageContent(course.id, module.id, item.id))
        },
      },
      {
        label: 'Move to',
        disabled: otherModules.length === 0 || deleted,
        items: [
          ...(movedTo !== undefined
            ? [{ label: 'Keep in this module', onSelect: () => change(d => withMove(d, item, null)) }]
            : []),
          ...otherModules
            .filter(({ module: target }) => String(target.id) !== String(movedTo))
            .map(({ module: target, number: targetNumber }) => ({
              label: `${targetNumber}. ${target.title}`,
              onSelect: () => change(d => withMove(d, item, target.id)),
            })),
        ],
      },
      deleted
        ? { label: 'Undo delete', onSelect: () => change(d => withDeleted(d, item, false)) }
        : { label: 'Delete', danger: true, onSelect: () => change(d => withDeleted(d, item, true)) },
    ]
  }

  return (
    <>
      <PageHeader title={module.title} kicker={`Module ${number} of ${modules.length}`} />

      <NewContentLinks courseId={course.id} moduleId={module.id} />

      <section aria-labelledby="module-content-title">
        <h2 id="module-content-title" className="section-title">
          Content in this module
        </h2>
        {content.length === 0 ? (
          <p>No content yet. Create the first one above.</p>
        ) : (
          <ModuleContentTable
            courseId={course.id}
            module={module}
            content={shown}
            savedIndexOf={item => savedIds.indexOf(String(item.id))}
            onMove={moveRow}
            draft={draft}
            now={now}
            saving={saving}
            moduleLabel={moduleLabel}
            onRequiredChange={(item, required) => change(d => withRequired(d, item, required))}
            actionsFor={actionsFor}
          />
        )}
      </section>

      <ModuleAvailability
        module={module}
        modules={modules}
        value={availability}
        saved={savedDates}
        disabled={saving}
        onChange={changeAvailability}
      />

      <section aria-labelledby="delete-module-title">
        <h2 id="delete-module-title" className="section-title">
          Delete module
        </h2>
        {content.length > 0 ? (
          <p className="form-hint">Only an empty module can be deleted. Delete or move its content first.</p>
        ) : (
          <>
            {deleteModule.error && <p role="alert">Could not delete the module. Try again.</p>}
            <button
              type="button"
              className="button-danger"
              disabled={saving || deleteModule.isPending}
              onClick={() => void removeModule()}
            >
              {deleteModule.isPending ? 'Deleting…' : 'Delete module'}
            </button>
          </>
        )}
      </section>

      {/* One bar for every change on the page. */}
      <SaveBar
        changes={changes}
        saving={saving}
        error={error}
        saved={saved}
        onSave={() => void save()}
        onCancel={cancel}
      />
      {confirmDialog}
    </>
  )
}
