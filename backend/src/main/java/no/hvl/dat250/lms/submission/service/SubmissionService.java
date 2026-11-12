
package no.hvl.dat250.lms.submission.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.service.ContentService;

import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.service.StudentEnrollmentLookupService;

import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.repository.SubmissionRepository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {

    private final ContentService contentService;
    private final StudentEnrollmentLookupService enrollmentLookupService;
    private final SubmissionRepository submissionRepository;
    private final Clock clock;

    public SubmissionService(
            ContentService contentService,
            StudentEnrollmentLookupService enrollmentLookupService,
            SubmissionRepository submissionRepository,
            @Qualifier("submissionClock") Clock clock) {

        this.contentService = contentService;
        this.enrollmentLookupService = enrollmentLookupService;
        this.submissionRepository = submissionRepository;
        this.clock = clock;
    }

    @Transactional
    public Submission submitIndividual(
            Long contentId,
            Long userId,
            SubmissionType type,
            String payload) {

        // 1. Validate input.
        if (contentId == null || contentId <= 0
                || userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                "Content ID and User ID must be positive"
            );
        }

        if (type == null
                || payload == null
                || payload.isBlank()
                || payload.length() > 10000) {
            throw new IllegalArgumentException(
                "Valid submission type and payload are required"
            );
        }

        // 2. Retrieve Content through its own feature Service.
        Content content = contentService.requireContent(contentId);

        // 3. Determine deadline rules.
        Instant deadline;
        boolean allowLateSubmission;

        if (content instanceof Assignment assignment) {

            deadline = assignment.getDeadline();
            allowLateSubmission =
                assignment.isAllowLateSubmission();

        } else if (content instanceof CodingExercise exercise) {

            deadline = exercise.getDeadline();
            allowLateSubmission =
                exercise.isAllowLateSubmission();

        } else {
            throw new IllegalArgumentException(
                "This Content does not accept submissions"
            );
        }

        // 4. Determine the Course owning this Content.
        Long courseId = content
            .getModule()
            .getCourse()
            .getId();

        // 5. Verify the student and obtain their Enrollment.
        Enrollment enrollment =
            enrollmentLookupService
                .requireActiveStudentEnrollment(
                    userId,
                    courseId
                );

        // 6. Calculate lateness using the server clock.
        Instant now = clock.instant();

        boolean late = deadline != null
            && now.isAfter(deadline);

        if (late && !allowLateSubmission) {
            throw new IllegalStateException(
                "Submission deadline has passed"
            );
        }

        // 7. Save a new Submission.
        // Previous submissions remain in the database.
        Submission submission = new Submission(
            content,
            enrollment,
            type,
            payload,
            now,
            late
        );

        return submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public Submission findOwnSubmission(
            Long submissionId,
            Long userId) {

        if (submissionId == null || submissionId <= 0
                || userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                "Submission ID and User ID must be positive"
            );
        }

        return submissionRepository
            .findByIdAndSubmittedBy_User_UserId(
                submissionId,
                userId
            )
            .orElseThrow(() -> new NoSuchElementException(
                "Submission not found"
            ));
    }

    @Transactional(readOnly = true)
    public List<Submission> findMySubmissionHistory(
            Long contentId,
            Long userId) {

        if (contentId == null || contentId <= 0
                || userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                "Content ID and User ID must be positive"
            );
        }

        Content content = contentService.requireContent(contentId);

        if (!(content instanceof Assignment)
                && !(content instanceof CodingExercise)) {
            throw new NoSuchElementException(
                "Submittable content not found"
            );
        }

        return submissionRepository
            .findByContent_IdAndSubmittedBy_User_UserIdOrderBySubmittedAtDescIdDesc(
                contentId,
                userId
            );
    }

}
