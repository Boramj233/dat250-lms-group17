package no.hvl.dat250.lms.content.service;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.Quiz;
import no.hvl.dat250.lms.content.repository.ContentRepository;
import no.hvl.dat250.lms.content.repository.StaticPageCompletionRepository;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import no.hvl.dat250.lms.submission.service.SubmissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deletes unused content only; learning history is never removed or cascaded. */
@Service
public class ContentDeletionService {
    private final ContentService contents;
    private final ContentRepository contentRepository;
    private final EnrollmentService enrollments;
    private final SubmissionService submissions;
    private final QuizService quizzes;
    private final StaticPageCompletionRepository pageCompletions;

    public ContentDeletionService(ContentService contents, ContentRepository contentRepository,
            EnrollmentService enrollments, SubmissionService submissions, QuizService quizzes,
            StaticPageCompletionRepository pageCompletions) {
        this.contents = contents;
        this.contentRepository = contentRepository;
        this.enrollments = enrollments;
        this.submissions = submissions;
        this.quizzes = quizzes;
        this.pageCompletions = pageCompletions;
    }

    @Transactional
    public void delete(Long contentId, Long teacherUserId) {
        // Reuse the existing Content row lock, without acquiring owner/group locks.
        Content content = contents.lockSubmissionContent(contentId);
        enrollments.requireActiveTeacher(teacherUserId, content.getModule().getCourse().getId());
        if (submissions.hasSubmissions(contentId) || pageCompletions.existsByContent_Id(contentId)) {
            throw new ContentDeletionConflictException("Content with learning records cannot be deleted");
        }
        if (content instanceof Quiz) {
            quizzes.deleteUnusedDefinition(contentId);
        }
        contentRepository.delete(content);
        // Surface foreign-key conflicts within the operation; the entire transaction rolls back.
        contentRepository.flush();
    }
}
