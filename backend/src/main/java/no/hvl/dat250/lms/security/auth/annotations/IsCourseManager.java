package no.hvl.dat250.lms.security.auth.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Callers with global access COURSE_MANAGER or higher (ADMIN is included through the
 * role hierarchy in {@code SecurityConfig}). This is global access only; it says nothing
 * about the caller's role inside a particular course.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('COURSE_MANAGER')")
public @interface IsCourseManager {
    

}
