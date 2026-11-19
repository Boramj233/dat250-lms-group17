package no.hvl.dat250.lms.group.api.controllers;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import no.hvl.dat250.lms.group.api.dto.AddGroupMemberRequest;
import no.hvl.dat250.lms.group.api.dto.CreateGroupRequest;
import no.hvl.dat250.lms.group.api.dto.GroupDTO;
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
    public ResponseEntity<GroupDTO> createGroup(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        GroupDTO createdGroup = groupService.createGroup(courseId, request.groupNumber(), request.maxGroupSize());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdGroup.id())
                .encode()
                .toUri();

        return ResponseEntity.created(location).body(createdGroup);
    }

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
            @Valid @RequestBody AddGroupMemberRequest request
    ) {
        GroupDTO updatedGroup = groupService.addGroupMember(
                courseId, groupId, request.userId());
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
