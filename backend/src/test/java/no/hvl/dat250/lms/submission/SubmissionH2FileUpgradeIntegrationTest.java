package no.hvl.dat250.lms.submission;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;

import jakarta.persistence.Entity;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionFileMetadata;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
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

/** Upgrade a disposable file-backed H2, then validate and use the current ORM mappings. */
class SubmissionH2FileUpgradeIntegrationTest {

    @TempDir
    Path directory;

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
    void preservesHistoryAndSupportsFileAfterManualUpgradeAndOrmRestart() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("lms");
        Instant submittedAt = Instant.parse("2026-11-15T12:00:00Z");
        Long assignmentId;
        Long enrollmentId;
        Long textId;
        Long gitId;

        try (SessionFactory factory = sessionFactory(url, "create"); var session = factory.openSession()) {
            var transaction = session.beginTransaction();
            User user = new User("upgrade@example.com", "Upgrade", "Test");
            session.persist(user);
            Course course = new Course("DAT250", "Upgrade fixture", Term.AUTUMN, 2026);
            session.persist(course);
            Module module = new Module(course, "Week 1", 0);
            session.persist(module);
            Enrollment enrollment = new Enrollment(user, course, CourseRole.STUDENT);
            session.persist(enrollment);
            Assignment assignment = new Assignment(module, "Report", 0, "Write report", null, false);
            session.persist(assignment);
            Submission text = new Submission(assignment, enrollment, SubmissionType.TEXT, "Original answer", submittedAt, false);
            Submission git = new Submission(assignment, enrollment, SubmissionType.GIT_URL,
                    "https://example.com/repo", submittedAt.plusSeconds(1), true);
            session.persist(text);
            session.persist(git);
            transaction.commit();
            assignmentId = assignment.getId();
            enrollmentId = enrollment.getId();
            textId = text.getId();
            gitId = git.getId();
        }

        // Reproduce the legacy submissions shape without changing any historical data.
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            for (String column : List.of("file_bucket", "file_object_key", "file_original_filename",
                    "file_media_type", "file_size_bytes", "file_sha256")) {
                sql.execute("ALTER TABLE submissions DROP COLUMN " + column);
            }
            sql.execute("ALTER TABLE submissions ALTER COLUMN payload SET NOT NULL");
            sql.execute("ALTER TABLE submissions ALTER COLUMN submission_type ENUM ('GIT_URL', 'TEXT') NOT NULL");
        }
        Files.copy(directory.resolve("lms.mv.db"), directory.resolve("lms-backup.mv.db"));

        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-file-submissions.sql"));
        }

        // validate, rather than update: the manual script must produce a compatible schema.
        try (SessionFactory factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            var transaction = session.beginTransaction();
            Submission text = session.find(Submission.class, textId);
            Submission git = session.find(Submission.class, gitId);
            assertEquals("Original answer", text.getPayload());
            assertEquals(submittedAt, text.getSubmittedAt());
            assertEquals(enrollmentId, text.getSubmittedBy().getId());
            assertNull(text.getFile());
            assertEquals("https://example.com/repo", git.getPayload());
            assertTrue(git.isLate());
            assertNull(git.getFile());

            var file = new SubmissionFileMetadata("lms-submissions", "submissions/new-version", "report.pdf",
                    "application/pdf", 123, "a".repeat(64));
            Submission uploaded = Submission.file(session.find(Assignment.class, assignmentId),
                    session.find(Enrollment.class, enrollmentId), file, submittedAt.plusSeconds(2), false);
            session.persist(uploaded);
            transaction.commit();
            session.clear();

            Submission stored = session.find(Submission.class, uploaded.getId());
            assertNull(stored.getPayload());
            assertEquals("submissions/new-version", stored.getFile().getObjectKey());
            assertEquals(3L, session.createSelectionQuery("select count(s) from Submission s", Long.class).getSingleResult());
        }
        assertTrue(Files.size(directory.resolve("lms-backup.mv.db")) > 0);
    }
}
