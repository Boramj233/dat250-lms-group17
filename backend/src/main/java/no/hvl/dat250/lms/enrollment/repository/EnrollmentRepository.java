package no.hvl.dat250.lms.enrollment.repository;

import java.util.List;
import java.util.Optional;

import no.hvl.dat250.lms.enrollment.domain.Enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /** The user's active enrollments, in every course. */
    @Query("select e from Enrollment e join fetch e.course where e.user.userId = :userId and e.active = true")
    List<Enrollment> findActiveByUser(Long userId);

    /** The course's active members with their profiles, sorted by last name, first name. */
    @Query("""
        select e from Enrollment e join fetch e.user u
        where e.course.id = :courseId and e.active = true
        order by lower(u.lastName), lower(u.firstName), u.userId
        """)
    List<Enrollment> findActiveMembers(Long courseId);
}