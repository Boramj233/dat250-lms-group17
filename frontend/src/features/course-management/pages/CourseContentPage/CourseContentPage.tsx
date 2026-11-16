import { useState, type DragEvent } from 'react'
import { Link } from 'react-router'
import { paths } from '@shared/paths'
import { moveItem } from '@shared/lib/moveItem'
import SaveBar from '@shared/ui/SaveBar'
import { useCourseContent } from '@domain/content/hooks'
import { useReorderModules } from '@domain/modules/hooks'
import type { CourseModule } from '@domain/modules/types'
import { formatDateTime } from '@shared/lib/dateFormat'
import { useManageCourse } from '../../manageCourseContext'
import AddModuleForm from './AddModuleForm'
import './CourseContentPage.css'

/**
 * The Content tab: the course's modules in their order (rank), which is the order every
 * student sees them in, each with a button to manage it, and "Add a module". The order is
 * changed by dragging a row, or with the ↑ and ↓ buttons (keyboard, touch); it is saved
 * for all modules at once with Save changes.
 */
export default function CourseContentPage() {
  const { course, modules } = useManageCourse()
  // All modules' content at once, for the counts; the same cache as the module pages.
  const content = useCourseContent(
    course.id,
    modules.map(m => m.id),
  )
  const reorder = useReorderModules()
  // The unsaved order as module ids (strings); null: the saved order.
  const [order, setOrder] = useState<string[] | null>(null)
  // The module being dragged.
  const [dragId, setDragId] = useState<string | null>(null)
  const [announcement, setAnnouncement] = useState('')
  const [saved, setSaved] = useState<string | null>(null)

  const savedIds = modules.map(m => String(m.id))
  const shown = (order ?? savedIds)
    .map(id => modules.find(m => String(m.id) === id))
    .filter((m): m is CourseModule => m !== undefined)
  // Modules whose rank is not the saved one.
  const changes = order ? order.filter((id, index) => savedIds[index] !== id).length : 0

  function setNewOrder(next: string[]) {
    setOrder(next.join() === savedIds.join() ? null : next)
    setSaved(null)
    reorder.reset()
  }

  /** ↑ / ↓: one place, and says where it went (for screen readers). */
  function move(module: CourseModule, by: -1 | 1) {
    const ids = shown.map(m => String(m.id))
    const from = ids.indexOf(String(module.id))
    const to = from + by
    if (to < 0 || to >= ids.length) return
    setNewOrder(moveItem(ids, from, to))
    setAnnouncement(`${module.title} moved to place ${to + 1} of ${ids.length}. Not saved yet.`)
  }

  // Dragging: the row moves along while it is dragged over the others.
  function dragStart(event: DragEvent, id: string) {
    setDragId(id)
    event.dataTransfer.effectAllowed = 'move'
    // Firefox only drags when there is data.
    event.dataTransfer.setData('text/plain', id)
  }

  function dragOver(event: DragEvent, overId: string) {
    if (dragId === null) return
    event.preventDefault()
    event.dataTransfer.dropEffect = 'move'
    if (overId === dragId) return
    const ids = shown.map(m => String(m.id))
    setNewOrder(moveItem(ids, ids.indexOf(dragId), ids.indexOf(overId)))
  }

  function dragEnd() {
    if (dragId !== null) {
      const ids = shown.map(m => String(m.id))
      const module = shown.find(m => String(m.id) === dragId)
      if (module)
        setAnnouncement(`${module.title} moved to place ${ids.indexOf(dragId) + 1} of ${ids.length}. Not saved yet.`)
    }
    setDragId(null)
  }

  function save() {
    if (!order) return
    reorder.mutate(
      { courseId: course.id, moduleIds: order },
      {
        onSuccess: () => {
          setOrder(null)
          setSaved('The new order is saved. Students see the modules in this order.')
        },
      },
    )
  }

  /**
   * When students can open the module, e.g. "From 21 Nov 08:00 · After module 1 and 2 ·
   * For all from 5 Dec 08:00", or "Open from the start". Module numbers are the saved ones.
   */
  function availabilityText(module: CourseModule): string {
    const numbers = modules
      .map((m, index) => ({ id: String(m.id), number: index + 1 }))
      .filter(m => (module.prerequisiteModuleIds ?? []).some(id => String(id) === m.id))
      .map(m => m.number)
    const parts: string[] = []
    if (module.availableFrom) parts.push(`From ${formatDateTime(new Date(module.availableFrom))}`)
    if (numbers.length > 0) {
      const list = numbers.length === 1 ? `${numbers[0]}` : `${numbers.slice(0, -1).join(', ')} and ${numbers.at(-1)}`
      parts.push(`After module ${list}`)
    }
    if (module.openToAllFrom) parts.push(`For all from ${formatDateTime(new Date(module.openToAllFrom))}`)
    return parts.length > 0 ? parts.join(' · ') : 'Open from the start'
  }

  return (
    <>
      {modules.length === 0 ? (
        <p>This course has no modules yet. Add the first one below.</p>
      ) : (
        <>
          <p className="form-hint manage-module-help">
            The order here is the order students see the modules in. Drag a module by its handle, or use ↑ and ↓, then
            save.
          </p>
          <ol className="manage-module-list">
            {shown.map((module, index) => {
              const id = String(module.id)
              const items = content.byModule.get(id)
              const savedIndex = savedIds.indexOf(id)
              const moved = order !== null && savedIndex !== index
              const rowClass = ['manage-module-row', moved ? 'is-moved' : '', dragId === id ? 'is-dragging' : '']
                .filter(Boolean)
                .join(' ')
              return (
                <li
                  key={module.id}
                  className={rowClass}
                  draggable={!reorder.isPending}
                  onDragStart={event => dragStart(event, id)}
                  onDragOver={event => dragOver(event, id)}
                  onDrop={event => event.preventDefault()}
                  onDragEnd={dragEnd}
                >
                  <span className="manage-module-handle" aria-hidden="true" title="Drag to change the order">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" focusable="false">
                      <circle cx="9" cy="6" r="1.6" />
                      <circle cx="15" cy="6" r="1.6" />
                      <circle cx="9" cy="12" r="1.6" />
                      <circle cx="15" cy="12" r="1.6" />
                      <circle cx="9" cy="18" r="1.6" />
                      <circle cx="15" cy="18" r="1.6" />
                    </svg>
                  </span>
                  <span className="manage-module-rank" aria-hidden="true">
                    {index + 1}
                  </span>
                  <div className="manage-module-name">
                    <span className="manage-module-number">
                      Module {index + 1}
                      {moved && <span className="tag tag-strong manage-module-moved">Was {savedIndex + 1}</span>}
                    </span>
                    <span className="manage-module-title">{module.title}</span>
                    <span className="manage-module-count">
                      {items === undefined ? '…' : `${items.length} ${items.length === 1 ? 'item' : 'items'}`}
                      {' · '}
                      {availabilityText(module)}
                    </span>
                  </div>
                  <div className="manage-module-actions">
                    <button
                      type="button"
                      className="button-secondary manage-module-move"
                      disabled={index === 0 || reorder.isPending}
                      onClick={() => move(module, -1)}
                      aria-label={`Move ${module.title} up`}
                      title="Move up"
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      className="button-secondary manage-module-move"
                      disabled={index === shown.length - 1 || reorder.isPending}
                      onClick={() => move(module, 1)}
                      aria-label={`Move ${module.title} down`}
                      title="Move down"
                    >
                      ↓
                    </button>
                    <Link className="button-secondary" to={paths.manageModule(course.id, module.id)} draggable={false}>
                      Manage<span className="visually-hidden"> {module.title}</span>
                    </Link>
                  </div>
                </li>
              )
            })}
          </ol>
          <p className="visually-hidden" aria-live="polite">
            {announcement}
          </p>
          <SaveBar
            changes={changes}
            saving={reorder.isPending}
            error={reorder.error ? 'Could not save the new order. Try again.' : null}
            saved={saved}
            saveLabel="Save order"
            onSave={save}
            onCancel={() => {
              setOrder(null)
              setAnnouncement('The order is back to the saved one.')
            }}
          />
        </>
      )}
      <AddModuleForm courseId={course.id} position={modules.length} />
    </>
  )
}
