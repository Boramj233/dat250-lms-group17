
package no.hvl.dat250.lms.content;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.repository.ContentRepository;
import no.hvl.dat250.lms.content.service.CodingExerciseService;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import org.springframework.security.access.AccessDeniedException;
import no.hvl.dat250.lms.module.service.ModuleService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodingExerciseServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ModuleService moduleService;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private ContentService contentService;

    @InjectMocks
    private CodingExerciseService exerciseService;

    @Test
    void createsCodingExerciseInExistingModule() {

        Module module = mock(Module.class);

        Instant deadline =
            Instant.parse("2026-11-25T23:00:00Z");

        when(moduleService.findModuleInCourse(1L, 10L))
            .thenReturn(Optional.of(module));

        when(contentRepository.save(any(CodingExercise.class)))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        CodingExercise result =
            exerciseService.createCodingExercise(
                1L,
                10L,
                "REST Exercise",
                2,
                "Implement GET and POST endpoints.",
                "https://gitlab.com/example/starter",
                "  // TODO\n",
                deadline,
                false,
                5L
            );

        assertSame(module, result.getModule());
        assertEquals("REST Exercise", result.getTitle());
        assertEquals(2, result.getPosition());

        assertEquals(
            "Implement GET and POST endpoints.",
            result.getInstructions()
        );

        assertEquals(
            "https://gitlab.com/example/starter",
            result.getStarterRepositoryUrl()
        );

        assertEquals("  // TODO\n", result.getStarterCode());
        assertEquals(deadline, result.getDeadline());
        assertFalse(result.isAllowLateSubmission());

        verify(contentRepository).save(result);
        verify(enrollmentService).requireActiveTeacher(5L, 1L);
    }

    @Test
    void nonTeacherCannotCreateExercise() {
        doThrow(new AccessDeniedException("Active teacher enrollment required"))
            .when(enrollmentService).requireActiveTeacher(5L, 1L);

        assertThrows(AccessDeniedException.class, () -> exerciseService.createCodingExercise(
            1L, 10L, "REST", 0, "Implement", "https://example.com/starter", null, null, false, 5L));

        verifyNoInteractions(moduleService, contentRepository);
    }

    @Test
    void missingModuleIsRejected() {

        when(moduleService.findModuleInCourse(1L, 99L))
            .thenReturn(Optional.empty());

        assertThrows(
            NoSuchElementException.class,
            () -> exerciseService.createCodingExercise(
                1L,
                99L,
                "REST Exercise",
                0,
                "Implement REST endpoints.",
                "https://gitlab.com/example/starter",
                null,
                null,
                false,
                5L
            )
        );

        verifyNoInteractions(contentRepository);
    }

    @Test
    void blankTitleIsRejected() {

        assertThrows(
            IllegalArgumentException.class,
            () -> exerciseService.createCodingExercise(
                1L,
                10L,
                "   ",
                0,
                "Implement REST endpoints.",
                "https://gitlab.com/example/starter",
                null,
                null,
                false,
                5L
            )
        );

        verifyNoInteractions(
            moduleService,
            contentRepository
        );
    }

    @Test
    void negativePositionIsRejected() {

        assertThrows(
            IllegalArgumentException.class,
            () -> exerciseService.createCodingExercise(
                1L,
                10L,
                "REST Exercise",
                -1,
                "Implement REST endpoints.",
                "https://gitlab.com/example/starter",
                null,
                null,
                false,
                5L
            )
        );

        verifyNoInteractions(
            moduleService,
            contentRepository
        );
    }

    @Test
    void oversizedStarterCodeIsRejectedBeforeSave() {
        Module module = mock(Module.class);
        when(moduleService.findModuleInCourse(1L, 10L)).thenReturn(Optional.of(module));

        assertThrows(IllegalArgumentException.class, () -> exerciseService.createCodingExercise(
            1L, 10L, "REST", 0, "Implement", "https://example.com/starter",
            "x".repeat(10001), null, false, 5L));

        verifyNoInteractions(contentRepository);
    }

    @Test
    void readsExistingCodingExercise() {

        CodingExercise exercise =
            mock(CodingExercise.class);

        when(contentRepository.findById(42L))
            .thenReturn(Optional.of(exercise));

        CodingExercise result =
            exerciseService.findCodingExercise(42L);

        assertSame(exercise, result);
    }

    @Test
    void rejectsStaticPageIdAsCodingExerciseId() {

        StaticPage page = mock(StaticPage.class);

        when(contentRepository.findById(42L))
            .thenReturn(Optional.of(page));

        assertThrows(
            NoSuchElementException.class,
            () -> exerciseService.findCodingExercise(42L)
        );
    }
}
