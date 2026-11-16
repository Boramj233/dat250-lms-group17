package no.hvl.dat250.lms.enrollment;

import java.util.UUID;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.repository.EnrollmentRepository;
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
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET/POST /api/courses/{id}/enrollments and PATCH/DELETE …/enrollments/{userId}. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EnrollmentApiTest {
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager entities;
    @Autowired private EnrollmentRepository enrollments;

    private <T> T save(T entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    private User user(String first, String last, Access access) {
        User user = new User(first.toLowerCase() + "_" + UUID.randomUUID() + "@example.com", first, last);
        user.setAccess(access);
        return save(user);
    }

    private String path(Course course) {
        return "/api/courses/" + course.getId() + "/enrollments";
    }

    @Test
    void membersSeeTheActiveMembersSortedByName() throws Exception {
        Course course = save(new Course("DAT250", "Course", Term.AUTUMN, 2026));
        User siri = user("Siri", "Berg", Access.STANDARD);
        User tore = user("Tore", "Alm", Access.STANDARD);
        User gone = user("Gone", "Aas", Access.STANDARD);
        User outsider = user("Out", "Side", Access.STANDARD);
        User manager = user("Mona", "Manager", Access.COURSE_MANAGER);
        User admin = user("Ada", "Admin", Access.ADMIN);
        save(new Enrollment(siri, course, CourseRole.STUDENT));
        save(new Enrollment(tore, course, CourseRole.TEACHER));
        save(new Enrollment(gone, course, CourseRole.STUDENT)).deactivate();
        entities.flush();

        mvc.perform(get(path(course)).header("Authorization", TestTokens.bearer(siri)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].userId").value(tore.getUserId()))
            .andExpect(jsonPath("$[0].role").value("TEACHER"))
            .andExpect(jsonPath("$[0].firstName").value("Tore"))
            .andExpect(jsonPath("$[0].lastName").value("Alm"))
            .andExpect(jsonPath("$[0].email").value(tore.getEmail()))
            .andExpect(jsonPath("$[1].userId").value(siri.getUserId()));
        // An admin who is not enrolled can read it too; an outsider cannot, not even one
        // with COURSE_MANAGER access (a teacher of other courses).
        mvc.perform(get(path(course)).header("Authorization", TestTokens.bearer(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get(path(course)).header("Authorization", TestTokens.bearer(outsider)))
            .andExpect(status().isForbidden());
        mvc.perform(get(path(course)).header("Authorization", TestTokens.bearer(manager)))
            .andExpect(status().isForbidden());
        mvc.perform(get(path(course))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/courses/999999/enrollments").header("Authorization", TestTokens.bearer(admin)))
            .andExpect(status().isNotFound());
    }

    @Test
    void teachersEnrollChangeRolesAndRemoveMembers() throws Exception {
        Course course = save(new Course("DAT250", "Course", Term.AUTUMN, 2026));
        User teacher = user("Tore", "Teacher", Access.COURSE_MANAGER);
        save(new Enrollment(teacher, course, CourseRole.TEACHER));
        User nina = user("Nina", "New", Access.STANDARD);
        String token = TestTokens.bearer(teacher);

        // Enroll: STUDENT by default; a second time is a conflict.
        mvc.perform(post(path(course)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + nina.getUserId() + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(nina.getUserId()))
            .andExpect(jsonPath("$.role").value("STUDENT"))
            .andExpect(jsonPath("$.firstName").value("Nina"));
        mvc.perform(post(path(course)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + nina.getUserId() + ",\"role\":\"TEACHER\"}"))
            .andExpect(status().isConflict());

        // Change the role.
        mvc.perform(patch(path(course) + "/" + nina.getUserId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TEACHER\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("TEACHER"));

        // Remove: the enrollment is kept, inactive; removing again is 404.
        mvc.perform(delete(path(course) + "/" + nina.getUserId()).header("Authorization", token))
            .andExpect(status().isNoContent());
        Enrollment removed = enrollments.findByUser_UserIdAndCourse_Id(nina.getUserId(), course.getId()).orElseThrow();
        assertFalse(removed.isActive());
        mvc.perform(delete(path(course) + "/" + nina.getUserId()).header("Authorization", token))
            .andExpect(status().isNotFound());
        mvc.perform(patch(path(course) + "/" + nina.getUserId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"STUDENT\"}"))
            .andExpect(status().isNotFound());

        // Enrolling again reactivates the same enrollment with the new role.
        mvc.perform(post(path(course)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + nina.getUserId() + ",\"role\":\"STUDENT\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("STUDENT"));
        Enrollment back = enrollments.findByUser_UserIdAndCourse_Id(nina.getUserId(), course.getId()).orElseThrow();
        assertTrue(back.isActive());
        assertEquals(removed.getId(), back.getId());
    }

    @Test
    void invalidRequestsAndMissingTargets() throws Exception {
        Course course = save(new Course("DAT250", "Course", Term.AUTUMN, 2026));
        User teacher = user("Tore", "Teacher", Access.COURSE_MANAGER);
        save(new Enrollment(teacher, course, CourseRole.TEACHER));
        User admin = user("Ada", "Admin", Access.ADMIN);
        String token = TestTokens.bearer(teacher);

        mvc.perform(post(path(course)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(path(course)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"role\":\"OWNER\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(path(course)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":999999}"))
            .andExpect(status().isNotFound());
        // Nobody teaches a course that does not exist, so only an admin gets as far as 404.
        mvc.perform(post("/api/courses/999999/enrollments").header("Authorization", TestTokens.bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + teacher.getUserId() + "}"))
            .andExpect(status().isNotFound());
        mvc.perform(patch(path(course) + "/" + teacher.getUserId()).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void onlyTeachersOfTheCourseAndAdminsMayChangeMembers() throws Exception {
        Course course = save(new Course("DAT250", "Course", Term.AUTUMN, 2026));
        Course other = save(new Course("DAT100", "Other course", Term.AUTUMN, 2026));
        User teacher = user("Tore", "Teacher", Access.COURSE_MANAGER);
        User otherTeacher = user("Olav", "Other", Access.COURSE_MANAGER);
        User student = user("Siri", "Student", Access.STANDARD);
        User admin = user("Ada", "Admin", Access.ADMIN);
        User nina = user("Nina", "New", Access.STANDARD);
        save(new Enrollment(teacher, course, CourseRole.TEACHER));
        save(new Enrollment(otherTeacher, other, CourseRole.TEACHER));
        save(new Enrollment(student, course, CourseRole.STUDENT));
        String body = "{\"userId\":" + nina.getUserId() + "}";

        // A student cannot change members. Nor can a teacher of another course: COURSE_MANAGER
        // access (which every teacher has) gives no rights in a course they do not teach.
        mvc.perform(patch(path(course) + "/" + student.getUserId()).header("Authorization", TestTokens.bearer(student))
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TEACHER\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post(path(course)).header("Authorization", TestTokens.bearer(otherTeacher))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mvc.perform(delete(path(course) + "/" + student.getUserId()).header("Authorization", TestTokens.bearer(otherTeacher)))
            .andExpect(status().isForbidden());

        // A teacher of the course and an admin can.
        mvc.perform(post(path(course)).header("Authorization", TestTokens.bearer(teacher))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mvc.perform(delete(path(course) + "/" + student.getUserId()).header("Authorization", TestTokens.bearer(admin)))
            .andExpect(status().isNoContent());
    }
}
