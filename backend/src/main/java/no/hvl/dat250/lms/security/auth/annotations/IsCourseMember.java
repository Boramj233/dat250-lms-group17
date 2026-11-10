package no.hvl.dat250.lms.security.auth.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Callers enrolled in the course given by the method's {@code courseId} parameter, as
 * TEACHER or STUDENT. Global access (also ADMIN) does not count.
 *
 * <p>Only on methods, and the method must have a parameter named {@code courseId}
 * (e.g. {@code @PathVariable Long courseId}); without it the check always denies.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@courseAccess.isMember(authentication, #courseId)")
public @interface IsCourseMember {}
