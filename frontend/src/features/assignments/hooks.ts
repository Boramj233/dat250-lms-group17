import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ContentId, UserId } from "../../shared/types";
import { getContentSubmissions, getMySubmissionHistory, getSubmissionResult, gradeSubmission, publishSubmissionGrade, submitMySubmission } from "./api";
import { gradeKeys } from "../grades/queryKeys";
import { submissionKeys } from "./queryKeys";
import type { ContentSubmission, SubmissionId } from "./types";

// The logged-in user's submissions for an assignment or coding exercise, newest first.
export function useMySubmissionHistory(contentId: ContentId) {
  return useQuery({
    queryKey: submissionKeys.myHistory(contentId),
    queryFn: () => getMySubmissionHistory(contentId),
    select: submissions =>
      [...submissions].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt)),
  });
}

// Fetches grading status, released feedback, and permitted coding test results.
/** Does nothing while `submissionId` is undefined (e.g. nothing submitted yet). */
export function useSubmissionResult(submissionId: SubmissionId | undefined, viewerId: UserId) {
  return useQuery({
    queryKey: submissionKeys.result(submissionId ?? "", viewerId),
    queryFn: () => getSubmissionResult(submissionId!),
    enabled: submissionId !== undefined,
  });
}

// Hands in the logged-in user's answer (backend) and refreshes their submission
// history and grades, so the new submission shows up at once.
export function useSubmitMySubmission() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: submitMySubmission,
    onSuccess: (submission) => Promise.all([
      queryClient.invalidateQueries({ queryKey: submissionKeys.myHistory(submission.contentId) }),
      queryClient.invalidateQueries({ queryKey: gradeKeys.all }),
    ]),
  });
}

const newestFirst = (submissions: ContentSubmission[]) =>
  [...submissions].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt));

// Teachers: every submission for an assignment or coding exercise, newest first.
export function useContentSubmissions(contentId: ContentId) {
  return useQuery({
    queryKey: submissionKeys.contentSubmissions(contentId),
    queryFn: () => getContentSubmissions(contentId),
    select: newestFirst,
  });
}

// Teachers: the submissions of several items at once (the course's Submissions table).
// Shares the cache with useContentSubmissions.
export function useContentSubmissionsFor(contentIds: ContentId[]) {
  return useQueries({
    queries: contentIds.map(contentId => ({
      queryKey: submissionKeys.contentSubmissions(contentId),
      queryFn: () => getContentSubmissions(contentId),
    })),
    combine: results => ({
      /** Submissions per content id (as string); missing while loading or if it failed. */
      byContent: new Map<string, ContentSubmission[]>(
        results.flatMap((result, index) =>
          result.data ? [[String(contentIds[index]), result.data] as const] : [])
      ),
      isPending: results.some(result => result.isPending),
    }),
  });
}

// Teachers: saves a grade (not yet visible to the student) and refreshes the list.
export function useGradeSubmission() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: gradeSubmission,
    onSuccess: (_submission, { contentId }) =>
      queryClient.invalidateQueries({ queryKey: submissionKeys.contentSubmissions(contentId) }),
  });
}

// Teachers: publishes the saved grade to the student and refreshes the list.
export function usePublishSubmissionGrade() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: publishSubmissionGrade,
    onSuccess: (_submission, { contentId }) =>
      queryClient.invalidateQueries({ queryKey: submissionKeys.contentSubmissions(contentId) }),
  });
}
