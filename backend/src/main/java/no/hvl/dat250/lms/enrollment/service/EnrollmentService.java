package no.hvl.dat250.lms.enrollment.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

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
    /**
     * The courses where the user has an active enrollment, with their role in each
     * (course id -> role). Empty when the user is in no course.
     */
    @Transactional(readOnly = true)
    public Map<Long, CourseRole> activeCourseRoles(Long userId) {
        return enrollmentRepository.findActiveByUser(userId).stream()
            .collect(Collectors.toMap(e -> e.getCourse().getId(), Enrollment::getRole, (first, second) -> first));
    }

    /**
     * The user's active enrollments with their courses, the newest term first
     * (year descending, autumn before spring), then by course title.
     */
    @Transactional(readOnly = true)
    public List<Enrollment> activeEnrollmentsWithCourses(Long userId) {
        Comparator<Enrollment> newestFirst = Comparator
            .comparingInt((Enrollment e) -> e.getCourse().getYear()).reversed()
            .thenComparing(e -> e.getCourse().getTerm(), Comparator.reverseOrder())
            .thenComparing(e -> e.getCourse().getTitle(), String.CASE_INSENSITIVE_ORDER);
        return enrollmentRepository.findActiveByUser(userId).stream().sorted(newestFirst).toList();
    }

    @Transactional(readOnly = true)
    public Enrollment requireActiveTeacher(
            Long userId,
            Long courseId) {
        if (userId == null || userId <= 0 || courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Valid User ID and Course ID are required");
        }
        return enrollmentRepository.findByUser_UserIdAndCourse_Id(userId, courseId)
            .filter(enrollment -> enrollment.isActive()
                && enrollment.getRole() == CourseRole.TEACHER)
            .orElseThrow(() -> new AccessDeniedException(
                "Active teacher enrollment required"));
    }

    // Called after the submission owner lock. Refresh defeats the first-level cache;
    // the row lock also prevents revocation between this check and grade commit.
    @Transactional(propagation = Propagation.MANDATORY)
    public Enrollment lockActiveTeacher(Long userId, Long courseId) {
        Enrollment teacher = enrollmentRepository.findByUser_UserIdAndCourse_Id(userId, courseId)
            .orElseThrow(() -> new AccessDeniedException("Active teacher enrollment required"));
        entityManager.refresh(teacher, LockModeType.PESSIMISTIC_WRITE);
        if (!teacher.isActive() || teacher.getRole() != CourseRole.TEACHER) {
            throw new AccessDeniedException("Active teacher enrollment required");
        }
        return teacher;
    }

    // Serialize submission acceptance and grading without holding a lock during object upload.
    @Transactional
    public void lockSubmissionOwner(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new NoSuchElementException("Enrollment not found"));
        entityManager.refresh(enrollment, LockModeType.PESSIMISTIC_WRITE);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Enrollment lockActiveStudent(Long userId, Long courseId) {
        return findLockedActiveStudent(userId, courseId)
            .orElseThrow(() -> new AccessDeniedException("Active student enrollment required"));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Enrollment> findLockedActiveStudent(Long userId, Long courseId) {
        return enrollmentRepository.findByUser_UserIdAndCourse_Id(userId, courseId)
            .filter(enrollment -> {
                entityManager.refresh(enrollment, LockModeType.PESSIMISTIC_WRITE);
                return enrollment.isActive() && enrollment.getRole() == CourseRole.STUDENT;
            });
    }

}
