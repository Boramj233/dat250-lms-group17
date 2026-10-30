import type { ContentId, CourseId, ModuleId, UserId } from "../shared/types";

// A quiz is a content item. Attempt and question IDs are local to this domain.
export type QuizId = ContentId;
export type QuizAttemptId = string | number;
export type QuizQuestionId = string;

export interface QuizOption {
  id: string;
  text: string;
}

interface QuizQuestionBase {
  id: QuizQuestionId;
  prompt: string;
  points: number;
}

export interface MultipleChoiceQuestion extends QuizQuestionBase {
  type: "MULTIPLE_CHOICE";
  options: QuizOption[];
  allowMultiple: boolean;
}

export interface OrderingQuestion extends QuizQuestionBase {
  type: "ORDERING";
  items: QuizOption[];
}

export interface MatchingQuestion extends QuizQuestionBase {
  type: "MATCHING";
  leftItems: QuizOption[];
  rightItems: QuizOption[];
}

export interface FillInBlanksQuestion extends QuizQuestionBase {
  type: "FILL_IN_BLANKS";
  blanks: { id: string; label: string }[];
}

// Student-facing questions never include correct answers or grading rules.
export type QuizQuestion =
  | MultipleChoiceQuestion
  | OrderingQuestion
  | MatchingQuestion
  | FillInBlanksQuestion;

// Teacher-only authoring data. These fields must not appear in student responses.
export type QuizQuestionDefinition = (
  | (MultipleChoiceQuestion & { correctOptionIds: string[] })
  | (OrderingQuestion & { correctItemIds: string[] })
  | (MatchingQuestion & { correctMatches: { leftId: string; rightId: string }[] })
  | (FillInBlanksQuestion & { acceptedAnswers: { blankId: string; values: string[] }[] })
) & { hints?: string[] };

export type QuizAnswer =
  | { questionId: QuizQuestionId; type: "MULTIPLE_CHOICE"; selectedOptionIds: string[] }
  | { questionId: QuizQuestionId; type: "ORDERING"; orderedItemIds: string[] }
  | { questionId: QuizQuestionId; type: "MATCHING"; matches: { leftId: string; rightId: string }[] }
  | { questionId: QuizQuestionId; type: "FILL_IN_BLANKS"; values: { blankId: string; value: string }[] };

export interface Quiz {
  id: QuizId;
  title: string;
  instructions: string;
  questions: QuizQuestion[];
  // Null means unlimited attempts.
  maxAttempts: number | null;
  hintsEnabled: boolean;
  deadline: string | null;
}

export interface QuizDefinition extends Omit<Quiz, "questions"> {
  questions: QuizQuestionDefinition[];
}

export interface UpdateQuizInput {
  quizId: QuizId;
  moduleId: ModuleId;
  // Title and position use the shared content update operation.
  changes: Partial<Omit<QuizDefinition, "id" | "title">>;
}

export type QuizAttemptStatus = "IN_PROGRESS" | "SUBMITTED" | "GRADED";

export interface QuizAttemptSummary {
  id: QuizAttemptId;
  quizId: QuizId;
  userId: UserId;
  attemptNumber: number;
  status: QuizAttemptStatus;
  startedAt: string;
  submittedAt: string | null;
}

export interface QuizAttemptResult {
  score: number;
  maxScore: number;
  feedback: string | null;
}

export interface QuizAttempt extends QuizAttemptSummary {
  // Snapshot of the questions for this attempt, even if the quiz is edited later.
  questions: QuizQuestion[];
  answers: QuizAnswer[];
  hintsUsed: number;
  // Null until a result is available and released to this user by the backend.
  result: QuizAttemptResult | null;
}

export interface StartQuizAttemptInput {
  quizId: QuizId;
  // Cache scope only; the backend identifies the actor from authentication.
  userId: UserId;
}

export interface SaveQuizAnswerInput extends StartQuizAttemptInput {
  attemptId: QuizAttemptId;
  answer: QuizAnswer;
}

export interface SubmitQuizAttemptInput extends StartQuizAttemptInput {
  attemptId: QuizAttemptId;
  // Cache scope for the user's module completion and access.
  courseId: CourseId;
}

export interface RequestQuizHintInput extends StartQuizAttemptInput {
  attemptId: QuizAttemptId;
  questionId: QuizQuestionId;
}

export interface QuizHint {
  questionId: QuizQuestionId;
  hint: string;
}
