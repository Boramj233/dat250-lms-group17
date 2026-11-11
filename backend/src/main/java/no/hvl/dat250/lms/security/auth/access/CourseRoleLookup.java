package no.hvl.dat250.lms.security.auth.access;

/**
 * What {@link CourseAccess} needs to know about course roles. Security only defines the
 * question; the enrollment feature answers it by implementing this interface as a Spring
 * bean (for example its EnrollmentService). Until such a bean exists, every course-scoped
 * check denies.
 */
public interface CourseRoleLookup {

    /** True if the user has role TEACHER in the course. */
    boolean isTeacher(Long userId, Long courseId);

    /** True if the user has role STUDENT in the course. */
    boolean isStudent(Long userId, Long courseId);

    /** True if the user is enrolled in the course, as TEACHER or STUDENT. */
    boolean isEnrolled(Long userId, Long courseId);
}
