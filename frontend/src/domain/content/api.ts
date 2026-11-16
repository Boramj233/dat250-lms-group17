import { apiClient, apiPath, jsonBody } from '@shared/api/apiClient'
import type { ContentId, CourseId, ModuleId } from '@shared/types'
import { questionRequest } from './questionRequest'
import type {
  AssignmentContent,
  CodingExerciseContent,
  CompletionSettingsInput,
  Content,
  ContentSummary,
  ContentType,
  CreateContentInput,
  DeleteContentInput,
  EditContentInput,
  MoveContentInput,
  QuizContent,
  StaticPageContent,
  UpdateContentInput,
} from './types'

// The backend sends "Assignment" for assignments in content lists, unlike the other
// types (STATIC_PAGE, QUIZ, CODING_EXERCISE). Accept both spellings.
function normalizeType(type: string): ContentType {
  return type === 'Assignment' ? 'ASSIGNMENT' : (type as ContentType)
}

/** A module's content, sorted by position. */
export async function getModuleContent(courseId: CourseId, moduleId: ModuleId): Promise<ContentSummary[]> {
  const items = await apiClient<(Omit<ContentSummary, 'type'> & { type: string })[]>(
    apiPath`/api/courses/${courseId}/modules/${moduleId}/contents`,
  )
  return items.map(item => ({ ...item, type: normalizeType(item.type) })).sort((a, b) => a.position - b.position)
}

/**
 * The details of one content item. The backend has one endpoint per type and the
 * responses have no `type` field, so the caller passes the type (from the summary) and
 * it is added to the result.
 */
export async function getContent(type: ContentType, contentId: ContentId): Promise<Content> {
  switch (type) {
    case 'STATIC_PAGE':
      return { ...(await apiClient<Omit<StaticPageContent, 'type'>>(apiPath`/api/contents/${contentId}`)), type }
    case 'QUIZ':
      return { ...(await apiClient<Omit<QuizContent, 'type'>>(apiPath`/api/quizzes/${contentId}`)), type }
    case 'ASSIGNMENT':
      return { ...(await apiClient<Omit<AssignmentContent, 'type'>>(apiPath`/api/assignments/${contentId}`)), type }
    case 'CODING_EXERCISE':
      return {
        ...(await apiClient<Omit<CodingExerciseContent, 'type'>>(apiPath`/api/coding-exercises/${contentId}`)),
        type,
      }
  }
}

function post<T>(url: string, body: unknown): Promise<T> {
  return apiClient<T>(url, jsonBody('POST', body))
}

const CREATE_PATH: Record<CreateContentInput['type'], string> = {
  STATIC_PAGE: 'pages',
  ASSIGNMENT: 'assignments',
  CODING_EXERCISE: 'coding-exercises',
  QUIZ: 'quizzes',
}

/**
 * Creates content with the backend endpoint for its type. A quiz is created first, then
 * its passing percentage is set (CreateQuizRequest has no field for it) and its
 * questions are added one by one. Resolves to the new content's id.
 */
export async function createContent(input: CreateContentInput): Promise<{ id: ContentId }> {
  const { courseId, moduleId, type, ...fields } = input
  const url = apiPath`/api/courses/${courseId}/modules/${moduleId}/contents/${CREATE_PATH[type]}`
  if (input.type !== 'QUIZ') {
    return post<{ id: ContentId }>(url, fields)
  }
  const { questions, passingPercentage, ...quizFields } = fields as Omit<typeof input, 'courseId' | 'moduleId' | 'type'>
  const quiz = await post<{ id: ContentId }>(url, quizFields)
  // New content is never required for module completion, so `false` changes nothing.
  await updateCompletionSettings({
    moduleId,
    contentId: quiz.id,
    requiredForModuleCompletion: false,
    passingPercentage,
  })
  for (const [position, question] of questions.entries()) {
    await post(apiPath`/api/quizzes/${quiz.id}/questions`, questionRequest(question, position))
  }
  return quiz
}

/**
 * Proposed: saves all fields of an unpublished item (PATCH /api/contents/{id}); a quiz's
 * questions are then replaced as a whole (PUT /api/quizzes/{id}/questions). 409 once
 * the item is published.
 */
export async function editContent({ contentId, fields }: EditContentInput): Promise<void> {
  const { type, ...changes } = fields
  if (type !== 'QUIZ') {
    await apiClient<Content>(apiPath`/api/contents/${contentId}`, jsonBody('PATCH', changes))
    return
  }
  const { questions, ...quizChanges } = changes as Omit<typeof fields & { type: 'QUIZ' }, 'type'>
  await apiClient<Content>(apiPath`/api/contents/${contentId}`, jsonBody('PATCH', quizChanges))
  await apiClient<unknown>(
    apiPath`/api/quizzes/${contentId}/questions`,
    jsonBody('PUT', questions.map(questionRequest)),
  )
}

/** Proposed: changes some fields (PATCH /api/contents/{id}). */
export function updateContent({ contentId, changes }: UpdateContentInput): Promise<Content> {
  return apiClient<Content>(apiPath`/api/contents/${contentId}`, jsonBody('PATCH', changes))
}

/**
 * Teachers: whether the item is required for module completion and, for a quiz, its
 * passing percentage (backend: ContentCompletionController, 204).
 */
export function updateCompletionSettings({
  contentId,
  requiredForModuleCompletion,
  passingPercentage,
}: CompletionSettingsInput): Promise<void> {
  return apiClient<void>(
    apiPath`/api/contents/${contentId}/completion-settings`,
    jsonBody('PATCH', { requiredForModuleCompletion, passingPercentage: passingPercentage ?? null }),
  )
}

/** Proposed: DELETE /api/contents/{id}. */
export function deleteContent({ contentId }: DeleteContentInput): Promise<void> {
  return apiClient<void>(apiPath`/api/contents/${contentId}`, { method: 'DELETE' })
}

/** Proposed: PATCH /api/contents/{id}/move. */
export function moveContent({ contentId, targetModuleId, position }: MoveContentInput): Promise<Content> {
  return apiClient<Content>(
    apiPath`/api/contents/${contentId}/move`,
    jsonBody('PATCH', { moduleId: targetModuleId, position }),
  )
}
