package no.hvl.dat250.lms.group.service;

import jakarta.transaction.Transactional;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.repository.CourseRepository;
import no.hvl.dat250.lms.group.api.dto.GroupDTO;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.repository.GroupRepository;
import org.springframework.http.HttpStatus;
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

    public GroupService(GroupRepository groupRepository, CourseRepository courseRepository) {
        this.groupRepository = groupRepository;
        this.courseRepository = courseRepository;
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
}
