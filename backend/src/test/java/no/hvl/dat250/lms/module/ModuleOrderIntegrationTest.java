package no.hvl.dat250.lms.module;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;
import no.hvl.dat250.lms.security.TestTokens;
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

/** PUT /api/courses/{courseId}/modules/order: the whole module order at once. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:module_order_it;DB_CLOSE_DELAY=-1",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModuleOrderIntegrationTest {
    @Autowired private EntityManager entities;
    @Autowired private MockMvc mvc;
    @Autowired private ModuleRepository modules;

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user() {
        return save(new User(UUID.randomUUID() + "@example.com", "Test", "User"));
    }

    private record Fixture(Course course, User teacher, User student, User otherTeacher,
                           Module first, Module second, Module third, Module elsewhere) {}

    private Fixture fixture() {
        Course course = save(new Course("DAT250", "Order", Term.AUTUMN, 2026));
        Course other = save(new Course("DAT251", "Other", Term.AUTUMN, 2026));
        User teacher = user(), student = user(), otherTeacher = user();
        save(new Enrollment(teacher, course, CourseRole.TEACHER));
        save(new Enrollment(student, course, CourseRole.STUDENT));
        save(new Enrollment(otherTeacher, other, CourseRole.TEACHER));
        return new Fixture(course, teacher, student, otherTeacher,
            save(new Module(course, "First", 0)), save(new Module(course, "Second", 1)),
            save(new Module(course, "Third", 2)), save(new Module(other, "Elsewhere", 0)));
    }

    private String order(Long... ids) {
        return "{\"moduleIds\":" + List.of(ids) + "}";
    }

    private String path(Fixture f) {
        return "/api/courses/" + f.course().getId() + "/modules/order";
    }

    @Test
    void teacherSetsTheWholeOrderAtOnce() throws Exception {
        Fixture f = fixture();
        mvc.perform(put(path(f)).header("Authorization", TestTokens.bearer(f.teacher()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(order(f.third().getId(), f.first().getId(), f.second().getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Third"))
            .andExpect(jsonPath("$[0].position").value(0))
            .andExpect(jsonPath("$[2].title").value("Second"))
            .andExpect(jsonPath("$[2].position").value(2));
        entities.clear();
        assertEquals(List.of("Third", "First", "Second"),
            modules.findByCourse_IdOrderByPositionAsc(f.course().getId()).stream().map(Module::getTitle).toList());
    }

    @Test
    void anIncompleteOrRepeatedOrderIsRefusedAndChangesNothing() throws Exception {
        Fixture f = fixture();
        String token = TestTokens.bearer(f.teacher());
        // A module left out.
        mvc.perform(put(path(f)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(order(f.second().getId(), f.first().getId())))
            .andExpect(status().isBadRequest());
        // One module twice.
        mvc.perform(put(path(f)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(order(f.first().getId(), f.first().getId(), f.second().getId())))
            .andExpect(status().isBadRequest());
        // A module from another course.
        mvc.perform(put(path(f)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(order(f.first().getId(), f.second().getId(), f.elsewhere().getId())))
            .andExpect(status().isBadRequest());
        mvc.perform(put(path(f)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
        entities.clear();
        assertEquals(List.of("First", "Second", "Third"),
            modules.findByCourse_IdOrderByPositionAsc(f.course().getId()).stream().map(Module::getTitle).toList());
    }

    @Test
    void onlyTeachersOfTheCourseMayReorder() throws Exception {
        Fixture f = fixture();
        String body = order(f.third().getId(), f.second().getId(), f.first().getId());
        for (User denied : new User[]{f.student(), f.otherTeacher()}) {
            mvc.perform(put(path(f)).header("Authorization", TestTokens.bearer(denied))
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        }
        mvc.perform(put(path(f)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
    }
}
