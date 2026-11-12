package no.hvl.dat250.lms.enrollment;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {
    // Mock the repository and construct EnrollmentService when its API is defined.


    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void activeStudentCanAccessCourse() {

        Enrollment enrollment = mock(Enrollment.class);

        when(enrollment.isActive()).thenReturn(true);
        when(enrollment.getRole()).thenReturn(CourseRole.STUDENT);

        when(enrollmentRepository
                .findByUser_UserIdAndCourse_Id(1L, 2L))
            .thenReturn(Optional.of(enrollment));

        Enrollment result = enrollmentService
            .requireActiveStudent(1L, 2L);

        assertSame(enrollment, result);
    }

    @Test
    void missingEnrollmentIsRejected() {

        when(enrollmentRepository
                .findByUser_UserIdAndCourse_Id(1L, 2L))
            .thenReturn(Optional.empty());

        assertThrows(
            NoSuchElementException.class,
            () -> enrollmentService
                .requireActiveStudent(1L, 2L)
        );
    }

    @Test
    void inactiveStudentIsRejected() {

        Enrollment enrollment = mock(Enrollment.class);

        when(enrollment.isActive()).thenReturn(false);

        when(enrollmentRepository
                .findByUser_UserIdAndCourse_Id(1L, 2L))
            .thenReturn(Optional.of(enrollment));

        assertThrows(
            IllegalStateException.class,
            () -> enrollmentService
                .requireActiveStudent(1L, 2L)
        );
    }

    @Test
    void teacherCannotSubmitAsStudent() {

        Enrollment enrollment = mock(Enrollment.class);

        when(enrollment.isActive()).thenReturn(true);
        when(enrollment.getRole()).thenReturn(CourseRole.TEACHER);

        when(enrollmentRepository
                .findByUser_UserIdAndCourse_Id(1L, 2L))
            .thenReturn(Optional.of(enrollment));

        assertThrows(
            IllegalStateException.class,
            () -> enrollmentService
                .requireActiveStudent(1L, 2L)
        );
    }

    @Test
    void invalidIdsAreRejected() {

        assertThrows(
            IllegalArgumentException.class,
            () -> enrollmentService
                .requireActiveStudent(null, 2L)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> enrollmentService
                .requireActiveStudent(1L, -1L)
        );

        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    @Disabled("Not implemented yet")
    void onlyTeacherInTargetCourseCanEnrollStudents() {
        // In an existing course, allow an authenticated user with role TEACHER in
        // that course to enroll a student. Reject STUDENT, no enrollment, and TEACHER
        // only in another course. Check the actor's persisted role, not the target's.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void globalAccessDoesNotOverrideStudentEnrollmentPermission() {
        // For each Access level (STANDARD, COURSE_MANAGER, ADMIN), allow student
        // enrollment only when the actor is TEACHER in the target course.
        // Access permits course creation/initial role assignment, not ongoing enrollment.
        // Verify denied requests save nothing, including student self-enrollment.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void enrollmentRoleDoesNotChangeGlobalAccess() {
        // Assigning STUDENT or TEACHER in a course must not change User.Access.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentCanEnrollInMultipleCourses() {
        // Have an authorized teacher in each course enroll the same student in two
        // different courses; verify both memberships are accepted.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentCannotEnrollInSameCourseTwice() {
        // Given an existing student enrollment, reject a second enrollment for the
        // same user and course. Verify that no additional enrollment is saved.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void userCannotBeBothTeacherAndStudentInSameCourse() {
        // Reject adding STUDENT when the user is already TEACHER in that course,
        // and TEACHER when already STUDENT. Verify the existing role stays unchanged.
        // This rule must not prevent different roles in different courses.
        fail("Not implemented yet");
    }
}
