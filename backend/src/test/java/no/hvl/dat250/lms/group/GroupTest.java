package no.hvl.dat250.lms.group;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupTest {

    private Group group;
    private Course course;

    @BeforeEach
    void setUp() {
        course = mock(Course.class);
        group = new Group(1, course, 3);
    }

    @Test
    void groupIsCreatedWithCorrectValues() {
        assertEquals(1, group.getGroupNumber());
        assertEquals(3, group.getMaxGroupSize());
        assertEquals(course, group.getCourse());
        assertTrue(group.getGroupMembers().isEmpty());
    }

    @Test
    void groupCanHaveUnlimitedMembers() {
        Group unlimitedGroup = new Group(1, course, null);

        for (int i = 0; i < 10; i++) {
            User user = mock(User.class);
            unlimitedGroup.addGroupMember(user);
        }

        assertEquals(10, unlimitedGroup.getGroupMembers().size());
    }

    @Test
    void groupRejectsInvalidMaximumSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Group(1, course, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Group(1, course, -1)
        );
    }

    @Test
    void userCanBeAddedToGroup() {
        User user = mock(User.class);

        group.addGroupMember(user);

        assertEquals(1, group.getGroupMembers().size());
        assertTrue(group.getGroupMembers().contains(user));
    }

    @Test
    void sameUserCannotBeAddedTwice() {
        User user = mock(User.class);

        group.addGroupMember(user);
        group.addGroupMember(user);

        assertEquals(1, group.getGroupMembers().size());
    }

    @Test
    void groupCannotExceedMaximumSize() {
        User user1 = mock(User.class);
        User user2 = mock(User.class);
        User user3 = mock(User.class);
        User user4 = mock(User.class);

        group.addGroupMember(user1);
        group.addGroupMember(user2);
        group.addGroupMember(user3);

        assertThrows(
                IllegalStateException.class,
                () -> group.addGroupMember(user4)
        );

        assertEquals(3, group.getGroupMembers().size());
    }

    @Test
    void userCanBeRemovedFromGroup() {
        User user = mock(User.class);

        group.addGroupMember(user);
        group.removeGroupMember(user);

        assertTrue(group.getGroupMembers().isEmpty());
    }

    @Test
    void removingNonexistentUserDoesNothing() {
        User user = mock(User.class);

        assertDoesNotThrow(
                () -> group.removeGroupMember(user)
        );

        assertTrue(group.getGroupMembers().isEmpty());
    }

    @Test
    void removingMemberFreesUpSpace() {
        User user1 = mock(User.class);
        User user2 = mock(User.class);
        User user3 = mock(User.class);
        User user4 = mock(User.class);

        group.addGroupMember(user1);
        group.addGroupMember(user2);
        group.addGroupMember(user3);

        group.removeGroupMember(user1);
        group.addGroupMember(user4);

        assertEquals(3, group.getGroupMembers().size());
        assertFalse(group.getGroupMembers().contains(user1));
        assertTrue(group.getGroupMembers().contains(user4));
    }
}