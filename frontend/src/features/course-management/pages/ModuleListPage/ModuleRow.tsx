import type { DragEvent } from 'react'
import { Link } from 'react-router'
import { DragHandleIcon } from '@shared/ui/icons'
import type { CourseModule } from '@domain/modules/types'

type ModuleRowProps = {
  module: CourseModule
  /** Its place in the shown (maybe unsaved) order, from 0. */
  index: number
  /** Its place in the saved order, from 0. */
  savedIndex: number
  /** Not in its saved place: marked, with "Was 3". */
  moved: boolean
  dragging: boolean
  /** Items in the module; undefined while loading. */
  itemCount: number | undefined
  /** When students can open it, e.g. "After module 1". */
  availability: string
  /** The last row: ↓ is off. */
  last: boolean
  /** While saving, nothing can be moved. */
  busy: boolean
  manageTo: string
  onMove: (by: -1 | 1) => void
  onDragStart: (event: DragEvent) => void
  onDragOver: (event: DragEvent) => void
  onDragEnd: () => void
}

/** One module in the list: drag handle, rank, name, item count and availability, ↑ ↓ and Manage. */
export default function ModuleRow(props: ModuleRowProps) {
  const { module, index, savedIndex, moved, dragging, itemCount, availability, last, busy, manageTo } = props
  const rowClass = ['manage-module-row', moved ? 'is-moved' : '', dragging ? 'is-dragging' : '']
    .filter(Boolean)
    .join(' ')
  return (
    <li
      className={rowClass}
      draggable={!busy}
      onDragStart={props.onDragStart}
      onDragOver={props.onDragOver}
      onDrop={event => event.preventDefault()}
      onDragEnd={props.onDragEnd}
    >
      <span className="manage-module-handle" aria-hidden="true" title="Drag to change the order">
        <DragHandleIcon />
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
          {itemCount === undefined ? '…' : `${itemCount} ${itemCount === 1 ? 'item' : 'items'}`}
          {' · '}
          {availability}
        </span>
      </div>
      <div className="manage-module-actions">
        <button
          type="button"
          className="button-secondary manage-module-move"
          disabled={index === 0 || busy}
          onClick={() => props.onMove(-1)}
          aria-label={`Move ${module.title} up`}
          title="Move up"
        >
          ↑
        </button>
        <button
          type="button"
          className="button-secondary manage-module-move"
          disabled={last || busy}
          onClick={() => props.onMove(1)}
          aria-label={`Move ${module.title} down`}
          title="Move down"
        >
          ↓
        </button>
        <Link className="button-secondary" to={manageTo} draggable={false}>
          Manage<span className="visually-hidden"> {module.title}</span>
        </Link>
      </div>
    </li>
  )
}
