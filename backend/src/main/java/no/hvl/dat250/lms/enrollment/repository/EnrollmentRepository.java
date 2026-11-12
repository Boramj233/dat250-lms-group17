package no.hvl.dat250.lms.enrollment.repository;

import java.util.List;
import java.util.Optional;

import no.hvl.dat250.lms.enrollment.domain.Enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository
        extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUser_UserIdAndCourse_Id(
        Long userId,
        Long courseId
    );

    boolean existsByUser_UserIdAndCourse_Id(
        Long userId,
        Long courseId
    );

    List<Enrollment> findByCourse_Id(
        Long courseId
    );
}