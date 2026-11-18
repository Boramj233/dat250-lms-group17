import { apiClient, apiPath, jsonBody } from '@shared/api/apiClient'
import type { ContentId, CourseId, ModuleId } from '@shared/types'
import { questionRequest } from './questionRequest'
import type {
  AssignmentContent,
  CodingExerciseContent,
  CompletionSettingsInput,
  Content,
  ContentCompletion,
  ContentSummary,
  ContentType,
  ContentVisibility,
  CreateContentInput,
  DeleteContentInput,
  EditContentInput,
  ExamRequirementInput,
  MoveContentInput,
  PageCompletionInput,
  QuizContent,
  ReorderContentInput,
  StaticPageContent,
  VisibilityInput,
} from './types'

// The backend sends "Assignment" for assignments in content lists, unlike the other
// types (STATIC_PAGE, QUIZ, CODING_EXERCISE). Accept both spellings.
function normalizeType(type: string): ContentType {
  return type === 'Assignment' ? 'ASSIGNMENT' : (type as ContentType)
}

/** A module's content, sorted by position. Students only get the items visible now. */
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

function put<T>(url: string, body: unknown): Promise<T> {
  return apiClient<T>(url, jsonBody('PUT', body))
}

/** The request body of the backend's create endpoint for each type (only its fields). */
function createRequest(input: CreateContentInput): object {
  const { title, position } = input
  switch (input.type) {
    case 'STATIC_PAGE':
      return { title, position, body: input.body }
    case 'ASSIGNMENT':
      return {
        title,
        position,
        instructions: input.instructions,
        deadline: input.deadline,
        allowLateSubmission: input.allowLateSubmission,
        mandatoryForExam: input.mandatoryForExam,
      }
    case 'CODING_EXERCISE':
      return {
        title,
        position,
        instructions: input.instructions,
        starterRepositoryUrl: input.starterRepositoryUrl,
        starterCode: input.starterCode,
        deadline: input.deadline,
        allowLateSubmission: input.allowLateSubmission,
      }
    case 'QUIZ':
      return { title, position, maxAttempts: input.maxAttempts }
  }
}

const CREATE_PATH: Record<CreateContentInput['type'], string> = {
  STATIC_PAGE: 'pages',
  ASSIGNMENT: 'assignments',
  CODING_EXERCISE: 'coding-exercises',
  QUIZ: 'quizzes',
}

/**
 * Creates content with the backend endpoint for its type. The create requests have no
 * `visibleFrom`, so a date is then set with PUT /api/contents/{id}/visibility. A quiz's
 * passing percentage is then set (CreateQuizRequest has no field for it) and its
 * questions are added one by one. Resolves to the new content's id.
 */
export async function createContent(input: CreateContentInput): Promise<{ id: ContentId }> {
  const { courseId, moduleId, type } = input
  const url = apiPath`/api/courses/${courseId}/modules/${moduleId}/contents/${CREATE_PATH[type]}`
  const created = await post<{ id: ContentId }>(url, createRequest(input))
  if (input.visibleFrom) {
    await updateVisibility({ moduleId, contentId: created.id, visibleFrom: input.visibleFrom })
  }
  if (input.type === 'QUIZ') {
    // New content is never required for module completion, so `false` changes nothing.
    await updateCompletionSettings({
      moduleId,
      contentId: created.id,
      requiredForModuleCompletion: false,
      passingPercentage: input.passingPercentage,
    })
    for (const [position, question] of input.questions.entries()) {
      await post(apiPath`/api/quizzes/${created.id}/questions`, questionRequest(question, position))
    }
  }
  return created
}

/**
 * Saves the fields the edit form changes, with the backend's endpoints for the item's
 * type:
 * - a page: PUT /api/contents/{id} (title, body);
 * - an assignment: PATCH /api/assignments/{id}/exam-requirement first (so it is kept even
 *   if the rest is refused), then PUT /api/assignments/{id} (title, instructions) and
 *   PUT /api/assignments/{id}/submission-settings (deadline, allowLateSubmission);
 * - a coding exercise: PUT /api/coding-exercises/{id} (title, instructions, starter
 *   repository and code) and PUT …/submission-settings;
 * - a quiz: PUT /api/quizzes/{id} (title, maxAttempts), its passing percentage with
 *   PATCH /api/contents/{id}/completion-settings and, only when `questionsChanged`, all
 *   its questions with PUT /api/quizzes/{id}/questions (409 once a student has made an
 *   attempt, so it is not sent when the questions are the same).
 * Last, for every type, `visibleFrom` with PUT /api/contents/{id}/visibility (its
 * `visibleUntil` is kept).
 */
export async function editContent({
  moduleId,
  contentId,
  requiredForModuleCompletion,
  questionsChanged = false,
  fields,
}: EditContentInput): Promise<void> {
  switch (fields.type) {
    case 'STATIC_PAGE':
      await put(apiPath`/api/contents/${contentId}`, { title: fields.title, body: fields.body })
      break
    case 'ASSIGNMENT':
      await updateExamRequirement({ contentId, mandatoryForExam: fields.mandatoryForExam })
      await put(apiPath`/api/assignments/${contentId}`, { title: fields.title, instructions: fields.instructions })
      await put(apiPath`/api/assignments/${contentId}/submission-settings`, {
        deadline: fields.deadline,
        allowLateSubmission: fields.allowLateSubmission,
      })
      break
    case 'CODING_EXERCISE':
      await put(apiPath`/api/coding-exercises/${contentId}`, {
        title: fields.title,
        instructions: fields.instructions,
        starterRepositoryUrl: fields.starterRepositoryUrl,
        starterCode: fields.starterCode,
      })
      await put(apiPath`/api/coding-exercises/${contentId}/submission-settings`, {
        deadline: fields.deadline,
        allowLateSubmission: fields.allowLateSubmission,
      })
      break
    case 'QUIZ':
      await put(apiPath`/api/quizzes/${contentId}`, { title: fields.title, maxAttempts: fields.maxAttempts })
      await updateCompletionSettings({
        moduleId,
        contentId,
        requiredForModuleCompletion,
        passingPercentage: fields.passingPercentage,
      })
      if (questionsChanged) {
        await put(apiPath`/api/quizzes/${contentId}/questions`, {
          questions: fields.questions.map((question, position) => questionRequest(question, position)),
        })
      }
      break
  }
  await updateVisibility({ moduleId, contentId, visibleFrom: fields.visibleFrom })
}

/**
 * Teachers: when students see the item (PUT /api/contents/{id}/visibility). Null
 * `visibleFrom` means from now on (or when the module opens). `visibleUntil` is only sent
 * when it is given, so leaving it out keeps the saved one; null removes it.
 */
export function updateVisibility({
  contentId,
  visibleFrom,
  visibleUntil,
}: VisibilityInput): Promise<ContentVisibility> {
  return put<ContentVisibility>(apiPath`/api/contents/${contentId}/visibility`, {
    visibleFrom,
    ...(visibleUntil !== undefined ? { visibleUntil } : {}),
  })
}

/** Teachers: the module's content in a new order; every item once (204). */
export function reorderContent({ courseId, moduleId, contentIds }: ReorderContentInput): Promise<void> {
  return put<void>(apiPath`/api/courses/${courseId}/modules/${moduleId}/contents/order`, { contentIds })
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

/** Teachers: whether an assignment must be passed to sit the exam (204). */
export function updateExamRequirement({ contentId, mandatoryForExam }: ExamRequirementInput): Promise<void> {
  return apiClient<void>(
    apiPath`/api/assignments/${contentId}/exam-requirement`,
    jsonBody('PATCH', { mandatoryForExam }),
  )
}

/** Students: whether they have completed the item (backend: ContentCompletionController). */
export function getCompletion(contentId: ContentId): Promise<ContentCompletion> {
  return apiClient<ContentCompletion>(apiPath`/api/contents/${contentId}/completion`)
}

/** Students: marks a page as done or not done; returns the new status. */
export function setPageCompletion({ contentId, completed }: PageCompletionInput): Promise<ContentCompletion> {
  return apiClient<ContentCompletion>(apiPath`/api/contents/${contentId}/completion`, jsonBody('PUT', { completed }))
}

/**
 * Teachers: DELETE /api/contents/{id} (204). 409 when students have work on it
 * (submissions, quiz attempts or a page marked as done); such content is never deleted.
 */
export function deleteContent({ contentId }: DeleteContentInput): Promise<void> {
  return apiClient<void>(apiPath`/api/contents/${contentId}`, { method: 'DELETE' })
}

/** Proposed, not in the backend yet: PATCH /api/contents/{id}/move. */
export function moveContent({ contentId, targetModuleId, position }: MoveContentInput): Promise<Content> {
  return apiClient<Content>(
    apiPath`/api/contents/${contentId}/move`,
    jsonBody('PATCH', { moduleId: targetModuleId, position }),
  )
}
