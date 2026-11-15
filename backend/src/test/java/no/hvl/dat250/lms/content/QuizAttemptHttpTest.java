
package no.hvl.dat250.lms.content;

import java.time.Instant;

import no.hvl.dat250.lms.content.api.controllers.QuizAttemptController;
import no.hvl.dat250.lms.content.domain.QuizAttempt;
import no.hvl.dat250.lms.content.service.QuizAttemptService;
import no.hvl.dat250.lms.security.AuthenticatedUser;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptHttpTest {

    @Mock
    private QuizAttemptService attemptService;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        // Controller/DTO test only; real token validation is covered by SecurityIntegrationTest.
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(new AuthenticatedUser(5L), null));
        QuizAttemptController controller =
            new QuizAttemptController(
                attemptService
            );

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setValidator(validator)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    @Test
    void rejectsEmptyAnswers()
            throws Exception {

        mockMvc.perform(
            post("/api/quizzes/7/attempts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "answers": []
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(attemptService);
    }

    @Test
    void rejectsInvalidQuestionId()
            throws Exception {

        mockMvc.perform(
            post("/api/quizzes/7/attempts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "answers": [
                        {
                          "questionId": -1,
                          "selectedOptionIds": [21]
                        }
                      ]
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(attemptService);
    }

    @Test
    void authenticatedSubmissionReturns201()
            throws Exception {

        QuizAttempt savedAttempt =
            mock(QuizAttempt.class);

        when(savedAttempt.getId()).thenReturn(100L);

        when(savedAttempt.getSubmittedAt()).thenReturn(
            Instant.parse("2026-11-09T12:00:00Z")
        );

        when(savedAttempt.getCorrectCount()).thenReturn(1);
        when(savedAttempt.getTotalQuestions()).thenReturn(2);

        when(attemptService.submitAttempt(
            eq(7L),
            eq(5L),
            anyList()
        )).thenReturn(savedAttempt);

        mockMvc.perform(
            post("/api/quizzes/7/attempts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "answers": [
                        {
                          "questionId": 11,
                          "selectedOptionIds": [21, 23]
                        }
                      ]
                    }
                    """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(100))
        .andExpect(jsonPath("$.quizId").value(7))
        .andExpect(jsonPath("$.correctCount").value(1))
        .andExpect(jsonPath("$.totalQuestions").value(2));

        verify(attemptService).submitAttempt(
            eq(7L),
            eq(5L),
            argThat(answers ->
                answers.size() == 1
                && answers.get(0).questionId().equals(11L)
                && answers.get(0).selectedOptionIds()
                    .equals(java.util.List.of(21L, 23L))
            )
        );
    }
}
