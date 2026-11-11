package no.hvl.dat250.lms.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.Optional;
import no.hvl.dat250.lms.security.auth.access.CourseAccess;
import no.hvl.dat250.lms.security.auth.access.CourseRoleLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class CourseAccessTest {
    private static final long COURSE = 3L;
    private static final long STUDENT = 1L;
    private static final long TEACHER = 2L;
    private static final long OUTSIDER = 4L;

    @Mock
    private CourseRoleLookup roles;

    private CourseAccess access;

    @BeforeEach
    void setUp() {
        access = new CourseAccess(Optional.of(roles));
        lenient().when(roles.isStudent(STUDENT, COURSE)).thenReturn(true);
        lenient().when(roles.isTeacher(TEACHER, COURSE)).thenReturn(true);
        lenient().when(roles.isEnrolled(STUDENT, COURSE)).thenReturn(true);
        lenient().when(roles.isEnrolled(TEACHER, COURSE)).thenReturn(true);
    }

    @Test
    void studentIsStudentAndEnrolledButNotTeacher() {
        assertTrue(access.isStudent(as(STUDENT), COURSE));
        assertTrue(access.isEnrolled(as(STUDENT), COURSE));
        assertFalse(access.isTeacher(as(STUDENT), COURSE));
    }

    @Test
    void teacherIsTeacherAndEnrolledButNotStudent() {
        assertTrue(access.isTeacher(as(TEACHER), COURSE));
        assertTrue(access.isEnrolled(as(TEACHER), COURSE));
        assertFalse(access.isStudent(as(TEACHER), COURSE));
    }

    @Test
    void outsiderIsDeniedEverything() {
        assertFalse(access.isStudent(as(OUTSIDER), COURSE));
        assertFalse(access.isTeacher(as(OUTSIDER), COURSE));
        assertFalse(access.isEnrolled(as(OUTSIDER), COURSE));
    }

    @Test
    void missingCourseIdOrApiPrincipalIsDenied() {
        assertFalse(access.isStudent(as(STUDENT), null));
        assertFalse(access.isStudent(new TestingAuthenticationToken("someone", null), COURSE));
        assertFalse(access.isStudent(null, COURSE));
    }

    @Test
    void everythingIsDeniedUntilTheLookupIsImplemented() {
        var withoutLookup = new CourseAccess(Optional.empty());
        assertFalse(withoutLookup.isStudent(as(STUDENT), COURSE));
        assertFalse(withoutLookup.isTeacher(as(TEACHER), COURSE));
        assertFalse(withoutLookup.isEnrolled(as(STUDENT), COURSE));
    }

    private static Authentication as(long userId) {
        return new TestingAuthenticationToken(new AuthenticatedUser(userId), null);
    }
}
