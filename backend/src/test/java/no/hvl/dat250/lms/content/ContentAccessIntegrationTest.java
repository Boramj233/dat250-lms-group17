package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;
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

/** Real JWT filters and enrollment checks, committed fixtures and no request-spanning test transaction. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:content_access_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentAccessIntegrationTest {
    @Autowired private EntityManager entities;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private MockMvc mvc;

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

    private record Fixture(Long courseId, Long moduleId, Long pageId, Long assignmentId,
                           User teacher, User student) {
    }

    private Fixture fixture() {
        return tx(() -> {
            Course course = save(new Course("DAT250", "Content access", Term.AUTUMN, 2026));
            User teacher = user();
            User student = user();
            save(new Enrollment(teacher, course, CourseRole.TEACHER));
            save(new Enrollment(student, course, CourseRole.STUDENT));
            Module module = save(new Module(course, "Week", 0));
            StaticPage page = save(new StaticPage(module, "Page", 0, "Course material"));
            Assignment assignment = save(new Assignment(module, "Report", 1, "Write a report", null, false));
            return new Fixture(course.getId(), module.getId(), page.getId(), assignment.getId(), teacher, student);
        });
    }

    private User inactive(Fixture fixture, CourseRole role) {
        return tx(() -> {
            User user = user();
            Enrollment enrollment = save(new Enrollment(user, entities.find(Course.class, fixture.courseId()), role));
            enrollment.deactivate();
            return user;
        });
    }

    private String modulePath(Long courseId, Long moduleId) {
        return "/api/courses/" + courseId + "/modules/" + moduleId + "/contents";
    }

    private String createPath(Fixture fixture) {
        return modulePath(fixture.courseId(), fixture.moduleId()) + "/pages";
    }

    private String createBody() {
        return """
            {"title":"New page","position":2,"body":"New course material"}
            """;
    }

    private List<String> readPaths(Fixture fixture) {
        return List.of(modulePath(fixture.courseId(), fixture.moduleId()),
            "/api/contents/" + fixture.pageId(), "/api/assignments/" + fixture.assignmentId());
    }

    @Test
    void activeTeacherCanCreateAndPageIsPersisted() throws Exception {
        Fixture fixture = fixture();
        mvc.perform(post(createPath(fixture)).header("Authorization", TestTokens.bearer(fixture.teacher()))
                .contentType(MediaType.APPLICATION_JSON).content(createBody()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.courseId").value(fixture.courseId()))
            .andExpect(jsonPath("$.moduleId").value(fixture.moduleId()))
            .andExpect(jsonPath("$.body").value("New course material"));
        assertEquals(1L, tx(() -> entities.createQuery(
                "select count(p) from StaticPage p where p.module.id = :moduleId and p.title = 'New page'", Long.class)
            .setParameter("moduleId", fixture.moduleId()).getSingleResult()));
    }

    @Test
    void activeTeacherAndStudentCanReadCourseContents() throws Exception {
        Fixture fixture = fixture();
        for (User reader : List.of(fixture.teacher(), fixture.student())) {
            String token = TestTokens.bearer(reader);
            mvc.perform(get(modulePath(fixture.courseId(), fixture.moduleId())).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(fixture.pageId()))
                .andExpect(jsonPath("$[1].id").value(fixture.assignmentId()));
            mvc.perform(get("/api/contents/{id}", fixture.pageId()).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.body").value("Course material"));
            mvc.perform(get("/api/assignments/{id}", fixture.assignmentId()).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.instructions").value("Write a report"));
        }
    }

    @Test
    void studentsOutsidersInactiveUsersAndGlobalAdminCannotCreatePages() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        User unenrolled = tx(this::user);
        User admin = tx(() -> {
            User user = user();
            user.setAccess(Access.ADMIN);
            return user;
        });
        long before = tx(() -> entities.createQuery("select count(p) from StaticPage p", Long.class).getSingleResult());
        for (User denied : List.of(fixture.student(), other.teacher(), other.student(),
                inactive(fixture, CourseRole.TEACHER), inactive(fixture, CourseRole.STUDENT), unenrolled, admin)) {
            mvc.perform(post(createPath(fixture)).header("Authorization", TestTokens.bearer(denied))
                    .contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isForbidden());
        }
        assertEquals(before, tx(() -> entities.createQuery("select count(p) from StaticPage p", Long.class).getSingleResult()));
    }

    @Test
    void outsidersInactiveUsersAndGlobalAdminCannotReadCourseContents() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        User admin = tx(() -> {
            User user = user();
            user.setAccess(Access.ADMIN);
            return user;
        });
        for (User denied : List.of(other.teacher(), other.student(), inactive(fixture, CourseRole.TEACHER),
                inactive(fixture, CourseRole.STUDENT), tx(this::user), admin)) {
            for (String path : readPaths(fixture)) {
                mvc.perform(get(path).header("Authorization", TestTokens.bearer(denied)))
                    .andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void allFourEndpointsRejectUnauthenticatedRequests() throws Exception {
        Fixture fixture = fixture();
        mvc.perform(post(createPath(fixture)).contentType(MediaType.APPLICATION_JSON).content(createBody()))
            .andExpect(status().isUnauthorized());
        for (String path : readPaths(fixture)) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void courseModuleMismatchAndMissingModuleAreNotFoundForAuthorizedTeacher() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        String token = TestTokens.bearer(fixture.teacher());
        for (Long moduleId : List.of(other.moduleId(), Long.MAX_VALUE)) {
            String path = modulePath(fixture.courseId(), moduleId);
            mvc.perform(get(path).header("Authorization", token)).andExpect(status().isNotFound());
            mvc.perform(post(path + "/pages").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isNotFound());
        }
        mvc.perform(get(modulePath(other.courseId(), fixture.moduleId())).header("Authorization", token))
            .andExpect(status().isForbidden());
    }

    @Test
    void missingResourcesAndWrongContentTypesReturn404() throws Exception {
        Fixture fixture = fixture();
        String token = TestTokens.bearer(fixture.student());
        for (String path : List.of("/api/contents/" + Long.MAX_VALUE,
                "/api/assignments/" + Long.MAX_VALUE, "/api/contents/" + fixture.assignmentId(),
                "/api/assignments/" + fixture.pageId())) {
            mvc.perform(get(path).header("Authorization", token)).andExpect(status().isNotFound());
        }
    }

    @Test
    void rolesArePerCourseRatherThanGlobal() throws Exception {
        Fixture fixture = fixture();
        Fixture other = fixture();
        tx(() -> save(new Enrollment(fixture.student(), entities.find(Course.class, other.courseId()), CourseRole.TEACHER)));
        String token = TestTokens.bearer(fixture.student());
        mvc.perform(post(createPath(other)).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(createBody())).andExpect(status().isCreated());
        mvc.perform(post(createPath(fixture)).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(createBody())).andExpect(status().isForbidden());
    }

    @Test
    void enrolledStudentCanReadAnEmptyModule() throws Exception {
        Fixture fixture = fixture();
        Long moduleId = tx(() -> save(new Module(entities.find(Course.class, fixture.courseId()), "Empty", 1)).getId());
        mvc.perform(get(modulePath(fixture.courseId(), moduleId))
                .header("Authorization", TestTokens.bearer(fixture.student())))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
