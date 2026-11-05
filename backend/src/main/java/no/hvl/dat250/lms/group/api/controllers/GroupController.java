package no.hvl.dat250.lms.group.api.controllers;

import no.hvl.dat250.lms.group.api.dto.GroupDTO;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;


/**
 * GroupController is responsible for HTTP communication
 */

@RestController
@RequestMapping("/api/courses/{courseId}/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping()
    public List<GroupDTO> getAllGroups(@PathVariable Long courseId) {
        return groupService.getAllGroups(courseId);
    }

    @GetMapping("/{groupId}")
    public GroupDTO getGroup(@PathVariable Long courseId, @PathVariable Long groupId) {
        return groupService.getGroupById(groupId, courseId);
    }

    @PostMapping()
    public ResponseEntity<GroupDTO> createGroup(@PathVariable Long courseId, @RequestBody Group group) {
        GroupDTO createdGroup = groupService.createGroup(group.getGroupNumber(), courseId);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdGroup.id())
                .encode()
                .toUri();

        return ResponseEntity.created(location).body(createdGroup);
    }

    @PutMapping("/{groupId}")
    public Group updateGroup() { return null; }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup() { return null; }

}
