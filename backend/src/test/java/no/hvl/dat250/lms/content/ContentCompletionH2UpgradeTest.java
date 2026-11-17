package no.hvl.dat250.lms.content;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import jakarta.persistence.Entity;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.course.domain.*;
import no.hvl.dat250.lms.enrollment.domain.*;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.submission.domain.*;
import no.hvl.dat250.lms.grade.domain.Grade;
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
class ContentCompletionH2UpgradeTest {
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
    void manualUpgradePreservesSubmissionAndGradeSurvivesRestart() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("grade-upgrade");
        Long submissionId;
        Long teacherId;
        try (var factory = sessionFactory(url, "create"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            User student = new User("student@example.com", "Test", "Student");
            User teacher = new User("teacher@example.com", "Test", "Teacher");
            session.persist(student);
            session.persist(teacher);
            Course course = new Course("DAT250", "Upgrade", Term.AUTUMN, 2026);
            session.persist(course);
            Enrollment owner = new Enrollment(student, course, CourseRole.STUDENT);
            Enrollment grader = new Enrollment(teacher, course, CourseRole.TEACHER);
            session.persist(owner);
            session.persist(grader);
            Module module = new Module(course, "Week", 0);
            session.persist(module);
            Assignment assignment = new Assignment(module, "Report", 0, "Write", null, false);
            session.persist(assignment);
            Submission submission = Submission.file(assignment, owner,
                new SubmissionFileMetadata("private", "submissions/test", "report.pdf", "application/pdf", 123, "a".repeat(64)),
                Instant.parse("2026-01-01T12:00:00Z"), false);
            session.persist(submission);
            tx.commit();
            submissionId = submission.getId();
            teacherId = grader.getId();
        }
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.execute("ALTER TABLE grades DROP COLUMN passed");
            sql.execute("ALTER TABLE contents DROP COLUMN required_for_module_completion");
            sql.execute("ALTER TABLE quizzes DROP COLUMN passing_percentage");
            sql.execute("DROP TABLE static_page_completions");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-content-completion.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-content-completion.sql"));
        }
        Long gradeId;
        Instant releasedAt = Instant.parse("2026-11-16T10:00:00Z");
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            Submission submission = session.find(Submission.class, submissionId);
            assertEquals("submissions/test", submission.getFile().getObjectKey());
            Grade grade = new Grade(submission, new BigDecimal("100.00"), "Preserved feedback", session.find(Enrollment.class, teacherId), releasedAt);
            grade.decidePassed(true);
            grade.release(session.find(Enrollment.class, teacherId), releasedAt);
            session.persist(grade);
            tx.commit();
            gradeId = grade.getId();
        }
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            Grade grade = session.find(Grade.class, gradeId);
            assertEquals(new BigDecimal("100.00"), grade.getScore());
            assertEquals("Preserved feedback", grade.getFeedback());
            assertEquals(releasedAt, grade.getReleasedAt());
            assertEquals(submissionId, grade.getSubmission().getId());
            assertEquals(teacherId, grade.getGradedBy().getId());
            assertEquals(teacherId, grade.getReleasedBy().getId());
            assertEquals(0L, grade.getVersion());
            assertEquals(Boolean.TRUE, grade.getPassed());
            assertFalse(grade.getSubmission().getContent().isRequiredForModuleCompletion());
            assertEquals(1L, session.createSelectionQuery("select count(s) from Submission s", Long.class).getSingleResult());
        }
    }
}
