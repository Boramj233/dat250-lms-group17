import type { QueryClient } from "@tanstack/react-query";
import { contentKeys } from "../content/queryKeys";
import { gradeKeys } from "../grades/queryKeys";
import { moduleKeys } from "../modules/queryKeys";
import { submissionKeys } from "./queryKeys";
import type { SubmissionSummary } from "./types";

// Refresh lists, grades, and progress for every beneficiary of a group submission.
// The grade pages (data/grades) are refreshed for everyone, since a teacher's change
// shows up on the students' grade pages.
export async function invalidateSubmissionData(
  queryClient: QueryClient,
  submission: SubmissionSummary
) {
  const userIds = submission.owner.type === "GROUP"
    ? submission.owner.memberIds
    : [submission.owner.userId];

  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: submissionKeys.contentSubmissions(submission.contentId),
    }),
    queryClient.invalidateQueries({ queryKey: submissionKeys.details(submission.id) }),
    queryClient.invalidateQueries({ queryKey: submissionKeys.results(submission.id) }),
    queryClient.invalidateQueries({ queryKey: gradeKeys.all }),
    ...userIds.flatMap((userId) => [
      queryClient.invalidateQueries({
        queryKey: contentKeys.completion(submission.contentId, userId),
      }),
      queryClient.invalidateQueries({
        queryKey: moduleKeys.userModuleProgress(submission.courseId, userId),
      }),
    ]),
  ]);
}
