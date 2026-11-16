/** A copy with the newest first, by `submittedAt` (an ISO date-time), e.g. as a query's `select`. */
export function newestFirst<T extends { submittedAt: string }>(items: T[]): T[] {
  return [...items].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt))
}
