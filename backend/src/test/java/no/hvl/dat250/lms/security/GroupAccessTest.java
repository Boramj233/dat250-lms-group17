package no.hvl.dat250.lms.security;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import no.hvl.dat250.lms.security.auth.access.CourseRoleLookup;
import no.hvl.dat250.lms.security.auth.access.GroupAccess;
import no.hvl.dat250.lms.security.auth.access.GroupMembershipLookup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GroupAccessTest {
    private static final long GROUP = 7L;
    private static final long COURSE = 3L;
    private static final long MEMBER = 1L;
    private static final long TEACHER = 2L;
    private static final long OTHER_STUDENT = 4L;

    @Mock
    private GroupMembershipLookup groups;
    @Mock
    private CourseRoleLookup roles;

    private GroupAccess access;

    @BeforeEach
    void setUp() {
        access = new GroupAccess(Optional.of(groups), Optional.of(roles));
        lenient().when(groups.isMember(MEMBER, GROUP)).thenReturn(true);
        lenient().when(groups.findCourseId(GROUP)).thenReturn(Optional.of(COURSE));
        lenient().when(roles.isTeacher(TEACHER, COURSE)).thenReturn(true);
    }

    @Test
    void memberCanViewAndActAsGroup() {
        assertTrue(access.canView(as(MEMBER), GROUP));
        assertTrue(access.isMember(as(MEMBER), GROUP));
    }

    @Test
    void teacherInTheGroupsCourseCanViewButNotActAsGroup() {
        assertTrue(access.canView(as(TEACHER), GROUP));
        assertFalse(access.isMember(as(TEACHER), GROUP));
    }

    @Test
    void otherStudentCanNeitherViewNorAct() {
        assertFalse(access.canView(as(OTHER_STUDENT), GROUP));
        assertFalse(access.isMember(as(OTHER_STUDENT), GROUP));
    }

    @Test
    void unknownGroupIsDenied() {
        assertFalse(access.canView(as(TEACHER), 999L));
    }

    @Test
    void callerWithoutApiPrincipalIsDenied() {
        Authentication anonymous = new TestingAuthenticationToken("someone", null);
        assertFalse(access.canView(anonymous, GROUP));
        assertFalse(access.canView(null, GROUP));
    }

    @Test
    void everythingIsDeniedUntilTheLookupsAreImplemented() {
        var withoutLookups = new GroupAccess(Optional.empty(), Optional.empty());
        assertFalse(withoutLookups.canView(as(MEMBER), GROUP));
        assertFalse(withoutLookups.isMember(as(MEMBER), GROUP));
    }

    private static Authentication as(long userId) {
        return new TestingAuthenticationToken(new AuthenticatedUser(userId), null);
    }
}
