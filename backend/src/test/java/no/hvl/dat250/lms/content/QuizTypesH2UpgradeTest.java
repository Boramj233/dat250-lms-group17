package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.Set;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import jakarta.persistence.Entity;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.course.domain.*;
import no.hvl.dat250.lms.enrollment.domain.*;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.user.domain.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.junit.jupiter.api.Assertions.*;

/** All DDL is confined to a disposable file-backed H2, never the developer database. */
class QuizTypesH2UpgradeTest {
    @TempDir Path directory;
    private SessionFactory sessionFactory(String url, String schemaAction) throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                // Include abstract Content, which owns the JPA inheritance hierarchy.
                return definition.getMetadata().isIndependent();
            }
        };
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        Configuration configuration = new Configuration();
        for (var entity : scanner.findCandidateComponents("no.hvl.dat250.lms")) {
            configuration.addAnnotatedClass(Class.forName(entity.getBeanClassName()));
        }
        configuration.setProperty("hibernate.connection.url", url);
        configuration.setProperty("hibernate.connection.username", "sa");
        configuration.setProperty("hibernate.connection.password", "");
        configuration.setProperty("hibernate.hbm2ddl.auto", schemaAction);
        configuration.setProperty("hibernate.hbm2ddl.halt_on_error", "true");
        configuration.setProperty("hibernate.show_sql", "false");
        return configuration.buildSessionFactory();
    }


    @Test
    void manualUpgradePreservesChoiceHistoryAndNewTextAnswersSurviveRestart() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("quiz-types-upgrade");
        Long quizId;
        Long enrollmentId;
        Long oldAnswerId;
        try (var factory = sessionFactory(url, "create"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            User user = new User("student@example.com", "Test", "Student");
            session.persist(user);
            Course course = new Course("DAT250", "Quiz upgrade", Term.AUTUMN, 2026);
            session.persist(course);
            Enrollment enrollment = new Enrollment(user, course, CourseRole.STUDENT);
            session.persist(enrollment);
            Module module = new Module(course, "Week", 0);
            session.persist(module);
            Quiz quiz = new Quiz(module, "Quiz", 0, 3);
            session.persist(quiz);
            QuizQuestion question = new QuizQuestion(quiz, "Choose", 0, "Hint");
            session.persist(question);
            ChoiceOption option = new ChoiceOption(question, "Correct", 0, true);
            session.persist(option);
            session.persist(new ChoiceOption(question, "Incorrect", 1, false));
            QuizAttempt attempt = new QuizAttempt(quiz, enrollment, Instant.now(), 1, 1);
            session.persist(attempt);
            QuizAnswer answer = new QuizAnswer(attempt, question, Set.of(option), true);
            session.persist(answer);
            tx.commit();
            quizId = quiz.getId();
            enrollmentId = enrollment.getId();
            oldAnswerId = answer.getId();
        }
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.execute("DROP TABLE quiz_accepted_answers");
            sql.execute("ALTER TABLE quiz_questions DROP COLUMN question_type");
            sql.execute("ALTER TABLE quiz_answers DROP COLUMN text_answer");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-quiz-question-types.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-quiz-question-types.sql"));
        }
        Long textAnswerId;
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            QuizAnswer old = session.find(QuizAnswer.class, oldAnswerId);
            assertEquals(QuestionType.MULTIPLE_CHOICE, old.getQuestion().getType());
            assertNull(old.getTextAnswer());
            assertTrue(old.isCorrect());
            assertEquals(1, old.getSelectedOptions().size());
            assertEquals(1, old.getAttempt().getCorrectCount());
            Quiz quiz = session.find(Quiz.class, quizId);
            QuizQuestion fill = new QuizQuestion(quiz, "Language?", 1, "JVM language",
                QuestionType.FILL_IN_BLANK, List.of("Java", "Java language"));
            session.persist(fill);
            QuizAttempt attempt = new QuizAttempt(quiz, session.find(Enrollment.class, enrollmentId), Instant.now(), 1, 1);
            session.persist(attempt);
            QuizAnswer answer = new QuizAnswer(attempt, fill, Set.of(), true, "  JAVA  ");
            session.persist(answer);
            tx.commit();
            textAnswerId = answer.getId();
        }
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            QuizAnswer answer = session.find(QuizAnswer.class, textAnswerId);
            assertEquals("  JAVA  ", answer.getTextAnswer());
            assertEquals(QuestionType.FILL_IN_BLANK, answer.getQuestion().getType());
            assertEquals(Set.of("Java", "Java language"), answer.getQuestion().getAcceptedAnswers());
            assertTrue(answer.getSelectedOptions().isEmpty());
            assertEquals(1, session.find(QuizAnswer.class, oldAnswerId).getSelectedOptions().size());
        }
    }
}
