
package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.*;
import no.hvl.dat250.lms.content.service.QuizService;
import no.hvl.dat250.lms.content.service.QuizService.OptionDraft;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.service.ModuleService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private QuizQuestionRepository questionRepository;

    @Mock
    private ChoiceOptionRepository optionRepository;

    @Mock
    private ModuleService moduleService;

    @Mock
    private QuizAttemptRepository attemptRepository;

    @Mock
    private QuizItemRepository itemRepository;

    @InjectMocks
    private QuizService quizService;

    @Test
    void createQuizInExistingModule() {

        Module module = mock(Module.class);

        when(moduleService.findModuleInCourse(1L, 2L))
                .thenReturn(Optional.of(module));

        when(contentRepository.save(any(Quiz.class)))
                .thenAnswer(invocation ->
                    invocation.getArgument(0));

        Quiz result = quizService.createQuiz(
            1L, 2L, "Java Quiz", 1, 2
        );

        assertEquals("Java Quiz", result.getTitle());
        assertEquals(1, result.getPosition());
        assertEquals(2, result.getMaxAttempts());
        assertSame(module, result.getModule());

        verify(contentRepository).save(result);
    }

    @Test
    void wrongCourseModulePreventsQuizCreation() {

        when(moduleService.findModuleInCourse(2L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
            NoSuchElementException.class,
            () -> quizService.createQuiz(
                2L, 10L, "Java Quiz", 1, 2
            )
        );

        verifyNoInteractions(
            contentRepository,
            questionRepository,
            optionRepository
        );
    }

    @Test
    void addQuestionStoresAllOptions() {

        Quiz quiz = new Quiz(
            mock(Module.class), "Java Quiz", 0, 2
        );

        when(contentRepository.findById(7L))
                .thenReturn(Optional.of(quiz));

        when(questionRepository.save(any(QuizQuestion.class)))
                .thenAnswer(invocation ->
                    invocation.getArgument(0));

        List<OptionDraft> options = List.of(
            new OptionDraft("public", 0, true),
            new OptionDraft("private", 1, true),
            new OptionDraft("static", 2, false)
        );

        QuizQuestion result = quizService.addQuestion(
            7L,
            "Which are Java access modifiers?",
            0,
            "Think about visibility",
            options
        );

        assertSame(quiz, result.getQuiz());
        assertEquals(0, result.getPosition());

        ArgumentCaptor<ChoiceOption> captor =
                ArgumentCaptor.forClass(ChoiceOption.class);

        verify(optionRepository, times(3))
                .save(captor.capture());

        List<ChoiceOption> saved = captor.getAllValues();

        assertEquals("public", saved.get(0).getOptionText());
        assertEquals("private", saved.get(1).getOptionText());
        assertEquals("static", saved.get(2).getOptionText());

        assertTrue(saved.get(0).isCorrect());
        assertTrue(saved.get(1).isCorrect());
        assertFalse(saved.get(2).isCorrect());

        for (ChoiceOption option : saved) {
            assertSame(result, option.getQuestion());
        }
    }

    @Test
    void questionWithoutCorrectOptionIsRejected() {

        List<OptionDraft> options = List.of(
            new OptionDraft("public", 0, false),
            new OptionDraft("private", 1, false)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> quizService.addQuestion(
                7L,
                "Which are access modifiers?",
                0,
                null,
                options
            )
        );

        verifyNoInteractions(
            contentRepository,
            questionRepository,
            optionRepository
        );
    }

    @Test
    void cannotAddQuestionToStaticPage() {

        StaticPage page = new StaticPage(
            mock(Module.class),
            "Introduction",
            0,
            "Page body"
        );

        when(contentRepository.findById(7L))
                .thenReturn(Optional.of(page));

        assertThrows(
            NoSuchElementException.class,
            () -> quizService.addQuestion(
                7L,
                "Some question",
                0,
                null,
                List.of(
                    new OptionDraft("A", 0, true),
                    new OptionDraft("B", 1, false)
                )
            )
        );

        verifyNoInteractions(
            questionRepository,
            optionRepository
        );
    }


    
    @Test
    void findQuizWithQuestionsReturnsOrderedData() {

        Quiz quiz = new Quiz(
            mock(Module.class),
            "Java Basics Quiz",
            1,
            2
        );

        QuizQuestion question = new QuizQuestion(
            quiz,
            "Which are Java access modifiers?",
            0,
            "Think about visibility"
        );

        ReflectionTestUtils.setField(question, "id", 11L);

        ChoiceOption optionA = new ChoiceOption(
            question, "public", 0, true
        );

        ChoiceOption optionB = new ChoiceOption(
            question, "static", 1, false
        );

        when(contentRepository.findById(7L))
            .thenReturn(Optional.of(quiz));

        when(questionRepository.findByQuiz_IdOrderByPositionAsc(7L))
            .thenReturn(List.of(question));

        when(optionRepository.findByQuestion_IdOrderByPositionAsc(11L))
            .thenReturn(List.of(optionA, optionB));

        var result = quizService.findQuizWithQuestions(7L);

        assertSame(quiz, result.quiz());
        assertEquals(1, result.questions().size());

        var questionResult = result.questions().get(0);

        assertSame(question, questionResult.question());
        assertEquals(2, questionResult.options().size());

        assertSame(optionA, questionResult.options().get(0));
        assertSame(optionB, questionResult.options().get(1));

        verify(contentRepository).findById(7L);
        verify(questionRepository)
            .findByQuiz_IdOrderByPositionAsc(7L);
        verify(optionRepository)
            .findByQuestion_IdOrderByPositionAsc(11L);
    }

}
