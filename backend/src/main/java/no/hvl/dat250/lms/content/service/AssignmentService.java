package no.hvl.dat250.lms.content.service;

import java.time.Instant;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.repository.ContentRepository;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.service.ModuleService;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {

    private final ContentRepository contentRepository;
    private final ModuleService moduleService;
    private final EnrollmentService enrollmentService;

    public AssignmentService(
            ContentRepository contentRepository,
            ModuleService moduleService,
            EnrollmentService enrollmentService) {

        this.contentRepository = contentRepository;
        this.moduleService = moduleService;
        this.enrollmentService = enrollmentService;
    }

    @Transactional
    public Assignment createAssignment(
            Long courseId,
            Long moduleId,
            String title,
            int position,
            String instructions,
            Instant deadline,
            boolean allowLateSubmission,
            Long teacherUserId) {

        enrollmentService.requireActiveTeacher(teacherUserId, courseId);

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                "Assignment title is required"
            );
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                "Position must be non-negative"
            );
        }

        Module module = moduleService.findModuleInCourse(
            courseId,
            moduleId
        ).orElseThrow(() -> new NoSuchElementException(
            "Module " + moduleId
                + " not found in course " + courseId
        ));

        Assignment assignment = new Assignment(
            module,
            title,
            position,
            instructions,
            deadline,
            allowLateSubmission
        );

        return contentRepository.save(assignment);
    }

    @Transactional
    public void updateExamRequirement(
            Long assignmentId,
            Long teacherUserId,
            boolean mandatoryForExam) {

        Assignment assignment = findAssignment(assignmentId);
        Long courseId = assignment.getModule().getCourse().getId();
        enrollmentService.requireActiveTeacher(teacherUserId, courseId);
        assignment.setMandatoryForExam(mandatoryForExam);
    }

    @Transactional(readOnly = true)
    public Assignment findAssignment(Long assignmentId) {

        Content content = contentRepository.findById(
            assignmentId
        ).orElseThrow(() ->
            new NoSuchElementException(
                "Assignment not found: " + assignmentId
            )
        );

        if (!(Hibernate.unproxy(content) instanceof Assignment assignment)) {
            throw new NoSuchElementException(
                "Assignment not found: " + assignmentId
            );
        }

        return assignment;
    }
}
