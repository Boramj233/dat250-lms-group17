import type { ContentId, CourseId, ModuleId } from "../../shared/types";

// The content types in the backend (ContentSummaryResponse.type). The backend sends
// "Assignment" for assignments; contentQueries normalizes it to "ASSIGNMENT".
export type ContentType = "STATIC_PAGE" | "QUIZ" | "ASSIGNMENT" | "CODING_EXERCISE";

// Matches the backend's ContentSummaryResponse: one item in a module's content list.
export interface ContentSummary {
  id: ContentId;
  type: ContentType;
  title: string;
  position: number;
  /**
   * The user must complete this item to complete its module. Proposed field (see
   * README), not in the backend yet; missing means not required. Separate from an
   * assignment's `mandatory` (must be passed for the course).
   */
  requiredForCompletion?: boolean;
  /**
   * Students can see the item. Teachers switch it off to prepare content before it is
   * shown, or to hide it. Proposed field (see README), not in the backend yet; missing
   * means active.
   */
  active?: boolean;
  /**
   * Proposed (see README): ISO 8601 instant students can see the item from. Null or
   * missing: it inherits its module's start date (availableFrom). Never before it.
   */
  visibleFrom?: string | null;
}

interface ContentDetailBase extends ContentSummary {
  moduleId: ModuleId;
}

// Matches StaticPageResponse (GET /api/contents/{id}) plus `type`.
export interface StaticPageContent extends ContentDetailBase {
  type: "STATIC_PAGE";
  courseId: CourseId;
  body: string;
}

// Matches QuizDetailResponse (GET /api/quizzes/{id}) plus `type`. Attempts are in
// ../quizzes.
export interface QuizContent extends ContentDetailBase {
  type: "QUIZ";
  courseId: CourseId;
  maxAttempts: number;
  /** In order; missing in older responses. */
  questions?: QuizContentQuestion[];
  /** Proposed (see README): what the quiz is about, shown before it is started. */
  description?: string | null;
  /** Proposed (see README): ISO 8601 instant after which it cannot be taken; null for none. */
  deadline?: string | null;
  /**
   * Proposed (see README): how many questions must be correct for an attempt to pass.
   * Null or missing: the quiz has no pass requirement.
   */
  requiredCorrectAnswers?: number | null;
}

/**
 * Proposed (see README): how a question is answered. All are choice questions, so the
 * answer is always the chosen option ids. Missing means SINGLE_CHOICE.
 */
export type QuizQuestionType = "SINGLE_CHOICE" | "MULTIPLE_CHOICE" | "TRUE_FALSE";

// QuizDetailResponse.QuestionResponse.
export interface QuizContentQuestion {
  id: string | number;
  questionText: string;
  position: number;
  options: QuizContentOption[];
  /** Proposed (see README); missing means SINGLE_CHOICE. */
  questionType?: QuizQuestionType;
  /**
   * Proposed, for teachers of the course only (see README): the hint. The backend
   * leaves it out today, so students cannot see it either.
   */
  hint?: string | null;
}

// QuizDetailResponse.OptionResponse.
export interface QuizContentOption {
  id: string | number;
  optionText: string;
  position: number;
  /**
   * Proposed, for teachers of the course only (see README): whether this is a correct
   * answer. Missing today, so the UI cannot show which answers are correct.
   */
  correct?: boolean;
}

// Matches AssignmentResponse (GET /api/assignments/{id}) plus `type`.
export interface AssignmentContent extends ContentDetailBase {
  type: "ASSIGNMENT";
  instructions: string;
  /** ISO 8601 instant, or null when there is no deadline. */
  deadline: string | null;
  allowLateSubmission: boolean;
  /**
   * Whether the assignment must be passed. Proposed field, not in the backend's
   * AssignmentResponse yet; the UI leaves it out while it is missing.
   */
  mandatory?: boolean;
  /**
   * Whether students hand in as a group (one submission per group). Proposed field,
   * not in the backend yet; missing until it is.
   */
  groupAssignment?: boolean;
  /**
   * File name extensions that may be handed in, lower case with a dot, e.g. [".pdf",
   * ".zip"]; empty for any file type. Proposed field, not in the backend yet.
   */
  acceptedFileTypes?: string[];
}

// Matches CodingExerciseResponse (GET /api/coding-exercises/{id}) plus `type`.
export interface CodingExerciseContent extends ContentDetailBase {
  type: "CODING_EXERCISE";
  instructions: string;
  starterRepositoryUrl: string | null;
  /**
   * Code the students start from, shown on the exercise. Proposed field, not in the
   * backend's CodingExerciseResponse yet; missing or null when there is none.
   */
  starterCode?: string | null;
  deadline: string | null;
  allowLateSubmission: boolean;
}

export type Content =
  | StaticPageContent
  | QuizContent
  | AssignmentContent
  | CodingExerciseContent;

// Creating content (teachers). Each type has its own backend endpoint:
// POST /api/courses/{courseId}/modules/{moduleId}/contents/{pages|assignments|coding-exercises|quizzes}
interface CreateContentBase {
  courseId: CourseId;
  moduleId: ModuleId;
  title: string;
  position: number;
  /** Proposed: visible to students from; null inherits the module's start date. */
  visibleFrom: string | null;
}

export interface CreateStaticPageInput extends CreateContentBase {
  type: "STATIC_PAGE";
  body: string;
}

export interface CreateAssignmentInput extends CreateContentBase {
  type: "ASSIGNMENT";
  instructions: string;
  /** ISO 8601 instant, or null for no deadline. */
  deadline: string | null;
  allowLateSubmission: boolean;
  /** Proposed; the backend ignores it until it has the field. */
  mandatory: boolean;
  /** Proposed (see AssignmentContent.groupAssignment). */
  groupAssignment: boolean;
  /** Proposed (see AssignmentContent.acceptedFileTypes); empty for any file type. */
  acceptedFileTypes: string[];
}

export interface CreateCodingExerciseInput extends CreateContentBase {
  type: "CODING_EXERCISE";
  instructions: string;
  /** A repository and/or code to start from; at least one of them. */
  starterRepositoryUrl: string | null;
  /** Proposed field (see CodingExerciseContent.starterCode). */
  starterCode: string | null;
  deadline: string | null;
  allowLateSubmission: boolean;
}

// Matches the backend's CreateQuizQuestionRequest (without position; set from the order).
export interface NewQuizQuestion {
  questionText: string;
  hint?: string;
  options: { optionText: string; correct: boolean }[];
}

export interface CreateQuizInput extends CreateContentBase {
  type: "QUIZ";
  maxAttempts: number;
  /** Proposed: correct answers needed to pass; null for no pass requirement. */
  requiredCorrectAnswers: number | null;
  /** Added one by one after the quiz is created (POST /api/quizzes/{id}/questions). */
  questions: NewQuizQuestion[];
}

export type CreateContentInput =
  | CreateStaticPageInput
  | CreateAssignmentInput
  | CreateCodingExerciseInput
  | CreateQuizInput;

/** One field set per type: what the create and edit forms produce (no place in the module). */
export type ContentFields = CreateContentInput extends infer Input
  ? Input extends CreateContentInput ? Omit<Input, "courseId" | "moduleId" | "position"> : never
  : never;

/**
 * Proposed: changes all of an unpublished item's fields (see README). A quiz's questions
 * are replaced as a whole.
 */
export interface EditContentInput {
  moduleId: ModuleId;
  contentId: ContentId;
  fields: ContentFields;
}

export interface UpdateContentInput {
  moduleId: ModuleId;
  contentId: ContentId;
  changes: Partial<Pick<StaticPageContent, "title" | "position" | "body" | "requiredForCompletion" | "active" | "visibleFrom">>;
}

export interface DeleteContentInput {
  moduleId: ModuleId;
  contentId: ContentId;
}

export interface MoveContentInput {
  contentId: ContentId;
  sourceModuleId: ModuleId;
  targetModuleId: ModuleId;
  position: number;
}

