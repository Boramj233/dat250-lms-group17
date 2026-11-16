package no.hvl.dat250.lms.content;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
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
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses the real JWT filters and course authorization, with an isolated in-memory database. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:coding_exercise_security_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CodingExerciseSecurityIntegrationTest {
    @Autowired private EntityManager entities;
    @Autowired private MockMvc mvc;

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user() {
        return save(new User(UUID.randomUUID() + "@example.com", "Test", "User"));
    }

    private record Fixture(Module module, User teacher, User student, CodingExercise exercise) {
    }

    private Fixture fixture() {
        Course course = save(new Course("DAT250", "Coding", Term.AUTUMN, 2026));
        User teacher = user();
        User student = user();
        save(new Enrollment(teacher, course, CourseRole.TEACHER));
        save(new Enrollment(student, course, CourseRole.STUDENT));
        Module module = save(new Module(course, "Week", 0));
        CodingExercise exercise = save(new CodingExercise(
            module, "Existing", 0, "Implement", "https://example.com/starter", null, false));
        return new Fixture(module, teacher, student, exercise);
    }

    private String createPath(Fixture fixture) {
        return "/api/courses/" + fixture.module().getCourse().getId()
            + "/modules/" + fixture.module().getId() + "/contents/coding-exercises";
    }

    private String createBody(String codeJson) {
        return """
            {"title":"New exercise","position":1,"instructions":"Implement API",
             "starterRepositoryUrl":"https://example.com/starter","allowLateSubmission":true
            """ + (codeJson == null ? "}" : ",\"starterCode\":" + codeJson + "}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"omitted", "null", "\"\"", "\"  public class Main {\\n    // TODO\\n  }\\n\""})
    void teacherCreatesAndEnrolledUsersReadPersistedStarterCode(String codeJson) throws Exception {
        Fixture fixture = fixture();
        mvc.perform(post(createPath(fixture)).header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("omitted".equals(codeJson) ? null : codeJson)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("New exercise"));
        entities.flush();
        entities.clear();
        CodingExercise stored = entities.createQuery(
                "select e from CodingExercise e where e.module.id = :moduleId and e.title = 'New exercise'",
                CodingExercise.class)
            .setParameter("moduleId", fixture.module().getId()).getSingleResult();
        String expected = switch (codeJson) {
            case "omitted", "null" -> null;
            case "\"\"" -> "";
            default -> "  public class Main {\n    // TODO\n  }\n";
        };
        assertEquals(expected, stored.getStarterCode());
        assertEquals("https://example.com/starter", stored.getStarterRepositoryUrl());
        assertTrue(stored.isAllowLateSubmission());
        assertFalse(stored.isRequiredForModuleCompletion());
        for (User reader : new User[]{fixture.teacher(), fixture.student()}) {
            var result = mvc.perform(get("/api/coding-exercises/{id}", stored.getId())
                    .header("Authorization", TestTokens.bearer(reader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.starterRepositoryUrl").value("https://example.com/starter"));
            if (expected == null) {
                result.andExpect(jsonPath("$.starterCode").isEmpty());
            } else {
                result.andExpect(jsonPath("$.starterCode").value(expected));
            }
        }
    }

    @Test
    void unauthenticatedCreateAndReadReturn401() throws Exception {
        Fixture fixture = fixture();
        mvc.perform(post(createPath(fixture)).contentType(MediaType.APPLICATION_JSON).content(createBody(null)))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/coding-exercises/{id}", fixture.exercise().getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void studentOtherCourseTeacherAndInactiveTeacherCannotCreate() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        User inactiveTeacher = user();
        Enrollment inactive = save(new Enrollment(inactiveTeacher, fixture.module().getCourse(), CourseRole.TEACHER));
        inactive.deactivate();
        entities.flush();
        for (User denied : new User[]{fixture.student(), other.teacher(), inactiveTeacher, user()}) {
            mvc.perform(post(createPath(fixture)).header("Authorization", TestTokens.bearer(denied))
                    .contentType(MediaType.APPLICATION_JSON).content(createBody("\"code\"")))
                .andExpect(status().isForbidden());
        }
        assertEquals(2L, entities.createQuery("select count(e) from CodingExercise e", Long.class).getSingleResult());
    }

    @Test
    void nonEnrolledAndInactiveUsersCannotRead() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        User inactiveStudent = user();
        User inactiveTeacher = user();
        for (User denied : new User[]{inactiveStudent, inactiveTeacher}) {
            CourseRole role = denied == inactiveStudent ? CourseRole.STUDENT : CourseRole.TEACHER;
            Enrollment enrollment = save(new Enrollment(denied, fixture.module().getCourse(), role));
            enrollment.deactivate();
        }
        entities.flush();
        for (User denied : new User[]{other.student(), other.teacher(), inactiveStudent, inactiveTeacher, user()}) {
            mvc.perform(get("/api/coding-exercises/{id}", fixture.exercise().getId())
                    .header("Authorization", TestTokens.bearer(denied)))
                .andExpect(status().isForbidden());
        }
    }

    @Test
    void teacherCannotCreateInModuleOfAnotherCourse() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        mvc.perform(post("/api/courses/{courseId}/modules/{moduleId}/contents/coding-exercises",
                    fixture.module().getCourse().getId(), other.module().getId())
                .header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content(createBody(null)))
            .andExpect(status().isNotFound());
        assertEquals(2L, entities.createQuery("select count(e) from CodingExercise e", Long.class).getSingleResult());
    }

    @Test
    void missingExerciseAndWrongContentTypeReturn404() throws Exception {
        Fixture fixture = fixture();
        StaticPage page = save(new StaticPage(fixture.module(), "Page", 2, "Read"));
        for (Long id : new Long[]{Long.MAX_VALUE, page.getId()}) {
            mvc.perform(get("/api/coding-exercises/{id}", id)
                    .header("Authorization", TestTokens.bearer(fixture.student())))
                .andExpect(status().isNotFound());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {10000, 10001})
    void starterCodeLengthBoundaryIsValidated(int length) throws Exception {
        Fixture fixture = fixture();
        mvc.perform(post(createPath(fixture)).header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content(createBody("\"" + "x".repeat(length) + "\"")))
            .andExpect(length == 10000 ? status().isCreated() : status().isBadRequest());
        assertEquals(length == 10000 ? 2L : 1L,
            entities.createQuery("select count(e) from CodingExercise e", Long.class).getSingleResult());
    }
}
