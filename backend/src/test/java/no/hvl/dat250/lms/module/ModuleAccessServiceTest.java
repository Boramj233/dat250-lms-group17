package no.hvl.dat250.lms.module;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;
import no.hvl.dat250.lms.module.service.ModuleAccessService;

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
    private ContentService contentService;

    @InjectMocks
    private ModuleAccessService moduleAccessService;

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private Module mockModule(Long id, List<Content> contents) {

        Module module = mock(Module.class);

        when(moduleRepository.findById(id))
                .thenReturn(Optional.of(module));

        when(module.getContents())
                .thenReturn(contents);

        return module;
    }

    private Content mockRequiredContent(Long id) {

        Content content = mock(Content.class);

        when(content.isRequiredForModuleCompletion())
                .thenReturn(true);

        when(content.getId())
                .thenReturn(id);

        return content;
    }

    // ------------------------------------------------------------
    // Module completion
    // ------------------------------------------------------------

    @Test
    void isModuleCompletedReturnsTrueWhenRequiredContentCompleted() {

        Content content = mockRequiredContent(100L);

        mockModule(MODULE_ID, List.of(content));

        when(contentService.isCompleted(100L, STUDENT_ID))
                .thenReturn(true);

        boolean completed =
                moduleAccessService.isModuleCompleted(
                        MODULE_ID, STUDENT_ID);

        assertTrue(completed);

        verify(contentService)
                .isCompleted(100L, STUDENT_ID);
    }

    @Test
    void isModuleCompletedReturnsFalseWhenRequiredContentIncomplete() {

        Content content = mockRequiredContent(100L);

        mockModule(MODULE_ID, List.of(content));

        when(contentService.isCompleted(100L, STUDENT_ID))
                .thenReturn(false);

        boolean completed =
                moduleAccessService.isModuleCompleted(
                        MODULE_ID, STUDENT_ID);

        assertFalse(completed);

        verify(contentService)
                .isCompleted(100L, STUDENT_ID);
    }

    @Test
    void isModuleCompletedThrowsWhenModuleNotFound() {

        when(moduleRepository.findById(MODULE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> moduleAccessService.isModuleCompleted(
                        MODULE_ID, STUDENT_ID)
        );

        verifyNoInteractions(contentService);
    }

    @Test
    void isModuleCompletedReturnsTrueWhenNoRequiredContent() {

        mockModule(MODULE_ID, List.of());

        boolean completed =
                moduleAccessService.isModuleCompleted(
                        MODULE_ID, STUDENT_ID);

        assertTrue(completed);

        verifyNoInteractions(contentService);
    }

    @Test
    void isModuleCompletedIgnoresOptionalContent() {

        Content optionalContent = mock(Content.class);

        when(optionalContent.isRequiredForModuleCompletion())
                .thenReturn(false);

        mockModule(MODULE_ID, List.of(optionalContent));

        boolean completed =
                moduleAccessService.isModuleCompleted(
                        MODULE_ID, STUDENT_ID);

        assertTrue(completed);

        verifyNoInteractions(contentService);
    }

    @Test
    void isModuleCompletedReturnsFalseWhenOneOfMultipleRequiredContentsIncomplete() {

        Content first = mockRequiredContent(100L);
        Content second = mockRequiredContent(101L);

        mockModule(MODULE_ID, List.of(first, second));

        when(contentService.isCompleted(100L, STUDENT_ID))
                .thenReturn(true);

        when(contentService.isCompleted(101L, STUDENT_ID))
                .thenReturn(false);

        boolean completed =
                moduleAccessService.isModuleCompleted(
                        MODULE_ID, STUDENT_ID);

        assertFalse(completed);

        verify(contentService).isCompleted(100L, STUDENT_ID);
        verify(contentService).isCompleted(101L, STUDENT_ID);
    }

    // ------------------------------------------------------------
    // Module visibility
    // ------------------------------------------------------------

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

        verifyNoInteractions(contentService);
    }

    @Test
    void isModuleVisibleReturnsTrueWhenAllDependenciesCompleted() {

        Module module = mock(Module.class);
        Module dependency = mock(Module.class);

        Content content = mockRequiredContent(100L);

        when(moduleRepository.findById(MODULE_ID))
                .thenReturn(Optional.of(module));

        when(module.getDependencies())
                .thenReturn(Set.of(dependency));

        when(dependency.getId())
                .thenReturn(2L);

        when(moduleRepository.findById(2L))
                .thenReturn(Optional.of(dependency));

        when(dependency.getContents())
                .thenReturn(List.of(content));

        when(contentService.isCompleted(100L, STUDENT_ID))
                .thenReturn(true);

        boolean visible =
                moduleAccessService.isModuleVisible(
                        MODULE_ID, STUDENT_ID);

        assertTrue(visible);

        verify(contentService).isCompleted(100L, STUDENT_ID);
    }

    @Test
    void isModuleVisibleReturnsFalseWhenDependencyIncomplete() {

        Module module = mock(Module.class);
        Module dependency = mock(Module.class);

        Content content = mockRequiredContent(100L);

        when(moduleRepository.findById(MODULE_ID))
                .thenReturn(Optional.of(module));

        when(module.getDependencies())
                .thenReturn(Set.of(dependency));

        when(dependency.getId())
                .thenReturn(2L);

        when(moduleRepository.findById(2L))
                .thenReturn(Optional.of(dependency));

        when(dependency.getContents())
                .thenReturn(List.of(content));

        when(contentService.isCompleted(100L, STUDENT_ID))
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

    // ------------------------------------------------------------
    // Require module visibility
    // ------------------------------------------------------------

    @Test
    void requireModuleVisibleThrowsWhenDependencyIncomplete() {

        Module module = mock(Module.class);
        Module dependency = mock(Module.class);

        Content content = mockRequiredContent(100L);

        when(moduleRepository.findById(MODULE_ID))
                .thenReturn(Optional.of(module));

        when(module.getDependencies())
                .thenReturn(Set.of(dependency));

        when(dependency.getId())
                .thenReturn(2L);

        when(moduleRepository.findById(2L))
                .thenReturn(Optional.of(dependency));

        when(dependency.getContents())
                .thenReturn(List.of(content));

        when(contentService.isCompleted(100L, STUDENT_ID))
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

        verifyNoInteractions(contentService);
    }
}