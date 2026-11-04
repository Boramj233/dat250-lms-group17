package no.hvl.dat250.lms.course;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.repository.CourseRepository;
import no.hvl.dat250.lms.course.service.CourseService;

import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {
    // Add @Mock repositories and construct the service when its API is defined.
    // Implement one scenario per business rule, using Arrange, Act, Assert.

    @Mock
    private CourseRepository repository;

    @Test
    void blankCourseTitleIsRejectedWithoutSaving() {

        // Arrange
        CourseService service = new CourseService(repository);

        // Act + Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> service.createCourse(
                " ",
                "Advanced Software Technologies",
                Term.AUTUMN,
                2026
            )
        );

        verifyNoInteractions(repository);
    }


    @Test
    void validCourseIsSavedSuccessfully() {

        // Arrange
        Course savedCourse = new Course(
            "DAT250",
            "Advanced Software Technologies",
            Term.AUTUMN,
            2026
        );

        when(repository.save(any(Course.class)))
            .thenReturn(savedCourse);

        CourseService service = new CourseService(repository);

        // Act
        Course result = service.createCourse(
            "DAT250",
            "Advanced Software Technologies",
            Term.AUTUMN,
            2026
        );

        // Assert
        assertSame(savedCourse, result);

        verify(repository).save(any(Course.class));
    }

    @Test
    void findExistingCourseById() {

        // Arrange
        Course course = new Course(
            "DAT250",
            "Advanced Software Technologies",
            Term.AUTUMN,
            2026
        );

        when(repository.findById(42L))
            .thenReturn(Optional.of(course));

        CourseService service = new CourseService(repository);

        // Act
        Optional<Course> result = service.findCourseById(42L);

        // Assert
        assertTrue(result.isPresent());
        assertSame(course, result.orElseThrow());

        verify(repository).findById(42L);
    }

    @Test
    void findMissingCourseById() {

        // Arrange
        when(repository.findById(999L))
            .thenReturn(Optional.empty());

        CourseService service = new CourseService(repository);

        // Act
        Optional<Course> result = service.findCourseById(999L);

        // Assert
        assertTrue(result.isEmpty());

        verify(repository).findById(999L);
    }

    @Test
    @Disabled("Not implemented yet")
    void courseCanBeCreatedWithTitleDescriptionAndSemester() {
        // Accept valid details and verify all required fields are saved.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void missingRequiredCourseDetailsAreRejected() {
        // Test missing/blank title, description, and missing semester separately.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void courseCreationUsesUserAccess() {
        // Test STANDARD, COURSE_MANAGER, and ADMIN separately against the agreed
        // permission policy. Define the allowed levels and any management scope.
        // A TEACHER enrollment alone must not grant course-creation permission.
        // Verify rejected requests save nothing.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void initialCourseRoleAssignmentUsesUserAccess() {
        // An authorized creator can assign initial course roles during course creation.
        // Verify this permission cannot be reused to enroll students in an existing
        // course without being a TEACHER there. User.Access remains unchanged.
        fail("Not implemented yet");
    }

    @Test
    @Disabled("Not implemented yet")
    void onlyCourseTeachersCanAdministerCourseContent() {
        // Accept administration by a teacher assigned to this course; reject students
        // and users who are teachers only in another course. Keep course-content
        // permissions separate from Access rules for creation and initial role assignment.
        fail("Not implemented yet");
    }
}
