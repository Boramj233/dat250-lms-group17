import type { QueryClient } from "@tanstack/react-query";
import { contentKeys } from "../content/queryKeys";
import { moduleKeys } from "../module/queryKeys";
import { submissionKeys } from "./queryKeys";
import type { SubmissionSummary } from "./types";

// Refresh lists, grades, and progress for every beneficiary of a group submission.
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
