package no.hvl.dat250.lms.module.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import no.hvl.dat250.lms.course.domain.Course;
import no.hvl.dat250.lms.course.service.CourseService;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.repository.ModuleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseService courseService;

    public ModuleService(
            ModuleRepository moduleRepository,
            CourseService courseService) {

        this.moduleRepository = moduleRepository;
        this.courseService = courseService;
    }

    @Transactional
    public Module createModule(
            Long courseId,
            String title,
            int position) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                "Module title must not be blank");
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                "Module position must not be negative");
        }

        if (courseId == null) {
            throw new IllegalArgumentException(
                "Course ID must not be null");
        }

        Course course = courseService.findCourseById(courseId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Course not found: " + courseId
                ));

            Module module = new Module(course, title, position);

        return moduleRepository.save(module);
    }

    public Optional<Module> findModuleById(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                "Module ID must not be null");
        }

        return moduleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Module> findModulesByCourse(Long courseId) {

        if (courseId == null) {
            throw new IllegalArgumentException(
                "Course ID must not be null");
        }

        courseService.findCourseById(courseId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Course not found: " + courseId));

        return moduleRepository
                .findByCourse_IdOrderByPositionAsc(courseId);
    }

    @Transactional(readOnly = true)
    public Optional<Module> findModuleInCourse(
            Long courseId,
            Long moduleId) {

        if (courseId == null || moduleId == null) {
            throw new IllegalArgumentException(
                "Course ID and Module ID must not be null");
        }

        return moduleRepository.findByIdAndCourse_Id(
            moduleId,
            courseId
        );
    }
}