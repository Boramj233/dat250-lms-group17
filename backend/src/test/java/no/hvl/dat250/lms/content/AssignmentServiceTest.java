
package no.hvl.dat250.lms.content;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.repository.ContentRepository;
import no.hvl.dat250.lms.content.service.AssignmentService;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.service.ModuleService;

import no.hvl.dat250.lms.enrollment.service.EnrollmentService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ModuleService moduleService;

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private AssignmentService assignmentService;

    @Test
    void createsAssignmentInExistingModule() {

        Module module = mock(Module.class);

        Instant deadline =
            Instant.parse("2026-11-20T23:00:00Z");

        when(moduleService.findModuleInCourse(1L, 10L))
            .thenReturn(Optional.of(module));

        when(contentRepository.save(any(Assignment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Assignment result = assignmentService.createAssignment(
            1L,
            10L,
            "JPA Assignment",
            2,
            "Implement JPA entities",
            deadline,
            false,
            5L
        );

        assertSame(module, result.getModule());
        assertEquals("JPA Assignment", result.getTitle());
        assertEquals(
            "Implement JPA entities",
            result.getInstructions()
        );
        assertEquals(2, result.getPosition());
        assertEquals(deadline, result.getDeadline());
        assertFalse(result.isAllowLateSubmission());

        verify(enrollmentService).requireActiveTeacher(5L, 1L);
        verify(contentRepository).save(result);
    }

    @Test
    void rejectsCreatingAssignmentInMissingModule() {

        when(moduleService.findModuleInCourse(1L, 99L))
            .thenReturn(Optional.empty());

        assertThrows(
            NoSuchElementException.class,
            () -> assignmentService.createAssignment(
                1L,
                99L,
                "JPA Assignment",
                2,
                "Implement JPA entities",
                null,
                false,
                5L
            )
        );

        verifyNoInteractions(contentRepository);
    }

    @Test
    void rejectsBlankTitleBeforeSaving() {

        assertThrows(
            IllegalArgumentException.class,
            () -> assignmentService.createAssignment(
                1L,
                10L,
                "   ",
                2,
                "Implement JPA entities",
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
    void readsExistingAssignment() {

        Assignment assignment = mock(Assignment.class);

        when(contentRepository.findById(42L))
            .thenReturn(Optional.of(assignment));

        Assignment result =
            assignmentService.findAssignment(42L);

        assertSame(assignment, result);
    }

    @Test
    void rejectsStaticPageIdAsAssignmentId() {

        StaticPage page = mock(StaticPage.class);

        when(contentRepository.findById(42L))
            .thenReturn(Optional.of(page));

        assertThrows(
            NoSuchElementException.class,
            () -> assignmentService.findAssignment(42L)
        );
    }
}
