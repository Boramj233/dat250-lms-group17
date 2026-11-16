import type { ContentId, CourseId, ModuleId } from '@shared/types'

// The content types in the backend (ContentSummaryResponse.type). The backend sends
// "Assignment" for assignments; contentQueries normalizes it to "ASSIGNMENT".
export type ContentType = 'STATIC_PAGE' | 'QUIZ' | 'ASSIGNMENT' | 'CODING_EXERCISE'

// Matches the backend's ContentSummaryResponse: one item in a module's content list.
export interface ContentSummary {
  id: ContentId
  type: ContentType
  title: string
  position: number
  /**
   * The user must complete this item to complete its module. Sent in the content list
   * (ContentSummaryResponse); the detail responses leave it out, so missing means "not
   * known here", not "not required". Changed with PATCH /api/contents/{id}/completion-settings.
   * Separate from an assignment's `mandatoryForExam`.
   */
  requiredForModuleCompletion?: boolean
  /**
   * Proposed (see README): ISO 8601 instant students can see the item from. Null or
   * missing: it inherits its module's start date (availableFrom). Never before it.
   */
  visibleFrom?: string | null
}

interface ContentDetailBase extends ContentSummary {
  moduleId: ModuleId
}

// Matches StaticPageResponse (GET /api/contents/{id}) plus `type`.
export interface StaticPageContent extends ContentDetailBase {
  type: 'STATIC_PAGE'
  courseId: CourseId
  body: string
}

// Matches QuizDetailResponse (GET /api/quizzes/{id}) plus `type`. Attempts are in
// ../quizzes.
export interface QuizContent extends ContentDetailBase {
  type: 'QUIZ'
  courseId: CourseId
  maxAttempts: number
  /** In order; missing in older responses. */
  questions?: QuizContentQuestion[]
  /** Proposed (see README): what the quiz is about, shown before it is started. */
  description?: string | null
  /** Proposed (see README): ISO 8601 instant after which it cannot be taken; null for none. */
  deadline?: string | null
  /**
   * Percentage (1–100) of the questions that must be correct for an attempt to pass;
   * the backend's default is 60. Changed with PATCH /api/contents/{id}/completion-settings.
   */
  passingPercentage: number
}

/**
 * The backend's QuestionType. MULTIPLE_CHOICE (the default) and TRUE_FALSE are answered
 * with option ids; FILL_IN_BLANK with a text; ORDERING with the item ids in order;
 * MATCHING with left/right item id pairs.
 */
export type QuizQuestionType = 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'FILL_IN_BLANK' | 'ORDERING' | 'MATCHING'

// QuizDetailResponse.ItemResponse: an item of an ORDERING or MATCHING question.
export interface QuizContentItem {
  /** A UUID. */
  id: string
  text: string
}

// QuizDetailResponse.QuestionResponse.
export interface QuizContentQuestion {
  id: string | number
  questionText: string
  position: number
  type: QuizQuestionType
  /** Empty for FILL_IN_BLANK, ORDERING and MATCHING. */
  options: QuizContentOption[]
  /** ORDERING: the items to put in order (not in the correct order); empty otherwise. */
  items?: QuizContentItem[]
  /** MATCHING: the items on the left and the right side; empty otherwise. */
  leftItems?: QuizContentItem[]
  rightItems?: QuizContentItem[]
  /**
   * Proposed, for teachers of the course only (see README): the hint. The backend
   * leaves it out today, so students cannot see it either.
   */
  hint?: string | null
}

// QuizDetailResponse.OptionResponse.
export interface QuizContentOption {
  id: string | number
  optionText: string
  position: number
  /**
   * Proposed, for teachers of the course only (see README): whether this is a correct
   * answer. Missing today, so the UI cannot show which answers are correct.
   */
  correct?: boolean
}

// Matches AssignmentResponse (GET /api/assignments/{id}) plus `type`.
export interface AssignmentContent extends ContentDetailBase {
  type: 'ASSIGNMENT'
  instructions: string
  /** ISO 8601 instant, or null when there is no deadline. */
  deadline: string | null
  allowLateSubmission: boolean
  /**
   * Whether the assignment must be passed to sit the exam. Set on creation, changed with
   * PATCH /api/assignments/{id}/exam-requirement.
   */
  mandatoryForExam: boolean
  /**
   * Whether students hand in as a group (one submission per group). Proposed field,
   * not in the backend yet; missing until it is.
   */
  groupAssignment?: boolean
  /**
   * File name extensions that may be handed in, lower case with a dot, e.g. [".pdf",
   * ".zip"]; empty for any file type. Proposed field, not in the backend yet.
   */
  acceptedFileTypes?: string[]
}

// Matches CodingExerciseResponse (GET /api/coding-exercises/{id}) plus `type`.
export interface CodingExerciseContent extends ContentDetailBase {
  type: 'CODING_EXERCISE'
  instructions: string
  starterRepositoryUrl: string | null
  /**
   * Code the students start from, shown on the exercise. Proposed field, not in the
   * backend's CodingExerciseResponse yet; missing or null when there is none.
   */
  starterCode?: string | null
  deadline: string | null
  allowLateSubmission: boolean
}

export type Content = StaticPageContent | QuizContent | AssignmentContent | CodingExerciseContent

// Creating content (teachers). Each type has its own backend endpoint:
// POST /api/courses/{courseId}/modules/{moduleId}/contents/{pages|assignments|coding-exercises|quizzes}
interface CreateContentBase {
  courseId: CourseId
  moduleId: ModuleId
  title: string
  position: number
  /** Proposed: visible to students from; null inherits the module's start date. */
  visibleFrom: string | null
}

export interface CreateStaticPageInput extends CreateContentBase {
  type: 'STATIC_PAGE'
  body: string
}

export interface CreateAssignmentInput extends CreateContentBase {
  type: 'ASSIGNMENT'
  instructions: string
  /** ISO 8601 instant, or null for no deadline. */
  deadline: string | null
  allowLateSubmission: boolean
  /** CreateAssignmentRequest.mandatoryForExam. */
  mandatoryForExam: boolean
  /** Proposed (see AssignmentContent.groupAssignment). */
  groupAssignment: boolean
  /** Proposed (see AssignmentContent.acceptedFileTypes); empty for any file type. */
  acceptedFileTypes: string[]
}

export interface CreateCodingExerciseInput extends CreateContentBase {
  type: 'CODING_EXERCISE'
  instructions: string
  /** A repository and/or code to start from; at least one of them. */
  starterRepositoryUrl: string | null
  /** Proposed field (see CodingExerciseContent.starterCode). */
  starterCode: string | null
  deadline: string | null
  allowLateSubmission: boolean
}

/**
 * A question as the teacher writes it; `questionRequest` (api.ts) turns it into the
 * backend's CreateQuizQuestionRequest. Only the fields of its type are sent.
 */
export interface NewQuizQuestion {
  /** Missing means MULTIPLE_CHOICE. */
  type?: QuizQuestionType
  questionText: string
  hint?: string
  /** MULTIPLE_CHOICE (at least two, one or more correct) and TRUE_FALSE (two, one correct). */
  options: { optionText: string; correct: boolean }[]
  /** FILL_IN_BLANK: the answers that count as correct (compared without case). */
  acceptedAnswers?: string[]
  /** ORDERING: the items, in the correct order (students get them shuffled). */
  orderingItems?: string[]
  /** MATCHING: the correct pairs (students get the right side shuffled). */
  matchingPairs?: { left: string; right: string }[]
}

export interface CreateQuizInput extends CreateContentBase {
  type: 'QUIZ'
  maxAttempts: number
  /**
   * 1–100. Not in CreateQuizRequest: set right after the quiz is created, with
   * PATCH /api/contents/{id}/completion-settings.
   */
  passingPercentage: number
  /** Added one by one after the quiz is created (POST /api/quizzes/{id}/questions). */
  questions: NewQuizQuestion[]
}

export type CreateContentInput =
  CreateStaticPageInput | CreateAssignmentInput | CreateCodingExerciseInput | CreateQuizInput

/** One field set per type: what the create and edit forms produce (no place in the module). */
export type ContentFields = CreateContentInput extends infer Input
  ? Input extends CreateContentInput
    ? Omit<Input, 'courseId' | 'moduleId' | 'position'>
    : never
  : never

/**
 * Proposed: changes all of an unpublished item's fields (see README). A quiz's questions
 * are replaced as a whole.
 */
export interface EditContentInput {
  moduleId: ModuleId
  contentId: ContentId
  fields: ContentFields
}

export interface UpdateContentInput {
  moduleId: ModuleId
  contentId: ContentId
  changes: Partial<Pick<StaticPageContent, 'title' | 'position' | 'body' | 'visibleFrom'>>
}

// The backend's ContentCompletionController.Settings:
// PATCH /api/contents/{contentId}/completion-settings (teachers, 204).
export interface CompletionSettingsInput {
  moduleId: ModuleId
  contentId: ContentId
  requiredForModuleCompletion: boolean
  /** Quizzes only, 1–100; leave out to keep the current one. */
  passingPercentage?: number
}

// The backend's ContentCompletionService.Status: GET /api/contents/{contentId}/completion
// and PUT …/completion (students only; 403 for others).
export interface ContentCompletion {
  contentId: ContentId
  requiredForModuleCompletion: boolean
  /**
   * A page: marked as done by the student. A quiz: an attempt passed. An assignment:
   * a released, passed grade. A coding exercise: always false for now.
   */
  completed: boolean
}

// PUT /api/contents/{contentId}/completion with { completed }: pages only (400 for other
// types, 403 outside the page's visible window).
export interface PageCompletionInput {
  contentId: ContentId
  completed: boolean
}

// The backend's UpdateAssignmentExamRequirementRequest:
// PATCH /api/assignments/{assignmentId}/exam-requirement (teachers, 204).
export interface ExamRequirementInput {
  contentId: ContentId
  mandatoryForExam: boolean
}

export interface DeleteContentInput {
  moduleId: ModuleId
  contentId: ContentId
}

export interface MoveContentInput {
  contentId: ContentId
  sourceModuleId: ModuleId
  targetModuleId: ModuleId
  position: number
}
