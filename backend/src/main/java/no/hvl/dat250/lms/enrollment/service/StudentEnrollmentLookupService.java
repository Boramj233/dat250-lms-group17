package no.hvl.dat250.lms.enrollment.service;

import java.util.NoSuchElementException;

import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentEnrollmentLookupService {

    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollmentRepository;

    public StudentEnrollmentLookupService(
            EnrollmentService enrollmentService,
            EnrollmentRepository enrollmentRepository) {

        this.enrollmentService = enrollmentService;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public Enrollment requireActiveStudentEnrollment(
            Long userId,
            Long courseId) {

        // Reuse the existing business authorization rules.
        enrollmentService.requireActiveStudent(
            userId,
            courseId
        );

        // Return the Enrollment needed for the Submission FK.
        return enrollmentRepository
            .findByUser_IdAndCourse_Id(userId, courseId)
            .orElseThrow(() -> new NoSuchElementException(
                "Enrollment not found for user "
                    + userId + " in course " + courseId
            ));
    }
}
