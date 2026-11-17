package no.hvl.dat250.lms.module;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;
import no.hvl.dat250.lms.module.service.ModuleAccessService;
import no.hvl.dat250.lms.interaction.service.StudentInteractionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleAccessServiceTest {

    private static final Long STUDENT_ID = 10L;
    private static final Long MODULE_ID = 1L;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private StudentInteractionService interactionService;

    @InjectMocks
    private ModuleAccessService moduleAccessService;

    @Test
    void isModuleCompletedReturnsTrueWhenRequiredContentCompleted() {

        when(moduleRepository.existsById(MODULE_ID))
                .thenReturn(true);

        when(interactionService.areRequiredContentCompleted(
                MODULE_ID, STUDENT_ID))
                .thenReturn(true);

        boolean completed =
                moduleAccessService.isModuleCompleted(
                        MODULE_ID, STUDENT_ID);

        assertTrue(completed);

        verify(interactionService)
                .areRequiredContentCompleted(MODULE_ID, STUDENT_ID);
    }

    @Test
    void isModuleCompletedReturnsFalseWhenRequiredContentIncomplete() {

        when(moduleRepository.existsById(MODULE_ID))
                .thenReturn(true);

        when(interactionService.areRequiredContentCompleted(
                MODULE_ID, STUDENT_ID))
                .thenReturn(false);

        boolean completed =
                moduleAccessService.isModuleCompleted(
                        MODULE_ID, STUDENT_ID);

        assertFalse(completed);
    }

    @Test
    void isModuleCompletedThrowsWhenModuleNotFound() {

        when(moduleRepository.existsById(MODULE_ID))
                .thenReturn(false);

        assertThrows(
                NoSuchElementException.class,
                () -> moduleAccessService.isModuleCompleted(
                        MODULE_ID, STUDENT_ID)
        );

        verifyNoInteractions(interactionService);
    }

    @Test
    void isModuleVisibleReturnsTrueWhenNoDependencies() {

        Module module = mock(Module.class);

        when(moduleRepository.findById(MODULE_ID))
                .thenReturn(Optional.of(module));

        when(module.getDependencies())
                .thenReturn(Set.of());

        boolean visible =
                moduleAccessService.isModuleVisible(
                        MODULE_ID, STUDENT_ID);

        assertTrue(visible);

        verifyNoInteractions(interactionService);
    }

    @Test
    void isModuleVisibleReturnsTrueWhenAllDependenciesCompleted() {

        Module module = mock(Module.class);
        Module dependency = mock(Module.class);

        when(moduleRepository.findById(MODULE_ID))
                .thenReturn(Optional.of(module));

        when(module.getDependencies())
                .thenReturn(Set.of(dependency));

        when(dependency.getId())
                .thenReturn(2L);

        when(moduleRepository.existsById(2L))
                .thenReturn(true);

        when(interactionService.areRequiredContentCompleted(
                2L, STUDENT_ID))
                .thenReturn(true);

        boolean visible =
                moduleAccessService.isModuleVisible(
                        MODULE_ID, STUDENT_ID);

        assertTrue(visible);
    }

    @Test
    void isModuleVisibleReturnsFalseWhenDependencyIncomplete() {

        Module module = mock(Module.class);
        Module dependency = mock(Module.class);

        when(moduleRepository.findById(MODULE_ID))
                .thenReturn(Optional.of(module));

        when(module.getDependencies())
                .thenReturn(Set.of(dependency));

        when(dependency.getId())
                .thenReturn(2L);

        when(moduleRepository.existsById(2L))
                .thenReturn(true);

        when(interactionService.areRequiredContentCompleted(
                2L, STUDENT_ID))
                .thenReturn(false);

        boolean visible =
                moduleAccessService.isModuleVisible(
                        MODULE_ID, STUDENT_ID);

        assertFalse(visible);
    }

    @Test
    void isModuleVisibleThrowsWhenModuleNotFound() {

        when(moduleRepository.findById(MODULE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> moduleAccessService.isModuleVisible(
                        MODULE_ID, STUDENT_ID)
        );
    }

    @Test
    void requireModuleVisibleThrowsWhenDependencyIncomplete() {

        Module module = mock(Module.class);
        Module dependency = mock(Module.class);

        when(moduleRepository.findById(MODULE_ID))
                .thenReturn(Optional.of(module));

        when(module.getDependencies())
                .thenReturn(Set.of(dependency));

        when(dependency.getId())
                .thenReturn(2L);

        when(moduleRepository.existsById(2L))
                .thenReturn(true);

        when(interactionService.areRequiredContentCompleted(
                2L, STUDENT_ID))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> moduleAccessService.requireModuleVisible(
                        MODULE_ID, STUDENT_ID)
        );
    }

    @Test
    void requireModuleVisibleDoesNotThrowWhenAccessible() {

        Module module = mock(Module.class);

        when(moduleRepository.findById(MODULE_ID))
                .thenReturn(Optional.of(module));

        when(module.getDependencies())
                .thenReturn(Set.of());

        assertDoesNotThrow(
                () -> moduleAccessService.requireModuleVisible(
                        MODULE_ID, STUDENT_ID)
        );
    }
}