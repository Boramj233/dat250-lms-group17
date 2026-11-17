
package no.hvl.dat250.lms.module;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.domain.Term;
import no.hvl.dat250.lms.course.service.CourseService;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;
import no.hvl.dat250.lms.module.service.ModuleService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private CourseService courseService;

    private ModuleService createService() {
        return new ModuleService(
                moduleRepository,
                courseService
        );
    }

    private Course createTestCourse() {
        return new Course(
                "DAT250",
                "Advanced Software Technologies",
                Term.AUTUMN,
                2026
        );
    }

    @Test
    void validModuleIsSavedForExistingCourse() {

        // Arrange
        Course course = createTestCourse();

        when(courseService.findCourseById(42L))
                .thenReturn(Optional.of(course));

        when(moduleRepository.save(any(Module.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        ModuleService service = createService();

        // Act
        Module result = service.createModule(
                42L, "Java Refresh", 0);

        // Assert
        ArgumentCaptor<Module> captor =
                ArgumentCaptor.forClass(Module.class);

        verify(courseService).findCourseById(42L);
        verify(moduleRepository).save(captor.capture());

        Module saved = captor.getValue();

        assertSame(saved, result);
        assertSame(course, saved.getCourse());
        assertEquals("Java Refresh", saved.getTitle());
        assertEquals(0, saved.getPosition());
    }

    @Test
    void missingCoursePreventsModuleCreation() {

        // Arrange
        when(courseService.findCourseById(999L))
                .thenReturn(Optional.empty());

        ModuleService service = createService();

        // Act + Assert
        assertThrows(
                NoSuchElementException.class,
                () -> service.createModule(
                        999L, "Java Refresh", 0)
        );

        verify(courseService).findCourseById(999L);
        verifyNoInteractions(moduleRepository);
    }

    @Test
    void invalidTitlePreventsModuleCreation() {

        ModuleService service = createService();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createModule(
                        42L, "", 0)
        );

        verifyNoInteractions(
                courseService, moduleRepository);
    }

    @Test
    void existingCourseReturnsModules() {

        // Arrange
        Course course = createTestCourse();

        Module first = new Module(
                course, "Java Refresh", 0);
        Module second = new Module(
                course, "Spring Boot", 1);

        List<Module> expected = List.of(first, second);

        when(courseService.findCourseById(42L))
                .thenReturn(Optional.of(course));

        when(moduleRepository
                .findByCourse_IdOrderByPositionAsc(42L))
                .thenReturn(expected);

        ModuleService service = createService();

        // Act
        List<Module> result =
                service.findModulesByCourse(42L);

        // Assert
        assertEquals(expected, result);

        verify(courseService).findCourseById(42L);
        verify(moduleRepository)
                .findByCourse_IdOrderByPositionAsc(42L);
    }

    @Test
    void missingCoursePreventsListingModules() {

        // Arrange
        when(courseService.findCourseById(999L))
                .thenReturn(Optional.empty());

        ModuleService service = createService();

        // Act + Assert
        assertThrows(
                NoSuchElementException.class,
                () -> service.findModulesByCourse(999L)
        );

        verify(courseService).findCourseById(999L);
        verifyNoInteractions(moduleRepository);
    }

        @Test
        void findModuleInCorrectCourse() {

        // Arrange
        Module module = mock(Module.class);

        when(moduleRepository.findByIdAndCourse_Id(10L, 1L))
                .thenReturn(Optional.of(module));

        ModuleService service = new ModuleService(
                moduleRepository,
                courseService
        );

        // Act
        Optional<Module> result =
                service.findModuleInCourse(1L, 10L);

        // Assert
        assertTrue(result.isPresent());
        assertSame(module, result.get());

        verify(moduleRepository)
                .findByIdAndCourse_Id(10L, 1L);

        verifyNoInteractions(courseService);
        }

        @Test
        void findModuleInWrongCourseReturnsEmpty() {

        // Arrange
        when(moduleRepository.findByIdAndCourse_Id(10L, 2L))
                .thenReturn(Optional.empty());

        ModuleService service = new ModuleService(
                moduleRepository,
                courseService
        );

        // Act
        Optional<Module> result =
                service.findModuleInCourse(2L, 10L);

        // Assert
        assertTrue(result.isEmpty());

        verify(moduleRepository)
                .findByIdAndCourse_Id(10L, 2L);

        verifyNoInteractions(courseService);
        }
}
