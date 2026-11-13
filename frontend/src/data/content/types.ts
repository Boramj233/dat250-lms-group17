import type { ContentId, CourseId, ModuleId, UserId } from "../shared/types";

// The content types in the backend (ContentSummaryResponse.type). The backend sends
// "Assignment" for assignments; contentQueries normalizes it to "ASSIGNMENT".
export type ContentType = "STATIC_PAGE" | "QUIZ" | "ASSIGNMENT" | "CODING_EXERCISE";

// Matches the backend's ContentSummaryResponse: one item in a module's content list.
export interface ContentSummary {
  id: ContentId;
  type: ContentType;
  title: string;
  position: number;
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

// Matches QuizResponse (GET /api/quizzes/{id}) plus `type`. Questions and attempts
// are in ../quizzes.
export interface QuizContent extends ContentDetailBase {
  type: "QUIZ";
  courseId: CourseId;
  maxAttempts: number;
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
}

// Matches CodingExerciseResponse (GET /api/coding-exercises/{id}) plus `type`.
export interface CodingExerciseContent extends ContentDetailBase {
  type: "CODING_EXERCISE";
  instructions: string;
  starterRepositoryUrl: string | null;
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
}

export interface CreateCodingExerciseInput extends CreateContentBase {
  type: "CODING_EXERCISE";
  instructions: string;
  starterRepositoryUrl: string;
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
  /** Added one by one after the quiz is created (POST /api/quizzes/{id}/questions). */
  questions: NewQuizQuestion[];
}

export type CreateContentInput =
  | CreateStaticPageInput
  | CreateAssignmentInput
  | CreateCodingExerciseInput
  | CreateQuizInput;

export interface UpdateContentInput {
  moduleId: ModuleId;
  contentId: ContentId;
  changes: Partial<Pick<StaticPageContent, "title" | "position" | "body">>;
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

/**
 * A content item the logged-in user has completed: an assignment or coding exercise
 * with a submission, or a quiz with a submitted attempt. See README (completions).
 */
export interface CompletedContent {
  contentId: ContentId;
  /** ISO 8601 instant of the submission / submitted attempt. */
  completedAt: string;
}

export interface ContentCompletion {
  contentId: ContentId;
  userId: UserId;
  completed: boolean;
}

export interface SetContentCompletionInput {
  // Used to refresh module progress and access after the backend records completion.
  courseId: CourseId;
  contentId: ContentId;
  userId: UserId;
  completed: boolean;
}

