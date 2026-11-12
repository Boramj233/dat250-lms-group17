package no.hvl.dat250.lms.content;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;
import no.hvl.dat250.lms.content.domain.ChoiceOption;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.Quiz;
import no.hvl.dat250.lms.content.domain.QuizQuestion;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.repository.ChoiceOptionRepository;
import no.hvl.dat250.lms.content.repository.ContentRepository;
import no.hvl.dat250.lms.content.repository.QuizQuestionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DataJpaTest
class ContentRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private QuizQuestionRepository quizQuestionRepository;

    @Autowired
    private ChoiceOptionRepository choiceOptionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void staticPageCanBeLoadedThroughContentRepository() {

        // Arrange: create Course and Module
        Course course = courseRepository.saveAndFlush(
            new Course(
                "DAT250",
                "Advanced Software Technologies",
                Term.AUTUMN,
                2026
            )
        );

        Module module = moduleRepository.saveAndFlush(
            new Module(course, "Java Refresh", 0)
        );

        // Create a concrete subtype of Content
        StaticPage page = new StaticPage(
            module,
            "Java Introduction",
            0,
            "Welcome to Java programming!"
        );

        // Act: save through the parent repository
        Content saved = contentRepository.saveAndFlush(page);
        Long contentId = saved.getId();

        // Force Hibernate to read it again
        entityManager.clear();

        Content found = contentRepository.findById(contentId)
                .orElseThrow();

        // Assert: Hibernate restored the correct subtype
        StaticPage foundPage =
                assertInstanceOf(StaticPage.class, found);

        assertEquals("Java Introduction", foundPage.getTitle());
        assertEquals(0, foundPage.getPosition());
        assertEquals(
            "Welcome to Java programming!",
            foundPage.getBody()
        );
        assertEquals(module.getId(), foundPage.getModule().getId());
    }


    @Test
    void contentsAreFilteredByModuleAndOrderedByPosition() {

        // Arrange
        Course course = courseRepository.saveAndFlush(
            new Course(
                "DAT250",
                "Advanced Software Technologies",
                Term.AUTUMN,
                2026
            )
        );

        Module module1 = moduleRepository.saveAndFlush(
            new Module(course, "Java Refresh", 0)
        );

        Module module2 = moduleRepository.saveAndFlush(
            new Module(course, "Spring Boot", 1)
        );

        contentRepository.saveAll(List.of(
            new StaticPage(module1, "Advanced Java", 2, "Advanced"),
            new StaticPage(module2, "Spring Intro", 0, "Spring"),
            new StaticPage(module1, "Introduction", 0, "Basics"),
            new StaticPage(module1, "Java OOP", 1, "Classes")
        ));

        contentRepository.flush();
        entityManager.clear();

        // Act
        List<Content> contents =
            contentRepository.findByModule_IdOrderByPositionAsc(
                module1.getId()
            );

        // Assert
        assertEquals(
            List.of("Introduction", "Java OOP", "Advanced Java"),
            contents.stream()
                    .map(Content::getTitle)
                    .toList()
        );

        assertEquals(3, contents.size());
    }


    
    @Test
    void quizAndStaticPageCanBeStoredInSameContentRepository() {

        // Arrange: create a Course and a Module
        Course course = courseRepository.saveAndFlush(
            new Course(
                "DAT250",
                "Advanced Software Technologies",
                Term.AUTUMN,
                2026
            )
        );

        Module module = moduleRepository.saveAndFlush(
            new Module(course, "Java Refresh", 0)
        );

        // Create two different Content subtypes
        StaticPage page = new StaticPage(
            module,
            "Introduction to Java",
            0,
            "Welcome to Java!"
        );

        Quiz quiz = new Quiz(
            module,
            "Java Basics Quiz",
            1,
            2  // maxAttempts
        );

        contentRepository.saveAndFlush(page);
        contentRepository.saveAndFlush(quiz);

        Long quizId = quiz.getId();
        Long moduleId = module.getId();

        // Clear persistence context to force a database read
        entityManager.clear();

        // Act: retrieve the Quiz through ContentRepository
        Content found = contentRepository.findById(quizId)
                .orElseThrow();

        // Assert: JPA restores the correct subclass
        Quiz savedQuiz = assertInstanceOf(Quiz.class, found);

        assertEquals("Java Basics Quiz", savedQuiz.getTitle());
        assertEquals(1, savedQuiz.getPosition());
        assertEquals(2, savedQuiz.getMaxAttempts());

        // Both subtypes should appear in the Module content list
        List<Content> contents = contentRepository
                .findByModule_IdOrderByPositionAsc(moduleId);

        assertEquals(2, contents.size());

        assertInstanceOf(StaticPage.class, contents.get(0));
        assertInstanceOf(Quiz.class, contents.get(1));

        assertEquals("Introduction to Java",
                contents.get(0).getTitle());

        assertEquals("Java Basics Quiz",
                contents.get(1).getTitle());
    }

    
    @Test
    void quizQuestionsAndOptionsAreStoredAndOrdered() {

        // 1. Create Course -> Module -> Quiz
        Course course = courseRepository.saveAndFlush(
            new Course(
                "DAT250",
                "Advanced Software Technologies",
                Term.AUTUMN,
                2026
            )
        );

        Module module = moduleRepository.saveAndFlush(
            new Module(course, "Java Refresh", 0)
        );

        Quiz quiz = new Quiz(
            module,
            "Java Basics Quiz",
            0,
            2
        );

        contentRepository.saveAndFlush(quiz);

        // 2. Create two questions in reverse position order
        QuizQuestion questionA = quizQuestionRepository.saveAndFlush(
            new QuizQuestion(
                quiz,
                "Which keyword is used for inheritance?",
                1,
                "Think about class inheritance."
            )
        );

        QuizQuestion questionB = quizQuestionRepository.saveAndFlush(
            new QuizQuestion(
                quiz,
                "Which are Java access modifiers?",
                0,
                null
            )
        );

        // 3. Save options in reverse position order
        choiceOptionRepository.saveAndFlush(
            new ChoiceOption(questionB, "private", 1, true)
        );

        choiceOptionRepository.saveAndFlush(
            new ChoiceOption(questionB, "static", 2, false)
        );

        choiceOptionRepository.saveAndFlush(
            new ChoiceOption(questionB, "public", 0, true)
        );

        // Option belonging to a different question
        choiceOptionRepository.saveAndFlush(
            new ChoiceOption(questionA, "extends", 0, true)
        );

        choiceOptionRepository.saveAndFlush(
            new ChoiceOption(questionA, "import", 1, false)
        );

        Long quizId = quiz.getId();
        Long questionBId = questionB.getId();

        // 4. Clear JPA first-level cache
        entityManager.clear();

        // 5. Query questions
        List<QuizQuestion> questions =
            quizQuestionRepository
                .findByQuiz_IdOrderByPositionAsc(quizId);

        assertEquals(2, questions.size());

        assertEquals(
            "Which are Java access modifiers?",
            questions.get(0).getQuestionText()
        );

        assertEquals(0, questions.get(0).getPosition());
        assertEquals(1, questions.get(1).getPosition());

        // 6. Query options for questionB only
        List<ChoiceOption> options =
            choiceOptionRepository
                .findByQuestion_IdOrderByPositionAsc(questionBId);

        assertEquals(3, options.size());

        assertEquals("public", options.get(0).getOptionText());
        assertEquals("private", options.get(1).getOptionText());
        assertEquals("static", options.get(2).getOptionText());

        assertTrue(options.get(0).isCorrect());
        assertTrue(options.get(1).isCorrect());
        assertFalse(options.get(2).isCorrect());

        // 7. Verify the foreign-key relationship
        assertEquals(
            questionBId,
            options.get(0).getQuestion().getId()
        );
    }

}
