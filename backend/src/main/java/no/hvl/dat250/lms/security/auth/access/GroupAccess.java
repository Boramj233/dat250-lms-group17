package no.hvl.dat250.lms.security.auth.access;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Group-scoped checks for {@code @PreAuthorize}, referenced as {@code @groupAccess}.
 * The answers come from a {@link GroupMembershipLookup} bean (and {@link CourseRoleLookup}
 * for teachers); without them, every check denies.
 *
 * <ul>
 *   <li>{@link #isMember}: act as the group (submit, change the group's submissions).
 *       Only the group's own members.</li>
 *   <li>{@link #canView}: read the group's data. Members, and TEACHERs in the course the
 *       group belongs to.</li>
 * </ul>
 */
@Component("groupAccess")
public class GroupAccess {
    private final Optional<GroupMembershipLookup> groups;
    private final Optional<CourseRoleLookup> roles;

    public GroupAccess(Optional<GroupMembershipLookup> groups, Optional<CourseRoleLookup> roles) {
        this.groups = groups;
        this.roles = roles;
    }

    public boolean isMember(Authentication authentication, Long groupId) {
        Long userId = CourseAccess.userId(authentication);
        return userId != null && groupId != null
                && groups.map(lookup -> lookup.isMember(userId, groupId)).orElse(false);
    }

    public boolean canView(Authentication authentication, Long groupId) {
        if (isMember(authentication, groupId)) {
            return true;
        }
        Long userId = CourseAccess.userId(authentication);
        if (userId == null || groupId == null || groups.isEmpty() || roles.isEmpty()) {
            return false;
        }
        return groups.get().findCourseId(groupId)
                .map(courseId -> roles.get().isTeacher(userId, courseId))
                .orElse(false);
    }
}
