package no.hvl.dat250.lms.user.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import no.hvl.dat250.lms.grade.domain.Grade;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.user.api.dto.UpcomingDeadlineResponse;
import no.hvl.dat250.lms.user.api.dto.UserGradeResponse;
import no.hvl.dat250.lms.user.repository.UserDashboardRepository;
import no.hvl.dat250.lms.user.repository.UserDashboardRepository.DeadlineRow;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A user's overview across all of their courses: upcoming deadlines and grades.
 * Only courses where the user has an active enrollment count. The caller passes a
 * trusted local user id (from the authenticated principal), never one from the client
 * without checking it.
 */
@Service
public class UserDashboardService {
    private final UserDashboardRepository dashboard;
    private final EnrollmentService enrollments;
    private final Clock clock;

    @Autowired
    public UserDashboardService(UserDashboardRepository dashboard, EnrollmentService enrollments) {
        this(dashboard, enrollments, Clock.systemUTC());
    }

    UserDashboardService(UserDashboardRepository dashboard, EnrollmentService enrollments, Clock clock) {
        this.dashboard = dashboard;
        this.enrollments = enrollments;
        this.clock = clock;
    }

    /**
     * Assignments and coding exercises with a deadline still ahead, soonest first. A
     * student does not get content that is not visible yet (or no longer); a teacher of
     * the course gets all of it.
     */
    @Transactional(readOnly = true)
    public List<UpcomingDeadlineResponse> upcomingDeadlines(Long userId) {
        Instant now = clock.instant();
        Map<Long, CourseRole> roles = enrollments.activeCourseRoles(userId);
        return dashboard.findDeadlinesAfter(roles.keySet(), now).stream()
            .filter(row -> roles.get(courseIdOf(row.content())) == CourseRole.TEACHER
                || isVisible(row.content(), now))
            .sorted(Comparator.comparing(DeadlineRow::deadline)
                .thenComparing(row -> row.content().getTitle()))
            .map(row -> {
                Content content = row.content();
                return new UpcomingDeadlineResponse(
                    content.getId(),
                    typeOf(content),
                    content.getTitle(),
                    row.deadline(),
                    row.allowLateSubmission(),
                    row.mandatoryForExam(),
                    content.getModule().getCourse().getId(),
                    content.getModule().getCourse().getTitle(),
                    content.getModule().getId());
            })
            .toList();
    }

    /**
     * One row per assignment or coding exercise the user has handed in (alone or in a
     * group), for its latest submission, newest first. The grade is there once it is
     * released.
     */
    @Transactional(readOnly = true)
    public List<UserGradeResponse> grades(Long userId) {
        // Newest first, so the first submission seen per content is its latest.
        Map<Long, Submission> latest = new LinkedHashMap<>();
        var courseIds = enrollments.activeCourseRoles(userId).keySet();
        for (Submission submission : dashboard.findOwnSubmissions(userId, courseIds)) {
            Content content = (Content) Hibernate.unproxy(submission.getContent());
            if (content instanceof Assignment || content instanceof CodingExercise) {
                latest.putIfAbsent(content.getId(), submission);
            }
        }
        Map<Long, Grade> grades = dashboard
            .findReleasedGrades(latest.values().stream().map(Submission::getId).toList())
            .stream()
            .collect(Collectors.toMap(grade -> grade.getSubmission().getId(), Function.identity()));

        return latest.values().stream()
            .map(submission -> {
                Content content = (Content) Hibernate.unproxy(submission.getContent());
                Grade grade = grades.get(submission.getId());
                return new UserGradeResponse(
                    content.getId(),
                    typeOf(content),
                    content.getTitle(),
                    content.getModule().getCourse().getId(),
                    content.getModule().getCourse().getTitle(),
                    content.getModule().getId(),
                    submission.getId(),
                    submission.getSubmittedAt(),
                    submission.isLate(),
                    grade == null ? null : new UserGradeResponse.ReleasedGrade(
                        grade.getScore(), grade.getFeedback(), grade.getPassed(), grade.getReleasedAt()));
            })
            .toList();
    }

    private static Long courseIdOf(Content content) {
        return content.getModule().getCourse().getId();
    }

    /** Within the content's own visible window (no module dates exist yet). */
    private static boolean isVisible(Content content, Instant now) {
        return (content.getVisibleFrom() == null || !now.isBefore(content.getVisibleFrom()))
            && (content.getVisibleUntil() == null || now.isBefore(content.getVisibleUntil()));
    }

    /** The type name the frontend uses, as in ContentSummaryResponse. */
    private static String typeOf(Content content) {
        Object actual = Hibernate.unproxy(content);
        if (actual instanceof Assignment) return "ASSIGNMENT";
        if (actual instanceof CodingExercise) return "CODING_EXERCISE";
        throw new IllegalArgumentException("Not an assignment or coding exercise: " + content.getId());
    }
}
