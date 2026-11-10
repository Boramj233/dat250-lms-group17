package no.hvl.dat250.lms.security.auth.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Read access to the group given by the method's {@code groupId} parameter: members of
 * the group, and TEACHERs in the course the group belongs to. Use it on GET endpoints;
 * submitting or changing the group's work uses {@link IsGroupMember}.
 *
 * <p>Only on methods, and the method must have a parameter named {@code groupId}
 * (e.g. {@code @PathVariable Long groupId}); without it the check always denies.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@groupAccess.canView(authentication, #groupId)")
public @interface CanViewGroup {}
