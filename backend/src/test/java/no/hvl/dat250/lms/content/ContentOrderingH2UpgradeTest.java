package no.hvl.dat250.lms.content;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.math.BigDecimal;
import jakarta.persistence.Entity;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.course.domain.*;
import no.hvl.dat250.lms.enrollment.domain.*;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.grade.domain.Grade;
import no.hvl.dat250.lms.submission.domain.*;
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

/** All schema changes are confined to a disposable file-backed H2. */
class ContentOrderingH2UpgradeTest {
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
    void upgradeNormalizesDuplicateNegativePositionsAndPreservesHistory() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("content-order-upgrade");
        Long pageId, assignmentId, otherPageId, gradeId;
        try (var factory = sessionFactory(url, "create"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            Course course = new Course("DAT250", "Upgrade", Term.AUTUMN, 2026);
            session.persist(course);
            Module module = new Module(course, "Week", 0);
            Module other = new Module(course, "Other", 1);
            session.persist(module); session.persist(other);
            User student = new User("student@example.com", "Test", "Student");
            User teacher = new User("teacher@example.com", "Test", "Teacher");
            session.persist(student); session.persist(teacher);
            Enrollment owner = new Enrollment(student, course, CourseRole.STUDENT);
            Enrollment grader = new Enrollment(teacher, course, CourseRole.TEACHER);
            session.persist(owner); session.persist(grader);
            StaticPage page = new StaticPage(module, "Page", 0, "Body");
            Assignment assignment = new Assignment(module, "Report", 1, "Write", null, false);
            StaticPage otherPage = new StaticPage(other, "Other", 99, "Body");
            session.persist(page); session.persist(assignment); session.persist(otherPage);
            Submission submission = new Submission(assignment, owner, SubmissionType.TEXT, "Answer", Instant.now(), false);
            session.persist(submission);
            Grade grade = new Grade(submission, BigDecimal.ZERO, "Passed", grader, Instant.now());
            grade.decidePassed(true); grade.release(grader, Instant.now()); session.persist(grade);
            tx.commit();
            pageId = page.getId(); assignmentId = assignment.getId(); otherPageId = otherPage.getId(); gradeId = grade.getId();
        }
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.execute("ALTER TABLE contents DROP CONSTRAINT uq_content_module_position");
            sql.execute("ALTER TABLE contents DROP CONSTRAINT ck_content_position_nonnegative");
            sql.execute("UPDATE contents SET content_position=-1");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-content-ordering.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-content-ordering.sql"));
            SQLException duplicate = assertThrows(SQLException.class,
                () -> sql.executeUpdate("UPDATE contents SET content_position=0 WHERE id=" + assignmentId));
            assertEquals("23505", duplicate.getSQLState());
            SQLException negative = assertThrows(SQLException.class,
                () -> sql.executeUpdate("UPDATE contents SET content_position=-1 WHERE id=" + pageId));
            assertEquals("23513", negative.getSQLState());
        }
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            assertEquals(0, session.find(Content.class, pageId).getPosition());
            assertEquals(1, session.find(Content.class, assignmentId).getPosition());
            assertEquals(0, session.find(Content.class, otherPageId).getPosition());
            Grade grade = session.find(Grade.class, gradeId);
            assertEquals(assignmentId, grade.getSubmission().getContent().getId());
            assertEquals(Boolean.TRUE, grade.getPassed());
            assertNotNull(grade.getReleasedAt());
        }
    }
}
