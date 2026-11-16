package no.hvl.dat250.lms.enrollment;

import java.util.UUID;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.security.TestTokens;
import no.hvl.dat250.lms.user.domain.Access;
import no.hvl.dat250.lms.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/users/{userId}/courses. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserCoursesApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager entities;

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user(String name, Access access) {
        User user = new User(name.toLowerCase() + "_" + UUID.randomUUID() + "@example.com", name, "Test");
        user.setAccess(access);
        return save(user);
    }

    @Test
    void theUserGetsTheirActiveCoursesWithTheirRoleNewestTermFirst() throws Exception {
        User siri = user("Siri", Access.STANDARD);
        Course spring = save(new Course("DAT251", "Spring course", Term.SPRING, 2026));
        Course autumn = save(new Course("DAT250", "Autumn course", Term.AUTUMN, 2026));
        Course older = save(new Course("DAT109", "Last year", Term.AUTUMN, 2025));
        Course left = save(new Course("DAT999", "Left", Term.AUTUMN, 2026));
        save(new Course("DAT000", "Not mine", Term.AUTUMN, 2026));
        save(new Enrollment(siri, spring, CourseRole.STUDENT));
        save(new Enrollment(siri, autumn, CourseRole.TEACHER));
        save(new Enrollment(siri, older, CourseRole.STUDENT));
        save(new Enrollment(siri, left, CourseRole.STUDENT)).deactivate();
        entities.flush();

        mvc.perform(get("/api/users/" + siri.getUserId() + "/courses")
                .header("Authorization", TestTokens.bearer(siri)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].id").value(autumn.getId()))
            .andExpect(jsonPath("$[0].title").value("DAT250"))
            .andExpect(jsonPath("$[0].description").value("Autumn course"))
            .andExpect(jsonPath("$[0].term").value("AUTUMN"))
            .andExpect(jsonPath("$[0].year").value(2026))
            .andExpect(jsonPath("$[0].role").value("TEACHER"))
            .andExpect(jsonPath("$[1].id").value(spring.getId()))
            .andExpect(jsonPath("$[1].role").value("STUDENT"))
            .andExpect(jsonPath("$[2].id").value(older.getId()));
    }

    @Test
    void onlyTheUserThemselvesOrAnAdmin() throws Exception {
        User siri = user("Siri", Access.STANDARD);
        User other = user("Ola", Access.COURSE_MANAGER);
        User admin = user("Ada", Access.ADMIN);
        save(new Enrollment(siri, save(new Course("DAT250", "Course", Term.AUTUMN, 2026)), CourseRole.STUDENT));
        String path = "/api/users/" + siri.getUserId() + "/courses";

        mvc.perform(get(path).header("Authorization", TestTokens.bearer(other)))
            .andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization", TestTokens.bearer(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        // No courses: an empty list.
        mvc.perform(get("/api/users/" + other.getUserId() + "/courses")
                .header("Authorization", TestTokens.bearer(other)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
