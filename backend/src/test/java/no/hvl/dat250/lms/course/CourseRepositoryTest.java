package no.hvl.dat250.lms.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;

@DataJpaTest
class CourseRepositoryTest {

    @Autowired
    private CourseRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldSaveAndFindCourse() {

        // 1. Create a new entity
        Course course = new Course(
            "DAT250",
            "Advanced Software Technologies",
            Term.AUTUMN,
            2026
        );

        // 2. Save it to the database
        Course saved = repository.saveAndFlush(course);

        Long id = saved.getId();
        assertNotNull(id);

        // 3. Clear the JPA persistence context
        entityManager.clear();

        // 4. Read the course again
        Course found = repository.findById(id).orElseThrow();

        // 5. Verify the data
        assertEquals("DAT250", found.getTitle());
        assertEquals("Advanced Software Technologies",
                     found.getDescription());
        assertEquals(Term.AUTUMN, found.getTerm());
        assertEquals(2026, found.getYear());
    }
}