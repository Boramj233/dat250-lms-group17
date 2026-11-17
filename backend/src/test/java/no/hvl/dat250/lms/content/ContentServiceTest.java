package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.repository.ContentRepository;
import no.hvl.dat250.lms.content.service.ContentService;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.service.ModuleService;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ModuleService moduleService;

    private ContentService createService() {
        return new ContentService(
            contentRepository,
            moduleService,
            mock(no.hvl.dat250.lms.enrollment.service.EnrollmentService.class)
        );
    }

    // -------------------------------------------------
    // CREATE STATIC PAGE
    // -------------------------------------------------

    @Test
    void createStaticPageForExistingModule() {

        // Arrange
        Module module = mock(Module.class);

        when(moduleService.findModuleInCourse(1L, 10L))
                .thenReturn(Optional.of(module));

        when(contentRepository.save(any(StaticPage.class)))
                .thenAnswer(invocation ->
                    invocation.getArgument(0));

        ContentService service = createService();

        // Act
        StaticPage result = service.createStaticPage(
            1L,   // courseId
            10L,  // moduleId
            "Java Introduction",
            0,
            "Welcome to Java!"
        );

        // Assert
        assertEquals("Java Introduction", result.getTitle());
        assertEquals(0, result.getPosition());
        assertEquals("Welcome to Java!", result.getBody());
        assertSame(module, result.getModule());

        verify(moduleService).findModuleInCourse(1L, 10L);
        verify(contentRepository).save(result);
    }

    @Test
    void missingModulePreventsStaticPageCreation() {

        // Arrange
        when(moduleService.findModuleInCourse(1L, 999L))
                .thenReturn(Optional.empty());

        ContentService service = createService();

        // Act + Assert
        assertThrows(
            NoSuchElementException.class,
            () -> service.createStaticPage(
                1L,
                999L,
                "Java Introduction",
                0,
                "Welcome to Java!"
            )
        );

        verify(moduleService).findModuleInCourse(1L, 999L);
        verifyNoInteractions(contentRepository);
    }

    @Test
    void moduleInDifferentCoursePreventsCreation() {

        // Module 10 belongs to Course 1, not Course 2.
        when(moduleService.findModuleInCourse(2L, 10L))
                .thenReturn(Optional.empty());

        ContentService service = createService();

        assertThrows(
            NoSuchElementException.class,
            () -> service.createStaticPage(
                2L,
                10L,
                "Java Introduction",
                0,
                "Welcome to Java!"
            )
        );

        verify(moduleService).findModuleInCourse(2L, 10L);
        verifyNoInteractions(contentRepository);
    }

    @Test
    void invalidTitlePreventsCreation() {

        ContentService service = createService();

        assertThrows(
            IllegalArgumentException.class,
            () -> service.createStaticPage(
                1L,
                10L,
                "",
                0,
                "Welcome to Java!"
            )
        );

        verifyNoInteractions(
            moduleService,
            contentRepository
        );
    }

    @Test
    void negativePositionPreventsCreation() {

        ContentService service = createService();

        assertThrows(
            IllegalArgumentException.class,
            () -> service.createStaticPage(
                1L,
                10L,
                "Java Introduction",
                -1,
                "Welcome to Java!"
            )
        );

        verifyNoInteractions(
            moduleService,
            contentRepository
        );
    }

    @Test
    void blankBodyPreventsCreation() {

        ContentService service = createService();

        assertThrows(
            IllegalArgumentException.class,
            () -> service.createStaticPage(
                1L,
                10L,
                "Java Introduction",
                0,
                "   "
            )
        );

        verifyNoInteractions(
            moduleService,
            contentRepository
        );
    }

    // -------------------------------------------------
    // FIND CONTENTS BY MODULE
    // -------------------------------------------------

    @Test
    void existingModuleReturnsContents() {

        // Arrange
        Module module = mock(Module.class);

        StaticPage first = new StaticPage(
            module,
            "Java Introduction",
            0,
            "Introduction"
        );

        StaticPage second = new StaticPage(
            module,
            "Java OOP",
            1,
            "Classes and Objects"
        );

        List<Content> expected = List.of(first, second);

        when(moduleService.findModuleInCourse(1L, 10L))
                .thenReturn(Optional.of(module));

        when(contentRepository
                .findByModule_IdOrderByPositionAsc(10L))
                .thenReturn(expected);

        ContentService service = createService();

        // Act
        List<Content> result =
            service.findContentsByModule(1L, 10L);

        // Assert
        assertEquals(expected, result);

        verify(moduleService).findModuleInCourse(1L, 10L);

        verify(contentRepository)
            .findByModule_IdOrderByPositionAsc(10L);
    }

    @Test
    void emptyModuleReturnsEmptyList() {

        // Arrange
        Module module = mock(Module.class);

        when(moduleService.findModuleInCourse(1L, 10L))
                .thenReturn(Optional.of(module));

        when(contentRepository
                .findByModule_IdOrderByPositionAsc(10L))
                .thenReturn(List.of());

        ContentService service = createService();

        // Act
        List<Content> result =
            service.findContentsByModule(1L, 10L);

        // Assert
        assertTrue(result.isEmpty());

        verify(moduleService).findModuleInCourse(1L, 10L);

        verify(contentRepository)
            .findByModule_IdOrderByPositionAsc(10L);
    }

    @Test
    void missingModulePreventsListingContents() {

        // Arrange
        when(moduleService.findModuleInCourse(1L, 999L))
                .thenReturn(Optional.empty());

        ContentService service = createService();

        // Act + Assert
        assertThrows(
            NoSuchElementException.class,
            () -> service.findContentsByModule(1L, 999L)
        );

        verify(moduleService).findModuleInCourse(1L, 999L);
        verifyNoInteractions(contentRepository);
    }

    @Test
    void moduleInDifferentCoursePreventsListingContents() {

        // Module 10 does not belong to Course 2.
        when(moduleService.findModuleInCourse(2L, 10L))
                .thenReturn(Optional.empty());

        ContentService service = createService();

        assertThrows(
            NoSuchElementException.class,
            () -> service.findContentsByModule(2L, 10L)
        );

        verify(moduleService).findModuleInCourse(2L, 10L);
        verifyNoInteractions(contentRepository);
    }


    @Test
    void findExistingStaticPageById() {

        Module module = mock(Module.class);

        StaticPage page = new StaticPage(
            module,
            "Introduction to Java",
            0,
            "Java basics"
        );

        when(contentRepository.findById(5L))
                .thenReturn(Optional.of(page));

        ContentService service = createService();

        StaticPage result = service.findStaticPageById(5L);

        assertSame(page, result);

        verify(contentRepository).findById(5L);
        verifyNoInteractions(moduleService);
    }

    @Test
    void findMissingStaticPageByIdThrowsException() {

        when(contentRepository.findById(999L))
                .thenReturn(Optional.empty());

        ContentService service = createService();

        assertThrows(
            NoSuchElementException.class,
            () -> service.findStaticPageById(999L)
        );

        verify(contentRepository).findById(999L);
    }

    @Test
    @Disabled("Not implemented yet")
    void contentMustBelongToExistingModule() {
        // Reject missing/unknown modules; accept content attached to an existing module.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void courseTeacherCanCreateAndEditSupportedContentTypes() {
        // Cover static pages (including multimedia references), quizzes, coding
        // exercises, and hand-in assignments. Reject students and other-course teachers.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void studentsOnlySeeContentWithinVisibilityWindow() {
        // Use a fixed/injected clock. Test before opening, during visibility, and
        // after closing, including exact boundaries according to the chosen policy.
        // Missing time bounds should not unnecessarily hide otherwise accessible content.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void quizAnswersAreGradedCorrectly() {
        // Test correct, incorrect, and incomplete answers for each supported question
        // type, with the chosen scoring policy and configured hints for wrong answers.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void quizAttemptLimitIsEnforcedPerStudentAndQuiz() {
        // Allow attempts up to the configured limit; reject the next attempt.
        // Another student's attempts or another quiz must not consume that limit.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void codingExerciseIsGradedUsingConfiguredTests() {
        // When implemented, verify grading outcomes with a mocked execution component.
        // Cover passing, failing, and execution-error/timeout results; hidden test
        // details must not leak to students. Keep actual runner tests separate.
        fail("Not implemented yet");
    }
}
