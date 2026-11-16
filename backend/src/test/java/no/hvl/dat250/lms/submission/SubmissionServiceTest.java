
package no.hvl.dat250.lms.submission;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.module.domain.Module;

import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.service.StudentEnrollmentLookupService;

import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.repository.SubmissionRepository;
import no.hvl.dat250.lms.submission.service.SubmissionService;

import org.springframework.security.access.AccessDeniedException;
import no.hvl.dat250.lms.submission.service.SubmissionDeadlinePassedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    ContentService contentService;

    @Mock
    StudentEnrollmentLookupService enrollmentLookupService;

    @Mock
    SubmissionRepository submissionRepository;

    private SubmissionService service;

    private static final Instant NOW =
        Instant.parse("2026-11-11T10:00:00Z");

    @BeforeEach
    void setUp() {

        Clock fixedClock = Clock.fixed(
            NOW,
            ZoneOffset.UTC
        );

        service = new SubmissionService(
            contentService,
            enrollmentLookupService,
            submissionRepository,
            fixedClock
        );
    }

    private void stubContentInCourse(Content content) {

        Module module = mock(Module.class);
        Course course = mock(Course.class);

        when(contentService.requireContent(30L))
            .thenReturn(content);

        when(content.getModule()).thenReturn(module);
        when(module.getCourse()).thenReturn(course);
        when(course.getId()).thenReturn(10L);
    }

    private Assignment prepareAssignment() {

        Assignment assignment = mock(Assignment.class);

        stubContentInCourse(assignment);

        return assignment;
    }

    private Enrollment prepareEnrollment() {

        Enrollment enrollment = mock(Enrollment.class);

        when(enrollmentLookupService
                .requireActiveStudentEnrollment(5L, 10L))
            .thenReturn(enrollment);

        return enrollment;
    }

    private void mockSaving() {

        when(submissionRepository.save(any(Submission.class)))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );
    }

    @Test
    void studentCanSubmitBeforeDeadline() {

        Assignment assignment = prepareAssignment();

        when(assignment.getDeadline()).thenReturn(
            Instant.parse("2026-11-12T10:00:00Z")
        );

        Enrollment enrollment = prepareEnrollment();
        mockSaving();

        Submission result = service.submitIndividual(
            30L,
            5L,
            SubmissionType.TEXT,
            "My assignment answer"
        );

        assertSame(assignment, result.getContent());
        assertSame(enrollment, result.getSubmittedBy());

        assertEquals(
            SubmissionType.TEXT,
            result.getType()
        );

        assertEquals(
            "My assignment answer",
            result.getPayload()
        );

        assertEquals(NOW, result.getSubmittedAt());
        assertFalse(result.isLate());

        verify(enrollmentLookupService)
            .requireActiveStudentEnrollment(5L, 10L);

        verify(submissionRepository).save(result);
    }

    @Test
    void lateSubmissionIsAllowedWhenConfigured() {

        Assignment assignment = prepareAssignment();

        when(assignment.getDeadline()).thenReturn(
            Instant.parse("2026-11-10T10:00:00Z")
        );

        when(assignment.isAllowLateSubmission())
            .thenReturn(true);

        prepareEnrollment();
        mockSaving();

        Submission result = service.submitIndividual(
            30L,
            5L,
            SubmissionType.TEXT,
            "Late report"
        );

        assertTrue(result.isLate());

        assertEquals(
            NOW,
            result.getSubmittedAt()
        );

        verify(submissionRepository).save(result);
    }

    @Test
    void lateSubmissionIsRejectedWhenNotAllowed() {

        Assignment assignment = prepareAssignment();

        when(assignment.getDeadline()).thenReturn(
            Instant.parse("2026-11-10T10:00:00Z")
        );

        // Default mock boolean is false.
        prepareEnrollment();

        assertThrows(
            SubmissionDeadlinePassedException.class,
            () -> service.submitIndividual(
                30L,
                5L,
                SubmissionType.TEXT,
                "Too late"
            )
        );

        verifyNoInteractions(submissionRepository);
    }

    @Test
    void inactiveOrUnauthorizedStudentIsRejected() {

        prepareAssignment();

        when(enrollmentLookupService
                .requireActiveStudentEnrollment(5L, 10L))
            .thenThrow(new IllegalStateException(
                "Not an active student"
            ));

        assertThrows(
            AccessDeniedException.class,
            () -> service.submitIndividual(
                30L,
                5L,
                SubmissionType.TEXT,
                "Unauthorized submission"
            )
        );

        verifyNoInteractions(submissionRepository);
    }

    @Test
    void codingExerciseAcceptsGitUrlSubmission() {

        CodingExercise exercise =
            mock(CodingExercise.class);

        stubContentInCourse(exercise);

        prepareEnrollment();
        mockSaving();

        Submission result = service.submitIndividual(
            30L,
            5L,
            SubmissionType.GIT_URL,
            "https://gitlab.com/example/my-solution"
        );

        assertSame(exercise, result.getContent());

        assertEquals(
            SubmissionType.GIT_URL,
            result.getType()
        );

        assertFalse(result.isLate());

        verify(submissionRepository).save(result);
    }

    @Test
    void staticPageDoesNotAcceptSubmissions() {

        StaticPage page = mock(StaticPage.class);

        when(contentService.requireContent(30L))
            .thenReturn(page);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.submitIndividual(
                30L,
                5L,
                SubmissionType.TEXT,
                "Some text"
            )
        );

        verifyNoInteractions(
            enrollmentLookupService,
            submissionRepository
        );
    }

    @Test
    void missingContentIsRejected() {

        when(contentService.requireContent(30L))
            .thenThrow(new NoSuchElementException(
                "Content not found"
            ));

        assertThrows(
            NoSuchElementException.class,
            () -> service.submitIndividual(
                30L,
                5L,
                SubmissionType.TEXT,
                "Some answer"
            )
        );

        verifyNoInteractions(
            enrollmentLookupService,
            submissionRepository
        );
    }

    @Test
    void invalidIdsAreRejectedBeforeLookup() {

        assertThrows(
            IllegalArgumentException.class,
            () -> service.submitIndividual(
                -1L,
                5L,
                SubmissionType.TEXT,
                "Some answer"
            )
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.submitIndividual(
                30L,
                null,
                SubmissionType.TEXT,
                "Some answer"
            )
        );

        verifyNoInteractions(
            contentService,
            enrollmentLookupService,
            submissionRepository
        );
    }
    @Test
    void missingEnrollmentIsForbiddenWithoutSaving() {
        prepareAssignment();
        when(enrollmentLookupService.requireActiveStudentEnrollment(5L, 10L))
            .thenThrow(new NoSuchElementException("Enrollment not found"));
        assertThrows(AccessDeniedException.class,
            () -> service.submitIndividual(30L, 5L, SubmissionType.TEXT, "Answer"));
        verifyNoInteractions(submissionRepository);
    }

    @Test
    void unrelatedStateFailureIsNotConvertedToAccessDenied() {
        prepareAssignment();
        prepareEnrollment();
        IllegalStateException failure = new IllegalStateException("Persistence failure");
        when(submissionRepository.save(any(Submission.class))).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> service.submitIndividual(30L, 5L, SubmissionType.TEXT, "Answer")));
    }

    @Test
    void fileSubmittedExactlyAtDeadlineIsOnTimeAndUsesServerClock() {
        Assignment assignment = prepareAssignment();
        when(assignment.getDeadline()).thenReturn(NOW);
        Enrollment enrollment = prepareEnrollment();
        mockSaving();
        var metadata = new no.hvl.dat250.lms.submission.domain.SubmissionFileMetadata(
                "lms-submissions", "submissions/unique", "report.pdf", "application/pdf", 123, "a".repeat(64));

        Submission result = service.saveFileSubmission(30L, 5L, metadata);

        assertSame(enrollment, result.getSubmittedBy());
        assertEquals(NOW, result.getSubmittedAt());
        assertFalse(result.isLate());
        assertNull(result.getPayload());
        assertSame(metadata, result.getFile());
    }

}
