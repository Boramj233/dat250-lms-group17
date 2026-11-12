package no.hvl.dat250.lms.module;

import java.util.List;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.module.api.controllers.ModuleController;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.service.ModuleService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import no.hvl.dat250.lms.security.WithApiSecurity;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static no.hvl.dat250.lms.security.TestTokens.apiUser;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModuleController.class)
@WithApiSecurity
class ModuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModuleService moduleService;

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
    void createModuleReturns201() throws Exception {

        Module savedModule =
            createTestModule("Java Refresh", 0, 7L);

        when(moduleService.createModule(
                42L, "Java Refresh", 0))
            .thenReturn(savedModule);

        mockMvc.perform(post("/api/courses/42/modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Java Refresh",
                      "position": 0
                    }
                    """).with(apiUser()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.courseId").value(42))
            .andExpect(jsonPath("$.title").value("Java Refresh"))
            .andExpect(jsonPath("$.position").value(0));

        verify(moduleService).createModule(
            42L, "Java Refresh", 0);
    }

    @Test
    void invalidModuleReturns400() throws Exception {

        mockMvc.perform(post("/api/courses/42/modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "",
                      "position": -1
                    }
                    """).with(apiUser()))
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
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Java Refresh",
                      "position": 0
                    }
                    """).with(apiUser()))
            .andExpect(status().isNotFound());
    }

    @Test
    void getModulesReturns200AndOrderedList() throws Exception {

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
    }

    @Test
    void emptyCourseReturnsEmptyList() throws Exception {

        when(moduleService.findModulesByCourse(42L))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/courses/42/modules").with(apiUser()))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void missingCourseOnGetReturns404() throws Exception {

        when(moduleService.findModulesByCourse(999L))
            .thenThrow(new NoSuchElementException(
                "Course not found: 999"));

        mockMvc.perform(get("/api/courses/999/modules").with(apiUser()))
            .andExpect(status().isNotFound());
    }
}
