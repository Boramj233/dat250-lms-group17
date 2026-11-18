
package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.api.controllers.ContentController;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.interaction.service.StudentInteractionService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import no.hvl.dat250.lms.security.auth.access.CourseAccess;
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

@WebMvcTest(ContentController.class)
@WithApiSecurity
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "contentService")
    private ContentService contentService;

    @MockitoBean(name = "courseAccess")
    private CourseAccess courseAccess;

    @BeforeEach
    void allowCourseAccessForControllerContractTests() {
        // Real enrollment decisions are covered by ContentAccessIntegrationTest.
        when(courseAccess.isTeacher(any(), any())).thenReturn(true);
        when(courseAccess.isEnrolled(any(), any())).thenReturn(true);
    }

    // Helper: simulate an already-persisted StaticPage
    private StaticPage createTestPage(
            Long id, String title, int position, String body) {

        Module module = mock(Module.class);

        StaticPage page = new StaticPage(
            module, title, position, body
        );

        ReflectionTestUtils.setField(page, "id", id);

        return page;
    }

    // -----------------------------------------------
    // POST: CREATE STATIC PAGE
    // -----------------------------------------------

    @Test
    void createStaticPageReturns201() throws Exception {

        StaticPage savedPage = createTestPage(
            100L, "Java Introduction", 0, "Welcome to Java!"
        );

        when(contentService.createStaticPage(
                1L, 10L, "Java Introduction", 0, "Welcome to Java!"
        )).thenReturn(savedPage);

        mockMvc.perform(
                post("/api/courses/1/modules/10/contents/pages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "Java Introduction",
                          "position": 0,
                          "body": "Welcome to Java!"
                        }
                        """)
            .with(apiUser()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.courseId").value(1))
            .andExpect(jsonPath("$.moduleId").value(10))
            .andExpect(jsonPath("$.title").value("Java Introduction"))
            .andExpect(jsonPath("$.position").value(0))
            .andExpect(jsonPath("$.body").value("Welcome to Java!"));

        verify(contentService).createStaticPage(
            1L, 10L, "Java Introduction", 0, "Welcome to Java!"
        );
    }

    @Test
    void invalidStaticPageReturns400() throws Exception {

        mockMvc.perform(
                post("/api/courses/1/modules/10/contents/pages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "",
                          "position": -1,
                          "body": " "
                        }
                        """)
            .with(apiUser()))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(contentService);
    }

    @Test
    void missingPositionReturns400() throws Exception {

        mockMvc.perform(
                post("/api/courses/1/modules/10/contents/pages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "Java Introduction",
                          "body": "Welcome to Java!"
                        }
                        """)
            .with(apiUser()))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(contentService);
    }

    @Test
    void wrongCourseModuleOnCreateReturns404() throws Exception {

        when(contentService.createStaticPage(
                2L, 10L, "Java Introduction", 0, "Welcome to Java!"
        )).thenThrow(new NoSuchElementException(
                "Module not found in course"
        ));

        mockMvc.perform(
                post("/api/courses/2/modules/10/contents/pages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "Java Introduction",
                          "position": 0,
                          "body": "Welcome to Java!"
                        }
                        """)
            .with(apiUser()))
            .andExpect(status().isNotFound());
    }

    // -----------------------------------------------
    // GET: LIST CONTENT SUMMARIES
    // -----------------------------------------------

    @Test
    void getContentsReturnsOrderedSummaries() throws Exception {

        StaticPage first = createTestPage(
            100L, "Java Introduction", 0, "First page body"
        );

        StaticPage second = createTestPage(
            101L, "Java OOP", 1, "Second page body"
        );

        when(contentService.findContentsByModule(1L, 10L))
                .thenReturn(List.of(first, second));

        mockMvc.perform(
                get("/api/courses/1/modules/10/contents")
            .with(apiUser()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(100))
            .andExpect(jsonPath("$[0].title").value("Java Introduction"))
            .andExpect(jsonPath("$[0].type").value("STATIC_PAGE"))
            .andExpect(jsonPath("$[0].position").value(0))
            .andExpect(jsonPath("$[1].title").value("Java OOP"))
            .andExpect(jsonPath("$[1].position").value(1))
            .andExpect(jsonPath("$[0].body").doesNotExist());

        verify(contentService).findContentsByModule(1L, 10L);
    }

    @Test
    void emptyModuleReturnsEmptyList() throws Exception {

        when(contentService.findContentsByModule(1L, 10L))
                .thenReturn(List.of());

        mockMvc.perform(
                get("/api/courses/1/modules/10/contents")
            .with(apiUser()))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void wrongCourseModuleOnGetReturns404() throws Exception {

        when(contentService.findContentsByModule(2L, 10L))
                .thenThrow(new NoSuchElementException(
                    "Module not found in course"
                ));

        mockMvc.perform(
                get("/api/courses/2/modules/10/contents")
            .with(apiUser()))
            .andExpect(status().isNotFound());
    }

    @Test
    void getStaticPageByIdReturns200() throws Exception {

        // Arrange
        Course course = mock(Course.class);
        Module module = mock(Module.class);

        when(course.getId()).thenReturn(1L);
        when(module.getId()).thenReturn(2L);
        when(module.getCourse()).thenReturn(course);

        StaticPage page = new StaticPage(
            module,
            "Introduction to Java",
            0,
            "Java basics"
        );

        ReflectionTestUtils.setField(page, "id", 5L);

        when(contentService.findStaticPageById(5L))
                .thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/contents/5").with(apiUser()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.courseId").value(1))
            .andExpect(jsonPath("$.moduleId").value(2))
            .andExpect(jsonPath("$.title").value("Introduction to Java"))
            .andExpect(jsonPath("$.position").value(0))
            .andExpect(jsonPath("$.body").value("Java basics"));

        verify(contentService).findStaticPageById(5L);
    }

    @Test
    void getMissingStaticPageReturns404() throws Exception {

        when(contentService.staticPageCourseId(999L))
                .thenThrow(new NoSuchElementException(
                    "Content not found: 999"
                ));

        mockMvc.perform(get("/api/contents/999").with(apiUser()))
            .andExpect(status().isNotFound());

        verify(contentService).staticPageCourseId(999L);
    }

}
