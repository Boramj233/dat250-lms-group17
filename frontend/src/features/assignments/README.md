# Submissions and grades API

All of these exist in the backend; the frontend matches their DTOs in `types.ts`.

## Students

| Method | Endpoint | Request body | Response |
| --- | --- | --- | --- |
| POST | `/api/contents/{contentId}/submissions` | `{ type: "TEXT" \| "GIT_URL", payload, groupId? }` | `SubmissionResponse` (201) |
| POST | `/api/contents/{contentId}/submissions/file` | multipart: `file` (one PDF, at most 10 MB), `groupId?` | `SubmissionResponse` (201) |
| GET | `/api/contents/{contentId}/submissions/me` | — | `SubmissionResponse[]` (own and the user's groups') |
| GET | `/api/submissions/{submissionId}/file` | — | the file (own submission) |
| GET | `/api/submissions/{submissionId}/grade` | — | `ReleasedGradeResponse`; 404 until released |

`SubmissionResponse` (`MySubmission`) is
`{ id, contentId, type: "TEXT" | "GIT_URL" | "FILE", payload, submittedAt, late, file, groupId, submittedByUserId, members }`.
`file` is `{ name, mediaType, sizeBytes, downloadPath }` for a FILE submission, and
`members` (`{ userId, enrollmentId }[]`) the group's members when it was handed in.
Downloads need the token, so the frontend uses `apiDownload`, not a plain link.

Errors: 409 after the deadline (when late submissions are not accepted), 415 for
anything but a PDF, 413 for a file over the limit, 403 for a group the user is not in.

## Teachers of the course

| Method | Endpoint | Request body | Response |
| --- | --- | --- | --- |
| GET | `/api/contents/{contentId}/submissions/review?page&size` | — | `Page<SubmissionReviewResponse>`: the latest per student or group |
| GET | `/api/submissions/{submissionId}/review` | — | `SubmissionReviewResponse` |
| GET | `/api/submissions/{submissionId}/review/history` | — | `SubmissionReviewResponse[]`: every attempt by the same student or group |
| GET | `/api/submissions/{submissionId}/review/file` | — | the file |
| GET | `/api/submissions/{submissionId}/grade/review` | — | `GradeReviewResponse`; 404 when not graded |
| PUT | `/api/submissions/{submissionId}/grade` | `{ score, feedback, version }` | `GradeReviewResponse` |
| POST | `/api/submissions/{submissionId}/grade/release` | `{ version }` | `GradeReviewResponse` |

`SubmissionReviewResponse` (`ReviewSubmission`) is a `SubmissionResponse` plus
`studentUserId` and `enrollmentId`; names come from the course's members and groups
(`course-view/usePeopleNames.ts`). The frontend reads every page (size 100).

A grade is a **score from 0 to 100** (two decimals) with feedback.
`GradeReviewResponse` has `score, feedback, gradedAt, releasedAt, version` and who
graded and released it. Rules (the backend answers 409 otherwise):

- `version` is null to create the grade; changing it needs the current `version`.
- Releasing needs the current `version`; a released grade cannot be changed.
- Only the latest submission of a student or group can be graded.

The student sees the grade (`ReleasedGradeResponse`: `score, feedback, releasedAt`)
once it is released.
