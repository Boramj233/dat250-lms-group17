package no.hvl.dat250.lms.grade.repository;

import java.util.Optional;
import no.hvl.dat250.lms.grade.domain.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    @org.springframework.data.jpa.repository.Query("""
        select count(g) from Grade g join g.submission s
        where s.content.id = :contentId and g.releasedAt is not null and g.passed = true
          and ((s.group is null and s.submittedBy.user.userId = :userId)
            or exists (select m from Submission x join x.members m where x = s and m.userId = :userId))
        """)
    long countReleasedPassed(Long contentId, Long userId);

    Optional<Grade> findBySubmission_Id(Long submissionId);

    /** Unreleased grades whose scheduled release time has come (with a Pass/Fail decision). */
    @org.springframework.data.jpa.repository.Query("""
        select g from Grade g
        where g.releasedAt is null and g.visibleFrom is not null and g.visibleFrom <= :now
          and g.passed is not null
        """)
    java.util.List<Grade> findDueReleases(java.time.Instant now);
}
