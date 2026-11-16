package no.hvl.dat250.lms.group.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.service.CourseService;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import no.hvl.dat250.lms.group.api.dto.GroupDTO;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.repository.GroupRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class GroupService {

    private final EntityManager entities;
    private final EnrollmentService enrollments;

    // All group writes and group submission/grade acceptance share this row lock.
    @Transactional(propagation = Propagation.MANDATORY)
    public Group lockGroup(Long courseId, Long groupId) {
        Group group = findGroupByGroupIdAndCourseId(groupId, courseId);
        entities.refresh(group, LockModeType.PESSIMISTIC_WRITE);
        // Refresh the collection as well, including when preflight loaded it.
        org.hibernate.Hibernate.initialize(group.getGroupMembers());
        return group;
    }

    public Group submissionGroup(Long courseId, Long groupId, Long userId, boolean lock) {
        Group group = lock ? lockGroup(courseId, groupId)
            : findGroupByGroupIdAndCourseId(groupId, courseId);
        if (group.getGroupMembers().stream().noneMatch(u -> u.getUserId().equals(userId))) {
            throw new AccessDeniedException("Group membership required");
        }
        return group;
    }


    private final GroupRepository groupRepository;
    private final CourseService courseService;
    private final UserService userService;

    public GroupService(
            GroupRepository groupRepository,
            CourseService courseService,
            UserService userService,
            EntityManager entities,
            EnrollmentService enrollments
    ) {
        this.groupRepository = groupRepository;
        this.courseService = courseService;
        this.userService = userService;
        this.entities = entities;
        this.enrollments = enrollments;
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
        Course course = courseService.findCourseById(courseId)
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
        Group group = lockGroup(courseId, groupId);
        try {
            groupRepository.delete(group);
            groupRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group has retained history", ex);
        }
    }

    public GroupDTO addGroupMember(Long courseId, Long groupId, Long userId) {
        Group group = lockGroup(courseId, groupId);
        User user = findByUserId(userId);
        enrollments.lockActiveStudent(userId, courseId);

        if (group.getGroupMembers().contains(user)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User is already a member of this group"
            );
        }

        if (groupRepository.existsUserInCourseGroup(courseId, user.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User already belongs to another group in this course"
            );
        }

        group.addGroupMember(user);

        return GroupDTO.from(group);
    }


    public void removeGroupMember(Long courseId, Long groupId, Long userId) {
        Group group = lockGroup(courseId, groupId);
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

    private User findByUserId(Long userId) { return userService.getUser(userId); }
}
