import { ApiError } from './apiClient.ts'

/**
 * A message for the user about a failed request: the one for the backend's status code,
 * otherwise the fallback (also for network errors and errors that are not ApiError).
 *
 *   errorMessage(error, { 409: 'Group 3 already exists.', 403: 'Only teachers can create groups.' },
 *     'The group could not be created. Try again.')
 */
export function errorMessage(error: unknown, byStatus: Partial<Record<number, string>>, fallback: string): string {
  return (error instanceof ApiError ? byStatus[error.status] : undefined) ?? fallback
}
