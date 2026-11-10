
package no.hvl.dat250.lms.content.service;

import java.time.Instant;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.repository.ContentRepository;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.service.ModuleService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodingExerciseService {

    private final ContentRepository contentRepository;
    private final ModuleService moduleService;

    public CodingExerciseService(
            ContentRepository contentRepository,
            ModuleService moduleService) {

        this.contentRepository = contentRepository;
        this.moduleService = moduleService;
    }

    @Transactional
    public CodingExercise createCodingExercise(
            Long courseId,
            Long moduleId,
            String title,
            int position,
            String instructions,
            String starterRepositoryUrl,
            Instant deadline,
            boolean allowLateSubmission) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                "Exercise title is required"
            );
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                "Position must be non-negative"
            );
        }

        // ModuleService returns Optional<Module>.
        Module module = moduleService
            .findModuleInCourse(courseId, moduleId)
            .orElseThrow(() -> new NoSuchElementException(
                "Module " + moduleId
                    + " not found in course " + courseId
            ));

        CodingExercise exercise = new CodingExercise(
            module,
            title,
            position,
            instructions,
            starterRepositoryUrl,
            deadline,
            allowLateSubmission
        );

        return contentRepository.save(exercise);
    }

    @Transactional(readOnly = true)
    public CodingExercise findCodingExercise(Long exerciseId) {

        Content content = contentRepository.findById(
            exerciseId
        ).orElseThrow(() -> new NoSuchElementException(
            "Coding exercise not found: " + exerciseId
        ));

        if (!(content instanceof CodingExercise exercise)) {
            throw new NoSuchElementException(
                "Coding exercise not found: " + exerciseId
            );
        }

        return exercise;
    }
}
