# Groups API contract

The backend's `GroupController` (`/api/courses/{courseId}/groups`). `GroupDTO` is
`{ id, groupNumber, memberIds }`; types are in `types.ts`.

| Method | Endpoint                                                    | Body                  | Who                   | Frontend               |
| ------ | ----------------------------------------------------------- | --------------------- | --------------------- | ---------------------- |
| GET    | `/api/courses/{courseId}/groups`                            | —                     | members of the course | `useCourseGroups`      |
| POST   | `/api/courses/{courseId}/groups`                            | `{ groupNumber }`     | teachers              | `useCreateGroup`       |
| DELETE | `/api/courses/{courseId}/groups/{groupId}`                  | —                     | teachers              | `useDeleteGroup`       |
| POST   | `/api/courses/{courseId}/groups/{groupId}/members`          | the user id, e.g. `5` | teachers              | `useAddGroupMember`    |
| DELETE | `/api/courses/{courseId}/groups/{groupId}/members/{userId}` | —                     | teachers              | `useRemoveGroupMember` |

Rules the backend has (the frontend shows a message for each):

- Only teachers change members. On the Groups page a teacher adds a student who is in
  no group to a group, and removes members. Students only see the groups.
- Only active students can be members, and a student is in **at most one group per
  course**: adding them to a second group answers 409.
- A new group starts empty. 409 when the group number is taken, 400 below 1.
- A group with history (e.g. submissions) cannot be deleted: 409.

Groups have no name: the frontend shows "Group {groupNumber}". Members' names come from
the course's members (`GET /api/courses/{courseId}/enrollments`), which every member of
the course may read.

## Handing in as a group

A member of a group may hand in for it on any assignment or coding exercise: the
submission form asks "Only me" or "Group N" (see `../content/README.md`).
