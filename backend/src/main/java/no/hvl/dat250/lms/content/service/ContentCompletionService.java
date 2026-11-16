package no.hvl.dat250.lms.content.service;

import java.time.Instant;
import java.util.NoSuchElementException;
import no.hvl.dat250.lms.content.domain.*;
import no.hvl.dat250.lms.content.repository.*;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import no.hvl.dat250.lms.grade.service.GradeCompletionService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

/** Completion only: never traverses module prerequisites or unlocks modules. */
@Service
public class ContentCompletionService {
    private final ContentRepository contents;
    private final StaticPageCompletionRepository pages;
    private final QuizAttemptRepository attempts;
    private final EnrollmentService enrollments;
    private final GradeCompletionService grades;
    public ContentCompletionService(ContentRepository contents, StaticPageCompletionRepository pages,
            QuizAttemptRepository attempts, EnrollmentService enrollments, GradeCompletionService grades) {
        this.contents = contents; this.pages = pages; this.attempts = attempts;
        this.enrollments = enrollments; this.grades = grades;
    }
    private Content content(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("Positive content ID required");
        return (Content) Hibernate.unproxy(contents.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Content not found")));
    }
    private Enrollment student(Content content, Long userId) {
        try {
            return enrollments.requireActiveStudent(userId, content.getModule().getCourse().getId());
        } catch (NoSuchElementException | IllegalStateException ex) {
            throw new AccessDeniedException("Active course student required", ex);
        }
    }
    /** userId is a trusted local User ID, never a client-supplied enrollment ID.
     * Uses current quiz threshold; any historical released assignment pass counts.
     */
    @Transactional(readOnly = true)
    public boolean isCompleted(Long contentId, Long userId) {
        Content content = content(contentId);
        Enrollment student = student(content, userId);
        if (content instanceof StaticPage) return pages.existsByContent_IdAndEnrollment_Id(contentId, student.getId());
        if (content instanceof Quiz quiz) return attempts.countPassing(contentId, student.getId(), quiz.getPassingPercentage()) > 0;
        if (content instanceof Assignment) return grades.hasReleasedPass(contentId, userId);
        return false;
    }
    public record Status(Long contentId, boolean requiredForModuleCompletion, boolean completed) {}
    @Transactional(readOnly = true)
    public Status status(Long contentId, Long userId) {
        boolean completed = isCompleted(contentId, userId);
        return new Status(contentId, content(contentId).isRequiredForModuleCompletion(), completed);
    }
    @Transactional
    public Status markPage(Long contentId, Long userId, boolean completed) {
        Content content = content(contentId);
        Enrollment student = student(content, userId);
        if (!(content instanceof StaticPage page)) throw new IllegalArgumentException("Only pages support manual completion");
        Instant now = Instant.now();
        if ((page.getVisibleFrom() != null && now.isBefore(page.getVisibleFrom()))
                || (page.getVisibleUntil() != null && !now.isBefore(page.getVisibleUntil()))) {
            throw new AccessDeniedException("Page outside visibility window");
        }
        if (completed) {
            if (!pages.existsByContent_IdAndEnrollment_Id(contentId, student.getId()))
                pages.save(new StaticPageCompletion(page, student, now));
        } else pages.deleteByContent_IdAndEnrollment_Id(contentId, student.getId());
        return status(contentId, userId);
    }
    @Transactional
    public void configure(Long contentId, Long teacherUserId, boolean required, Integer passingPercentage) {
        Content content = content(contentId);
        try { enrollments.requireActiveTeacher(teacherUserId, content.getModule().getCourse().getId()); }
        catch (NoSuchElementException | IllegalStateException ex) { throw new AccessDeniedException("Active course teacher required", ex); }
        if (passingPercentage != null) {
            if (!(content instanceof Quiz quiz)) throw new IllegalArgumentException("Only quizzes have a passing percentage");
            quiz.setPassingPercentage(passingPercentage);
        }
        content.setRequiredForModuleCompletion(required);
    }
}
