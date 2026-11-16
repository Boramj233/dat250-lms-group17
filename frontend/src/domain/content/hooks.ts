import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useInvalidatingMutation } from '@shared/api/useInvalidatingMutation'
import { useQueriesById } from '@shared/api/useQueriesById'
import type { ContentId, CourseId, ModuleId } from '@shared/types'
import { quizKeys } from '@domain/quizzes/queryKeys'
import {
  createContent,
  deleteContent,
  editContent,
  getCompletion,
  getContent,
  getModuleContent,
  moveContent,
  setPageCompletion,
  updateCompletionSettings,
  updateContent,
  updateExamRequirement,
} from './api'
import { contentKeys } from './queryKeys'
import type { Content, ContentCompletion, ContentSummary, ContentType } from './types'

// Fetches and caches the full details of a content item. The type comes from the
// content summary, since the backend has one endpoint per content type.
export function useContent(type: ContentType | undefined, contentId: ContentId) {
  return useQuery({
    queryKey: contentKeys.content(contentId),
    queryFn: () => getContent(type!, contentId),
    // Wait until the type is known (from the module's content list).
    enabled: type !== undefined,
  })
}

// The full details of several content items at once, e.g. the deadlines in a course's
// table of assignments. Shares the cache with useContent (same query keys).
export function useContents(items: { type: ContentType; id: ContentId }[]) {
  const { byId, isPending } = useQueriesById(items, {
    queryKey: item => contentKeys.content(item.id),
    queryFn: (item): Promise<Content> => getContent(item.type, item.id),
    key: item => String(item.id),
  })
  /** Details per content id (as string); missing while that item is loading. */
  return { byId, isPending }
}

// The content of several modules of a course at once, e.g. "all assignments in the
// course". Shares the cache with useModuleContent (same query keys).
export function useCourseContent(courseId: CourseId, moduleIds: ModuleId[]) {
  const { byId, isPending, isError } = useQueriesById(moduleIds, {
    queryKey: moduleId => contentKeys.moduleContent(moduleId),
    queryFn: (moduleId): Promise<ContentSummary[]> => getModuleContent(courseId, moduleId),
  })
  /** Content per module id (as string); missing while that module is loading. */
  return { byModule: byId, isPending, isError }
}

// Fetches and caches the content summaries of a module, sorted by position.
/** Does nothing while `moduleId` is undefined (e.g. no module selected). */
export function useModuleContent(courseId: CourseId, moduleId: ModuleId | undefined) {
  return useQuery({
    queryKey: contentKeys.moduleContent(moduleId ?? ''),
    queryFn: () => getModuleContent(courseId, moduleId!),
    enabled: moduleId !== undefined,
  })
}

// Creates a page, assignment, coding exercise or quiz (with questions) and refreshes
// the module's content list.
//only for teachers, not for students
export function useCreateContent() {
  return useInvalidatingMutation(createContent, (_content, { moduleId }) => [contentKeys.moduleContent(moduleId)])
}

// Deletes content, clears its cached data (also the user's quiz attempts), and
// refreshes the module list.
//only for teachers, not for students
export function useDeleteContent() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deleteContent,
    onSuccess: async (_data, { contentId, moduleId }) => {
      queryClient.removeQueries({ queryKey: contentKeys.content(contentId) })
      queryClient.removeQueries({ queryKey: quizKeys.quizAttempts(contentId) })
      await queryClient.invalidateQueries({ queryKey: contentKeys.moduleContent(moduleId) })
    },
  })
}

// Moves or reorders content and refreshes its details and module lists.
//only for teachers, not for students
export function useMoveContent() {
  return useInvalidatingMutation(moveContent, (_content, { contentId, sourceModuleId, targetModuleId }) => [
    contentKeys.content(contentId),
    contentKeys.moduleContent(sourceModuleId),
    ...(String(sourceModuleId) !== String(targetModuleId) ? [contentKeys.moduleContent(targetModuleId)] : []),
  ])
}

// One item changed: its details and its module's content list.
const itemKeys = (_result: unknown, { contentId, moduleId }: { contentId: ContentId; moduleId: ModuleId }) => [
  contentKeys.content(contentId),
  contentKeys.moduleContent(moduleId),
]

// Updates content and refreshes its details and module content list.
//only for teachers, not for students
/** Teachers: saves all fields of an unpublished item (proposed), then reloads it and its module. */
export function useEditContent() {
  return useInvalidatingMutation(editContent, itemKeys)
}

export function useUpdateContent() {
  return useInvalidatingMutation(updateContent, itemKeys)
}

/** Teachers: "required for module completion" (and a quiz's passing percentage). */
export function useUpdateCompletionSettings() {
  return useInvalidatingMutation(updateCompletionSettings, itemKeys)
}

/** Teachers: whether an assignment is mandatory for the exam; refreshes its details. */
export function useUpdateExamRequirement() {
  return useInvalidatingMutation(updateExamRequirement, (_result, { contentId }) => [contentKeys.content(contentId)])
}

// Students: whether they have completed one item. Only students may ask (403 for
// teachers), so pass `enabled: false` for them.
export function useContentCompletion(contentId: ContentId, enabled = true) {
  return useQuery({
    queryKey: contentKeys.completion(contentId),
    queryFn: () => getCompletion(contentId),
    enabled,
  })
}

// Students: completion of several items at once, e.g. a table of pages. Shares the
// cache with useContentCompletion (same query keys).
export function useContentCompletions(contentIds: ContentId[]) {
  const { byId, isPending } = useQueriesById(contentIds, {
    queryKey: contentId => contentKeys.completion(contentId),
    queryFn: (contentId): Promise<ContentCompletion> => getCompletion(contentId),
  })
  /** Completion per content id (as string); missing while loading or if it failed. */
  return { byContent: byId, isPending }
}

// Students: marks a page as done or not done. The answer is the new status, so it goes
// straight into the cache.
export function useSetPageCompletion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: setPageCompletion,
    onSuccess: status => queryClient.setQueryData(contentKeys.completion(status.contentId), status),
  })
}
