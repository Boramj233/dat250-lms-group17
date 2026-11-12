package no.hvl.dat250.lms.group.api.dto;

import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.user.domain.User;

import java.util.Set;
import java.util.stream.Collectors;

public record GroupDTO(
        Long id,
        int groupNumber,
        //Long courseId,
        Set<Long> memberIds
) {
    public static GroupDTO from(Group group) {
        return new GroupDTO(
                group.getGroupId(),
                group.getGroupNumber(),
                //group.getCourse().getId(),
                group.getGroupMembers()
                        .stream()
                        .map(User::getId)
                        .collect(Collectors.toSet())
        );
    }
}
