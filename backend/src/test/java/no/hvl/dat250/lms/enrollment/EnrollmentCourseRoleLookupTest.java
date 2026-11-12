package no.hvl.dat250.lms.enrollment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;
import no.hvl.dat250.lms.enrollment.service.EnrollmentCourseRoleLookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentCourseRoleLookupTest {

    private static final Long USER_ID = 1L;
    private static final Long COURSE_ID = 2L;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private EnrollmentCourseRoleLookup lookup;

    private void enrolled(CourseRole role, boolean active) {
        Enrollment enrollment = mock(Enrollment.class);
        when(enrollment.isActive()).thenReturn(active);
        // The role is only read for active enrollments in isTeacher.
        if (active && role != null) {
            when(enrollment.getRole()).thenReturn(role);
        }
        when(enrollmentRepository.findByUser_UserIdAndCourse_Id(USER_ID, COURSE_ID))
            .thenReturn(Optional.of(enrollment));
    }

    private void notEnrolled() {
        when(enrollmentRepository.findByUser_UserIdAndCourse_Id(USER_ID, COURSE_ID))
            .thenReturn(Optional.empty());
    }

    @Test
    void activeTeacherIsTeacher() {
        enrolled(CourseRole.TEACHER, true);
        assertTrue(lookup.isTeacher(USER_ID, COURSE_ID));
    }

    @Test
    void activeStudentIsNotTeacher() {
        enrolled(CourseRole.STUDENT, true);
        assertFalse(lookup.isTeacher(USER_ID, COURSE_ID));
    }

    @Test
    void deactivatedTeacherIsNotTeacher() {
        enrolled(CourseRole.TEACHER, false);
        assertFalse(lookup.isTeacher(USER_ID, COURSE_ID));
    }

    @Test
    void notEnrolledIsNotTeacher() {
        notEnrolled();
        assertFalse(lookup.isTeacher(USER_ID, COURSE_ID));
    }

    @Test
    void activeStudentIsStudent() {
        enrolled(CourseRole.STUDENT, true);
        assertTrue(lookup.isStudent(USER_ID, COURSE_ID));
    }

    @Test
    void activeTeacherIsNotStudent() {
        enrolled(CourseRole.TEACHER, true);
        assertFalse(lookup.isStudent(USER_ID, COURSE_ID));
    }

    @Test
    void deactivatedStudentIsNotStudent() {
        enrolled(CourseRole.STUDENT, false);
        assertFalse(lookup.isStudent(USER_ID, COURSE_ID));
    }

    @Test
    void notEnrolledIsNotStudent() {
        notEnrolled();
        assertFalse(lookup.isStudent(USER_ID, COURSE_ID));
    }

    @Test
    void activeEnrollmentIsEnrolled() {
        enrolled(null, true);
        assertTrue(lookup.isEnrolled(USER_ID, COURSE_ID));
    }

    @Test
    void deactivatedEnrollmentIsNotEnrolled() {
        enrolled(null, false);
        assertFalse(lookup.isEnrolled(USER_ID, COURSE_ID));
    }

    @Test
    void missingEnrollmentIsNotEnrolled() {
        notEnrolled();
        assertFalse(lookup.isEnrolled(USER_ID, COURSE_ID));
    }
}
