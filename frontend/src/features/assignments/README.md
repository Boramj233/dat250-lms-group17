# Submissions API contract

The backend has the student's side (`SubmissionController`):

| Method | Endpoint | Request body | Response |
| --- | --- | --- | --- |
| POST | `/api/contents/{contentId}/submissions` | `{ type: "TEXT" \| "GIT_URL", payload }` | `SubmissionResponse` |
| GET | `/api/contents/{contentId}/submissions/me` | — | `SubmissionResponse[]` |
| GET | `/api/submissions/{submissionId}` | — | `SubmissionResponse` (own only) |

`SubmissionResponse` is `{ id, contentId, type, payload, submittedAt, late }`
(`MySubmission` in `types.ts`).

## Proposed

For students:

| Method | Endpoint | Response |
| --- | --- | --- |
| GET | `/api/submissions/{submissionId}/result` | `SubmissionResult`: `{ submissionId, status, grade, testResults, errorMessage }`; `grade` is null until published |

For teachers of the course (the course's Submissions pages):

| Method | Endpoint | Request body | Response |
| --- | --- | --- | --- |
| GET | `/api/contents/{contentId}/submissions` | — | `ContentSubmission[]` |
| PUT | `/api/submissions/{submissionId}/grade` | `{ grade, feedback }` | `ContentSubmission` |
| POST | `/api/submissions/{submissionId}/grade/publish` | — | `ContentSubmission` |

`ContentSubmission` is a `SubmissionResponse` plus
`student: { userId, firstName, lastName, email }` (who handed it in),
`group` (see content/README.md; null for an individual submission) and
`grade: { value, feedback, publishedAt } | null`. The list has **every** submission
(all attempts), not just the latest per student; the frontend counts students who
answered (group members count for a group submission), submissions and graded ones.

Saving a grade does not show it to the student: `publishedAt` stays null until the
teacher publishes it. Saving again after publishing hides it until it is published
again. The grade is free text chosen by the teacher, e.g. "Passed", "Not passed" or "B".
