
package no.hvl.dat250.lms.enrollment;

import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.enrollment.domain.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnrollmentTest {

    @Test
    void enrollmentStoresUserCourseAndRole() {

        User user = mock(User.class);
        Course course = mock(Course.class);

        Enrollment enrollment = new Enrollment(
            user, course, CourseRole.STUDENT
        );

        assertSame(user, enrollment.getUser());
        assertSame(course, enrollment.getCourse());
        assertEquals(
            CourseRole.STUDENT,
            enrollment.getRole()
        );
        assertTrue(enrollment.isActive());
    }

    @Test
    void enrollmentCanBeDeactivatedAndReactivated() {

        Enrollment enrollment = new Enrollment(
            mock(User.class),
            mock(Course.class),
            CourseRole.STUDENT
        );

        enrollment.deactivate();
        assertFalse(enrollment.isActive());

        enrollment.reactivate();
        assertTrue(enrollment.isActive());
    }

    @Test
    void enrollmentRequiresUserCourseAndRole() {

        User user = mock(User.class);
        Course course = mock(Course.class);

        assertThrows(
            IllegalArgumentException.class,
            () -> new Enrollment(
                null, course, CourseRole.STUDENT)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new Enrollment(
                user, null, CourseRole.STUDENT)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new Enrollment(
                user, course, null)
        );
    }
}
