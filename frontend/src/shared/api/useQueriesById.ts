import { useQueries, type QueryKey } from '@tanstack/react-query'

type QueriesByIdOptions<TItem, TData> = {
  queryKey: (item: TItem) => QueryKey
  queryFn: (item: TItem) => Promise<TData>
  /** The item's key in the map. Default: the item itself as a string (for a list of ids). */
  key?: (item: TItem) => string
}

/**
 * One query per item, combined into a map from the item's key to its data, e.g. the
 * details of every assignment in a table. Uses the same query keys (and cache) as the
 * hooks for one item.
 *
 * `byId` has no entry for an item while it loads or when it failed; `null` from the
 * backend is kept (e.g. "not graded").
 */
export function useQueriesById<TItem, TData>(
  items: TItem[],
  { queryKey, queryFn, key = String }: QueriesByIdOptions<TItem, TData>,
) {
  return useQueries({
    queries: items.map(item => ({
      queryKey: queryKey(item),
      queryFn: () => queryFn(item),
    })),
    combine: results => ({
      byId: new Map<string, TData>(
        results.flatMap((result, index) =>
          result.data !== undefined ? [[key(items[index]), result.data] as const] : [],
        ),
      ),
      isPending: results.some(result => result.isPending),
      isError: results.some(result => result.isError),
    }),
  })
}
