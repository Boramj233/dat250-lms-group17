# Groups API contract

`GET /api/courses/{courseId}/groups` exists in the backend (`GroupController`) and
returns `GroupDTO[]`: `{ id, groupNumber, memberIds }`. The backend also has
`GET /api/courses/{courseId}/groups/{groupId}`, `POST` and `DELETE` for groups, and
`POST`/`DELETE` for their members; the frontend does not use those yet.

**Proposed** additions to `GroupDTO`, so the Groups page of a course can show names:

| Field | Type | Meaning |
| --- | --- | --- |
| `name` | `string \| null` | A name for the group. Null: the frontend shows "Group {groupNumber}". |
| `members` | `{ userId, firstName, lastName }[]` | The members' names. Students cannot look users up, so without this they only see user ids. |

Every member of the course (students and teachers) may list its groups. Types are in
`types.ts`; `useCourseGroups(courseId)` fetches them, sorted by group number.
