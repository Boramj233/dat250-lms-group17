import { useQuery } from '@tanstack/react-query'
import type { UserId } from '@shared/types'
import { getUpcomingDeadlines } from './api'
import { deadlineKeys } from './queryKeys'

// Fetches and caches the upcoming deadlines in all courses the user is enrolled in.
export function useUpcomingDeadlines(userId: UserId) {
  return useQuery({
    queryKey: deadlineKeys.upcoming(userId),
    queryFn: () => getUpcomingDeadlines(userId),
  })
}
