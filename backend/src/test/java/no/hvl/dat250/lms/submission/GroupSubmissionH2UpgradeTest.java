package no.hvl.dat250.lms.submission;

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
class GroupSubmissionH2UpgradeTest {
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
    void upgradePreservesLegacyGradeAndPersistsGroupSnapshotsAcrossRestart() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("group-upgrade");
        Long legacyId;
        Long groupId;
        Long studentEnrollmentId;
        Long assignmentId;
        Long studentId;
        Instant now = Instant.parse("2026-11-16T10:00:00Z");
        try (var factory = sessionFactory(url, "create"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            var student = new User("student@example.com", "Test", "Student");
            var teacher = new User("teacher@example.com", "Test", "Teacher");
            session.persist(student); session.persist(teacher);
            var course = new Course("DAT250", "Upgrade", Term.AUTUMN, 2026); session.persist(course);
            var owner = new Enrollment(student, course, CourseRole.STUDENT); session.persist(owner);
            var grader = new Enrollment(teacher, course, CourseRole.TEACHER); session.persist(grader);
            var module = new Module(course, "Week", 0); session.persist(module);
            var assignment = new Assignment(module, "Report", 0, "Write", null, false); session.persist(assignment);
            var group = new no.hvl.dat250.lms.group.domain.Group(1, course, null);
            group.addGroupMember(student); session.persist(group);
            var submission = new Submission(assignment, owner, SubmissionType.TEXT, "Legacy", now, false);
            session.persist(submission);
            var grade = new Grade(submission, BigDecimal.TEN, "Preserved", grader, now);
            grade.decidePassed(true);
            grade.release(grader, now); session.persist(grade);
            tx.commit();
            legacyId = submission.getId(); groupId = group.getGroupId(); assignmentId = assignment.getId();
            studentEnrollmentId = owner.getId(); studentId = student.getUserId();
        }
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("DROP TABLE submission_members");
            statement.execute("ALTER TABLE submissions DROP COLUMN group_id");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-group-submissions.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-group-submissions.sql"));
        }
        Long groupSubmissionId;
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            var legacy = session.find(Submission.class, legacyId);
            assertNull(legacy.getGroup()); assertTrue(legacy.getMembers().isEmpty());
            assertEquals("Legacy", legacy.getPayload());
            var grade = session.createSelectionQuery("from Grade where submission.id = :id", Grade.class)
                .setParameter("id", legacyId).getSingleResult();
            assertEquals(now, grade.getReleasedAt());
            assertEquals("Preserved", grade.getFeedback());
            var tx = session.beginTransaction();
            var groupSubmission = new Submission(session.find(Assignment.class, assignmentId),
                session.find(Enrollment.class, studentEnrollmentId), SubmissionType.TEXT, "Group", now, false);
            groupSubmission.assignGroup(session.find(no.hvl.dat250.lms.group.domain.Group.class, groupId),
                java.util.List.of(new SubmissionMember(studentId, studentEnrollmentId)));
            session.persist(groupSubmission); tx.commit(); groupSubmissionId = groupSubmission.getId();
        }
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            var restored = session.find(Submission.class, groupSubmissionId);
            assertEquals(groupId, restored.getGroup().getGroupId());
            assertEquals(studentId, restored.getMembers().getFirst().getUserId());
            assertEquals(studentEnrollmentId, restored.getMembers().getFirst().getEnrollmentId());
        }
    }
}
