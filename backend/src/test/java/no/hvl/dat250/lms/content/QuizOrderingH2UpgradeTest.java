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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.junit.jupiter.api.Assertions.*;

/** All DDL is confined to a disposable file-backed H2, never the developer database. */
class QuizOrderingH2UpgradeTest {
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


    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void upgradePreservesLegacyHistoryAndOrderingSurvivesRestart(boolean varcharColumn) throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("ordering-upgrade");
        Long quizId;
        Long enrollmentId;
        Long oldAnswerId;
        try (var factory = sessionFactory(url, "create"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            User user = new User("student@example.com", "Test", "Student");
            session.persist(user);
            Course course = new Course("DAT250", "Ordering upgrade", Term.AUTUMN, 2026);
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
            sql.execute("DROP TABLE quiz_answer_ordered_items");
            sql.execute("DROP TABLE quiz_question_correct_order");
            sql.execute("DROP TABLE quiz_items");
            // Actual 3A databases may use Hibernate's ENUM or the manual script's VARCHAR.
            String oldType = varcharColumn ? "VARCHAR(32)" : "ENUM('FILL_IN_BLANK','MULTIPLE_CHOICE','TRUE_FALSE')";
            sql.execute("ALTER TABLE quiz_questions ALTER COLUMN question_type " + oldType + " NOT NULL");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-quiz-ordering.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-quiz-ordering.sql"));
        }
        Long orderingAnswerId;
        Long orderingQuestionId;
        List<UUID> expectedIds;
        List<UUID> submittedIds;
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            QuizAnswer old = session.find(QuizAnswer.class, oldAnswerId);
            assertEquals(QuestionType.MULTIPLE_CHOICE, old.getQuestion().getType());
            assertEquals(1, old.getSelectedOptions().size());
            assertTrue(old.getOrderedItems().isEmpty());
            assertTrue(old.isCorrect());
            Quiz quiz = session.find(Quiz.class, quizId);
            QuizQuestion ordering = new QuizQuestion(quiz, "Sequence", 1, "Review",
                QuestionType.ORDERING, List.of());
            session.persist(ordering);
            QuizItem first = new QuizItem(ordering, "First");
            QuizItem second = new QuizItem(ordering, "Second");
            QuizItem third = new QuizItem(ordering, "Third");
            session.persist(first);
            session.persist(second);
            session.persist(third);
            ordering.configureOrdering(List.of(first, second, third), List.of(second, third, first));
            List<QuizItem> submitted = new ArrayList<>(ordering.getCorrectOrder());
            Collections.reverse(submitted);
            QuizAttempt attempt = new QuizAttempt(quiz, session.find(Enrollment.class, enrollmentId), Instant.now(), 0, 1);
            session.persist(attempt);
            QuizAnswer answer = new QuizAnswer(attempt, ordering, Set.of(), false, null, submitted);
            session.persist(answer);
            tx.commit();
            orderingAnswerId = answer.getId();
            orderingQuestionId = ordering.getId();
            expectedIds = ordering.getCorrectOrder().stream().map(QuizItem::getId).toList();
            submittedIds = submitted.stream().map(QuizItem::getId).toList();
        }
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            QuizAnswer answer = session.find(QuizAnswer.class, orderingAnswerId);
            QuizQuestion question = session.find(QuizQuestion.class, orderingQuestionId);
            assertEquals(expectedIds, question.getCorrectOrder().stream().map(QuizItem::getId).toList());
            assertEquals(submittedIds, answer.getOrderedItems().stream().map(QuizItem::getId).toList());
            assertNotEquals(expectedIds, submittedIds);
            assertEquals(3, question.getItems().size());
            assertFalse(answer.isCorrect());
            assertEquals(0, answer.getAttempt().getCorrectCount());
            assertEquals(1, session.find(QuizAnswer.class, oldAnswerId).getSelectedOptions().size());
        }
    }
}
