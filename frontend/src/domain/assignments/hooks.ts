import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useQueriesById } from "@shared/api/useQueriesById";
import type { ContentId } from "@shared/types";
import { gradeKeys } from "@domain/grades/queryKeys";
import { getGradeReview, releaseAllGrades, getMyReleasedGrade, getMySubmissionHistory, getReviewHistory, getReviewSubmission, getReviewSubmissions, releaseGrade, saveGrade, submitFile, submitMySubmission } from "./api";
import { submissionKeys } from "./queryKeys";
import type { MySubmission, SubmissionId } from "./types";

const newestFirst = <T extends { submittedAt: string }>(submissions: T[]) =>
  [...submissions].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt));

// --- Students -------------------------------------------------------------------------

// The logged-in user's submissions (and their groups') for an item, newest first.
export function useMySubmissionHistory(contentId: ContentId) {
  return useQuery({
    queryKey: submissionKeys.myHistory(contentId),
    queryFn: () => getMySubmissionHistory(contentId),
    select: newestFirst,
  });
}

// The released grade of a submission: null until released. Does nothing while
// `submissionId` is undefined (nothing handed in yet).
export function useMyReleasedGrade(submissionId: SubmissionId | undefined) {
  return useQuery({
    queryKey: submissionKeys.myGrade(submissionId ?? ""),
    queryFn: () => getMyReleasedGrade(submissionId!),
    enabled: submissionId !== undefined,
  });
}

// After handing in: refresh the history and the grades, so it shows up at once.
function useAfterSubmit() {
  const queryClient = useQueryClient();
  return (submission: MySubmission) => Promise.all([
    queryClient.invalidateQueries({ queryKey: submissionKeys.myHistory(submission.contentId) }),
    queryClient.invalidateQueries({ queryKey: gradeKeys.all }),
  ]);
}

// Hands in a text or a repository URL.
export function useSubmitMySubmission() {
  const onSuccess = useAfterSubmit();
  return useMutation({ mutationFn: submitMySubmission, onSuccess });
}

// Hands in a PDF file.
export function useSubmitFile() {
  const onSuccess = useAfterSubmit();
  return useMutation({ mutationFn: submitFile, onSuccess });
}

// --- Teachers -------------------------------------------------------------------------

// The latest submission per student or group for an item, newest first.
export function useReviewSubmissions(contentId: ContentId) {
  return useQuery({
    queryKey: submissionKeys.review(contentId),
    queryFn: () => getReviewSubmissions(contentId),
    select: newestFirst,
  });
}

// The latest submissions of several items at once (the course's Submissions table).
// Shares the cache with useReviewSubmissions.
export function useReviewSubmissionsFor(contentIds: ContentId[]) {
  const { byId, isPending } = useQueriesById(contentIds, {
    queryKey: contentId => submissionKeys.review(contentId),
    queryFn: getReviewSubmissions,
  });
  /** Submissions per content id (as string); missing while loading or if it failed. */
  return { byContent: byId, isPending };
}

// One submission, as a teacher sees it.
export function useReviewSubmission(submissionId: SubmissionId) {
  return useQuery({
    queryKey: submissionKeys.reviewDetail(submissionId),
    queryFn: () => getReviewSubmission(submissionId),
  });
}

// Every submission by the same student or group for the same item, newest first.
export function useReviewHistory(submissionId: SubmissionId) {
  return useQuery({
    queryKey: submissionKeys.reviewHistory(submissionId),
    queryFn: () => getReviewHistory(submissionId),
    select: newestFirst,
  });
}

// The grade of a submission (released or not); null when not graded.
export function useGradeReview(submissionId: SubmissionId) {
  return useQuery({
    queryKey: submissionKeys.gradeReview(submissionId),
    queryFn: () => getGradeReview(submissionId),
  });
}

// The grades of several submissions at once (the tables). Shares the cache with
// useGradeReview.
export function useGradeReviewsFor(submissionIds: SubmissionId[]) {
  const { byId, isPending } = useQueriesById(submissionIds, {
    queryKey: submissionId => submissionKeys.gradeReview(submissionId),
    queryFn: getGradeReview,
  });
  /** Grade per submission id (as string): null when not graded, missing while loading. */
  return { bySubmission: byId, isPending };
}

// Saves (creates or changes) a grade; the answer is the new grade with its version.
export function useSaveGrade() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: saveGrade,
    onSuccess: (grade) => queryClient.setQueryData(submissionKeys.gradeReview(grade.submissionId), grade),
  });
}

// Releases a grade to the student.
export function useReleaseGrade() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: releaseGrade,
    onSuccess: (grade) => queryClient.setQueryData(submissionKeys.gradeReview(grade.submissionId), grade),
  });
}


/** Teachers: makes every saved, unpublished grade in the assignment's submissions visible now. */
export function useReleaseAllGrades(contentId: ContentId) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => releaseAllGrades(contentId),
    // Every grade may have changed; the students' views as well.
    onSettled: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: submissionKeys.details }),
      queryClient.invalidateQueries({ queryKey: gradeKeys.all }),
    ]),
  });
}
