import type { ReactNode } from 'react'

/** The parts of a React Query result that QueryState needs. */
type QueryLike<T> = {
  data: T | undefined
  isPending: boolean
  error: unknown
}

type QueryStateProps<T> = {
  query: QueryLike<T>
  /** Shown while loading. */
  loading?: ReactNode
  /** Shown when loading failed. */
  error: ReactNode
  /** Shown instead of the content when the data is empty. Leave out to always show the content. */
  empty?: ReactNode
  /** When the data counts as empty. Default: an empty array. */
  isEmpty?: (data: T) => boolean
  /** The content, with the loaded data. */
  children: (data: T) => ReactNode
}

const isEmptyArray = (data: unknown) => Array.isArray(data) && data.length === 0

/**
 * Loading, error and empty states for one query, so every page shows them the same
 * way. The content is only rendered once the data is there:
 *
 *   <QueryState query={grades} loading="Loading grades…" error="Could not load your grades."
 *     empty="No grades yet.">
 *     {entries => <GradeSections entries={entries} />}
 *   </QueryState>
 */
export default function QueryState<T>(
  { query, loading = 'Loading…', error, empty, isEmpty = isEmptyArray, children }: QueryStateProps<T>,
) {
  if (query.isPending) return <p role="status">{loading}</p>
  if (query.error || query.data === undefined) return <p role="alert">{error}</p>
  if (empty !== undefined && isEmpty(query.data)) return <p>{empty}</p>
  return <>{children(query.data)}</>
}
