package no.hvl.dat250.lms.group;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Set;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.repository.GroupRepository;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.user.domain.User;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupIntegrationTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MockMvc mvc;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void groupAndMembershipCanBePersisted() {
        Course course = createCourse();
        User student = enroll(course, "student", CourseRole.STUDENT);
        Group group = new Group(1, course, 3);
        group.addGroupMember(student);
        groupRepository.saveAndFlush(group);
        entityManager.clear();

        Group reloaded = groupRepository.findById(group.getGroupId()).orElseThrow();

        assertEquals(course.getId(), reloaded.getCourse().getId());
        assertEquals(1, reloaded.getGroupNumber());
        assertEquals(3, reloaded.getMaxGroupSize());
        assertEquals(Set.of(student.getUserId()), memberIds(reloaded));
    }

    @Test
    void databaseRejectsDuplicateGroupMembership() {
        Course course = createCourse();
        User student = enroll(course, "student", CourseRole.STUDENT);
        Group group = new Group(1, course, null);
        group.addGroupMember(student);
        groupRepository.saveAndFlush(group);

        // Insert directly because the domain set already prevents duplicates.
        assertThrows(ConstraintViolationException.class, () -> entityManager.createNativeQuery(
                "insert into group_members (group_id, user_id) values (:groupId, :userId)")
                .setParameter("groupId", group.getGroupId())
                .setParameter("userId", student.getUserId())
                .executeUpdate());
    }

    @Test
    void databaseRejectsGroupWithoutCourse() {
        assertThrows(DataIntegrityViolationException.class,
                () -> groupRepository.saveAndFlush(new Group(1, null, null)));
    }

    @Test
    void databaseRejectsGroupWithUnknownCourse() {
        Course unknownCourse = entityManager.getReference(Course.class, -1L);

        assertThrows(DataIntegrityViolationException.class,
                () -> groupRepository.saveAndFlush(new Group(1, unknownCourse, null)));
    }

    @Test
    void groupApiRejectsDuplicateMembership() throws Exception {
        Course course = createCourse();
        User teacher = enroll(course, "teacher", CourseRole.TEACHER);
        User student = enroll(course, "student", CourseRole.STUDENT);
        Group group = groupRepository.saveAndFlush(new Group(1, course, 2));

        String membersPath = "/api/courses/{courseId}/groups/{groupId}/members";
        mvc.perform(post(membersPath, course.getId(), group.getGroupId())
                        .header("Authorization", TestTokens.bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(student.getUserId().toString()))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        mvc.perform(post(membersPath, course.getId(), group.getGroupId())
                        .header("Authorization", TestTokens.bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(student.getUserId().toString()))
                .andExpect(status().isConflict());

        entityManager.flush();
        entityManager.clear();
        Group reloaded = groupRepository.findById(group.getGroupId()).orElseThrow();
        assertEquals(Set.of(student.getUserId()), memberIds(reloaded));
    }

    @Test
    void groupApiRejectsMissingOrUnknownCourse() throws Exception {
        Course course = createCourse();
        User teacher = enroll(course, "teacher", CourseRole.TEACHER);
        User student = enroll(course, "student", CourseRole.STUDENT);
        Group group = groupRepository.saveAndFlush(new Group(1, course, 2));

        mvc.perform(post("/api/courses/groups/{groupId}/members", group.getGroupId())
                        .header("Authorization", TestTokens.bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(student.getUserId().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/courses/{courseId}/groups/{groupId}/members", -1L, group.getGroupId())
                        .header("Authorization", TestTokens.bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(student.getUserId().toString()))
                .andExpect(status().isForbidden());

        entityManager.clear();
        assertTrue(groupRepository.findById(group.getGroupId()).orElseThrow().getGroupMembers().isEmpty());
    }

    private Course createCourse() {
        return courseRepository.saveAndFlush(new Course("DAT250", "Groups", Term.AUTUMN, 2026));
    }

    private User enroll(Course course, String name, CourseRole role) {
        User user = new User(name + "@group-integration.example.com", "Test", name);
        entityManager.persist(user);
        entityManager.persist(new Enrollment(user, course, role));
        return user;
    }

    private Set<Long> memberIds(Group group) {
        return group.getGroupMembers().stream()
                .map(User::getUserId)
                .collect(toSet());
    }
}
