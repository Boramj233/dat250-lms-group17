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

## Proposed: publishing grades (visible date and publish all)

The UI says "publish" for what the backend calls "release". A saved grade is hidden
from the student until it is published. **Each grade has its own visible date**; it
has nothing to do with the assignment's dates.

1. **Visible date** (proposed): `visibleFrom` (ISO instant or null) on
   `SaveGradeRequest` and `GradeReviewResponse`. At that time the backend publishes
   the grade by itself, with `visibleFrom` as `releasedAt` (e.g. a scheduled job).
   Null: hidden until published. A time that is not in the future: 400.
2. **Publish** one grade now: `POST /api/submissions/{id}/grade/release` (exists). Its
   `releasedAt` is the time it became visible.
3. **Publish all** (proposed): `POST /api/contents/{contentId}/grades/release` publishes
   every saved, unpublished grade of the assignment's latest submissions now (also those
   with a later visible date) and answers `{ released }` (how many). Teachers only.

A published grade cannot be changed.

## Proposed: passed / not passed

The teacher also marks a submission as passed or not passed. The frontend sends and
shows it already; the backend needs one new field, `passed: Boolean` (nullable):

- `SaveGradeRequest` (PUT `/grade`): `{ score, feedback, passed, version }`. `true` is
  passed, `false` not passed, `null` not decided yet. Anything else: 400.
- `GradeReviewResponse` and `ReleasedGradeResponse`: `passed`. The student only sees it
  once the grade is released, like the score; the same release rules apply.

Until the backend sends it, the field is missing and the frontend shows no verdict.
