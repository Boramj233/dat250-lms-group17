package no.hvl.dat250.lms.group.service;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.service.CourseService;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.repository.GroupRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserNotFoundException;
import no.hvl.dat250.lms.user.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    private static final Long COURSE_ID = 10L;
    private static final Long GROUP_ID = 20L;
    private static final Long USER_ID = 30L;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private UserService userService;

    @InjectMocks
    private GroupService service;

    private Course course;
    private Group group;
    private User user;

    @BeforeEach
    void setUp() {
        course = mock(Course.class);
        group = new Group(2, course);
        user = mock(User.class);
    }

    // ---------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------

    private void givenGroupExists() {
        when(groupRepository.findByIdAndCourseId(GROUP_ID, COURSE_ID))
                .thenReturn(Optional.of(group));
    }

    private void givenUserExists() {
        when(userService.getUser(USER_ID))
                .thenReturn(user);
    }

    private static void assertStatus(
            HttpStatus status,
            ExecutableAction action
    ) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                action::run
        );

        assertEquals(status, exception.getStatusCode());
    }

    @FunctionalInterface
    private interface ExecutableAction {
        void run();
    }

    // ---------------------------------------------------------
    // GET ALL GROUPS
    // ---------------------------------------------------------

    @Test
    void getAllGroupsMapsResultsToDtos() {
        when(groupRepository.findByCourseId(COURSE_ID))
                .thenReturn(List.of(group));

        var result = service.getAllGroups(COURSE_ID);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).groupNumber());
        assertTrue(result.get(0).memberIds().isEmpty());

        verify(groupRepository).findByCourseId(COURSE_ID);
    }

    // ---------------------------------------------------------
    // GET GROUP BY ID
    // ---------------------------------------------------------

    @Test
    void getGroupByIdReturnsDto() {
        givenGroupExists();

        var result = service.getGroupById(COURSE_ID, GROUP_ID);

        assertEquals(2, result.groupNumber());

        verify(groupRepository)
                .findByIdAndCourseId(GROUP_ID, COURSE_ID);
    }

    @Test
    void getGroupByIdReturns404WhenMissing() {
        when(groupRepository.findByIdAndCourseId(GROUP_ID, COURSE_ID))
                .thenReturn(Optional.empty());

        assertStatus(
                HttpStatus.NOT_FOUND,
                () -> service.getGroupById(COURSE_ID, GROUP_ID)
        );
    }

    // ---------------------------------------------------------
    // CREATE GROUP
    // ---------------------------------------------------------

    @Test
    void createGroupSavesNewGroup() {
        when(courseService.findCourseById(COURSE_ID))
                .thenReturn(Optional.of(course));

        when(groupRepository.save(any(Group.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createGroup(COURSE_ID, 3);

        assertEquals(3, result.groupNumber());

        verify(groupRepository)
                .existsByCourseIdAndGroupNumber(COURSE_ID, 3);

        verify(groupRepository).save(argThat(g ->
                g.getGroupNumber() == 3 &&
                        g.getCourse() == course
        ));
    }

    @Test
    void createGroupReturns404WhenCourseMissing() {
        when(courseService.findCourseById(COURSE_ID))
                .thenReturn(Optional.empty());

        assertStatus(
                HttpStatus.NOT_FOUND,
                () -> service.createGroup(COURSE_ID, 3)
        );

        verify(groupRepository, never()).save(any());
    }

    @Test
    void createGroupRejectsInvalidNumber() {
        when(courseService.findCourseById(COURSE_ID))
                .thenReturn(Optional.of(course));

        assertStatus(
                HttpStatus.BAD_REQUEST,
                () -> service.createGroup(COURSE_ID, 0)
        );

        assertStatus(
                HttpStatus.BAD_REQUEST,
                () -> service.createGroup(COURSE_ID, null)
        );

        verify(groupRepository, never()).save(any());
    }

    @Test
    void createGroupRejectsDuplicateNumber() {
        when(courseService.findCourseById(COURSE_ID))
                .thenReturn(Optional.of(course));

        when(groupRepository.existsByCourseIdAndGroupNumber(COURSE_ID, 2))
                .thenReturn(true);

        assertStatus(
                HttpStatus.CONFLICT,
                () -> service.createGroup(COURSE_ID, 2)
        );

        verify(groupRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // DELETE GROUP
    // ---------------------------------------------------------

    @Test
    void deleteGroupDeletesFoundGroup() {
        givenGroupExists();

        service.deleteGroup(COURSE_ID, GROUP_ID);

        verify(groupRepository).delete(group);
    }

    @Test
    void deleteGroupReturns404WhenMissing() {
        when(groupRepository.findByIdAndCourseId(GROUP_ID, COURSE_ID))
                .thenReturn(Optional.empty());

        assertStatus(
                HttpStatus.NOT_FOUND,
                () -> service.deleteGroup(COURSE_ID, GROUP_ID)
        );

        verify(groupRepository, never()).delete(any());
    }

    // ---------------------------------------------------------
    // ADD GROUP MEMBER
    // ---------------------------------------------------------

    @Test
    void addGroupMemberAddsUserAndReturnsMemberId() {
        givenGroupExists();
        givenUserExists();

        when(user.getUserId()).thenReturn(USER_ID);

        var result = service.addGroupMember(
                COURSE_ID,
                GROUP_ID,
                USER_ID
        );

        assertTrue(group.getGroupMembers().contains(user));

        assertEquals(
                Set.of(USER_ID),
                result.memberIds()
        );

        verify(groupRepository)
                .existsUserInCourseGroup(COURSE_ID, USER_ID);

        verify(groupRepository, never()).save(any());
    }

    @Test
    void addGroupMemberRejectsExistingMember() {
        group.addGroupMember(user);

        givenGroupExists();
        givenUserExists();

        assertStatus(
                HttpStatus.CONFLICT,
                () -> service.addGroupMember(
                        COURSE_ID,
                        GROUP_ID,
                        USER_ID
                )
        );

        verify(groupRepository, never())
                .existsUserInCourseGroup(any(), any());
    }

    @Test
    void addGroupMemberRejectsUserInAnotherGroup() {
        givenGroupExists();
        givenUserExists();

        when(user.getUserId()).thenReturn(USER_ID);

        when(groupRepository.existsUserInCourseGroup(COURSE_ID, USER_ID))
                .thenReturn(true);

        assertStatus(
                HttpStatus.CONFLICT,
                () -> service.addGroupMember(
                        COURSE_ID,
                        GROUP_ID,
                        USER_ID
                )
        );

        assertTrue(group.getGroupMembers().isEmpty());
    }

    @Test
    void addGroupMemberReturns404WhenUserMissing() {
        givenGroupExists();

        when(userService.getUser(USER_ID))
                .thenThrow(new UserNotFoundException());

        assertStatus(
                HttpStatus.NOT_FOUND,
                () -> service.addGroupMember(
                        COURSE_ID,
                        GROUP_ID,
                        USER_ID
                )
        );
    verify(userService).getUser(USER_ID);
    }

    // ---------------------------------------------------------
    // REMOVE GROUP MEMBER
    // ---------------------------------------------------------

    @Test
    void removeGroupMemberRemovesExistingUser() {
        group.addGroupMember(user);

        givenGroupExists();
        givenUserExists();

        service.removeGroupMember(
                COURSE_ID,
                GROUP_ID,
                USER_ID
        );

        assertFalse(group.getGroupMembers().contains(user));

        verify(groupRepository, never()).save(any());
    }

    @Test
    void removeGroupMemberRejectsNonMember() {
        givenGroupExists();
        givenUserExists();

        assertStatus(
                HttpStatus.CONFLICT,
                () -> service.removeGroupMember(
                        COURSE_ID,
                        GROUP_ID,
                        USER_ID
                )
        );
    }
}