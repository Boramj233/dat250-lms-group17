package no.hvl.dat250.lms.grade.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.Objects;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import no.hvl.dat250.lms.grade.api.dto.*;
import no.hvl.dat250.lms.grade.domain.Grade;
import no.hvl.dat250.lms.grade.repository.GradeRepository;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.service.SubmissionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingService {
    private final GradeRepository grades;
    private final SubmissionService submissions;
    private final EnrollmentService enrollments;
    private final Clock clock;

    public GradingService(GradeRepository grades, SubmissionService submissions,
                          EnrollmentService enrollments, @Qualifier("submissionClock") Clock clock) {
        this.grades = grades;
        this.submissions = submissions;
        this.enrollments = enrollments;
        this.clock = clock;
    }

    @Transactional
    public GradeReviewResponse save(Long submissionId, Long teacherUserId,
                                    BigDecimal score, String feedback, Long version) {
        return save(submissionId, teacherUserId, score, feedback, version, null);
    }

    @Transactional
    public GradeReviewResponse save(Long submissionId, Long teacherUserId,
                                    BigDecimal score, String feedback, Long version, Boolean passed) {
        Submission submission = submissions.lockTeacherSubmission(submissionId, teacherUserId);
        requireLatest(submission);
        Enrollment teacher = teacher(submission, teacherUserId);
        Grade grade = grades.findBySubmission_Id(submissionId).orElse(null);
        if (grade == null) {
            if (version != null) {
                throw new GradeConflictException("Grade does not exist; refresh before creating");
            }
            grade = new Grade(submission, score, feedback, teacher, clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        } else {
            if (grade.getReleasedAt() != null) {
                throw new GradeConflictException("Released grades cannot be edited");
            }
            requireVersion(grade, version);
            grade.revise(score, feedback, teacher, clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        }
        grade.decidePassed(passed);
        return GradeReviewResponse.from(grades.saveAndFlush(grade));
    }

    @Transactional
    public GradeReviewResponse release(Long submissionId, Long teacherUserId, Long version) {
        Submission submission = submissions.lockTeacherSubmission(submissionId, teacherUserId);
        Grade grade = requireGrade(submissionId);
        // A retry must preserve the original release, including after a resubmission.
        if (grade.getReleasedAt() != null) {
            return GradeReviewResponse.from(grade);
        }
        requireVersion(grade, version);
        requireLatest(submission);
        grade.release(teacher(submission, teacherUserId), clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        return GradeReviewResponse.from(grades.saveAndFlush(grade));
    }

    @Transactional(readOnly = true)
    public GradeReviewResponse review(Long submissionId, Long teacherUserId) {
        submissions.requireTeacherSubmission(submissionId, teacherUserId);
        return GradeReviewResponse.from(requireGrade(submissionId));
    }

    @Transactional(readOnly = true)
    public ReleasedGradeResponse ownReleased(Long submissionId, Long userId) {
        submissions.findOwnSubmission(submissionId, userId);
        Grade grade = requireGrade(submissionId);
        if (grade.getReleasedAt() == null) {
            throw new NoSuchElementException("Released grade not found");
        }
        return ReleasedGradeResponse.from(grade);
    }

    private Enrollment teacher(Submission submission, Long userId) {
        return enrollments.requireActiveTeacher(userId, submission.getContent().getModule().getCourse().getId());
    }

    private Grade requireGrade(Long submissionId) {
        return grades.findBySubmission_Id(submissionId)
            .orElseThrow(() -> new NoSuchElementException("Grade not found"));
    }

    private void requireLatest(Submission submission) {
        if (!submissions.isLatestSubmission(submission)) {
            throw new GradeConflictException("A newer submission exists; review the latest version");
        }
    }

    private void requireVersion(Grade grade, Long version) {
        if (version == null || !Objects.equals(grade.getVersion(), version)) {
            throw new GradeConflictException("Grade changed; refresh before updating");
        }
    }
}
