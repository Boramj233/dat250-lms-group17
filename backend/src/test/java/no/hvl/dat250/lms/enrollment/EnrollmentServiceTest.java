package no.hvl.dat250.lms.enrollment;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {
    // Mock the repository and construct EnrollmentService when its API is defined.

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
