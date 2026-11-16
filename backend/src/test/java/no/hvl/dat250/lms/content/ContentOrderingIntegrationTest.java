package no.hvl.dat250.lms.content;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.grade.domain.Grade;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT filters, committed data and database constraints; no request-spanning transaction. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:content_ordering_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentOrderingIntegrationTest {
    @Autowired private EntityManager entities;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private MockMvc mvc;
    @Autowired private ContentService contents;

    private <T> T tx(Supplier<T> action) {
        return new TransactionTemplate(transactions).execute(status -> action.get());
    }
    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }
    private User user() {
        return save(new User(UUID.randomUUID() + "@example.com", "Test", "User"));
    }
    private record Fixture(Long courseId, Long moduleId, User teacher, User student,
                           Long teacherEnrollmentId, Long studentEnrollmentId, List<Long> ids) {
    }
    private Fixture fixture() {
        return tx(() -> {
            Course course = save(new Course("DAT250", "Ordering", Term.AUTUMN, 2026));
            User teacher = user();
            User student = user();
            Enrollment teacherEnrollment = save(new Enrollment(teacher, course, CourseRole.TEACHER));
            Enrollment studentEnrollment = save(new Enrollment(student, course, CourseRole.STUDENT));
            Module module = save(new Module(course, "Week", 0));
            StaticPage page = save(new StaticPage(module, "Page", 0, "Body"));
            Quiz quiz = save(new Quiz(module, "Quiz", 2, 3));
            Assignment assignment = save(new Assignment(module, "Report", 5, "Write", null, false));
            CodingExercise coding = save(new CodingExercise(module, "Code", Integer.MAX_VALUE,
                "Implement", "https://example.com/starter", null, false));
            return new Fixture(course.getId(), module.getId(), teacher, student,
                teacherEnrollment.getId(), studentEnrollment.getId(),
                List.of(page.getId(), quiz.getId(), assignment.getId(), coding.getId()));
        });
    }
    private String modulePath(Fixture fixture) {
        return "/api/courses/" + fixture.courseId() + "/modules/" + fixture.moduleId() + "/contents";
    }
    private String body(List<Long> ids) {
        return "{\"contentIds\":" + ids + "}";
    }
    private void assertOrder(Fixture fixture, List<Long> ids) throws Exception {
        var result = mvc.perform(get(modulePath(fixture)).header("Authorization", TestTokens.bearer(fixture.student())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(ids.size()));
        for (int index = 0; index < ids.size(); index++) {
            result.andExpect(jsonPath("$[" + index + "].id").value(ids.get(index)));
        }
    }
    @Test
    void reorderMixedContentNormalizesGapsAndIsIdempotent() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        List<Long> order = List.of(fixture.ids().get(3), fixture.ids().get(0), fixture.ids().get(2), fixture.ids().get(1));
        for (int request = 0; request < 2; request++) {
            mvc.perform(put(modulePath(fixture) + "/order")
                    .header("Authorization", TestTokens.bearer(fixture.teacher()))
                    .contentType(MediaType.APPLICATION_JSON).content(body(order)))
                .andExpect(status().isNoContent());
            assertOrder(fixture, order);
            tx(() -> {
                for (int index = 0; index < order.size(); index++) {
                    Content content = entities.find(Content.class, order.get(index));
                    assertEquals(index, content.getPosition());
                    assertEquals(fixture.moduleId(), content.getModule().getId());
                }
                return null;
            });
        }
        assertOrder(other, other.ids());
        tx(() -> {
            assertEquals(Integer.MAX_VALUE, entities.find(Content.class, other.ids().get(3)).getPosition());
            return null;
        });
    }
    @Test
    void invalidMembershipIsRejectedWithoutChangingAnyPosition() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        List<Long> ids = fixture.ids();
        for (List<Long> invalid : List.of(List.<Long>of(), ids.subList(0, 3),
                List.of(ids.get(0), ids.get(0), ids.get(2), ids.get(3)),
                List.of(ids.get(0), ids.get(1), ids.get(2), Long.MAX_VALUE),
                List.of(ids.get(0), ids.get(1), ids.get(2), other.ids().get(0)))) {
            mvc.perform(put(modulePath(fixture) + "/order")
                    .header("Authorization", TestTokens.bearer(fixture.teacher()))
                    .contentType(MediaType.APPLICATION_JSON).content(body(invalid)))
                .andExpect(status().isBadRequest());
            assertOrder(fixture, ids);
        }
        tx(() -> {
            assertEquals(5, entities.find(Content.class, ids.get(2)).getPosition());
            assertEquals(Integer.MAX_VALUE, entities.find(Content.class, ids.get(3)).getPosition());
            return null;
        });
    }
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"contentIds\":null}", "{\"contentIds\":[null]}",
        "{\"contentIds\":[0]}", "{\"contentIds\":[-1]}"})
    void invalidRequestShapeReturns400(String body) throws Exception {
        Fixture fixture = fixture();
        mvc.perform(put(modulePath(fixture) + "/order")
                .header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertOrder(fixture, fixture.ids());
    }
    @Test
    void reorderRequiresActiveTeacherInTheSpecifiedCourse() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        User inactive = tx(() -> {
            User teacher = user();
            Enrollment enrollment = save(new Enrollment(teacher, entities.find(Course.class, fixture.courseId()), CourseRole.TEACHER));
            enrollment.deactivate();
            return teacher;
        });
        String path = modulePath(fixture) + "/order";
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(body(fixture.ids())))
            .andExpect(status().isUnauthorized());
        for (User denied : List.of(fixture.student(), other.teacher(), inactive)) {
            mvc.perform(put(path).header("Authorization", TestTokens.bearer(denied))
                    .contentType(MediaType.APPLICATION_JSON).content(body(fixture.ids())))
                .andExpect(status().isForbidden());
        }
        assertOrder(fixture, fixture.ids());
    }
    @Test
    void missingAndCrossCourseModulesReturn404AndEmptyModuleAcceptsEmptyOrder() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        for (Long id : List.of(Long.MAX_VALUE, other.moduleId())) {
            mvc.perform(put("/api/courses/{course}/modules/{module}/contents/order", fixture.courseId(), id)
                    .header("Authorization", TestTokens.bearer(fixture.teacher()))
                    .contentType(MediaType.APPLICATION_JSON).content(body(fixture.ids())))
                .andExpect(status().isNotFound());
        }
        Long emptyModule = tx(() -> save(new Module(entities.find(Course.class, fixture.courseId()), "Empty", 1)).getId());
        mvc.perform(put("/api/courses/{course}/modules/{module}/contents/order", fixture.courseId(), emptyModule)
                .header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content(body(List.of())))
            .andExpect(status().isNoContent());
    }
    @ParameterizedTest
    @ValueSource(strings = {"pages", "quizzes", "assignments", "coding-exercises"})
    void allCreationEndpointsRejectOccupiedPositions(String type) throws Exception {
        Fixture fixture = fixture();
        String fields = switch (type) {
            case "pages" -> "\"body\":\"Body\"";
            case "quizzes" -> "\"maxAttempts\":3";
            case "assignments" -> "\"instructions\":\"Write\",\"allowLateSubmission\":false";
            case "coding-exercises" -> "\"instructions\":\"Implement\",\"starterRepositoryUrl\":\"https://example.com/starter\",\"allowLateSubmission\":false";
            default -> throw new IllegalArgumentException(type);
        };
        for (int position : new int[]{0, 1}) {
            mvc.perform(post(modulePath(fixture) + "/" + type)
                    .header("Authorization", TestTokens.bearer(fixture.teacher()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"New\",\"position\":" + position + "," + fields + "}"))
                .andExpect(position == 0 ? status().isConflict() : status().isCreated());
        }
        assertEquals(5, contents.findContentsByModule(fixture.courseId(), fixture.moduleId()).size());
    }
    @Test
    void reorderingPreservesReleasedGradesAndCompletionEvidence() throws Exception {
        Fixture fixture = fixture();
        Long submissionId = tx(() -> {
            var owner = entities.find(Enrollment.class, fixture.studentEnrollmentId());
            var teacher = entities.find(Enrollment.class, fixture.teacherEnrollmentId());
            var assignment = entities.find(Assignment.class, fixture.ids().get(2));
            assignment.setRequiredForModuleCompletion(true);
            var submission = save(new Submission(assignment, owner, SubmissionType.TEXT, "Answer", Instant.now(), false));
            var grade = new Grade(submission, BigDecimal.ZERO, "Pass", teacher, Instant.now());
            grade.decidePassed(true);
            grade.release(teacher, Instant.now());
            save(grade);
            save(new StaticPageCompletion(entities.find(StaticPage.class, fixture.ids().get(0)), owner, Instant.now()));
            save(new QuizAttempt(entities.find(Quiz.class, fixture.ids().get(1)), owner, Instant.now(), 1, 1));
            return submission.getId();
        });
        mvc.perform(put(modulePath(fixture) + "/order").header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content(body(fixture.ids().reversed())))
            .andExpect(status().isNoContent());
        for (Long id : fixture.ids().subList(0, 3)) {
            assertTrue(contents.isCompleted(id, fixture.student().getUserId()));
        }
        mvc.perform(get("/api/submissions/{id}/grade", submissionId)
                .header("Authorization", TestTokens.bearer(fixture.student())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.passed").value(true));
        tx(() -> {
            assertTrue(entities.find(Content.class, fixture.ids().get(2)).isRequiredForModuleCompletion());
            return null;
        });
    }
    @Test
    void databaseRejectsDuplicateAndNegativePositions() {
        Fixture fixture = fixture();
        assertThrows(jakarta.persistence.PersistenceException.class, () -> tx(() -> {
            entities.createNativeQuery("update contents set content_position=0 where id=:id")
                .setParameter("id", fixture.ids().get(1)).executeUpdate();
            return null;
        }));
        assertThrows(jakarta.persistence.PersistenceException.class, () -> tx(() -> {
            entities.createNativeQuery("update contents set content_position=-1 where id=:id")
                .setParameter("id", fixture.ids().get(1)).executeUpdate();
            return null;
        }));
    }
}
