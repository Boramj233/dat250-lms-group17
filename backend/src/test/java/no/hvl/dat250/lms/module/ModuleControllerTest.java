package no.hvl.dat250.lms.module;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.module.api.controllers.ModuleController;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.service.ModuleService;
import no.hvl.dat250.lms.module.service.ModuleAccessService;

import no.hvl.dat250.lms.security.auth.access.CourseAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import no.hvl.dat250.lms.security.WithApiSecurity;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static no.hvl.dat250.lms.security.TestTokens.apiUser;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(ModuleController.class)
@WithApiSecurity
class ModuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModuleService moduleService;

    @MockitoBean
    private ModuleAccessService moduleAccessService;

    @MockitoBean(name = "courseAccess")
    private CourseAccess courseAccess;

    // Helper: create a Module for testing
    private Module createTestModule(
            String title, int position, Long id) {

        Course course = new Course(
            "DAT250",
            "Advanced Software Technologies",
            Term.AUTUMN,
            2026
        );

        Module module = new Module(course, title, position);

        // Simulate a generated database ID
        ReflectionTestUtils.setField(module, "id", id);

        return module;
    }

    @Test
    void createModuleReturns201ForCourseManager() throws Exception {

        Module savedModule =
            createTestModule("Java Refresh", 0, 7L);

        when(moduleService.createModule(
                42L, "Java Refresh", 0))
            .thenReturn(savedModule);

        mockMvc.perform(post("/api/courses/42/modules")
                .with(user("manager").roles("COURSE_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Java Refresh",
                      "position": 0
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.courseId").value(42))
            .andExpect(jsonPath("$.title").value("Java Refresh"))
            .andExpect(jsonPath("$.position").value(0));

        verify(moduleService).createModule(
            42L, "Java Refresh", 0);
    }

    @Test
    void createModuleReturns403ForStudent() throws Exception {

        mockMvc.perform(post("/api/courses/42/modules")
                .with(user("student").roles("STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Java Refresh",
                      "position": 0
                    }
                    """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moduleService);
    }

    @Test
    void updateModuleReturns200ForCourseManager() throws Exception {

        Module updated =
                createTestModule("Updated Module", 2, 7L);

        when(moduleService.updateModule(
                42L, 7L, "Updated Module", 2))
                .thenReturn(updated);

        mockMvc.perform(put("/api/courses/42/modules/7")
                        .with(user("manager").roles("COURSE_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "title": "Updated Module",
                          "position": 2
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Module"));

        verify(moduleService).updateModule(
                42L, 7L, "Updated Module", 2);
    }

    @Test
    void updateModuleReturns403ForStudent() throws Exception {

        Module updated =
                createTestModule("Updated Module", 2, 7L);

        when(moduleService.updateModule(
                42L, 7L, "Updated Module", 2))
                .thenReturn(updated);

        mockMvc.perform(put("/api/courses/42/modules/7")
                        .with(user("student").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "title": "Updated Module",
                          "position": 2
                        }
                        """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moduleService);
    }

    @Test
    void addDependencyReturns200ForCourseManager() throws Exception {

        Module module =
                createTestModule("Advanced Java", 1 ,8L);

        when(moduleService.addDependency(42L, 8L, 7L))
                .thenReturn(module);

        mockMvc.perform(post(
                "/api/courses/42/modules/8/dependencies/7")
                .with(user("manager").roles("COURSE_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.title").value("Advanced Java"))
                .andExpect(jsonPath("$.courseId").value(42));

        verify(moduleService).addDependency(42L, 8L, 7L);
    }

    @Test
    void addDependencyReturns403ForStudent() throws Exception {

        mockMvc.perform(post(
                        "/api/courses/42/modules/8/dependencies/7")
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moduleService);
    }

    @Test
    void removeDependencyReturns204ForCourseManager() throws Exception {
        mockMvc.perform(delete(
                "/api/courses/42/modules/8/dependencies/7")
                .with(user("manager").roles("COURSE_MANAGER")))
                .andExpect(status().isNoContent());

        verify(moduleService).removeDependency(42L, 8L, 7L);
    }

    @Test
    void removeDependencyReturns403ForStudent() throws Exception {

        mockMvc.perform(delete(
                        "/api/courses/42/modules/8/dependencies/7")
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moduleService);
    }

    @Test
    void invalidModuleReturns400() throws Exception {

        mockMvc.perform(post("/api/courses/42/modules")
                .with(user("manager").roles("COURSE_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "",
                      "position": -1
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(moduleService);
    }

    @Test
    void missingCourseOnCreateReturns404() throws Exception {

        when(moduleService.createModule(
                999L, "Java Refresh", 0))
            .thenThrow(new NoSuchElementException(
                "Course not found: 999"));

        mockMvc.perform(post("/api/courses/999/modules")
                .with(user("manager").roles("COURSE_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Java Refresh",
                      "position": 0
                    }
                    """))
            .andExpect(status().isNotFound());

        verify(moduleService).createModule(999L, "Java Refresh", 0);
    }

    @Test
    void getModulesReturns200AndOrderedList() throws Exception {

        when(courseAccess.isEnrolled(any(), eq(42L)))
                .thenReturn(true);

        Module first =
            createTestModule("Java Refresh", 0, 7L);

        Module second =
            createTestModule("Spring Boot", 1, 8L);

        when(moduleService.findModulesByCourse(42L))
            .thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/courses/42/modules").with(apiUser()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].title").value("Java Refresh"))
            .andExpect(jsonPath("$[1].title").value("Spring Boot"))
            .andExpect(jsonPath("$[0].courseId").value(42));

        verify(moduleService).findModulesByCourse(42L);
    }

    @Test
    void emptyCourseReturnsEmptyList() throws Exception {

        when(courseAccess.isEnrolled(any(), eq(42L)))
                .thenReturn(true);

        when(moduleService.findModulesByCourse(42L))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/courses/42/modules").with(apiUser()))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));

        verify(moduleService).findModulesByCourse(42L);
    }

    @Test
    void missingCourseOnGetReturns404() throws Exception {

        when(courseAccess.isEnrolled(any(), eq(999L)))
                .thenReturn(true);

        when(moduleService.findModulesByCourse(999L))
            .thenThrow(new NoSuchElementException(
                "Course not found: 999"));

        mockMvc.perform(get("/api/courses/999/modules").with(apiUser()))
            .andExpect(status().isNotFound());

        verify(moduleService).findModulesByCourse(999L);
    }

    @Test
    void getModulesReturns403WhenNotEnrolled() throws Exception {

        // Mockito returns false by default for boolean methods.
        mockMvc.perform(get("/api/courses/42/modules")
                        .with(apiUser()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moduleService);
    }

    @Test
    void getModuleReturns200WhenAccessible() throws Exception {

        Module module =
                createTestModule("Java Refresh", 0, 7L);

        when(courseAccess.isEnrolled(any(), eq(42L)))
                .thenReturn(true);

        // Treat the user as an enrolled teacher.
        // Teachers bypass the student prerequisite check.
        when(courseAccess.isStudent(any(), eq(42L)))
                .thenReturn(false);

        when(moduleService.findModuleInCourse(42L, 7L))
                .thenReturn(java.util.Optional.of(module));

        mockMvc.perform(get("/api/courses/42/modules/7")
                        .with(apiUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Java Refresh"));

        verify(moduleService).findModuleInCourse(42L, 7L);
        verifyNoInteractions(moduleAccessService);
    }

    @Test
    void getModuleReturns404WhenMissing() throws Exception {

        when(courseAccess.isEnrolled(any(), eq(42L)))
                .thenReturn(true);

        when(moduleService.findModuleInCourse(42L, 999L))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/courses/42/modules/999")
                        .with(apiUser()))
                .andExpect(status().isNotFound());

        verifyNoInteractions(moduleAccessService);
    }

    @Test
    void studentCanAccessUnlockedModule() throws Exception {

        Module module =
                createTestModule("Java Refresh", 0, 7L);

        when(courseAccess.isEnrolled(any(), eq(42L)))
                .thenReturn(true);

        when(courseAccess.isStudent(any(), eq(42L)))
                .thenReturn(true);

        when(moduleService.findModuleInCourse(42L, 7L))
                .thenReturn(Optional.of(module));

        mockMvc.perform(get("/api/courses/42/modules/7")
                        .with(apiUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(moduleAccessService)
                .requireModuleVisible(eq(7L), anyLong());
    }

    @Test
    void studentCannotAccessLockedModule() throws Exception {

        Module module =
                createTestModule("Advanced Java", 1, 8L);

        when(courseAccess.isEnrolled(any(), eq(42L)))
                .thenReturn(true);

        when(courseAccess.isStudent(any(), eq(42L)))
                .thenReturn(true);

        when(moduleService.findModuleInCourse(42L, 8L))
                .thenReturn(Optional.of(module));

        doThrow(new AccessDeniedException(
                "Module prerequisites are not completed"
        ))
                .when(moduleAccessService)
                .requireModuleVisible(eq(8L), anyLong());

        mockMvc.perform(get("/api/courses/42/modules/8")
                        .with(apiUser()))
                .andExpect(status().isForbidden());
    }
}
