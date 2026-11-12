package no.hvl.dat250.lms.group.api.dto;

import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupDTOTest {
    @Test
    void fromMapsGroupNumberAndMemberIds() {
        Group group = new Group(4, null);
        User alice = mock(User.class);
        User bob = mock(User.class);
        when(alice.getUserId()).thenReturn(11L);
        when(bob.getUserId()).thenReturn(12L);
        group.addGroupMember(alice);
        group.addGroupMember(bob);

        GroupDTO dto = GroupDTO.from(group);

        assertNull(dto.id()); // The group has not been persisted yet.
        assertEquals(4, dto.groupNumber());
        assertEquals(Set.of(11L, 12L), dto.memberIds());
    }
}
