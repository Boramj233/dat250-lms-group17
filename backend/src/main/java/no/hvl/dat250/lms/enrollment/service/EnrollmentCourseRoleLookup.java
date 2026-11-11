package no.hvl.dat250.lms.enrollment.service;

import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;
import no.hvl.dat250.lms.security.auth.access.CourseRoleLookup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers the course-role questions behind {@code @IsTeacherInCourse},
 * {@code @IsStudentInCourse} and {@code @IsEnrolledInCourse}. Only active enrollments
 * count; a deactivated enrollment gives no access to the course.
 */
@Component
public class EnrollmentCourseRoleLookup implements CourseRoleLookup {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentCourseRoleLookup(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTeacher(Long userId, Long courseId) {
        return hasActiveRole(userId, courseId, CourseRole.TEACHER);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStudent(Long userId, Long courseId) {
        return hasActiveRole(userId, courseId, CourseRole.STUDENT);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnrolled(Long userId, Long courseId) {
        return enrollmentRepository
            .findByUser_UserIdAndCourse_Id(userId, courseId)
            .map(enrollment -> enrollment.isActive())
            .orElse(false);
    }

    private boolean hasActiveRole(Long userId, Long courseId, CourseRole role) {
        return enrollmentRepository
            .findByUser_UserIdAndCourse_Id(userId, courseId)
            .filter(enrollment -> enrollment.isActive())
            .map(enrollment -> enrollment.getRole() == role)
            .orElse(false);
    }
}
