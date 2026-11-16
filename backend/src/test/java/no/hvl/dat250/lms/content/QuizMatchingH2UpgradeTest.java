package no.hvl.dat250.lms.content;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
class QuizMatchingH2UpgradeTest {
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
    void upgradePreservesOrderingAndMatchingPairsSurviveRestart(boolean varcharColumn) throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("matching-upgrade");
        Long quizId;
        Long enrollmentId;
        Long oldAnswerId;
        List<UUID> oldOrder;
        try (var factory = sessionFactory(url, "create"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            User user = new User("student@example.com", "Test", "Student");
            session.persist(user);
            Course course = new Course("DAT250", "Matching upgrade", Term.AUTUMN, 2026);
            session.persist(course);
            Enrollment enrollment = new Enrollment(user, course, CourseRole.STUDENT);
            session.persist(enrollment);
            Module module = new Module(course, "Week", 0);
            session.persist(module);
            Quiz quiz = new Quiz(module, "Quiz", 0, 3);
            session.persist(quiz);
            QuizQuestion ordering = new QuizQuestion(quiz, "Order", 0, null, QuestionType.ORDERING, List.of());
            session.persist(ordering);
            QuizItem first = new QuizItem(ordering, "First");
            QuizItem second = new QuizItem(ordering, "Second");
            session.persist(first);
            session.persist(second);
            ordering.configureOrdering(List.of(first, second), List.of(second, first));
            QuizAttempt attempt = new QuizAttempt(quiz, enrollment, Instant.now(), 1, 1);
            session.persist(attempt);
            QuizAnswer answer = new QuizAnswer(attempt, ordering, Set.of(), true, null, List.of(second, first));
            session.persist(answer);
            tx.commit();
            quizId = quiz.getId();
            enrollmentId = enrollment.getId();
            oldAnswerId = answer.getId();
            oldOrder = answer.getOrderedItems().stream().map(QuizItem::getId).toList();
        }
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.execute("DROP TABLE quiz_answer_matches");
            sql.execute("DROP TABLE quiz_question_correct_matches");
            sql.execute("ALTER TABLE quiz_items DROP COLUMN item_kind");
            String oldType = varcharColumn ? "VARCHAR(32)"
                : "ENUM('FILL_IN_BLANK','MULTIPLE_CHOICE','TRUE_FALSE','ORDERING')";
            sql.execute("ALTER TABLE quiz_questions ALTER COLUMN question_type " + oldType + " NOT NULL");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-quiz-matching.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-quiz-matching.sql"));
        }
        Long matchAnswerId;
        Long matchQuestionId;
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            QuizAnswer old = session.find(QuizAnswer.class, oldAnswerId);
            assertEquals(oldOrder, old.getOrderedItems().stream().map(QuizItem::getId).toList());
            assertTrue(old.getOrderedItems().stream().allMatch(item -> item.getKind() == QuizItemKind.ORDERING));
            assertTrue(old.getMatches().isEmpty());
            QuizQuestion question = new QuizQuestion(session.find(Quiz.class, quizId), "Match", 1, "Hint",
                QuestionType.MATCHING, List.of());
            session.persist(question);
            QuizItem leftA = new QuizItem(question, "Java", QuizItemKind.MATCHING_LEFT);
            QuizItem leftB = new QuizItem(question, "React", QuizItemKind.MATCHING_LEFT);
            QuizItem rightA = new QuizItem(question, "Language", QuizItemKind.MATCHING_RIGHT);
            QuizItem rightB = new QuizItem(question, "UI library", QuizItemKind.MATCHING_RIGHT);
            for (QuizItem item : List.of(leftA, leftB, rightA, rightB)) {
                session.persist(item);
            }
            question.configureMatching(List.of(leftA, leftB, rightA, rightB),
                List.of(new QuizMatch(leftA, rightA), new QuizMatch(leftB, rightB)));
            QuizAttempt attempt = new QuizAttempt(session.find(Quiz.class, quizId),
                session.find(Enrollment.class, enrollmentId), Instant.now(), 0, 1);
            session.persist(attempt);
            QuizAnswer answer = new QuizAnswer(attempt, question, Set.of(), false, null, List.of(),
                List.of(new QuizMatch(leftB, rightA), new QuizMatch(leftA, rightB)));
            session.persist(answer);
            tx.commit();
            matchAnswerId = answer.getId();
            matchQuestionId = question.getId();
        }
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            QuizQuestion question = session.find(QuizQuestion.class, matchQuestionId);
            QuizAnswer answer = session.find(QuizAnswer.class, matchAnswerId);
            var expected = question.getCorrectMatches().stream().collect(Collectors.toMap(
                pair -> pair.getLeftItem().getText(), pair -> pair.getRightItem().getText()));
            var actual = answer.getMatches().stream().collect(Collectors.toMap(
                pair -> pair.getLeftItem().getText(), pair -> pair.getRightItem().getText()));
            assertEquals(Map.of("Java", "Language", "React", "UI library"), expected);
            assertEquals(Map.of("Java", "UI library", "React", "Language"), actual);
            assertFalse(answer.isCorrect());
            assertEquals(oldOrder, session.find(QuizAnswer.class, oldAnswerId).getOrderedItems().stream().map(QuizItem::getId).toList());
        }
    }
}
