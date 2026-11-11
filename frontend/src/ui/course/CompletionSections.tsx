import type { ReactNode } from 'react'
import { splitByCompletion } from './contentTypes'

type CompletionSectionsProps<T extends { id: string | number }> = {
  items: T[]
  /** Completed content ids; undefined while loading or when unavailable. */
  completed: Set<string> | undefined
  /** Name used in the headings and empty texts, e.g. "assignments". */
  noun: string
  /** Renders one part (to do or completed). */
  render: (items: T[]) => ReactNode
}

/**
 * "To do" and "Completed" sections. Until the completion status is known, all items
 * are shown in one list.
 */
export default function CompletionSections<T extends { id: string | number }>(
  { items, completed, noun, render }: CompletionSectionsProps<T>,
) {
  if (!completed) return <>{render(items)}</>
  const { todo, done } = splitByCompletion(items, completed)
  return (
    <>
      <section className="completion-section" aria-labelledby="completion-todo">
        <h2 id="completion-todo" className="completion-title">
          To do <span className="completion-count">{todo.length}</span>
        </h2>
        {todo.length === 0 ? <p>All {noun} are completed.</p> : render(todo)}
      </section>
      <section className="completion-section" aria-labelledby="completion-done">
        <h2 id="completion-done" className="completion-title">
          Completed <span className="completion-count">{done.length}</span>
        </h2>
        {done.length === 0 ? <p>No completed {noun} yet.</p> : render(done)}
      </section>
    </>
  )
}
