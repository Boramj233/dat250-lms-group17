package no.hvl.dat250.lms.security.auth.access;

import java.util.Optional;

/**
 * What {@link GroupAccess} needs to know about groups. Security only defines the
 * question; the group feature answers it by implementing this interface as a Spring bean
 * (for example its GroupService). Until such a bean exists, every group check denies.
 */
public interface GroupMembershipLookup {

    /** True if the user is a member of the group. */
    boolean isMember(Long userId, Long groupId);

    /** The ID of the course the group belongs to, or empty if the group does not exist. */
    Optional<Long> findCourseId(Long groupId);
}
