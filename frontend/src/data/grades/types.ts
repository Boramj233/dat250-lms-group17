import type { ContentId, CourseId, ModuleId } from "../../shared/types";
import type { SubmissionGrade, SubmissionId } from "../submissions/types";

/**
 * One assignment or coding exercise the logged-in user has submitted: either graded
 * (grade published) or waiting for a grade.
 */
export interface GradeEntry {
  contentId: ContentId;
  type: "ASSIGNMENT" | "CODING_EXERCISE";
  title: string;
  courseId: CourseId;
  courseTitle: string;
  moduleId: ModuleId;
  /** The latest submission, which is the one that is graded. */
  submissionId: SubmissionId;
  /** ISO 8601 instant. */
  submittedAt: string;
  late: boolean;
  /** Null while the grade is not published (awaiting grade). */
  grade: SubmissionGrade | null;
}
