import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { formatDateTime, fromDateTimeLocal } from '@shared/lib/dateFormat'
import { paths } from '@shared/paths'
import type { ContentId, ModuleId } from '@shared/types'
import { useCourseContent, useDeleteContent, useMoveContent, useUpdateContent } from '@domain/content/hooks'
import type { ContentSummary } from '@domain/content/types'
import ActionMenu, { type ActionMenuItem } from '@shared/ui/ActionMenu'
import { useConfirm } from '@shared/ui/ConfirmDialog'
import PageHeader from '@shared/ui/PageHeader'
import SaveBar from '@shared/ui/SaveBar'
import { CONTENT_TYPES, contentTypeLabel } from '@domain/content/contentTypes'
import { checkModuleStartAgainstContent, isPublished } from '@domain/content/visibility'
import { useUpdateModule } from '@domain/modules/hooks'
import { checkAvailabilityDates } from '@domain/modules/prerequisites'
import { useManageCourse } from '../../../manageCourseContext'
import { useManageModule } from '../../manageModuleContext'
import { availabilityChanges, savedAvailability, type AvailabilityDraft } from './availabilityDraft'
import ModuleAvailability from './ModuleAvailability'
import './ManageModulePage.css'

/**
 * Changes to the module's content that are not saved yet, by content id (as a string):
 * "required for module completion" switched, moves to another module, and deletions.
 */
type Draft = {
  required: Record<string, boolean>
  moves: Record<string, ModuleId>
  deletes: string[]
}

const EMPTY_DRAFT: Draft = { required: {}, moves: {}, deletes: [] }

/**
 * One module: a button per content type to create new content, and a table of the
 * module's content (visible from, published or unpublished by that date, required for
 * module completion, and a "⋯" menu to edit, move or delete it), and below it when
 * students can open the module. Every change on the page is collected and marked first;
 * one Save changes applies them all (deleting asks once more), Cancel throws them away.
 */
export default function ManageModulePage() {
  const { course, modules } = useManageCourse()
  const { module, number, content } = useManageModule()
  const navigate = useNavigate()
  const moveContent = useMoveContent()
  const deleteContent = useDeleteContent()
  const updateContent = useUpdateContent()
  const updateModule = useUpdateModule()
  // Content counts of all modules, so moved content goes to the end of its new module.
  const allContent = useCourseContent(
    course.id,
    modules.map(m => m.id),
  )
  const [confirm, confirmDialog] = useConfirm()
  const [now] = useState(() => Date.now())
  const [draft, setDraft] = useState<Draft>(EMPTY_DRAFT)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState<string | null>(null)
  // The availability fields; they start from the module, which is new after a save.
  const savedDates = savedAvailability(module)
  const [availability, setAvailability] = useState<AvailabilityDraft>(savedDates)

  const changes =
    Object.keys(draft.required).length +
    Object.keys(draft.moves).length +
    draft.deletes.length +
    availabilityChanges(availability, savedDates)
  const moduleLabel = (id: ModuleId) => {
    const index = modules.findIndex(m => String(m.id) === String(id))
    return index >= 0 ? `Module ${index + 1}: ${modules[index].title}` : 'another module'
  }

  /** Changes the draft, and clears the last save's message. */
  function change(update: (draft: Draft) => Draft) {
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
  }

  function setRequired(item: ContentSummary, required: boolean) {
    change(d => {
      const next = { ...d.required }
      // Back to the saved value: no longer a change.
      if (required === (item.requiredForCompletion ?? false)) delete next[String(item.id)]
      else next[String(item.id)] = required
      return { ...d, required: next }
    })
  }

  function setMove(item: ContentSummary, target: ModuleId | null) {
    change(d => {
      const next = { ...d.moves }
      if (target === null) delete next[String(item.id)]
      else next[String(item.id)] = target
      return { ...d, moves: next }
    })
  }

  function setDeleted(item: ContentSummary, deleted: boolean) {
    const id = String(item.id)
    change(d => ({ ...d, deletes: deleted ? [...d.deletes, id] : d.deletes.filter(other => other !== id) }))
  }

  /** Removes one applied change from the draft, so a failed save can be tried again. */
  function done(kind: keyof Draft, id: ContentId) {
    setDraft(d =>
      kind === 'deletes'
        ? { ...d, deletes: d.deletes.filter(other => other !== String(id)) }
        : { ...d, [kind]: Object.fromEntries(Object.entries(d[kind]).filter(([key]) => key !== String(id))) },
    )
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
      for (const [id, required] of Object.entries(draft.required)) {
        if (draft.deletes.includes(id)) {
          done('required', id)
          continue
        }
        await updateContent.mutateAsync({
          contentId: id,
          moduleId: module.id,
          changes: { requiredForCompletion: required },
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
      for (const id of draft.deletes) {
        await deleteContent.mutateAsync({ contentId: id, moduleId: module.id })
        done('deletes', id)
      }
      setSaved('All changes are saved.')
    } catch {
      setError('Some changes could not be saved. Those still marked are not saved; try again.')
    } finally {
      setSaving(false)
    }
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
          ...(movedTo !== undefined ? [{ label: 'Keep in this module', onSelect: () => setMove(item, null) }] : []),
          ...otherModules
            .filter(({ module: target }) => String(target.id) !== String(movedTo))
            .map(({ module: target, number: targetNumber }) => ({
              label: `${targetNumber}. ${target.title}`,
              onSelect: () => setMove(item, target.id),
            })),
        ],
      },
      deleted
        ? { label: 'Undo delete', onSelect: () => setDeleted(item, false) }
        : { label: 'Delete', danger: true, onSelect: () => setDeleted(item, true) },
    ]
  }

  return (
    <>
      <PageHeader title={module.title} kicker={`Module ${number} of ${modules.length}`} />

      <section aria-labelledby="new-content-title">
        <h2 id="new-content-title" className="section-title">
          Create
        </h2>
        <ul className="new-content-types">
          {CONTENT_TYPES.map(info => (
            <li key={info.type}>
              <Link className="new-content-type" to={paths.manageNewContent(course.id, module.id, info.slug)}>
                <span className="new-content-type-label">+ {info.label}</span>
                <span className="new-content-type-description">{info.description}</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <section aria-labelledby="module-content-title">
        <h2 id="module-content-title" className="section-title">
          Content in this module
        </h2>
        {content.length === 0 ? (
          <p>No content yet. Create the first one above.</p>
        ) : (
          <div className="manage-content-table-wrap">
            <table className="manage-content-table">
              <thead>
                <tr>
                  <th scope="col">Title</th>
                  <th scope="col">Type</th>
                  <th scope="col">Visible from</th>
                  <th scope="col">Status</th>
                  <th scope="col" className="is-center">
                    Required for module completion
                  </th>
                  <th scope="col">
                    <span className="visually-hidden">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {content.map(item => {
                  const id = String(item.id)
                  const deleted = draft.deletes.includes(id)
                  const movedTo = draft.moves[id]
                  const requiredChanged = id in draft.required
                  const required = requiredChanged ? draft.required[id] : (item.requiredForCompletion ?? false)
                  const rowClass = [
                    item.active === false ? 'is-inactive' : '',
                    deleted ? 'is-deleted' : movedTo !== undefined || requiredChanged ? 'is-changed' : '',
                  ]
                    .filter(Boolean)
                    .join(' ')
                  return (
                    <tr key={item.id} className={rowClass || undefined}>
                      <th scope="row">
                        <Link className="manage-content-title" to={paths.manageContent(course.id, module.id, item.id)}>
                          {item.title}
                        </Link>
                        {deleted && <span className="tag tag-strong tag-danger manage-change">Will be deleted</span>}
                        {!deleted && movedTo !== undefined && (
                          <span className="tag tag-strong manage-change">Moves to {moduleLabel(movedTo)}</span>
                        )}
                      </th>
                      <td className="manage-content-type">{contentTypeLabel(item.type)}</td>
                      <td className="manage-content-type">
                        {item.visibleFrom ? (
                          formatDateTime(new Date(item.visibleFrom))
                        ) : module.availableFrom ? (
                          <>With the module · {formatDateTime(new Date(module.availableFrom))}</>
                        ) : (
                          'With the module'
                        )}
                      </td>
                      <td>
                        <PublishStatus published={isPublished(item, module, now)} />
                      </td>
                      <td className={`is-center${requiredChanged ? ' is-changed-cell' : ''}`}>
                        <input
                          type="checkbox"
                          className="manage-content-check"
                          aria-label={`${item.title} is required for module completion${requiredChanged ? ' (not saved)' : ''}`}
                          checked={required}
                          disabled={saving || deleted}
                          onChange={event => setRequired(item, event.target.checked)}
                        />
                      </td>
                      <td className="is-end">
                        <ActionMenu label={`Actions for ${item.title}`} items={actionsFor(item)} />
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
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

/** "Published" once the visible date has passed, "Unpublished" before. Only shown, not changed here. */
function PublishStatus({ published }: { published: boolean }) {
  return (
    <span className={`manage-publish-label ${published ? 'is-published' : 'is-unpublished'}`}>
      {published ? 'Published' : 'Unpublished'}
    </span>
  )
}
