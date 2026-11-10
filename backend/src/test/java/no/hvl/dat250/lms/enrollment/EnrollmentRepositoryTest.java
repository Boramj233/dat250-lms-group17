
package no.hvl.dat250.lms.enrollment;

import jakarta.persistence.EntityManager;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;

import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;

import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EnrollmentRepositoryTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EntityManager entityManager;

    private User createUser() {
        User user = userRepository.save(
            new User(
                "alice",
                "alice@example.com",
                "test-password"
            )
        );

        entityManager.flush();
        return user;
    }

    private Course createCourse(String title) {
        return courseRepository.saveAndFlush(
            new Course(
                title,
                "Test course",
                Term.AUTUMN,
                2026
            )
        );
    }

    // User currently does not expose getId().
    private Long userId(User user) {
        Object id = entityManager
            .getEntityManagerFactory()
            .getPersistenceUnitUtil()
            .getIdentifier(user);

        return ((Number) id).longValue();
    }

    @Test
    void enrollmentCanBeSavedAndRetrieved() {

        User user = createUser();
        Course course = createCourse("DAT250");

        Enrollment enrollment =
            enrollmentRepository.saveAndFlush(
                new Enrollment(
                    user,
                    course,
                    CourseRole.STUDENT
                )
            );

        Long enrollmentId = enrollment.getId();

        // Force a real database read.
        entityManager.clear();

        Enrollment found = enrollmentRepository
            .findById(enrollmentId)
            .orElseThrow();

        assertEquals(CourseRole.STUDENT, found.getRole());
        assertTrue(found.isActive());

        assertEquals(
            "alice@example.com",
            found.getUser().getEmail()
        );

        assertEquals(
            course.getId(),
            found.getCourse().getId()
        );
    }

    @Test
    void canFindEnrollmentByUserAndCourse() {

        User user = createUser();
        Course course = createCourse("DAT250");

        enrollmentRepository.saveAndFlush(
            new Enrollment(
                user,
                course,
                CourseRole.STUDENT
            )
        );

        Long uid = userId(user);
        Long cid = course.getId();

        entityManager.clear();

        Enrollment found = enrollmentRepository
            .findByUser_IdAndCourse_Id(uid, cid)
            .orElseThrow();

        assertEquals(CourseRole.STUDENT, found.getRole());

        assertTrue(enrollmentRepository
            .existsByUser_IdAndCourse_Id(uid, cid));
    }

    @Test
    void userCanEnrollInDifferentCourses() {

        User user = createUser();

        Course dat250 = createCourse("DAT250");
        Course inf264 = createCourse("INF264");

        enrollmentRepository.saveAndFlush(
            new Enrollment(
                user,
                dat250,
                CourseRole.STUDENT
            )
        );

        enrollmentRepository.saveAndFlush(
            new Enrollment(
                user,
                inf264,
                CourseRole.TEACHER
            )
        );

        Long uid = userId(user);

        entityManager.clear();

        Enrollment studentEnrollment =
            enrollmentRepository
                .findByUser_IdAndCourse_Id(
                    uid, dat250.getId())
                .orElseThrow();

        Enrollment teacherEnrollment =
            enrollmentRepository
                .findByUser_IdAndCourse_Id(
                    uid, inf264.getId())
                .orElseThrow();

        assertEquals(
            CourseRole.STUDENT,
            studentEnrollment.getRole()
        );

        assertEquals(
            CourseRole.TEACHER,
            teacherEnrollment.getRole()
        );
    }

    @Test
    void duplicateEnrollmentIsRejected() {

        User user = createUser();
        Course course = createCourse("DAT250");

        enrollmentRepository.saveAndFlush(
            new Enrollment(
                user,
                course,
                CourseRole.STUDENT
            )
        );

        assertThrows(
            DataIntegrityViolationException.class,
            () -> enrollmentRepository.saveAndFlush(
                new Enrollment(
                    user,
                    course,
                    CourseRole.TEACHER
                )
            )
        );
    }

    @Test
    void deactivationIsPersisted() {

        User user = createUser();
        Course course = createCourse("DAT250");

        Enrollment enrollment =
            enrollmentRepository.saveAndFlush(
                new Enrollment(
                    user,
                    course,
                    CourseRole.STUDENT
                )
            );

        Long enrollmentId = enrollment.getId();

        enrollment.deactivate();
        enrollmentRepository.saveAndFlush(enrollment);

        entityManager.clear();

        Enrollment found = enrollmentRepository
            .findById(enrollmentId)
            .orElseThrow();

        assertFalse(found.isActive());
    }
}
