package no.hvl.dat250.lms.group.service;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.repository.CourseRepository;
import no.hvl.dat250.lms.group.api.dto.GroupDTO;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.repository.GroupRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class GroupService {

    // TODO: As of now there is no check if a user is enrolled in a course

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

    @Transactional(readOnly = true)
    public List<GroupDTO> getAllGroups(Long courseId) {
        return groupRepository.findByCourseId(courseId)
                .stream()
                .map(GroupDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDTO getGroupById(Long courseId, Long groupId) {
        Group group = findGroupByGroupIdAndCourseId(groupId, courseId);
        return GroupDTO.from(group);
    }

    public GroupDTO createGroup(Long courseId, Integer groupNumber) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found"
                ));

        if (groupNumber == null || groupNumber < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Group number must be a positive integer"
            );
        }

        if (groupRepository.existsByCourseIdAndGroupNumber(courseId, groupNumber)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Group number already exists in this course"
            );
        }

        Group group = new Group(groupNumber, course);

        return GroupDTO.from(groupRepository.save(group));
    }

    public void deleteGroup(Long courseId, Long groupId) {
        Group group = findGroupByGroupIdAndCourseId(groupId, courseId);
        groupRepository.delete(group);
    }

    public GroupDTO addGroupMember(Long courseId, Long groupId, Long userId) {
        Group group = findGroupByGroupIdAndCourseId(groupId, courseId);
        User user = findByUserId(userId);

        if (group.getGroupMembers().contains(user)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User is already a member of this group"
            );
        }

        if (groupRepository.existsUserInCourseGroup(courseId, user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User already belongs to another group in this course"
            );
        }

        group.addGroupMember(user);

        return GroupDTO.from(group);
    }


    public void removeGroupMember(Long courseId, Long groupId, Long userId) {
        Group group = findGroupByGroupIdAndCourseId(groupId, courseId);
        User user = findByUserId(userId);

        if (!group.getGroupMembers().remove(user)) { // <- .remove depends on User.equals() + User.hashCode()
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User is not a member of this group"
            );
        }
    }

    // ---------------------------------------------------------------- helpers

    private Group findGroupByGroupIdAndCourseId(Long groupId, Long courseId) {
        return groupRepository.findByIdAndCourseId(groupId, courseId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Group not found"
                        ));
    }

    private User findByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "User not found"
                        ));
    }
}
