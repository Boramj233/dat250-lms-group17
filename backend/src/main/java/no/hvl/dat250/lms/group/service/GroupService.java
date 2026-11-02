package no.hvl.dat250.lms.group.service;


import jakarta.transaction.Transactional;
import no.hvl.dat250.lms.group.api.dto.GroupDTO;
import no.hvl.dat250.lms.group.repository.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * GroupService is responsible for business logic
 */

@Service
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public List<GroupDTO> getAllGroups() {
        return groupRepository.findAll()
                .stream()
                .map(GroupDTO::from)
                .toList();
    }
}
