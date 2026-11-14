import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ContentId, UserId } from "../../shared/types";
import { getAssignmentSubmissions, getContentSubmissions, getMySubmissionHistory, getMySubmissions, getSubmission, getSubmissionResult, gradeSubmission, publishSubmissionGrade, submitAssignment, submitCodingExercise, submitMySubmission } from "./api";
import { gradeKeys } from "../grades/queryKeys";
import { invalidateSubmissionData } from "./cache";
import { submissionKeys } from "./queryKeys";
import type { SubmissionId } from "./types";

// Fetches all submissions for an assignment or coding exercise for the teacher's overview.
// Backend must restrict access to teachers of the course.
export function useAssignmentSubmissions(contentId: ContentId, viewerId: UserId) {
  return useQuery({
    queryKey: submissionKeys.assignmentSubmissions(contentId, viewerId),
    queryFn: () => getAssignmentSubmissions(contentId),
  });
}

// Teachers: all students' latest submissions for an assignment or coding exercise.
export function useContentSubmissions(contentId: ContentId, enabled = true) {
  return useQuery({
    queryKey: submissionKeys.contentSubmissions(contentId),
    queryFn: () => getContentSubmissions(contentId),
    enabled,
  });
}

// The logged-in user's submissions for an assignment or coding exercise, newest first.
export function useMySubmissionHistory(contentId: ContentId) {
  return useQuery({
    queryKey: submissionKeys.myHistory(contentId),
    queryFn: () => getMySubmissionHistory(contentId),
    select: submissions =>
      [...submissions].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt)),
  });
}

// Fetches a user's individual and group submissions for an assignment or coding exercise.
export function useMySubmissions(contentId: ContentId, userId: UserId) {
  return useQuery({
    queryKey: submissionKeys.mySubmissions(contentId, userId),
    queryFn: () => getMySubmissions(contentId, userId),
  });
}

// Fetches a submission's files or repository and the result visible to the current viewer.
export function useSubmission(submissionId: SubmissionId, viewerId: UserId) {
  return useQuery({
    queryKey: submissionKeys.submission(submissionId, viewerId),
    queryFn: () => getSubmission(submissionId),
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

// Saves a teacher's grade and feedback without publishing them to students.
// A group submission has one shared grade for all submitting members.
export function useGradeSubmission() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: gradeSubmission,
    onSuccess: (submission) => invalidateSubmissionData(queryClient, submission),
  });
}

// Publishes a submission's grade and feedback and refreshes cached results and progress.
// Backend restricts this operation to course teachers.
export function usePublishSubmissionGrade() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: publishSubmissionGrade,
    onSuccess: (submission) => invalidateSubmissionData(queryClient, submission),
  });
}

// Uploads an individual or group file submission and refreshes submissions and progress.
export function useSubmitAssignment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: submitAssignment,
    onSuccess: (submission) => invalidateSubmissionData(queryClient, submission),
  });
}

// Submits a repository and commit reference and refreshes submissions and progress.
export function useSubmitCodingExercise() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: submitCodingExercise,
    onSuccess: (submission) => invalidateSubmissionData(queryClient, submission),
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
