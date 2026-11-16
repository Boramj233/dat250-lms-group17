
package no.hvl.dat250.lms.content;

import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.api.controllers.CodingExerciseController;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.service.CodingExerciseService;
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
class CodingExerciseHttpTest {

    @Mock
    private CodingExerciseService exerciseService;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {

        // DTO/controller tests only; real filters are covered by CodingExerciseSecurityIntegrationTest.
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(new AuthenticatedUser(5L), null));
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
            .standaloneSetup(
                new CodingExerciseController(exerciseService)
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

    private CodingExercise savedExercise() {

        CodingExercise exercise = mock(CodingExercise.class);
        Module module = mock(Module.class);

        when(exercise.getId()).thenReturn(42L);
        when(exercise.getModule()).thenReturn(module);
        when(module.getId()).thenReturn(10L);

        when(exercise.getTitle())
            .thenReturn("REST Exercise");

        when(exercise.getPosition()).thenReturn(2);

        when(exercise.getInstructions())
            .thenReturn("Implement GET and POST endpoints.");

        when(exercise.getStarterRepositoryUrl())
            .thenReturn("https://gitlab.com/example/starter");

        when(exercise.isAllowLateSubmission())
            .thenReturn(true);

        return exercise;
    }

    @Test
    void createReturns201() throws Exception {

        CodingExercise exercise = savedExercise();

        when(exerciseService.createCodingExercise(
            eq(1L),
            eq(10L),
            eq("REST Exercise"),
            eq(2),
            eq("Implement GET and POST endpoints."),
            eq("https://gitlab.com/example/starter"),
            isNull(),
            isNull(),
            eq(true),
            eq(5L)
        )).thenReturn(exercise);

        mockMvc.perform(
            post("/api/courses/1/modules/10/contents/coding-exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "REST Exercise",
                      "position": 2,
                      "instructions": "Implement GET and POST endpoints.",
                      "starterRepositoryUrl": "https://gitlab.com/example/starter",
                      "deadline": null,
                      "allowLateSubmission": true
                    }
                    """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(jsonPath("$.moduleId").value(10))
        .andExpect(jsonPath("$.title").value("REST Exercise"))
        .andExpect(jsonPath("$.allowLateSubmission").value(true))
        .andExpect(
            jsonPath("$.starterRepositoryUrl")
                .value("https://gitlab.com/example/starter")
        );
    }

    @Test
    void getReturns200() throws Exception {

        CodingExercise exercise = savedExercise();

        when(exerciseService.findCodingExercise(42L))
            .thenReturn(exercise);

        mockMvc.perform(
            get("/api/coding-exercises/42")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(
            jsonPath("$.instructions")
                .value("Implement GET and POST endpoints.")
        );
    }

    @Test
    void blankInstructionsReturns400() throws Exception {

        mockMvc.perform(
            post("/api/courses/1/modules/10/contents/coding-exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "REST Exercise",
                      "position": 2,
                      "instructions": "   ",
                      "starterRepositoryUrl": "https://gitlab.com/example/starter"
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(exerciseService);
    }

    @Test
    void blankStarterRepositoryReturns400() throws Exception {

        mockMvc.perform(
            post("/api/courses/1/modules/10/contents/coding-exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "REST Exercise",
                      "position": 2,
                      "instructions": "Implement an API",
                      "starterRepositoryUrl": "   "
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(exerciseService);
    }

    @Test
    void negativePositionReturns400() throws Exception {

        mockMvc.perform(
            post("/api/courses/1/modules/10/contents/coding-exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "REST Exercise",
                      "position": -1,
                      "instructions": "Implement an API",
                      "starterRepositoryUrl": "https://gitlab.com/example/starter"
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(exerciseService);
    }

    @Test
    void missingExerciseReturns404() throws Exception {

        when(exerciseService.findCodingExercise(999L))
            .thenThrow(
                new NoSuchElementException(
                    "Coding exercise not found"
                )
            );

        mockMvc.perform(
            get("/api/coding-exercises/999")
        )
        .andExpect(status().isNotFound());
    }
}
