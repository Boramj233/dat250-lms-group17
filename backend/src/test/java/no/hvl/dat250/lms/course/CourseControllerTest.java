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
}
