package no.hvl.dat250.lms.group;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.service.CourseService;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import no.hvl.dat250.lms.group.api.dto.GroupDTO;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.repository.GroupRepository;
import no.hvl.dat250.lms.group.service.GroupService;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private UserService userService;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private GroupService groupService;

    private Course course;
    private Group group;
    private User user;

    private final Long courseId = 1L;
    private final Long groupId = 10L;
    private final Long userId = 100L;

    @BeforeEach
    void setUp() {
        course = mock(Course.class);
        lenient().when(course.getId()).thenReturn(courseId);

        group = new Group(1, course, 3);

        user = mock(User.class);
    }

    // --------------------------------------------------
    // GET GROUPS
    // --------------------------------------------------

    @Test
    void getAllGroupsReturnsGroups() {
        when(groupRepository.findByCourseId(courseId))
                .thenReturn(List.of(group));

        List<GroupDTO> result =
                groupService.getAllGroups(courseId);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).groupNumber());

        verify(groupRepository).findByCourseId(courseId);
    }

    @Test
    void getAllGroupsReturnsEmptyList() {
        when(groupRepository.findByCourseId(courseId))
                .thenReturn(List.of());

        List<GroupDTO> result =
                groupService.getAllGroups(courseId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getGroupByIdReturnsGroup() {
        when(groupRepository.findByIdAndCourseId(groupId, courseId))
                .thenReturn(Optional.of(group));

        GroupDTO result =
                groupService.getGroupById(courseId, groupId);

        assertEquals(1, result.groupNumber());
        assertEquals(courseId, result.courseId());
    }

    @Test
    void getGroupByIdThrowsWhenGroupDoesNotExist() {
        when(groupRepository.findByIdAndCourseId(groupId, courseId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> groupService.getGroupById(courseId, groupId)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }

    // --------------------------------------------------
    // CREATE GROUP
    // --------------------------------------------------

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = 3)
    void createGroupPreservesOptionalCapacity(Integer maxGroupSize) {
        when(courseService.findCourseById(courseId))
                .thenReturn(Optional.of(course));

        when(groupRepository.existsByCourseIdAndGroupNumber(courseId, 1))
                .thenReturn(false);

        when(groupRepository.save(any(Group.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GroupDTO result =
                groupService.createGroup(courseId, 1, maxGroupSize);

        assertEquals(1, result.groupNumber());
        assertEquals(maxGroupSize, result.maxGroupSize());
        assertEquals(courseId, result.courseId());

        verify(groupRepository).save(any(Group.class));
    }

    @Test
    void createGroupRejectsInvalidGroupNumber() {
        when(courseService.findCourseById(courseId))
                .thenReturn(Optional.of(course));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> groupService.createGroup(courseId, 0, 3)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verify(groupRepository, never()).save(any());
    }

    @Test
    void createGroupRejectsDuplicateGroupNumber() {
        when(courseService.findCourseById(courseId))
                .thenReturn(Optional.of(course));

        when(groupRepository.existsByCourseIdAndGroupNumber(courseId, 1))
                .thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> groupService.createGroup(courseId, 1, 3)
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(groupRepository, never()).save(any());
    }

    @Test
    void createGroupRejectsUnknownCourse() {
        when(courseService.findCourseById(courseId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> groupService.createGroup(courseId, 1, 3)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }

    // --------------------------------------------------
    // ADD MEMBERS
    // --------------------------------------------------

    @Test
    void addGroupMemberSuccessfully() {
        when(groupRepository.findByIdAndCourseId(groupId, courseId))
                .thenReturn(Optional.of(group));

        when(userService.getUser(userId))
                .thenReturn(user);

        when(groupRepository.existsUserInCourseGroup(courseId, userId))
                .thenReturn(false);

        when(user.getUserId()).thenReturn(userId);

        GroupDTO result =
                groupService.addGroupMember(courseId, groupId, userId);

        assertEquals(1, result.memberIds().size());
        assertTrue(result.memberIds().contains(userId));

        verify(enrollmentService)
                .lockActiveStudent(userId, courseId);

        verify(entityManager)
                .refresh(group, LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void addGroupMemberRejectsDuplicateMembership() {
        group.addGroupMember(user);

        when(groupRepository.findByIdAndCourseId(groupId, courseId))
                .thenReturn(Optional.of(group));

        when(userService.getUser(userId))
                .thenReturn(user);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> groupService.addGroupMember(
                                courseId, groupId, userId
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(groupRepository, never())
                .existsUserInCourseGroup(anyLong(), anyLong());
    }

    @Test
    void addGroupMemberRejectsUserAlreadyInAnotherGroup() {
        when(groupRepository.findByIdAndCourseId(groupId, courseId))
                .thenReturn(Optional.of(group));

        when(userService.getUser(userId))
                .thenReturn(user);

        when(groupRepository.existsUserInCourseGroup(courseId, userId))
                .thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> groupService.addGroupMember(
                                courseId, groupId, userId
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertTrue(group.getGroupMembers().isEmpty());
    }

    @Test
    void addGroupMemberRejectsFullGroup() {
        Group fullGroup = new Group(1, course, 1);

        User existingUser = mock(User.class);
        fullGroup.addGroupMember(existingUser);

        when(groupRepository.findByIdAndCourseId(groupId, courseId))
                .thenReturn(Optional.of(fullGroup));

        when(userService.getUser(userId))
                .thenReturn(user);

        when(groupRepository.existsUserInCourseGroup(courseId, userId))
                .thenReturn(false);

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> groupService.addGroupMember(courseId, groupId, userId)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Group has reached maximum capacity", exception.getReason());

        assertEquals(1, fullGroup.getGroupMembers().size());
    }

    // --------------------------------------------------
    // REMOVE MEMBERS
    // --------------------------------------------------

    @Test
    void removeGroupMemberSuccessfully() {
        group.addGroupMember(user);

        when(groupRepository.findByIdAndCourseId(groupId, courseId))
                .thenReturn(Optional.of(group));

        when(userService.getUser(userId))
                .thenReturn(user);

        groupService.removeGroupMember(courseId, groupId, userId);

        assertTrue(group.getGroupMembers().isEmpty());
    }

    @Test
    void removeGroupMemberRejectsNonMember() {
        when(groupRepository.findByIdAndCourseId(groupId, courseId))
                .thenReturn(Optional.of(group));

        when(userService.getUser(userId))
                .thenReturn(user);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> groupService.removeGroupMember(
                                courseId, groupId, userId
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );
    }

    // --------------------------------------------------
    // SUBMISSION AUTHORIZATION
    // --------------------------------------------------

    @Test
    void submissionGroupAllowsMember() {
        group.addGroupMember(user);

        when(user.getUserId()).thenReturn(userId);

        when(groupRepository.findByIdAndCourseId(groupId, courseId))
                .thenReturn(Optional.of(group));

        Group result =
                groupService.submissionGroup(
                        courseId, groupId, userId, false
                );

        assertSame(group, result);
    }

    @Test
    void submissionGroupRejectsNonMember() {
        when(groupRepository.findByIdAndCourseId(groupId, courseId))
                .thenReturn(Optional.of(group));

        assertThrows(
                AccessDeniedException.class,
                () -> groupService.submissionGroup(
                        courseId, groupId, userId, false
                )
        );
    }
}