package no.hvl.dat250.lms.content;

import java.nio.file.Path;
import java.sql.DriverManager;
import jakarta.persistence.Entity;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.module.domain.Module;
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

/** Uses only a disposable file-backed H2; never the development database. */
class CodingExerciseH2UpgradeTest {
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
    void upgradePreservesExistingExerciseAndStarterCodeSurvivesRestart() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("coding-starter-code-upgrade");
        Long oldId;
        Long moduleId;
        try (var factory = sessionFactory(url, "create"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            Course course = new Course("DAT250", "Upgrade", Term.AUTUMN, 2026);
            session.persist(course);
            Module module = new Module(course, "Week", 0);
            session.persist(module);
            CodingExercise exercise = new CodingExercise(
                module, "Existing", 0, "Instructions", "https://example.com/starter", null, true);
            session.persist(exercise);
            tx.commit();
            oldId = exercise.getId();
            moduleId = module.getId();
        }
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.execute("ALTER TABLE coding_exercises DROP COLUMN starter_code");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-coding-starter-code.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/manual/h2-coding-starter-code.sql"));
        }
        Long newId;
        String starterCode = "  class Main {\n    // TODO\n  }\n";
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            CodingExercise existing = session.find(CodingExercise.class, oldId);
            assertNull(existing.getStarterCode());
            assertEquals("Instructions", existing.getInstructions());
            assertEquals("https://example.com/starter", existing.getStarterRepositoryUrl());
            assertTrue(existing.isAllowLateSubmission());
            CodingExercise exercise = new CodingExercise(session.find(Module.class, moduleId),
                "New", 1, "Implement", "https://example.com/new", starterCode, null, false);
            session.persist(exercise);
            tx.commit();
            newId = exercise.getId();
        }
        try (var factory = sessionFactory(url, "validate"); var session = factory.openSession()) {
            assertEquals(starterCode, session.find(CodingExercise.class, newId).getStarterCode());
            assertNull(session.find(CodingExercise.class, oldId).getStarterCode());
        }
    }
}
