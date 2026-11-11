
package no.hvl.dat250.lms.enrollment.service;

import java.util.NoSuchElementException;

import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public Enrollment requireActiveStudent(
            Long userId,
            Long courseId) {

        if (userId == null || userId <= 0 ||
                courseId == null || courseId <= 0) {
            throw new IllegalArgumentException(
                "Valid User ID and Course ID are required");
        }

        Enrollment enrollment = enrollmentRepository
            .findByUser_UserIdAndCourse_Id(userId, courseId)
            .orElseThrow(() -> new NoSuchElementException(
                "Enrollment not found"
            ));

        if (!enrollment.isActive()) {
            throw new IllegalStateException(
                "Enrollment is inactive");
        }

        if (enrollment.getRole() != CourseRole.STUDENT) {
            throw new IllegalStateException(
                "Enrollment does not have student role");
        }

        return enrollment;
    }
}
