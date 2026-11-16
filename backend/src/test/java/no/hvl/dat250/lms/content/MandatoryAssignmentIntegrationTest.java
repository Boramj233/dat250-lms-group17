package no.hvl.dat250.lms.content;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.grade.domain.Grade;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT filters, course enrollment checks and persistence; no live Keycloak needed. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:mandatory_assignment_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MandatoryAssignmentIntegrationTest {

    @Autowired private EntityManager entities;
    @Autowired private MockMvc mvc;
    @Autowired private ContentService contentService;

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user() {
        return save(new User(UUID.randomUUID() + "@example.com", "Test", "User"));
    }

    private record Fixture(User teacher, User student, Enrollment teacherEnrollment, Enrollment studentEnrollment,
                           Module module, Assignment assignment) {
    }

    private Fixture fixture() {
        Course course = save(new Course("DAT250", "Mandatory assignments", Term.AUTUMN, 2026));
        User teacher = user();
        User student = user();
        Enrollment teacherEnrollment = save(new Enrollment(teacher, course, CourseRole.TEACHER));
        Enrollment studentEnrollment = save(new Enrollment(student, course, CourseRole.STUDENT));
        Module module = save(new Module(course, "Week", 0));
        Assignment assignment = save(new Assignment(module, "Report", 0, "Write a report", null, false));
        return new Fixture(teacher, student, teacherEnrollment, studentEnrollment, module, assignment);
    }

    private String settingsPath(Long assignmentId) {
        return "/api/assignments/" + assignmentId + "/exam-requirement";
    }

    private String createPath(Fixture fixture) {
        return "/api/courses/" + fixture.module().getCourse().getId()
            + "/modules/" + fixture.module().getId() + "/contents/assignments";
    }

    private String createBody() {
        return """
            {"title":"New report","position":1,"instructions":"Write a report","allowLateSubmission":false}
            """;
    }

    @Test
    void teacherCanToggleFlagAndItPersistsIndependentlyOfCompletion() throws Exception {
        Fixture fixture = fixture();
        Long assignmentId = fixture.assignment().getId();
        String path = settingsPath(assignmentId);
        assertFalse(fixture.assignment().isMandatoryForExam());

        for (boolean requiredForModule : new boolean[]{false, true}) {
            Assignment assignment = entities.find(Assignment.class, assignmentId);
            assignment.setRequiredForModuleCompletion(requiredForModule);
            for (boolean mandatoryForExam : new boolean[]{true, false}) {
                mvc.perform(patch(path).header("Authorization", TestTokens.bearer(fixture.teacher()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mandatoryForExam\":" + mandatoryForExam + "}"))
                    .andExpect(status().isNoContent());
                entities.flush();
                entities.clear();
                Assignment stored = entities.find(Assignment.class, assignmentId);
                assertEquals(mandatoryForExam, stored.isMandatoryForExam());
                assertEquals(requiredForModule, stored.isRequiredForModuleCompletion());
                assertFalse(contentService.isCompleted(assignmentId, fixture.student().getUserId()));
                mvc.perform(get("/api/assignments/{id}", assignmentId)
                        .header("Authorization", TestTokens.bearer(fixture.student())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mandatoryForExam").value(mandatoryForExam));
            }
        }
    }

    @Test
    void examFlagDoesNotChangeAnExistingReleasedPass() throws Exception {
        Fixture fixture = fixture();
        var submission = save(new Submission(
            fixture.assignment(),
            fixture.studentEnrollment(),
            SubmissionType.TEXT, "Answer", Instant.now(), false));
        var grade = new Grade(submission, BigDecimal.ZERO,
            null, fixture.teacherEnrollment(), Instant.now());
        grade.decidePassed(true);
        grade.release(fixture.teacherEnrollment(), Instant.now());
        save(grade);
        for (boolean mandatory : new boolean[]{true, false}) {
            mvc.perform(patch(settingsPath(fixture.assignment().getId()))
                    .header("Authorization", TestTokens.bearer(fixture.teacher()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mandatoryForExam\":" + mandatory + "}"))
                .andExpect(status().isNoContent());
            assertTrue(contentService.isCompleted(fixture.assignment().getId(), fixture.student().getUserId()));
        }
    }

    @Test
    void studentsOtherCourseTeachersAndInactiveTeachersCannotWrite() throws Exception {
        Fixture fixture = fixture();
        Fixture otherCourse = fixture();
        User inactiveTeacher = user();
        Enrollment inactive = save(new Enrollment(inactiveTeacher, fixture.module().getCourse(), CourseRole.TEACHER));
        inactive.deactivate();
        entities.flush();

        for (User denied : new User[]{fixture.student(), otherCourse.teacher(), inactiveTeacher}) {
            mvc.perform(patch(settingsPath(fixture.assignment().getId()))
                    .header("Authorization", TestTokens.bearer(denied))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"mandatoryForExam\":true}"))
                .andExpect(status().isForbidden());
            mvc.perform(post(createPath(fixture)).header("Authorization", TestTokens.bearer(denied))
                    .contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isForbidden());
        }
        entities.clear();
        assertFalse(entities.find(Assignment.class, fixture.assignment().getId()).isMandatoryForExam());
        assertEquals(2L, entities.createQuery("select count(a) from Assignment a", Long.class).getSingleResult());
    }

    @Test
    void creatingAssignmentRequiresTeacherAndDefaultsToOptional() throws Exception {
        Fixture fixture = fixture();
        mvc.perform(post(createPath(fixture)).header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content(createBody()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.mandatoryForExam").value(false));
        Long count = entities.createQuery("select count(a) from Assignment a", Long.class).getSingleResult();
        assertEquals(2L, count);
    }

    @Test
    void missingAndInvalidAuthenticationCannotWrite() throws Exception {
        Fixture fixture = fixture();
        mvc.perform(patch(settingsPath(fixture.assignment().getId()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"mandatoryForExam\":true}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(patch(settingsPath(fixture.assignment().getId())).header("Authorization", "Bearer invalid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"mandatoryForExam\":true}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(createPath(fixture)).contentType(MediaType.APPLICATION_JSON).content(createBody()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void missingDecisionOrNullDecisionIsRejected() throws Exception {
        Fixture fixture = fixture();
        for (String body : new String[]{"{}", "{\"mandatoryForExam\":null}"}) {
            mvc.perform(patch(settingsPath(fixture.assignment().getId()))
                    .header("Authorization", TestTokens.bearer(fixture.teacher()))
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
        assertFalse(fixture.assignment().isMandatoryForExam());
    }

    @Test
    void unknownAssignmentAndOtherContentTypesReturn404() throws Exception {
        Fixture fixture = fixture();
        StaticPage page = save(new StaticPage(fixture.module(), "Page", 1, "Read"));
        for (Long id : new Long[]{Long.MAX_VALUE, page.getId()}) {
            mvc.perform(patch(settingsPath(id)).header("Authorization", TestTokens.bearer(fixture.teacher()))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"mandatoryForExam\":true}"))
                .andExpect(status().isNotFound());
        }
    }
}
