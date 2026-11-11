package no.hvl.dat250.lms.submission;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.identity.CurrentUserService;
import no.hvl.dat250.lms.submission.api.controllers.SubmissionController;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.service.SubmissionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SubmissionReadHttpTest {

    @Mock
    private SubmissionService submissionService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ObjectProvider<CurrentUserService> currentUserProvider;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
            .standaloneSetup(new SubmissionController(
                submissionService,
                currentUserProvider
            ))
            .setValidator(validator)
            .build();
    }

    @AfterEach
    void tearDown() {
        validator.destroy();
    }

    private void authenticatedStudent() {

        when(currentUserProvider.getIfAvailable())
            .thenReturn(currentUserService);

        when(currentUserService.requireCurrentUserId())
            .thenReturn(5L);
    }

    private Submission savedSubmission(
            Long submissionId,
            String payload) {

        Content content = mock(Content.class);
        Submission submission = mock(Submission.class);

        when(content.getId()).thenReturn(30L);
        when(submission.getId()).thenReturn(submissionId);
        when(submission.getContent()).thenReturn(content);
        when(submission.getType())
            .thenReturn(SubmissionType.TEXT);
        when(submission.getPayload()).thenReturn(payload);
        when(submission.getSubmittedAt())
            .thenReturn(Instant.parse(
                "2026-11-11T10:00:00Z"
            ));

        return submission;
    }

    @Test
    void ownSubmissionReturns200() throws Exception {

        authenticatedStudent();

        Submission saved = savedSubmission(
            101L, "My solution"
        );

        when(submissionService.findOwnSubmission(101L, 5L))
            .thenReturn(saved);

        mockMvc.perform(get("/api/submissions/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(101))
            .andExpect(jsonPath("$.contentId").value(30))
            .andExpect(jsonPath("$.payload")
                .value("My solution"));
    }

    @Test
    void otherUsersSubmissionReturns404() throws Exception {

        authenticatedStudent();

        when(submissionService.findOwnSubmission(101L, 5L))
            .thenThrow(new NoSuchElementException(
                "Submission not found"
            ));

        mockMvc.perform(get("/api/submissions/101"))
            .andExpect(status().isNotFound());
    }

    @Test
    void ownSubmissionHistoryReturns200() throws Exception {

        authenticatedStudent();

        Submission first = savedSubmission(
            101L, "First version"
        );
        Submission second = savedSubmission(
            102L, "Updated version"
        );

        when(submissionService.findMySubmissionHistory(30L, 5L))
            .thenReturn(List.of(second, first));

        mockMvc.perform(
            get("/api/contents/30/submissions/me")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(102))
        .andExpect(jsonPath("$[1].id").value(101));
    }

    @Test
    void emptyHistoryReturnsEmptyArray() throws Exception {

        authenticatedStudent();

        when(submissionService.findMySubmissionHistory(30L, 5L))
            .thenReturn(List.of());

        mockMvc.perform(
            get("/api/contents/30/submissions/me")
        )
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
    }

    @Test
    void missingIdentityProviderReturns503() throws Exception {

        mockMvc.perform(get("/api/submissions/101"))
            .andExpect(status().isServiceUnavailable());

        mockMvc.perform(
            get("/api/contents/30/submissions/me")
        )
        .andExpect(status().isServiceUnavailable());

        verifyNoInteractions(submissionService);
    }
}
