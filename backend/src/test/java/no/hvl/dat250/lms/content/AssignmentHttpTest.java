
package no.hvl.dat250.lms.content;

import java.time.Instant;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.api.controllers.AssignmentController;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.service.AssignmentService;
import no.hvl.dat250.lms.module.domain.Module;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AssignmentHttpTest {

    @Mock
    private AssignmentService assignmentService;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {

        // DTO/controller test only; real filters are covered by MandatoryAssignmentIntegrationTest.
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(new AuthenticatedUser(5L), null));
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
            .standaloneSetup(
                new AssignmentController(assignmentService)
            )
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setValidator(validator)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    private Assignment savedAssignment() {

        Assignment assignment = mock(Assignment.class);
        Module module = mock(Module.class);

        when(assignment.getId()).thenReturn(42L);
        when(assignment.getModule()).thenReturn(module);
        when(module.getId()).thenReturn(10L);

        when(assignment.getTitle())
            .thenReturn("JPA Assignment");

        when(assignment.getPosition()).thenReturn(2);

        when(assignment.getInstructions())
            .thenReturn("Write a report");

        when(assignment.getDeadline()).thenReturn(
            Instant.parse("2026-11-20T23:00:00Z")
        );

        return assignment;
    }

    @Test
    void createReturns201AndAssignmentJson()
            throws Exception {

        Assignment assignment = savedAssignment();
        when(assignment.isMandatoryForExam()).thenReturn(true);

        Instant deadline =
            Instant.parse("2026-11-20T23:00:00Z");

        when(assignmentService.createAssignment(
            eq(1L),
            eq(10L),
            eq("JPA Assignment"),
            eq(2),
            eq("Write a report"),
            eq(deadline),
            eq(false),
            eq(true),
            eq(5L)
        )).thenReturn(assignment);

        mockMvc.perform(
            post("/api/courses/1/modules/10/contents/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "JPA Assignment",
                      "position": 2,
                      "instructions": "Write a report",
                      "deadline": "2026-11-20T23:00:00Z",
                      "allowLateSubmission": false,
                      "mandatoryForExam": true
                    }
                    """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(jsonPath("$.moduleId").value(10))
        .andExpect(jsonPath("$.mandatoryForExam").value(true))
        .andExpect(
            jsonPath("$.title").value("JPA Assignment")
        )
        .andExpect(
            jsonPath("$.deadline")
                .value("2026-11-20T23:00:00Z")
        );
    }

    @Test
    void getReturns200() throws Exception {

        // Prepare the mock object first.
        Assignment assignment = savedAssignment();

        // Then configure the service mock.
        when(assignmentService.findAssignment(42L))
            .thenReturn(assignment);

        mockMvc.perform(
            get("/api/assignments/42")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(
            jsonPath("$.title").value("JPA Assignment")
        )
        .andExpect(
            jsonPath("$.instructions").value("Write a report")
        );
    }

    @Test
    void blankInstructionsReturns400()
            throws Exception {

        mockMvc.perform(
            post("/api/courses/1/modules/10/contents/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "JPA Assignment",
                      "position": 2,
                      "instructions": "   ",
                      "allowLateSubmission": false
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(assignmentService);
    }

    @Test
    void missingAssignmentReturns404()
            throws Exception {

        when(assignmentService.findAssignment(999L))
            .thenThrow(
                new NoSuchElementException(
                    "Assignment not found"
                )
            );

        mockMvc.perform(
            get("/api/assignments/999")
        )
        .andExpect(status().isNotFound());
    }
}
