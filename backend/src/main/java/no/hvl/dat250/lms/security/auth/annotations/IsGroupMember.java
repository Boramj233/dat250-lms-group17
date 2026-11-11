package no.hvl.dat250.lms.security.auth.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Only callers who are members of the group given by the method's {@code groupId}
 * parameter. Course role and global access (also ADMIN) do not count, so a TEACHER can
 * not submit or change the group's work. For read access use {@link CanViewGroup}.
 *
 * <p>Only on methods, and the method must have a parameter named {@code groupId}
 * (e.g. {@code @PathVariable Long groupId}); without it the check always denies.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@groupAccess.isMember(authentication, #groupId)")
public @interface IsGroupMember {}
