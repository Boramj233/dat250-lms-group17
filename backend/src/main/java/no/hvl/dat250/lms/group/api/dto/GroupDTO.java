package no.hvl.dat250.lms.group.api.dto;

import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.user.domain.User;

import java.util.Set;

/**
 * GroupDTO is the API representation of Group
 */

public record GroupDTO(
        Long id,
        int groupNumber,
        Set<User> groupMembers
) {
    public static GroupDTO from(Group group) {
        return new GroupDTO(
                group.getGroupId(),
                group.getGroupNumber(),
                group.getGroupMembers() // <- maybe get DTO? idk
        );
    }
}
