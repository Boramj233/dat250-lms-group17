import { useState, type DragEvent } from 'react'
import { moveItem } from '@shared/lib/moveItem'
import type { CourseModule } from '@domain/modules/types'

/**
 * A new order of the course's modules that is not saved yet, changed by dragging a row or
 * with ↑ / ↓. Every move is announced for screen readers. `onChange` runs on every change
 * (e.g. to clear "Saved."); `reset` goes back to the saved order.
 */
export function useModuleOrder(modules: CourseModule[], onChange: () => void) {
  // The unsaved order as module ids (strings); null: the saved order.
  const [order, setOrder] = useState<string[] | null>(null)
  // The module being dragged.
  const [dragId, setDragId] = useState<string | null>(null)
  const [announcement, setAnnouncement] = useState('')

  const savedIds = modules.map(m => String(m.id))
  const shown = (order ?? savedIds)
    .map(id => modules.find(m => String(m.id) === id))
    .filter((m): m is CourseModule => m !== undefined)
  const shownIds = shown.map(m => String(m.id))
  // Modules whose rank is not the saved one.
  const changes = order ? order.filter((id, index) => savedIds[index] !== id).length : 0

  function setNewOrder(next: string[]) {
    setOrder(next.join() === savedIds.join() ? null : next)
    onChange()
  }

  function announceMove(module: CourseModule, to: number) {
    setAnnouncement(`${module.title} moved to place ${to + 1} of ${shownIds.length}. Not saved yet.`)
  }

  /** ↑ / ↓: one place, and says where it went. */
  function move(module: CourseModule, by: -1 | 1) {
    const from = shownIds.indexOf(String(module.id))
    const to = from + by
    if (to < 0 || to >= shownIds.length) return
    setNewOrder(moveItem(shownIds, from, to))
    announceMove(module, to)
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
    setNewOrder(moveItem(shownIds, shownIds.indexOf(dragId), shownIds.indexOf(overId)))
  }

  function dragEnd() {
    if (dragId !== null) {
      const module = shown.find(m => String(m.id) === dragId)
      if (module) announceMove(module, shownIds.indexOf(dragId))
    }
    setDragId(null)
  }

  /** Back to the saved order (after saving, or Cancel with a message to announce). */
  function reset(message?: string) {
    setOrder(null)
    if (message) setAnnouncement(message)
  }

  return { order, shown, savedIds, changes, dragId, announcement, move, dragStart, dragOver, dragEnd, reset }
}
