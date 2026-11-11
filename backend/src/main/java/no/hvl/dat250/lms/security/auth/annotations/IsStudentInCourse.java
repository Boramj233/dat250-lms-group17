package no.hvl.dat250.lms.security.auth.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Only callers with role STUDENT in the course given by the method's {@code courseId}
 * parameter, e.g. for submitting work. TEACHERs in the course and global access (also
 * ADMIN) do not count; for read access shared with teachers use {@link IsEnrolledInCourse}.
 *
 * <p>Only on methods, and the method must have a parameter named {@code courseId}
 * (e.g. {@code @PathVariable Long courseId}); without it the check always denies.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@courseAccess.isStudent(authentication, #courseId)")
public @interface IsStudentInCourse {}
