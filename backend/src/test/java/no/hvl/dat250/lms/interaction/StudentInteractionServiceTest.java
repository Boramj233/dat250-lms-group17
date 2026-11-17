package no.hvl.dat250.lms.interaction;

import java.util.NoSuchElementException;
import java.util.Optional;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.interaction.domain.StudentInteraction;
import no.hvl.dat250.lms.interaction.repository.StudentInteractionRepository;
import no.hvl.dat250.lms.interaction.service.StudentInteractionService;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentInteractionServiceTest {

    private static final Long STUDENT_ID = 10L;
    private static final Long CONTENT_ID = 20L;

    @Mock
    private StudentInteractionRepository interactionRepository;

    @Mock
    private UserService userService;

    @Mock
    private ContentService contentService;

    @InjectMocks
    private StudentInteractionService interactionService;

    @Test
    void completeContentCreatesNewInteraction() {

        User student = mock(User.class);
        Content content = mock(Content.class);

        when(interactionRepository.findByStudent_UserIdAndContent_Id(
                STUDENT_ID, CONTENT_ID))
                .thenReturn(Optional.empty());

        when(userService.getUser(STUDENT_ID))
                .thenReturn(student);

        when(contentService.requireContent(CONTENT_ID))
                .thenReturn(content);

        when(interactionRepository.save(any(StudentInteraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentInteraction result =
                interactionService.completeContent(STUDENT_ID, CONTENT_ID);

        ArgumentCaptor<StudentInteraction> captor =
                ArgumentCaptor.forClass(StudentInteraction.class);

        verify(interactionRepository).save(captor.capture());

        StudentInteraction saved = captor.getValue();

        assertSame(student, saved.getStudent());
        assertSame(content, saved.getContent());
        assertNotNull(saved.getCompletedAt());
        assertTrue(saved.isCompleted());
        assertSame(saved, result);

        verify(userService).getUser(STUDENT_ID);
        verify(contentService).requireContent(CONTENT_ID);
    }

    @Test
    void completeContentUpdatesExistingInteraction() {

        User student = mock(User.class);
        Content content = mock(Content.class);

        StudentInteraction existing =
                new StudentInteraction(student, content);

        when(interactionRepository.findByStudent_UserIdAndContent_Id(
                STUDENT_ID, CONTENT_ID))
                .thenReturn(Optional.of(existing));

        when(interactionRepository.save(existing))
                .thenReturn(existing);

        StudentInteraction result =
                interactionService.completeContent(STUDENT_ID, CONTENT_ID);

        assertSame(existing, result);
        assertTrue(existing.isCompleted());
        assertNotNull(existing.getCompletedAt());

        verify(interactionRepository).save(existing);

        verifyNoInteractions(userService, contentService);
    }

    @Test
    void completeContentDoesNotChangeExistingCompletionTimestamp() {

        User student = mock(User.class);
        Content content = mock(Content.class);

        StudentInteraction existing =
                new StudentInteraction(student, content);

        existing.markCompleted();

        var originalTimestamp = existing.getCompletedAt();

        when(interactionRepository.findByStudent_UserIdAndContent_Id(
                STUDENT_ID, CONTENT_ID))
                .thenReturn(Optional.of(existing));

        when(interactionRepository.save(existing))
                .thenReturn(existing);

        StudentInteraction result =
                interactionService.completeContent(STUDENT_ID, CONTENT_ID);

        assertEquals(originalTimestamp, result.getCompletedAt());

        verify(interactionRepository).save(existing);
        verifyNoInteractions(userService, contentService);
    }

    @Test
    void completeContentThrowsWhenStudentNotFound() {

        when(interactionRepository.findByStudent_UserIdAndContent_Id(
                STUDENT_ID, CONTENT_ID))
                .thenReturn(Optional.empty());

        when(userService.getUser(STUDENT_ID))
                .thenThrow(new NoSuchElementException("Student not found"));

        assertThrows(
                NoSuchElementException.class,
                () -> interactionService.completeContent(
                        STUDENT_ID, CONTENT_ID)
        );

        verify(interactionRepository, never())
                .save(any(StudentInteraction.class));

        verifyNoInteractions(contentService);
    }

    @Test
    void completeContentThrowsWhenContentNotFound() {

        User student = mock(User.class);

        when(interactionRepository.findByStudent_UserIdAndContent_Id(
                STUDENT_ID, CONTENT_ID))
                .thenReturn(Optional.empty());

        when(userService.getUser(STUDENT_ID))
                .thenReturn(student);

        when(contentService.requireContent(CONTENT_ID))
                .thenThrow(new NoSuchElementException("Content not found"));

        assertThrows(
                NoSuchElementException.class,
                () -> interactionService.completeContent(
                        STUDENT_ID, CONTENT_ID)
        );

        verify(interactionRepository, never())
                .save(any(StudentInteraction.class));
    }
}