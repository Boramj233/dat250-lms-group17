package no.hvl.dat250.lms.user;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.grade.domain.Grade;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionMember;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/users/me/grades and GET /api/users/{userId}/upcoming-deadlines. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserDashboardApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager entities;

    private final Instant now = Instant.now();

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user(String name) {
        return save(new User(name + "_" + UUID.randomUUID() + "@example.com", name, "Test"));
    }

    private Instant inDays(long days) {
        return now.plus(days, ChronoUnit.DAYS);
    }

    private Grade releasedGrade(Submission submission, Enrollment teacher, String score, boolean passed) {
        Grade grade = new Grade(submission, new BigDecimal(score), "Well done", teacher, now);
        grade.decidePassed(passed);
        grade.release(teacher, now);
        return save(grade);
    }

    @Test
    void gradesListTheLatestSubmissionPerItemWithItsReleasedGrade() throws Exception {
        User siri = user("Siri"), teammate = user("Tom"), teacherUser = user("Tea"), other = user("Ola");
        Course course = save(new Course("DAT250", "Course", Term.AUTUMN, 2026));
        Course leftCourse = save(new Course("DAT251", "Left", Term.AUTUMN, 2026));
        Enrollment siriIn = save(new Enrollment(siri, course, CourseRole.STUDENT));
        Enrollment teammateIn = save(new Enrollment(teammate, course, CourseRole.STUDENT));
        Enrollment teacher = save(new Enrollment(teacherUser, course, CourseRole.TEACHER));
        Enrollment otherIn = save(new Enrollment(other, course, CourseRole.STUDENT));
        Enrollment siriLeft = save(new Enrollment(siri, leftCourse, CourseRole.STUDENT));
        siriLeft.deactivate();

        Module module = save(new Module(course, "Week 1", 0));
        Assignment essay = save(new Assignment(module, "Essay", 0, "Write", null, false));
        Assignment report = save(new Assignment(module, "Report", 1, "Write", null, false));
        CodingExercise compose = save(new CodingExercise(module, "Compose", 2, "Code",
            "https://example.com/repo", null, false));
        Assignment notHandedIn = save(new Assignment(module, "Not handed in", 3, "Write", null, false));
        Assignment leftCourseWork = save(new Assignment(
            save(new Module(leftCourse, "Old", 0)), "Old work", 0, "Write", null, false));

        // Essay: two submissions; only the latest counts, and its grade is released.
        save(new Submission(essay, siriIn, SubmissionType.TEXT, "Draft", inDays(-3), false));
        Submission essayLatest = save(new Submission(essay, siriIn, SubmissionType.TEXT, "Final", inDays(-2), true));
        releasedGrade(essayLatest, teacher, "87.50", true);
        // Report: graded but not released, so no grade yet.
        Submission reportSubmission = save(new Submission(report, siriIn, SubmissionType.TEXT, "R", inDays(-1), false));
        save(new Grade(reportSubmission, new BigDecimal("40"), "Draft", teacher, now));
        // Compose: handed in by the teammate for a group Siri is in.
        Group group = save(new Group(1, course, null));
        Submission groupSubmission = new Submission(compose, teammateIn, SubmissionType.GIT_URL,
            "https://example.com/ours", inDays(-4), false);
        groupSubmission.assignGroup(group, List.of(
            new SubmissionMember(teammate.getUserId(), teammateIn.getId()),
            new SubmissionMember(siri.getUserId(), siriIn.getId())));
        save(groupSubmission);
        // Someone else's work and work in a course Siri left are not hers.
        save(new Submission(notHandedIn, otherIn, SubmissionType.TEXT, "Mine", inDays(-1), false));
        save(new Submission(leftCourseWork, siriLeft, SubmissionType.TEXT, "Old", inDays(-30), false));

        mvc.perform(get("/api/users/me/grades").accept(MediaType.APPLICATION_JSON)
                .header("Authorization", TestTokens.bearer(siri)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            // Newest first: report, essay, compose.
            .andExpect(jsonPath("$[0].contentId").value(report.getId()))
            .andExpect(jsonPath("$[0].grade").value(nullValue()))
            .andExpect(jsonPath("$[1].contentId").value(essay.getId()))
            .andExpect(jsonPath("$[1].type").value("ASSIGNMENT"))
            .andExpect(jsonPath("$[1].title").value("Essay"))
            .andExpect(jsonPath("$[1].courseId").value(course.getId()))
            .andExpect(jsonPath("$[1].courseTitle").value("DAT250"))
            .andExpect(jsonPath("$[1].moduleId").value(module.getId()))
            .andExpect(jsonPath("$[1].submissionId").value(essayLatest.getId()))
            .andExpect(jsonPath("$[1].late").value(true))
            .andExpect(jsonPath("$[1].grade.score").value(87.5))
            .andExpect(jsonPath("$[1].grade.feedback").value("Well done"))
            .andExpect(jsonPath("$[1].grade.passed").value(true))
            .andExpect(jsonPath("$[1].grade.releasedAt").isNotEmpty())
            .andExpect(jsonPath("$[2].contentId").value(compose.getId()))
            .andExpect(jsonPath("$[2].type").value("CODING_EXERCISE"))
            .andExpect(jsonPath("$[2].submissionId").value(groupSubmission.getId()));
    }

    @Test
    void upcomingDeadlinesAreTheUsersFutureDeadlinesSoonestFirst() throws Exception {
        User siri = user("Siri"), other = user("Ola"), admin = user("Ada");
        admin.setAccess(Access.ADMIN);
        Course course = save(new Course("DAT250", "Course", Term.AUTUMN, 2026));
        Course leftCourse = save(new Course("DAT251", "Left", Term.AUTUMN, 2026));
        Course notMine = save(new Course("DAT109", "Other", Term.AUTUMN, 2026));
        save(new Enrollment(siri, course, CourseRole.STUDENT));
        save(new Enrollment(siri, leftCourse, CourseRole.STUDENT)).deactivate();
        save(new Enrollment(other, notMine, CourseRole.STUDENT));

        Module module = save(new Module(course, "Week 1", 0));
        Assignment later = new Assignment(module, "Assignment", 0, "Write", inDays(5), false);
        later.setMandatoryForExam(true);
        save(later);
        CodingExercise sooner = save(new CodingExercise(module, "Exercise", 1, "Code",
            "https://example.com/repo", inDays(1), true));
        save(new Assignment(module, "Already due", 2, "Write", inDays(-1), true));
        save(new Assignment(module, "No deadline", 3, "Write", null, false));
        save(new Assignment(save(new Module(leftCourse, "Old", 0)), "Left course", 0, "Write", inDays(2), false));
        save(new Assignment(save(new Module(notMine, "Other", 0)), "Not mine", 0, "Write", inDays(2), false));
        entities.flush();

        String path = "/api/users/" + siri.getUserId() + "/upcoming-deadlines";
        mvc.perform(get(path).accept(MediaType.APPLICATION_JSON).header("Authorization", TestTokens.bearer(siri)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].contentId").value(sooner.getId()))
            .andExpect(jsonPath("$[0].type").value("CODING_EXERCISE"))
            .andExpect(jsonPath("$[0].allowLateSubmission").value(true))
            .andExpect(jsonPath("$[0].mandatoryForExam").value(nullValue()))
            .andExpect(jsonPath("$[0].courseTitle").value("DAT250"))
            .andExpect(jsonPath("$[0].moduleId").value(module.getId()))
            .andExpect(jsonPath("$[1].contentId").value(later.getId()))
            .andExpect(jsonPath("$[1].type").value("ASSIGNMENT"))
            .andExpect(jsonPath("$[1].mandatoryForExam").value(true))
            .andExpect(jsonPath("$[1].deadline").isNotEmpty());

        // Only the user themselves, or an admin.
        mvc.perform(get(path).header("Authorization", TestTokens.bearer(other)))
            .andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization", TestTokens.bearer(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @Test
    void aUserWithNothingGetsEmptyLists() throws Exception {
        User nina = user("Nina");
        mvc.perform(get("/api/users/me/grades").header("Authorization", TestTokens.bearer(nina)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/users/" + nina.getUserId() + "/upcoming-deadlines")
                .header("Authorization", TestTokens.bearer(nina)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
