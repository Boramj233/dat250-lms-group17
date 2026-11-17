package no.hvl.dat250.lms.enrollment.service;

import java.util.List;
import java.util.NoSuchElementException;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.service.CourseService;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A course's members: who is enrolled, adding and removing them and changing their
 * course role. Who may call what is decided on the controller.
 *
 * <p>Removing a member deactivates the enrollment rather than deleting it, because
 * submissions and grades point to it. Enrolling a removed member again reactivates
 * the same enrollment.
 */
@Service
public class EnrollmentManagementService {
    private final EnrollmentRepository enrollments;
    private final CourseService courses;
    private final UserService users;

    public EnrollmentManagementService(EnrollmentRepository enrollments, CourseService courses,
                                       UserService users) {
        this.enrollments = enrollments;
        this.courses = courses;
        this.users = users;
    }

    /**
     * The course's active members with their profiles, sorted by name.
     *
     * @throws NoSuchElementException if the course does not exist
     */
    @Transactional(readOnly = true)
    public List<Enrollment> members(Long courseId) {
        requireCourse(courseId);
        return enrollments.findActiveMembers(courseId);
    }

    /**
     * Enrolls the user with this role (STUDENT when null). A removed member is enrolled
     * again with the new role.
     *
     * @throws NoSuchElementException if the course does not exist
     * @throws no.hvl.dat250.lms.user.service.UserNotFoundException if the user does not exist
     * @throws EnrollmentAlreadyExistsException if the user is already an active member
     */
    @Transactional
    public Enrollment enroll(Long courseId, Long userId, CourseRole role) {
        Course course = requireCourse(courseId);
        User user = users.getUser(userId);
        CourseRole chosen = role == null ? CourseRole.STUDENT : role;
        var existing = enrollments.findByUser_UserIdAndCourse_Id(userId, courseId);
        if (existing.isPresent()) {
            Enrollment enrollment = existing.get();
            if (enrollment.isActive()) {
                throw new EnrollmentAlreadyExistsException();
            }
            enrollment.reactivate();
            enrollment.changeRole(chosen);
            return enrollment;
        }
        // The unique (user, course) constraint catches a concurrent enroll (409).
        return enrollments.saveAndFlush(new Enrollment(user, course, chosen));
    }

    /**
     * Makes an active member a teacher or a student.
     *
     * @throws NoSuchElementException if the user is not an active member of the course
     */
    @Transactional
    public Enrollment changeRole(Long courseId, Long userId, CourseRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        Enrollment enrollment = requireActiveMember(courseId, userId);
        enrollment.changeRole(role);
        return enrollment;
    }

    /**
     * Removes an active member from the course (deactivates the enrollment).
     *
     * @throws NoSuchElementException if the user is not an active member of the course
     */
    @Transactional
    public void remove(Long courseId, Long userId) {
        requireActiveMember(courseId, userId).deactivate();
    }

    private Course requireCourse(Long courseId) {
        return courses.findCourseById(courseId)
            .orElseThrow(() -> new NoSuchElementException("Course not found"));
    }

    private Enrollment requireActiveMember(Long courseId, Long userId) {
        return enrollments.findByUser_UserIdAndCourse_Id(userId, courseId)
            .filter(Enrollment::isActive)
            .orElseThrow(() -> new NoSuchElementException("Not a member of this course"));
    }
}
