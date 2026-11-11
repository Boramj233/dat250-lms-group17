import './CompletedMark.css'

/** A check mark with "Completed" for screen readers (and as visible text if `label`). */
export default function CompletedMark({ label = false }: { label?: boolean }) {
  return (
    <span className="completed-mark" title="Completed">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
        strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
        <path d="M5 12.5l4.5 4.5L19 7.5" />
      </svg>
      {label ? 'Completed' : <span className="visually-hidden">Completed</span>}
    </span>
  )
}
