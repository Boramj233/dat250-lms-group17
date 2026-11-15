package no.hvl.dat250.lms.module.service;

import java.util.*;

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

    @Transactional
    public Module updateModule(
            Long courseId,
            Long moduleId,
            String title,
            int position) {

        Module module = findModuleInCourse(courseId, moduleId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Module not found"
                ));

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Module title must not be blank"
            );
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                    "Position cannot be negative"
            );
        }

        module.setTitle(title);
        module.setPosition(position);

        return module;
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

    @Transactional
    public Module addDependency(
            Long courseId,
            Long moduleId,
            Long requiredModuleId) {

        Module module = findModuleInCourse(courseId, moduleId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Module not found"
                ));

        Module requiredModule =
                findModuleInCourse(courseId, requiredModuleId)
                        .orElseThrow(() -> new NoSuchElementException(
                                "Required module not found"
                        ));

        // self-dependency
        if (module.getId().equals(requiredModule.getId())) {
            throw new IllegalArgumentException(
                    "A module cannot depend on itself"
            );
        }

        // circular-dependency
        if (hasDependencyPath(requiredModule, module.getId(),
                new HashSet<>())) {
            throw new IllegalArgumentException(
                    "Circular module dependency detected"
            );
        }

        module.addDependency(requiredModule);

        return module;
    }

    @Transactional
    public Set<Module> getDependencies(
            Long courseId,
            Long moduleId
    ) {

        Module module = findModuleInCourse(courseId, moduleId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Module not found"
                ));

        return Set.copyOf(module.getDependencies());
    }

    @Transactional
    public void removeDependency(
            Long courseId,
            Long moduleId,
            Long requiredModuleId
    ) {

        Module module = findModuleInCourse(courseId, moduleId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Module not found"
                ));

        Module requiredModule = findModuleInCourse(courseId, requiredModuleId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Required module not found"
                ));

        module.removeDependency(requiredModule);
    }

    // ---------------------------------------------------------------- helpers

    private boolean hasDependencyPath(
            Module current,
            Long targetId,
            Set<Long> visited) {

        if (current.getId().equals(targetId)) {
            return true;
        }

        if (!visited.add(current.getId())) {
            return false;
        }

        for (Module dependency : current.getDependencies()) {
            if (hasDependencyPath(dependency, targetId, visited)) {
                return true;
            }
        }

        return false;
    }
}