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

    boolean existsByContent_Id(Long contentId);

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
    @Query("select s from Submission s where s.id = :submissionId and "
        + "((s.group is null and s.submittedBy.user.userId = :userId) or "
        + "exists (select m from Submission x join x.members m where x = s and m.userId = :userId))")
    Optional<Submission> findByIdAndSubmittedBy_User_UserId(
        Long submissionId,
        Long userId
    );

    // New: retrieve the current user's submission history.
    @Query("select s from Submission s where s.content.id = :contentId and "
        + "((s.group is null and s.submittedBy.user.userId = :userId) or "
        + "exists (select m from Submission x join x.members m where x = s and m.userId = :userId)) "
        + "order by s.submittedAt desc, s.id desc")
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
              and ((s.group is null and newer.group is null and newer.submittedBy.id = s.submittedBy.id)
                or (s.group is not null and newer.group.id = s.group.id))
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

    @Query("select s from Submission s where s.content.id = :contentId and "
        + "((:groupId is null and s.group is null and s.submittedBy.id = :enrollmentId) "
        + "or (:groupId is not null and s.group.id = :groupId)) order by s.submittedAt desc, s.id desc")
    List<Submission> findSequence(Long contentId, Long enrollmentId, Long groupId);
}
