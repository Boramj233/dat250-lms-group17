package no.hvl.dat250.lms.submission.repository;

import java.util.List;
import java.util.Optional;

import no.hvl.dat250.lms.submission.domain.Submission;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository
        extends JpaRepository<Submission, Long> {

    // Existing: history for a specific Enrollment.
    List<Submission>
        findByContent_IdAndSubmittedBy_IdOrderBySubmittedAtDescIdDesc(
            Long contentId,
            Long enrollmentId
        );

    // Existing: count submissions by Enrollment.
    long countByContent_IdAndSubmittedBy_Id(
        Long contentId,
        Long enrollmentId
    );

    // New: retrieve a submission only if the given User owns it.
    Optional<Submission> findByIdAndSubmittedBy_User_Id(
        Long submissionId,
        Long userId
    );

    // New: retrieve the current user's submission history.
    List<Submission>
        findByContent_IdAndSubmittedBy_User_IdOrderBySubmittedAtDescIdDesc(
            Long contentId,
            Long userId
        );
}
