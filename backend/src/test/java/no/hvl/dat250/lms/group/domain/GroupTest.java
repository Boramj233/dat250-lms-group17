package no.hvl.dat250.lms.group.domain;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GroupTest {
    @Test
    void constructorSetsNumberAndCourse() {
        Course course = mock(Course.class);
        Group group = new Group(3, course);
        assertEquals(3, group.getGroupNumber());
        assertSame(course, group.getCourse());
        assertTrue(group.getGroupMembers().isEmpty());
    }

    @Test
    void addAndRemoveMember() {
        Group group = new Group(1, null);
        User user = mock(User.class);
        group.addGroupMember(user);
        assertTrue(group.getGroupMembers().contains(user));
        group.removeGroupMember(user);
        assertFalse(group.getGroupMembers().contains(user));
    }
}
