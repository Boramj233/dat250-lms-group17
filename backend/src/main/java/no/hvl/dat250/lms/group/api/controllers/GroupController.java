package no.hvl.dat250.lms.group.api.controllers;

import java.net.URI;
import java.util.List;
import no.hvl.dat250.lms.group.api.dto.GroupDTO;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.service.GroupService;
import no.hvl.dat250.lms.security.auth.annotations.IsEnrolledInCourse;
import no.hvl.dat250.lms.security.auth.annotations.IsTeacherInCourse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/courses/{courseId}/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @IsEnrolledInCourse
    @GetMapping()
    public List<GroupDTO> getAllGroups(@PathVariable Long courseId) {
        return groupService.getAllGroups(courseId);
    }

    @IsEnrolledInCourse
    @GetMapping("/{groupId}")
    public GroupDTO getGroup(@PathVariable Long courseId, @PathVariable Long groupId) {
        return groupService.getGroupById(courseId, groupId);
    }

    @IsTeacherInCourse
    @PostMapping()
    public ResponseEntity<GroupDTO> createGroup(@PathVariable Long courseId, @RequestBody Group group) {
        GroupDTO createdGroup = groupService.createGroup(courseId, group.getGroupNumber());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdGroup.id())
                .encode()
                .toUri();

        return ResponseEntity.created(location).body(createdGroup);
    }

    // If more groups are created than needed, it should be able to remove said groups
    @IsTeacherInCourse
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long courseId, @PathVariable Long groupId) {
        groupService.deleteGroup(courseId, groupId);
        return ResponseEntity.noContent().build();
    }

    @IsTeacherInCourse
    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupDTO> addGroupMember(
            @PathVariable Long courseId,
            @PathVariable Long groupId,
            @RequestBody Long userId
    ) {
        GroupDTO updatedGroup = groupService.addGroupMember(
                courseId, groupId, userId);
        return ResponseEntity.ok(updatedGroup);
    }

    @IsTeacherInCourse
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> deleteUserFromGroup(
            @PathVariable Long courseId,
            @PathVariable Long groupId,
            @PathVariable Long userId
    ) {
        groupService.removeGroupMember(courseId, groupId, userId);
        return ResponseEntity.noContent().build();
    }
}
