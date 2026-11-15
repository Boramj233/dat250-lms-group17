
package no.hvl.dat250.lms.submission;

import java.time.Instant;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import no.hvl.dat250.lms.submission.api.controllers.SubmissionController;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.service.SubmissionService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SubmissionHttpTest {

    @Mock
    private SubmissionService submissionService;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        // Controller/DTO test only; real token validation is covered by SecurityIntegrationTest.
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(new AuthenticatedUser(5L), null));
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        SubmissionController controller =
            new SubmissionController(
                submissionService
            );

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setValidator(validator)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.destroy();
    }

    private Submission savedSubmission() {

        Content content = mock(Content.class);
        Submission submission = mock(Submission.class);

        when(content.getId()).thenReturn(30L);
        when(submission.getContent()).thenReturn(content);
        when(submission.getId()).thenReturn(101L);
        when(submission.getType())
            .thenReturn(SubmissionType.TEXT);
        when(submission.getPayload())
            .thenReturn("My assignment answer");
        when(submission.getSubmittedAt())
            .thenReturn(Instant.parse(
                "2026-11-11T10:00:00Z"
            ));

        return submission;
    }

    @Test
    void validSubmissionReturns201() throws Exception {

        // Prepare this mock BEFORE configuring service stubbing.
        Submission saved = savedSubmission();

        when(submissionService.submitIndividual(
            30L,
            5L,
            SubmissionType.TEXT,
            "My assignment answer"
        )).thenReturn(saved);

        mockMvc.perform(
            post("/api/contents/30/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "type": "TEXT",
                      "payload": "My assignment answer"
                    }
                    """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(101))
        .andExpect(jsonPath("$.contentId").value(30))
        .andExpect(jsonPath("$.type").value("TEXT"))
        .andExpect(jsonPath("$.late").value(false));

        verify(submissionService).submitIndividual(
            30L,
            5L,
            SubmissionType.TEXT,
            "My assignment answer"
        );
    }

    @Test
    void blankPayloadReturns400() throws Exception {

        mockMvc.perform(
            post("/api/contents/30/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "type": "TEXT",
                      "payload": "   "
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(submissionService);
    }

    @Test
    void missingTypeReturns400() throws Exception {

        mockMvc.perform(
            post("/api/contents/30/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "payload": "My answer"
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(submissionService);
    }

    @Test
    void missingContentReturns404() throws Exception {

        when(submissionService.submitIndividual(
            30L,
            5L,
            SubmissionType.TEXT,
            "My answer"
        )).thenThrow(
            new NoSuchElementException("Content not found")
        );

        mockMvc.perform(
            post("/api/contents/30/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "type": "TEXT",
                      "payload": "My answer"
                    }
                    """)
        )
        .andExpect(status().isNotFound());
    }
}
