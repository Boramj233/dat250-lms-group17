package no.hvl.dat250.lms.group.service;

import jakarta.transaction.Transactional;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.repository.CourseRepository;
import no.hvl.dat250.lms.group.api.dto.GroupDTO;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.repository.GroupRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * GroupService is responsible for business logic
 */

@Service
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public GroupService(
            GroupRepository groupRepository,
            CourseRepository courseRepository,
            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    public List<GroupDTO> getAllGroups(Long courseId) {
        return groupRepository.findByCourseId(courseId)
                .stream()
                .map(GroupDTO::from)
                .toList();
    }

    public GroupDTO getGroupById(Long groupId, Long courseId) {
        Group group = groupRepository.findByIdAndCourseId(groupId, courseId)
                .orElseThrow(() ->
                        new RuntimeException("Group Not Found"));
        return GroupDTO.from(group);
    }

    public GroupDTO createGroup(Integer groupNumber, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found"
                ));
        Group group = new Group(groupNumber, course);
        Group savedGroup = groupRepository.save(group);

        return GroupDTO.from(savedGroup);
    }

    public void deleteGroup(Long groupId, Long courseId) {
        Group group = findGroupByGroupIdAndCourseId(groupId, courseId)
        groupRepository.delete(group);
    }

    public GroupDTO addGroupMember(Long courseId, Long groupId, String email) {
        Group group = findGroupByGroupIdAndCourseId(groupId, courseId);

        User user = findUserByEmail(email);

        // TODO: implement the following User methods:
        // is the user enrolled in the course?
        // does the user already belong to a group?

        group.addGroupMember(user);

        return GroupDTO.from(groupRepository.save(group));
    }


    public void deleteMemberFromGroup(Long courseId, Long groupId, String email) {
        Group group = findGroupByGroupIdAndCourseId(groupId, courseId);
        User user = findUserByEmail(email);

        if (!group.getGroupMembers().contains(user)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User is not a member of this group"
            );
        }

        group.removeGroupMember(user);
        groupRepository.save(group);
    }

    // ---------------------------------------------------------------- helpers

    private Group findGroupByGroupIdAndCourseId(Long groupId, Long courseId) {
        return groupRepository.findByIdAndCourseId(groupId, courseId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Group not found"
                        ));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "User not found"
                        ));
    }

    private boolean isUserPartOfGroup() { return false; }
}
