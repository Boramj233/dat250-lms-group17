package no.hvl.dat250.lms.module.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.module.api.dto.UpdateModuleRequest;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.api.dto.CreateModuleRequest;
import no.hvl.dat250.lms.module.api.dto.ModuleResponse;
import no.hvl.dat250.lms.module.service.ModuleAccessService;
import no.hvl.dat250.lms.module.service.ModuleService;

import no.hvl.dat250.lms.security.AuthenticatedUser;
import no.hvl.dat250.lms.security.auth.access.CourseAccess;
import no.hvl.dat250.lms.security.auth.annotations.IsCourseManager;
import no.hvl.dat250.lms.security.auth.annotations.IsEnrolledInCourse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/modules")
public class ModuleController {

    private final ModuleService moduleService;
    private final ModuleAccessService moduleAccessService;
    private final CourseAccess courseAccess;

    public ModuleController(
            ModuleService moduleService,
            ModuleAccessService moduleAccessService,
            CourseAccess courseAccess) {
        this.moduleService = moduleService;
        this.moduleAccessService = moduleAccessService;
        this.courseAccess = courseAccess;
    }

    // ---------------------------------------------------------------- Module

    @IsCourseManager
    @PostMapping
    public ResponseEntity<ModuleResponse> createModule(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateModuleRequest request) {

        Module module = moduleService.createModule(
                courseId,
                request.title(),
                request.position()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ModuleResponse.from(module, courseId));
    }

    @IsEnrolledInCourse
    @GetMapping
    public ResponseEntity<List<ModuleResponse>> getAllModules(
            @PathVariable Long courseId) {

        List<ModuleResponse> modules =
                moduleService.findModulesByCourse(courseId)
                        .stream()
                        .map(module -> ModuleResponse.from(module, courseId))
                        .toList();

        return ResponseEntity.ok(modules);
    }

    @IsEnrolledInCourse
    @GetMapping("/{moduleId}")
    public ResponseEntity<ModuleResponse> getModule(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @AuthenticationPrincipal AuthenticatedUser user,
            Authentication authentication) {

        Module module = moduleService
                .findModuleInCourse(courseId, moduleId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Module not found" + moduleId
                ));

        if (courseAccess.isStudent(authentication, courseId)) {
            moduleAccessService.requireModuleVisible(
                    moduleId,
                    user.userId()
            );
        }

        return ResponseEntity.ok(
                ModuleResponse.from(module, courseId)
        );
    }

    @IsCourseManager
    @PutMapping("/{moduleId}")
    public ResponseEntity<ModuleResponse> updateModule(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @Valid @RequestBody UpdateModuleRequest request) {

        Module module = moduleService.updateModule(
                courseId,
                moduleId,
                request.title(),
                request.position()
        );

        return ResponseEntity.ok(
                ModuleResponse.from(module, courseId)
        );
    }

    // TODO: DeleteModule
    // What logic should be used when a module is deleted?
    // what happens to the content and the dependencies?

    // ---------------------------------------------------------------- Dependency

    @IsCourseManager
    @PostMapping("/{moduleId}/dependencies/{requiredModuleId}")
    public ResponseEntity<ModuleResponse> addDependency(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @PathVariable Long requiredModuleId
    ) {

        Module module = moduleService.addDependency(
                courseId,
                moduleId,
                requiredModuleId
        );

        return ResponseEntity.ok(
                ModuleResponse.from(module, courseId)
        );
    }

    @IsEnrolledInCourse
    @GetMapping("/{moduleId}/dependencies")
    public ResponseEntity<Set<Long>> getAllModuleDependencies(
            @PathVariable Long courseId,
            @PathVariable Long moduleId
    ) {

        Set<Long> dependencyIds = moduleService
                .getDependencies(courseId, moduleId)
                .stream()
                .map(Module::getId)
                .collect(Collectors.toSet());

        return ResponseEntity.ok(dependencyIds);
    }

    @IsCourseManager
    @DeleteMapping("/{moduleId}/dependencies/{requiredModuleId}")
    public ResponseEntity<Void> removeDependency(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @PathVariable Long requiredModuleId
    ) {

        moduleService.removeDependency(
                courseId,
                moduleId,
                requiredModuleId
        );

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound(
            NoSuchElementException exception) {

        return ResponseEntity.notFound().build();
    }
}