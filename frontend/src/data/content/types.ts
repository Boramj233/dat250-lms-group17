import type { ContentId, CourseId, ModuleId, UserId } from "../shared/types";

// Start with text. Add assignment, quiz, and coding exercise variants when their
// fields and backend contracts are defined.
export type ContentType = "TEXT";

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

export type Content = TextContent;

export interface CreateContentInput {
  moduleId: ModuleId;
  type: "TEXT";
  title: string;
  position: number;
  body: string;
}

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

