package no.hvl.dat250.lms.course;

import no.hvl.dat250.lms.course.api.controllers.CourseController;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.service.CourseService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Optional;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService service;

    @Test
    void validCourseRequestReturns201() throws Exception {

        Course savedCourse = new Course(
            "DAT250",
            "Advanced Software Technologies",
            Term.AUTUMN,
            2026
        );

        when(service.createCourse(
            "DAT250",
            "Advanced Software Technologies",
            Term.AUTUMN,
            2026
        )).thenReturn(savedCourse);

        String json = """
            {
              "title": "DAT250",
              "description": "Advanced Software Technologies",
              "term": "AUTUMN",
              "year": 2026
            }
            """;

        mockMvc.perform(
            post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("DAT250"));
    }


    @Test
    void blankCourseTitleReturns400() throws Exception {

        String json = """
            {
            "title": " ",
            "description": "Advanced Software Technologies",
            "term": "AUTUMN",
            "year": 2026
            }
            """;

        mockMvc.perform(
            post("/api/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void existingCourseReturns200() throws Exception {

        // Arrange
        Course course = new Course(
            "DAT250",
            "Advanced Software Technologies",
            Term.AUTUMN,
            2026
        );

        when(service.findCourseById(42L))
            .thenReturn(Optional.of(course));

        // Act + Assert
        mockMvc.perform(
            get("/api/courses/42")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("DAT250"))
        .andExpect(jsonPath("$.term").value("AUTUMN"))
        .andExpect(jsonPath("$.year").value(2026));
    }

    @Test
    void missingCourseReturns404() throws Exception {

        // Arrange
        when(service.findCourseById(999L))
            .thenReturn(Optional.empty());

        // Act + Assert
        mockMvc.perform(
            get("/api/courses/999")
        )
        .andExpect(status().isNotFound());
    }
}
