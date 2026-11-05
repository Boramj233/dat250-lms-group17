package no.hvl.dat250.lms.module.api.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.api.dto.CreateModuleRequest;
import no.hvl.dat250.lms.module.api.dto.ModuleResponse;
import no.hvl.dat250.lms.module.service.ModuleService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/modules")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

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

    @GetMapping
    public ResponseEntity<List<ModuleResponse>> getModules(
            @PathVariable Long courseId) {

        List<ModuleResponse> modules =
                moduleService.findModulesByCourse(courseId)
                        .stream()
                        .map(module -> ModuleResponse.from(module, courseId))
                        .toList();

        return ResponseEntity.ok(modules);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound(
            NoSuchElementException exception) {

        return ResponseEntity.notFound().build();
    }
}