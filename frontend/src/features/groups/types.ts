import type { GroupId, UserId } from "../../shared/types";

// Matches the backend's GroupDTO (GET /api/courses/{courseId}/groups), plus two
// proposed fields (see README.md).
export interface CourseGroup {
  id: GroupId;
  groupNumber: number;
  memberIds: UserId[];
  /** Proposed: a name for the group. Missing or null: shown as "Group {groupNumber}". */
  name?: string | null;
  /**
   * Proposed: the members' names, so students (who cannot look users up) see who is
   * in a group. Missing until the backend sends it.
   */
  members?: GroupMember[];
}

export interface GroupMember {
  userId: UserId;
  firstName: string;
  lastName: string;
}
