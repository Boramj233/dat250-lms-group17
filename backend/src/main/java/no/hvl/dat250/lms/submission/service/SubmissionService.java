package no.hvl.dat250.lms.submission.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import no.hvl.dat250.lms.content.domain.Assignment;
import no.hvl.dat250.lms.content.domain.CodingExercise;
import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.enrollment.domain.Enrollment;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import no.hvl.dat250.lms.enrollment.service.StudentEnrollmentLookupService;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.service.GroupService;
import no.hvl.dat250.lms.submission.api.dto.SubmissionReviewResponse;
import no.hvl.dat250.lms.submission.domain.Submission;
import no.hvl.dat250.lms.submission.domain.SubmissionAccepted;
import no.hvl.dat250.lms.submission.domain.SubmissionFileMetadata;
import no.hvl.dat250.lms.submission.domain.SubmissionMember;
import no.hvl.dat250.lms.submission.domain.SubmissionType;
import no.hvl.dat250.lms.submission.repository.SubmissionRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {

    private final GroupService groups;
    private final ContentService contentService;

    private final StudentEnrollmentLookupService enrollmentLookupService;
    private final SubmissionRepository submissionRepository;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final EnrollmentService enrollmentService;

    public SubmissionService(
            ContentService contentService,
            StudentEnrollmentLookupService enrollmentLookupService,
            SubmissionRepository submissionRepository,
            @Qualifier("submissionClock") Clock clock,
            EnrollmentService enrollmentService,
            ApplicationEventPublisher events,
            GroupService groups) {

        this.contentService = contentService;
        this.enrollmentLookupService = enrollmentLookupService;
        this.submissionRepository = submissionRepository;
        this.clock = clock;
        this.events = events;
        this.groups = groups;
        this.enrollmentService = enrollmentService;
    }

    @Transactional
    public Submission submitIndividual(
            Long contentId,
            Long userId,
            SubmissionType type,
            String payload) {
        return submit(contentId, userId, type, payload, null);
    }

    @Transactional
    public Submission submit(Long contentId, Long userId, SubmissionType type, String payload, Long groupId) {

        // 1. Validate input.
        if (contentId == null || contentId <= 0
                || userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                "Content ID and User ID must be positive"
            );
        }

        if (type == null || type == SubmissionType.FILE
                || payload == null
                || payload.isBlank()
                || payload.length() > 10000) {
            throw new IllegalArgumentException(
                "Valid submission type and payload are required"
            );
        }

        // 2. Retrieve Content through its own feature Service.
        var group = groupFor(contentId, userId, groupId, true);
        Content content = contentService.requireContent(contentId);

        if (!(content instanceof Assignment) && !(content instanceof CodingExercise)) {
            throw new IllegalArgumentException("This Content does not accept submissions");
        }

        // 4. Determine the Course owning this Content.
        Long courseId = content
            .getModule()
            .getCourse()
            .getId();

        // 5. Verify the student and obtain their Enrollment.
        Enrollment enrollment = requireStudentEnrollment(userId, courseId);
        enrollmentService.lockSubmissionOwner(enrollment.getId());
        // refresh under the lock may reveal withdrawal while waiting.
        enrollment = requireStudentEnrollment(userId, courseId);

        // Owner -> content is the lock order. Read rules only after both locks.
        content = contentService.lockSubmissionContent(contentId);
        Instant deadline;
        boolean allowLateSubmission;
        if (content instanceof Assignment assignment) {
            deadline = assignment.getDeadline();
            allowLateSubmission = assignment.isAllowLateSubmission();
        } else if (content instanceof CodingExercise exercise) {
            deadline = exercise.getDeadline();
            allowLateSubmission = exercise.isAllowLateSubmission();
        } else {
            throw new IllegalArgumentException("This Content does not accept submissions");
        }

        // 6. Calculate lateness using refreshed rules and the server clock.
        Instant now = clock.instant();

        if (!content.isVisibleAt(now)) {
            throw new AccessDeniedException("Content outside visibility window");
        }

        boolean late = deadline != null
            && now.isAfter(deadline);

        if (late && !allowLateSubmission) {
            throw new SubmissionDeadlinePassedException();
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

        snapshot(submission, group);
        Submission saved = submissionRepository.save(submission);
        events.publishEvent(new SubmissionAccepted(
            UUID.randomUUID(), saved.getId(), contentId, saved.getSubmittedAt(), saved.isLate()));
        return saved;
    }

    // Short preflight transaction; no SQL transaction remains open during upload.
    @Transactional(readOnly = true)
    public void validateFileSubmission(
            Long contentId,
            Long userId) {
        validateFileSubmission(contentId, userId, null);
    }

    @Transactional(readOnly = true)
    public void validateFileSubmission(Long contentId, Long userId, Long groupId) {

        groupFor(contentId, userId, groupId, false);
        Assignment assignment = requireFileAssignment(contentId, userId);
        Long courseId = assignment.getModule().getCourse().getId();

        requireStudentEnrollment(userId, courseId);
        Instant now = clock.instant();
        if (!assignment.isVisibleAt(now)) {
            throw new AccessDeniedException("Content outside visibility window");
        }
        requireDeadlineAccepted(assignment, now);
    }

    // Joins the upload coordinator's transaction, which also handles commit rollback.
    @Transactional
    public Submission saveFileSubmission(
            Long contentId,
            Long userId,
            SubmissionFileMetadata file) {
        return saveFileSubmission(contentId, userId, file, null);
    }

    @Transactional
    public Submission saveFileSubmission(Long contentId, Long userId, SubmissionFileMetadata file, Long groupId) {

        var group = groupFor(contentId, userId, groupId, true);
        Assignment assignment = requireFileAssignment(contentId, userId);
        Long courseId = assignment.getModule().getCourse().getId();
        Enrollment enrollment = requireStudentEnrollment(userId, courseId);
        enrollmentService.lockSubmissionOwner(enrollment.getId());
        // refresh under the lock may reveal withdrawal while waiting.
        enrollment = requireStudentEnrollment(userId, courseId);

        assignment = (Assignment) contentService.lockSubmissionContent(contentId);
        Instant now = clock.instant();
        if (!assignment.isVisibleAt(now)) {
            throw new AccessDeniedException("Content outside visibility window");
        }
        boolean late = requireDeadlineAccepted(assignment, now);

        Submission submission = Submission.file(
            assignment,
            enrollment,
            file,
            now,
            late
        );

        snapshot(submission, group);
        Submission saved = submissionRepository.save(submission);
        events.publishEvent(new SubmissionAccepted(
            UUID.randomUUID(), saved.getId(), contentId, saved.getSubmittedAt(), saved.isLate()));
        return saved;
    }

    private Assignment requireFileAssignment(
            Long contentId,
            Long userId) {

        if (contentId == null || contentId <= 0
                || userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                "Content ID and User ID must be positive"
            );
        }

        Content content = contentService.requireContent(contentId);

        if (!(content instanceof Assignment assignment)) {
            throw new IllegalArgumentException(
                "File submissions require an Assignment"
            );
        }

        return assignment;
    }

    private Enrollment requireStudentEnrollment(
            Long userId,
            Long courseId) {

        try {
            return enrollmentLookupService.requireActiveStudentEnrollment(
                userId,
                courseId
            );
        } catch (NoSuchElementException | IllegalStateException ex) {
            throw new AccessDeniedException(
                "Active student enrollment required",
                ex
            );
        }
    }

    private boolean requireDeadlineAccepted(
            Assignment assignment,
            Instant now) {

        Instant deadline = assignment.getDeadline();
        boolean late = deadline != null && now.isAfter(deadline);

        if (late && !assignment.isAllowLateSubmission()) {
            throw new SubmissionDeadlinePassedException();
        }

        return late;
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

        Submission owned = submissionRepository
            .findByIdAndSubmittedBy_User_UserId(
                submissionId,
                userId
            )
            .orElseThrow(() -> new NoSuchElementException(
                "Submission not found"
            ));
        owned.getMembers();
        Hibernate.initialize(owned.getGroup());
        owned.getSubmittedBy().getUser().getUserId();
        return owned;
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

        List<Submission> history = submissionRepository
            .findByContent_IdAndSubmittedBy_User_UserIdOrderBySubmittedAtDescIdDesc(contentId, userId);
        history.forEach(submission -> {
            submission.getMembers();
            Hibernate.initialize(submission.getGroup());
            submission.getSubmittedBy().getUser().getUserId();
        });
        return history;
    }

    @Transactional(readOnly = true)
    public Submission requireTeacherSubmission(
            Long submissionId,
            Long teacherUserId) {
        if (submissionId == null || submissionId <= 0) {
            throw new IllegalArgumentException("Submission ID must be positive");
        }
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new NoSuchElementException("Submission not found"));
        requireTeacherGradableContent(submission.getContent(), teacherUserId);
        return submission;
    }

    private void requireTeacherGradableContent(Content content, Long teacherUserId) {
        // A lazy Content association may be a base-class proxy for either subtype.
        Content actualContent = (Content) Hibernate.unproxy(content);
        if (!(actualContent instanceof Assignment) && !(actualContent instanceof CodingExercise)) {
            throw new NoSuchElementException("Gradable content not found");
        }
        enrollmentService.requireActiveTeacher(
            teacherUserId, content.getModule().getCourse().getId());
    }

    @Transactional(readOnly = true)
    public SubmissionReviewResponse reviewSubmission(
            Long submissionId,
            Long teacherUserId) {
        return SubmissionReviewResponse.from(
            requireTeacherSubmission(submissionId, teacherUserId));
    }

    @Transactional(readOnly = true)
    public Page<SubmissionReviewResponse>
            reviewLatest(
                Long contentId,
                Long teacherUserId,
                int page,
                int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be nonnegative and size between 1 and 100");
        }
        Content content = contentService.requireContent(contentId);
        requireTeacherGradableContent(content, teacherUserId);
        return submissionRepository.findLatestForReview(
            contentId, PageRequest.of(page, size))
            .map(SubmissionReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public List<SubmissionReviewResponse> reviewHistory(
            Long submissionId,
            Long teacherUserId) {
        Submission submission = requireTeacherSubmission(submissionId, teacherUserId);
        return submissionRepository
            .findSequence(submission.getContent().getId(), submission.getSubmittedBy().getId(),
                submission.getGroup() == null ? null : submission.getGroup().getGroupId())
            .stream().map(SubmissionReviewResponse::from).toList();
    }

    @Transactional
    public Submission lockTeacherSubmission(Long submissionId, Long teacherUserId) {
        Submission submission = requireTeacherSubmission(submissionId, teacherUserId);
        if (submission.getGroup() == null) {
            enrollmentService.lockSubmissionOwner(submission.getSubmittedBy().getId());
        } else {
            groups.lockGroup(submission.getContent().getModule().getCourse().getId(),
                submission.getGroup().getGroupId());
        }
        enrollmentService.lockActiveTeacher(teacherUserId,
            submission.getContent().getModule().getCourse().getId());
        return submission;
    }

    @Transactional(readOnly = true)
    public boolean isLatestSubmission(Submission submission) {
        return submissionRepository
            .findSequence(submission.getContent().getId(), submission.getSubmittedBy().getId(),
                submission.getGroup() == null ? null : submission.getGroup().getGroupId())
            .stream().findFirst().map(latest -> latest.getId().equals(submission.getId())).orElse(false);
    }

    private Group groupFor(Long contentId, Long userId, Long groupId, boolean lock) {
        if (groupId == null) return null;
        if (groupId <= 0) throw new IllegalArgumentException("Invalid group ID");
        var content = contentService.requireContent(contentId);
        var group = groups.submissionGroup(content.getModule().getCourse().getId(), groupId, userId, lock);
        var memberEnrollments = group.getGroupMembers().stream()
            .map(u -> requireStudentEnrollment(u.getUserId(), content.getModule().getCourse().getId()))
            .sorted(java.util.Comparator.comparing(Enrollment::getId)).toList();
        if (lock) {
            for (var member : memberEnrollments) {
                enrollmentService.lockActiveStudent(member.getUser().getUserId(),
                    content.getModule().getCourse().getId());
            }
        }
        return group;
    }

    private void snapshot(Submission submission, Group group) {
        if (group == null) return;
        Long courseId = submission.getContent().getModule().getCourse().getId();
        submission.assignGroup(group, group.getGroupMembers().stream()
            .map(u -> {
                var e = requireStudentEnrollment(u.getUserId(), courseId);
                return new SubmissionMember(u.getUserId(), e.getId());
            }).toList());
    }

}
