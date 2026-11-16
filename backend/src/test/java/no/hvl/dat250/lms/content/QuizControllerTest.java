
package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.api.controllers.QuizController;
import no.hvl.dat250.lms.content.domain.ChoiceOption;
import no.hvl.dat250.lms.content.domain.Quiz;
import no.hvl.dat250.lms.content.domain.QuizQuestion;
import no.hvl.dat250.lms.content.domain.QuestionType;
import no.hvl.dat250.lms.content.service.QuizService;
import no.hvl.dat250.lms.content.service.QuizService.OptionDraft;
import no.hvl.dat250.lms.content.service.QuizService.QuestionWithOptions;
import no.hvl.dat250.lms.content.service.QuizService.QuizWithQuestions;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.module.domain.Module;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuizController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuizService quizService;

    private Quiz createTestQuiz(Long id) {
        Quiz quiz = new Quiz(
            mock(Module.class),
            "Java Basics Quiz",
            1,
            2
        );

        ReflectionTestUtils.setField(quiz, "id", id);
        return quiz;
    }

    // ------------------------------------
    // POST: CREATE QUIZ
    // ------------------------------------

    @Test
    void createQuizReturns201() throws Exception {

        Quiz quiz = createTestQuiz(7L);

        when(quizService.createQuiz(
            1L, 2L, "Java Basics Quiz", 1, 2
        )).thenReturn(quiz);

        mockMvc.perform(
            post("/api/courses/1/modules/2/contents/quizzes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Java Basics Quiz",
                      "position": 1,
                      "maxAttempts": 2
                    }
                    """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.courseId").value(1))
        .andExpect(jsonPath("$.moduleId").value(2))
        .andExpect(jsonPath("$.title").value("Java Basics Quiz"))
        .andExpect(jsonPath("$.position").value(1))
        .andExpect(jsonPath("$.maxAttempts").value(2));

        verify(quizService).createQuiz(
            1L, 2L, "Java Basics Quiz", 1, 2
        );
    }

    @Test
    void invalidQuizReturns400() throws Exception {

        mockMvc.perform(
            post("/api/courses/1/modules/2/contents/quizzes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "",
                      "position": -1,
                      "maxAttempts": 0
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(quizService);
    }

    @Test
    void wrongCourseModuleReturns404() throws Exception {

        when(quizService.createQuiz(
            2L, 10L, "Java Basics Quiz", 1, 2
        )).thenThrow(
            new NoSuchElementException("Module not found in course")
        );

        mockMvc.perform(
            post("/api/courses/2/modules/10/contents/quizzes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Java Basics Quiz",
                      "position": 1,
                      "maxAttempts": 2
                    }
                    """)
        )
        .andExpect(status().isNotFound());
    }

    // ------------------------------------
    // POST: ADD QUESTION
    // ------------------------------------

    @Test
    void addQuestionReturns201() throws Exception {

        Quiz quiz = createTestQuiz(7L);

        QuizQuestion question = new QuizQuestion(
            quiz,
            "Which are Java access modifiers?",
            0,
            "Think about visibility"
        );

        ReflectionTestUtils.setField(
            question, "id", 11L
        );

        List<OptionDraft> options = List.of(
            new OptionDraft("public", 0, true),
            new OptionDraft("private", 1, true),
            new OptionDraft("static", 2, false)
        );

        when(quizService.addQuestion(
            7L,
            "Which are Java access modifiers?",
            0,
            "Think about visibility",
            options, QuestionType.MULTIPLE_CHOICE, List.of()
        )).thenReturn(question);

        mockMvc.perform(
            post("/api/quizzes/7/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "questionText": "Which are Java access modifiers?",
                      "position": 0,
                      "hint": "Think about visibility",
                      "options": [
                        {
                          "optionText": "public",
                          "position": 0,
                          "correct": true
                        },
                        {
                          "optionText": "private",
                          "position": 1,
                          "correct": true
                        },
                        {
                          "optionText": "static",
                          "position": 2,
                          "correct": false
                        }
                      ]
                    }
                    """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(11))
        .andExpect(jsonPath("$.quizId").value(7))
        .andExpect(jsonPath("$.questionText")
            .value("Which are Java access modifiers?"))
        .andExpect(jsonPath("$.position").value(0))
        .andExpect(jsonPath("$.hint")
            .value("Think about visibility"))
        .andExpect(jsonPath("$.options").doesNotExist());

        verify(quizService).addQuestion(
            7L,
            "Which are Java access modifiers?",
            0,
            "Think about visibility",
            options, QuestionType.MULTIPLE_CHOICE, List.of()
        );
    }

    @Test
    void tooFewOptionsReturns400() throws Exception {

        mockMvc.perform(
            post("/api/quizzes/7/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "questionText": "Java question",
                      "position": 0,
                      "options": [
                        {
                          "optionText": "A",
                          "position": 0,
                          "correct": true
                        }
                      ]
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(quizService);
    }

    @Test
    void invalidNestedOptionReturns400() throws Exception {

        mockMvc.perform(
            post("/api/quizzes/7/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "questionText": "Java question",
                      "position": 0,
                      "options": [
                        {
                          "optionText": "A",
                          "position": 0,
                          "correct": true
                        },
                        {
                          "optionText": "",
                          "position": 1,
                          "correct": false
                        }
                      ]
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(quizService);
    }

    @Test
    void noCorrectOptionReturns400() throws Exception {

        List<OptionDraft> options = List.of(
            new OptionDraft("A", 0, false),
            new OptionDraft("B", 1, false)
        );

        when(quizService.addQuestion(
            7L, "Java question", 0, null, options, QuestionType.MULTIPLE_CHOICE, List.of()
        )).thenThrow(
            new IllegalArgumentException(
                "At least one option must be correct"
            )
        );

        mockMvc.perform(
            post("/api/quizzes/7/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "questionText": "Java question",
                      "position": 0,
                      "options": [
                        {
                          "optionText": "A",
                          "position": 0,
                          "correct": false
                        },
                        {
                          "optionText": "B",
                          "position": 1,
                          "correct": false
                        }
                      ]
                    }
                    """)
        )
        .andExpect(status().isBadRequest());

        verify(quizService).addQuestion(
            7L, "Java question", 0, null, options, QuestionType.MULTIPLE_CHOICE, List.of()
        );
    }

    @Test
    void addingQuestionToNonQuizReturns404() throws Exception {

        List<OptionDraft> options = List.of(
            new OptionDraft("A", 0, true),
            new OptionDraft("B", 1, false)
        );

        when(quizService.addQuestion(
            99L, "Java question", 0, null, options, QuestionType.MULTIPLE_CHOICE, List.of()
        )).thenThrow(
            new NoSuchElementException("Quiz not found: 99")
        );

        mockMvc.perform(
            post("/api/quizzes/99/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "questionText": "Java question",
                      "position": 0,
                      "options": [
                        {
                          "optionText": "A",
                          "position": 0,
                          "correct": true
                        },
                        {
                          "optionText": "B",
                          "position": 1,
                          "correct": false
                        }
                      ]
                    }
                    """)
        )
        .andExpect(status().isNotFound());
    }

    
    @Test
    void getQuizReturnsQuestionsWithoutCorrectAnswers()
            throws Exception {

        // Create Course -> Module -> Quiz
        Course course = new Course(
            "DAT250",
            "Advanced Software Technologies",
            Term.AUTUMN,
            2026
        );
        ReflectionTestUtils.setField(course, "id", 1L);

        Module module = new Module(
            course, "Java Refresh", 0
        );
        ReflectionTestUtils.setField(module, "id", 2L);

        Quiz quiz = new Quiz(
            module, "Java Basics Quiz", 1, 2
        );
        ReflectionTestUtils.setField(quiz, "id", 7L);

        QuizQuestion question = new QuizQuestion(
            quiz,
            "Which are Java access modifiers?",
            0,
            "Think about visibility"
        );
        ReflectionTestUtils.setField(question, "id", 11L);

        ChoiceOption correctOption = new ChoiceOption(
            question, "public", 0, true
        );
        ReflectionTestUtils.setField(correctOption, "id", 21L);

        ChoiceOption wrongOption = new ChoiceOption(
            question, "static", 1, false
        );
        ReflectionTestUtils.setField(wrongOption, "id", 22L);

        var details = new QuizWithQuestions(
            quiz,
            List.of(
                new QuestionWithOptions(
                    question,
                    List.of(correctOption, wrongOption)
                )
            )
        );

        when(quizService.findQuizWithQuestions(7L))
            .thenReturn(details);

        mockMvc.perform(get("/api/quizzes/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.courseId").value(1))
            .andExpect(jsonPath("$.moduleId").value(2))
            .andExpect(jsonPath("$.maxAttempts").value(2))
            .andExpect(jsonPath("$.questions[0].id").value(11))
            .andExpect(jsonPath("$.questions[0].questionText")
                .value("Which are Java access modifiers?"))
            .andExpect(jsonPath("$.questions[0].options[0].id")
                .value(21))
            .andExpect(jsonPath("$.questions[0].options[0].optionText")
                .value("public"))

            // Correct answers must never be exposed.
            .andExpect(jsonPath(
                "$.questions[0].options[0].correct"
            ).doesNotExist())
            .andExpect(jsonPath(
                "$.questions[0].options[1].correct"
            ).doesNotExist())

            // Hints are not published in this response yet.
            .andExpect(jsonPath(
                "$.questions[0].hint"
            ).doesNotExist());

        verify(quizService).findQuizWithQuestions(7L);
    }


    @Test
    void getMissingQuizReturns404() throws Exception {

        when(quizService.findQuizWithQuestions(999L))
            .thenThrow(
                new NoSuchElementException("Quiz not found: 999")
            );

        mockMvc.perform(get("/api/quizzes/999"))
            .andExpect(status().isNotFound());
    }

}
