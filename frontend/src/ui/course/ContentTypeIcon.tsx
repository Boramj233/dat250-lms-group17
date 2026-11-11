import type { ContentType } from '../../data/content/types'

// Simple line icons (24×24, stroke) so the content types can be told apart at a glance.
const ICON_PATHS: Record<ContentType, string[]> = {
  STATIC_PAGE: ['M7 3h7l5 5v13H7z', 'M14 3v5h5', 'M10 13h6', 'M10 17h6'],
  QUIZ: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z', 'M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.6V14', 'M12 17.5v.01'],
  ASSIGNMENT: ['M9 4h6v3H9z', 'M15 5h3v16H6V5h3', 'M9 12l2 2 4-4', 'M9 17h6'],
  CODING_EXERCISE: ['M8 8l-4 4 4 4', 'M16 8l4 4-4 4', 'M13.5 5l-3 14'],
}

export default function ContentTypeIcon({ type }: { type: ContentType }) {
  return (
    <svg className="content-type-icon" width="18" height="18" viewBox="0 0 24 24" fill="none"
      stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
      aria-hidden="true" focusable="false">
      {ICON_PATHS[type].map(d => <path key={d} d={d} />)}
    </svg>
  )
}
