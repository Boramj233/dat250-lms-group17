# Groups API contract

The backend's `GroupController` (`/api/courses/{courseId}/groups`). `GroupDTO` is
`{ id, groupNumber, memberIds }`; types are in `types.ts`.

| Method | Endpoint                                                    | Body                  | Who                                            | Frontend          |
| ------ | ----------------------------------------------------------- | --------------------- | ---------------------------------------------- | ----------------- |
| GET    | `/api/courses/{courseId}/groups`                            | —                     | members of the course                          | `useCourseGroups` |
| POST   | `/api/courses/{courseId}/groups`                            | `{ groupNumber }`     | teachers                                       | `useCreateGroup`  |
| DELETE | `/api/courses/{courseId}/groups/{groupId}`                  | —                     | teachers                                       | `useDeleteGroup`  |
| POST   | `/api/courses/{courseId}/groups/{groupId}/members`          | the user id, e.g. `5` | teachers, **proposed: the student themselves** | `useJoinGroup`    |
| DELETE | `/api/courses/{courseId}/groups/{groupId}/members/{userId}` | —                     | teachers, **proposed: the student themselves** | `useLeaveGroup`   |

Rules the backend already has (the frontend shows a message for each):

- Only active students can be members, and a student is in **at most one group per
  course**: joining a second group answers 409. To change group, the frontend asks the
  student in a pop-up, then leaves the old group and joins the new one
  (`useSwitchGroup`; if the join fails it puts them back in the old group).
- A new group starts empty. 409 when the group number is taken, 400 below 1.
- A group with history (e.g. submissions) cannot be deleted: 409.

## Needed in the backend

Students join and leave groups themselves, but the two member endpoints are
`@IsTeacherInCourse`, so a student gets 403 today. They should also allow a student of
the course when `userId` is the logged-in user (joining or leaving themselves). The mock
backend (`npm run dev:mock`) already works like this.

## Proposed additions to `GroupDTO`

So the Groups page of a course can show names:

| Field     | Type                                | Meaning                                                                                    |
| --------- | ----------------------------------- | ------------------------------------------------------------------------------------------ |
| `name`    | `string \| null`                    | A name for the group. Null: the frontend shows "Group {groupNumber}".                      |
| `members` | `{ userId, firstName, lastName }[]` | The members' names. Students cannot look users up, so without this they only see user ids. |
