package no.hvl.dat250.lms.content;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.repository.ContentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ContentRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ContentRepository contentRepository;

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
}