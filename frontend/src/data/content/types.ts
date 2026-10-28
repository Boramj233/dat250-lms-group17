import type { ContentId, CourseId, ModuleId, UserId } from "../shared/types";
import type { Quiz, QuizQuestionDefinition } from "../quizzes/types";

// Add assignment and coding exercise variants when their
// fields and backend contracts are defined.
export type ContentType = "TEXT" | "QUIZ";

export interface ContentSummary {
  id: ContentId;
  moduleId: ModuleId;
  title: string;
  position: number;
  type: ContentType;
}

export interface TextContent extends ContentSummary {
  type: "TEXT";
  body: string;
}

export interface QuizContent extends ContentSummary, Omit<Quiz, "id" | "title"> {
  type: "QUIZ";
}

export type Content = TextContent | QuizContent;

export interface CreateTextContentInput {
  moduleId: ModuleId;
  type: "TEXT";
  title: string;
  position: number;
  body: string;
}

export interface CreateQuizContentInput extends Omit<QuizContent, "id" | "questions"> {
  questions: QuizQuestionDefinition[];
}
export type CreateContentInput = CreateTextContentInput | CreateQuizContentInput;

export interface UpdateContentInput {
  moduleId: ModuleId;
  contentId: ContentId;
  changes: Partial<Pick<TextContent, "title" | "position" | "body">>;
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

