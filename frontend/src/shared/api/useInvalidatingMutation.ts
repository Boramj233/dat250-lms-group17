import { useMutation, useQueryClient, type QueryKey } from '@tanstack/react-query'

/**
 * A mutation that, when it succeeds, marks the cached queries it changed as outdated.
 * It waits until the ones on screen have reloaded, so the caller's onSuccess sees fresh data.
 *
 *   useInvalidatingMutation(updateModule, (_module, { courseId }) => [moduleKeys.courseModules(courseId)])
 *
 * For anything else on success (removing or setting cached data), use useMutation directly.
 */
export function useInvalidatingMutation<TData, TVariables>(
  mutationFn: (variables: TVariables) => Promise<TData>,
  invalidates: (data: TData, variables: TVariables) => QueryKey[],
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: async (data, variables) => {
      await Promise.all(invalidates(data, variables).map(queryKey => queryClient.invalidateQueries({ queryKey })))
    },
  })
}
