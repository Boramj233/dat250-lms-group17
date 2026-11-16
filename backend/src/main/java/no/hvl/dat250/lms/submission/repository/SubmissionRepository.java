package no.hvl.dat250.lms.submission.repository;

import java.util.List;
import java.util.Optional;

import no.hvl.dat250.lms.submission.domain.Submission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    Optional<Submission> findByIdAndSubmittedBy_User_UserId(
        Long submissionId,
        Long userId
    );

    // New: retrieve the current user's submission history.
    List<Submission>
        findByContent_IdAndSubmittedBy_User_UserIdOrderBySubmittedAtDescIdDesc(
            Long contentId,
            Long userId
        );
    @Query("""
        select s from Submission s
        where s.content.id = :contentId
          and not exists (
            select newer.id from Submission newer
            where newer.content.id = s.content.id
              and newer.submittedBy.id = s.submittedBy.id
              and (newer.submittedAt > s.submittedAt
                or (newer.submittedAt = s.submittedAt and newer.id > s.id))
          )
        order by s.submittedAt desc, s.id desc
        """)
    Page<Submission> findLatestForReview(
        @Param("contentId") Long contentId,
        Pageable pageable);

    Optional<Submission> findFirstByContent_IdAndSubmittedBy_IdOrderBySubmittedAtDescIdDesc(
        Long contentId, Long enrollmentId);

}
