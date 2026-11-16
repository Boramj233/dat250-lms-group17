package no.hvl.dat250.lms.user.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.grade.domain.Grade;
import no.hvl.dat250.lms.submission.domain.Submission;
import org.springframework.stereotype.Repository;

/**
 * Read-only queries for a user's own overview across their courses (upcoming deadlines,
 * grades). Which courses count is decided by the enrollment feature
 * ({@code EnrollmentService.activeCourseRoles}); these queries only read the content,
 * submissions and grades of the course ids they are given.
 *
 * <p>Features may not use each other's repositories, and the content, submission and
 * grade services have no per-user queries, so these read those entities directly. They
 * never write, and add no tables.
 */
@Repository
public class UserDashboardRepository {
    private final EntityManager entities;

    public UserDashboardRepository(EntityManager entities) {
        this.entities = entities;
    }

    /** An assignment or coding exercise with a deadline. */
    public record DeadlineRow(Content content, Instant deadline, boolean allowLateSubmission,
                              Boolean mandatoryForExam) {}

    /**
     * Assignments and coding exercises in these courses whose deadline is after
     * {@code now}, with their module and course. Visibility is left to the caller.
     */
    public List<DeadlineRow> findDeadlinesAfter(Collection<Long> courseIds, Instant now) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        Stream<DeadlineRow> assignments = entities.createQuery("""
                select a from Assignment a join fetch a.module m join fetch m.course c
                where c.id in :courseIds and a.deadline > :now
                """, Assignment.class)
            .setParameter("courseIds", courseIds)
            .setParameter("now", now)
            .getResultStream()
            .map(a -> new DeadlineRow(a, a.getDeadline(), a.isAllowLateSubmission(), a.isMandatoryForExam()));
        Stream<DeadlineRow> exercises = entities.createQuery("""
                select x from CodingExercise x join fetch x.module m join fetch m.course c
                where c.id in :courseIds and x.deadline > :now
                """, CodingExercise.class)
            .setParameter("courseIds", courseIds)
            .setParameter("now", now)
            .getResultStream()
            .map(x -> new DeadlineRow(x, x.getDeadline(), x.isAllowLateSubmission(), null));
        return Stream.concat(assignments, exercises).toList();
    }

    /**
     * Every submission in these courses the user handed in alone or is a member of
     * (group submissions), newest first, with its content, module and course.
     */
    public List<Submission> findOwnSubmissions(Long userId, Collection<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return entities.createQuery("""
                select s from Submission s
                  join fetch s.content ct join fetch ct.module m join fetch m.course c
                where c.id in :courseIds
                  and ((s.group is null and s.submittedBy.user.userId = :userId)
                    or exists (select mem from Submission x join x.members mem
                               where x = s and mem.userId = :userId))
                order by s.submittedAt desc, s.id desc
                """, Submission.class)
            .setParameter("userId", userId)
            .setParameter("courseIds", courseIds)
            .getResultList();
    }

    /** The released grades of these submissions (a grade not yet released is left out). */
    public List<Grade> findReleasedGrades(Collection<Long> submissionIds) {
        if (submissionIds.isEmpty()) {
            return List.of();
        }
        return entities.createQuery("""
                select g from Grade g
                where g.submission.id in :ids and g.releasedAt is not null
                """, Grade.class)
            .setParameter("ids", submissionIds)
            .getResultList();
    }
}
