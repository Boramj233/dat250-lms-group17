package no.hvl.dat250.lms.security.auth.access;

import java.util.Optional;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Course-scoped checks for {@code @PreAuthorize}, referenced as {@code @courseAccess}.
 * The answers come from a {@link CourseRoleLookup} bean; without one, every check denies.
 */
@Component("courseAccess")
public class CourseAccess {
    private final Optional<CourseRoleLookup> roles;

    public CourseAccess(Optional<CourseRoleLookup> roles) {
        this.roles = roles;
    }

    public boolean isTeacher(Authentication authentication, Long courseId) {
        Long userId = userId(authentication);
        return userId != null && courseId != null
                && roles.map(lookup -> lookup.isTeacher(userId, courseId)).orElse(false);
    }

    public boolean isMember(Authentication authentication, Long courseId) {
        Long userId = userId(authentication);
        return userId != null && courseId != null
                && roles.map(lookup -> lookup.isMember(userId, courseId)).orElse(false);
    }

    /** The caller's user ID, or null without an API token. Also used by GroupAccess. */
    static Long userId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        return null;
    }
}
