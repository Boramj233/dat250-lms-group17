package no.hvl.dat250.lms.group;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Set;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.repository.GroupRepository;
import no.hvl.dat250.lms.group.service.GroupMatchmakingScheduler;
import no.hvl.dat250.lms.group.service.GroupMatchmakingService;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toSet;
import static no.hvl.dat250.lms.enrollment.domain.CourseRole.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupMatchmakingIntegrationTest {
    private static final String PATH = "/api/courses/{courseId}/groups/matchmaking";
    private static final String MORNING = """
            {"targetGrade":1,"weeklyHours":2,"availability":"MORNING"}
            """;
    private static final String EVENING = """
            {"targetGrade":5,"weeklyHours":20,"availability":"EVENING"}
            """;

    @Autowired private EntityManager entities;
    @Autowired private MockMvc mvc;
    @Autowired private GroupRepository groups;
    @Autowired private GroupMatchmakingService matchmaking;
    @Autowired private GroupMatchmakingScheduler scheduler;

    private Course course;
    private User teacher;
    private User student;

    @BeforeEach
    void setUp() {
        course = new Course("DAT250", "Matchmaking", Term.AUTUMN, 2026);
        entities.persist(course);
        teacher = enroll("teacher", TEACHER).getUser();
        student = enroll("student", STUDENT).getUser();
    }

    @Test
    void teacherCreatesOneRoundAndCanReadItsSettings() throws Exception {
        read(teacher).andExpect(status().isNotFound());
        create(teacher, futureDeadline(), 3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maxGroupSize").value(3))
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.participation.submitted").value(0))
                .andExpect(jsonPath("$.participation.totalStudents").value(1));

        entities.flush();
        entities.clear();
        read(teacher)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxGroupSize").value(3));
        create(teacher, futureDeadline(), 2).andExpect(status().isConflict());
    }

    @Test
    void rejectsCreationWithAPastDeadline() throws Exception {
        create(teacher, Instant.now().minusSeconds(60), 2).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreationWithANonpositiveGroupSize() throws Exception {
        create(teacher, futureDeadline(), 0).andExpect(status().isBadRequest());
    }

    @Test
    void enforcesCourseRoles() throws Exception {
        User outsider = persistUser("outsider");
        create(student, futureDeadline(), 2).andExpect(status().isForbidden());
        create(teacher, futureDeadline(), 2).andExpect(status().isCreated());
        read(outsider).andExpect(status().isForbidden());
        submit(teacher, MORNING).andExpect(status().isForbidden());
        submit(outsider, MORNING).andExpect(status().isForbidden());
    }

    @Test
    void studentCanReplacePreferencesAndTeachersCountEachStudentOnce() throws Exception {
        User other = enroll("other", STUDENT).getUser();
        create(teacher, futureDeadline(), 2).andExpect(status().isCreated());
        submit(student, MORNING).andExpect(status().isOk());
        submit(student, EVENING).andExpect(status().isOk());
        entities.flush();
        entities.clear();

        read(student).andExpect(status().isOk())
                .andExpect(jsonPath("$.preference.targetGrade").value(5))
                .andExpect(jsonPath("$.preference.weeklyHours").value(20))
                .andExpect(jsonPath("$.preference.availability").value("EVENING"))
                .andExpect(jsonPath("$.participation").doesNotExist());
        read(other).andExpect(status().isOk())
                .andExpect(jsonPath("$.preference").doesNotExist());
        read(teacher).andExpect(status().isOk())
                .andExpect(jsonPath("$.preference").doesNotExist())
                .andExpect(jsonPath("$.participation.submitted").value(1))
                .andExpect(jsonPath("$.participation.totalStudents").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"targetGrade\":0,\"weeklyHours\":10,\"availability\":\"FLEXIBLE\"}",
            "{\"targetGrade\":6,\"weeklyHours\":10,\"availability\":\"FLEXIBLE\"}",
            "{\"targetGrade\":3,\"weeklyHours\":0,\"availability\":\"FLEXIBLE\"}",
            "{\"targetGrade\":3,\"weeklyHours\":21,\"availability\":\"FLEXIBLE\"}",
            "{\"targetGrade\":3,\"weeklyHours\":10}",
            "{\"targetGrade\":3,\"weeklyHours\":10,\"availability\":\"WEEKEND\"}"
    })
    void rejectsInvalidPreferences(String request) throws Exception {
        create(teacher, futureDeadline(), 2).andExpect(status().isCreated());
        submit(student, request).andExpect(status().isBadRequest());
        read(teacher).andExpect(jsonPath("$.participation.submitted").value(0));
    }

    @Test
    void rejectsEditsAfterTheDeadlineEvenBeforeTheSchedulerRuns() throws Exception {
        create(teacher, futureDeadline(), 2).andExpect(status().isCreated());
        submit(student, MORNING).andExpect(status().isOk());
        expireRound();

        read(student).andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(false))
                .andExpect(jsonPath("$.completed").value(false));
        submit(student, EVENING).andExpect(status().isConflict());
        read(student).andExpect(jsonPath("$.preference.availability").value("MORNING"));
    }

    @Test
    void schedulerMatchesEligibleRespondentsOnceAndRespectsGroupCapacity() throws Exception {
        User morning = enroll("morning", STUDENT).getUser();
        User evening = enroll("evening", STUDENT).getUser();
        User otherMorning = enroll("other-morning", STUDENT).getUser();
        User extraMorning = enroll("extra-morning", STUDENT).getUser();
        enroll("no-response", STUDENT);
        Enrollment withdrawn = enroll("withdrawn", STUDENT);
        User grouped = enroll("grouped", STUDENT).getUser();
        Group existing = new Group(1, course, 2);
        existing.addGroupMember(grouped);
        groups.saveAndFlush(existing);

        create(teacher, futureDeadline(), 2).andExpect(status().isCreated());
        submit(student, MORNING).andExpect(status().isOk());
        submit(morning, MORNING).andExpect(status().isOk());
        submit(evening, EVENING).andExpect(status().isOk());
        submit(otherMorning, MORNING).andExpect(status().isOk());
        submit(extraMorning, MORNING).andExpect(status().isOk());
        submit(grouped, EVENING).andExpect(status().isOk());
        submit(withdrawn.getUser(), EVENING).andExpect(status().isOk());
        submit(student, EVENING).andExpect(status().isOk());
        withdrawn.deactivate();

        scheduler.runDueMatchmaking();
        assertEquals(1, groups.findByCourseId(course.getId()).size());

        expireRound();
        assertTrue(matchmaking.dueCourses().contains(course.getId()));
        scheduler.runDueMatchmaking();
        entities.flush();
        entities.clear();

        assertFalse(matchmaking.dueCourses().contains(course.getId()));
        matchmaking.run(course.getId());
        var result = groups.findByCourseId(course.getId());
        assertEquals(4, result.size());
        assertTrue(result.stream().allMatch(group -> group.getMaxGroupSize() == 2
                && group.getGroupMembers().size() <= 2));
        assertEquals(Set.of(
                Set.of(grouped.getUserId()),
                Set.of(student.getUserId(), evening.getUserId()),
                Set.of(morning.getUserId(), otherMorning.getUserId()),
                Set.of(extraMorning.getUserId())
        ), result.stream().map(group -> group.getGroupMembers().stream().map(User::getUserId).collect(toSet()))
                .collect(toSet()));
        read(teacher).andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }

    private void expireRound() {
        entities.flush();
        entities.createNativeQuery("update group_matchmaking set deadline = :deadline where course_id = :courseId")
                .setParameter("deadline", Instant.now().minusSeconds(60))
                .setParameter("courseId", course.getId())
                .executeUpdate();
        entities.clear();
    }

    private Enrollment enroll(String name, CourseRole role) {
        Enrollment enrollment = new Enrollment(persistUser(name), course, role);
        entities.persist(enrollment);
        return enrollment;
    }

    private User persistUser(String name) {
        User user = new User(name + "@matchmaking.example.com", "Test", name);
        entities.persist(user);
        return user;
    }

    private static Instant futureDeadline() {
        return Instant.now().plusSeconds(600);
    }

    private ResultActions create(User user, Instant deadline, int maxGroupSize) throws Exception {
        return mvc.perform(post(PATH, course.getId())
                .header("Authorization", TestTokens.bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"deadline":"%s","maxGroupSize":%d}
                        """.formatted(deadline, maxGroupSize)));
    }

    private ResultActions read(User user) throws Exception {
        return mvc.perform(get(PATH, course.getId()).header("Authorization", TestTokens.bearer(user)));
    }

    private ResultActions submit(User user, String preferences) throws Exception {
        return mvc.perform(put(PATH + "/preferences", course.getId())
                .header("Authorization", TestTokens.bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(preferences));
    }
}
