
package no.hvl.dat250.lms.content.service;

import java.time.Instant;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.repository.ContentRepository;

import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.service.ModuleService;

import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodingExerciseService {

    private final ContentRepository contentRepository;
    private final ModuleService moduleService;
    private final EnrollmentService enrollmentService;
    private final ContentService contentService;

    public CodingExerciseService(
            ContentRepository contentRepository,
            ModuleService moduleService,
            EnrollmentService enrollmentService,
            ContentService contentService) {

        this.contentRepository = contentRepository;
        this.moduleService = moduleService;
        this.enrollmentService = enrollmentService;
        this.contentService = contentService;
    }

    @Transactional
    public CodingExercise createCodingExercise(
            Long courseId,
            Long moduleId,
            String title,
            int position,
            String instructions,
            String starterRepositoryUrl,
            String starterCode,
            Instant deadline,
            boolean allowLateSubmission,
            Long teacherUserId) {

        enrollmentService.requireActiveTeacher(teacherUserId, courseId);

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
            starterCode,
            deadline,
            allowLateSubmission
        );

        if (contentRepository.existsByModule_IdAndPosition(moduleId, position)) {
            throw new ContentPositionConflictException("Content position is already occupied in this module");
        }

        return contentRepository.save(exercise);
    }

    @Transactional
    public CodingExercise updateDetails(Long exerciseId, String title, String instructions,
                                       String starterRepositoryUrl, String starterCode) {
        CodingExercise exercise = findCodingExercise(exerciseId);
        exercise.updateDetails(title, instructions, starterRepositoryUrl, starterCode);
        return exercise;
    }

    @Transactional
    public CodingExercise updateSubmissionSettings(
            Long exerciseId,
            Instant deadline,
            boolean allowLateSubmission,
            Long teacherUserId) {
        // Share the submission content lock and refresh rules after any wait.
        Content content = contentService.lockSubmissionContent(exerciseId);
        if (!(content instanceof CodingExercise exercise)) {
            throw new NoSuchElementException("CodingExercise not found");
        }
        enrollmentService.requireActiveTeacher(teacherUserId, content.getModule().getCourse().getId());
        exercise.updateSubmissionSettings(deadline, allowLateSubmission);
        return exercise;
    }

    @Transactional(readOnly = true)
    public Long courseId(Long exerciseId) {
        return findCodingExercise(exerciseId).getModule().getCourse().getId();
    }

    /** Internal lookup; HTTP callers must pass the course enrollment check. */
    @Transactional(readOnly = true)
    public CodingExercise findCodingExercise(Long exerciseId) {

        Content content = contentRepository.findById(
            exerciseId
        ).orElseThrow(() -> new NoSuchElementException(
            "Coding exercise not found: " + exerciseId
        ));

        if (!(Hibernate.unproxy(content) instanceof CodingExercise exercise)) {
            throw new NoSuchElementException(
                "Coding exercise not found: " + exerciseId
            );
        }

        return exercise;
    }
}
