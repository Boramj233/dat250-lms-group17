package no.hvl.dat250.lms.content.service;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.NoSuchElementException;
import java.time.Instant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.repository.ContentRepository;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.service.ModuleService;
import no.hvl.dat250.lms.enrollment.domain.CourseRole;
import no.hvl.dat250.lms.enrollment.service.EnrollmentService;
import org.springframework.security.access.AccessDeniedException;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

    @org.springframework.beans.factory.annotation.Autowired
    private ContentCompletionService completion;

    /**
     * Completion evidence for one Content and a trusted local User ID (not Enrollment ID).
     * Requires an active STUDENT enrollment in the Content's course. The required flag
     * does not affect this result; Module callers filter required Content themselves.
     * Pages use manual completion, quizzes any attempt passing the current threshold,
     * assignments/coding exercises any historically owned released Grade with passed=true.
     * Invalid IDs throw IllegalArgumentException; missing Content throws NoSuchElementException;
     * absent/inactive/non-student enrollment throws AccessDeniedException.
     * Does not aggregate Module completion or calculate dependency access.
     */
    @Transactional(readOnly = true)
    public boolean isCompleted(Long contentId, Long studentUserId) {
        return completion.isCompleted(contentId, studentUserId);
    }

    private final ContentRepository contentRepository;

    @PersistenceContext
    private EntityManager entityManager;
    private final ModuleService moduleService;
    private final EnrollmentService enrollmentService;

    public ContentService(
            ContentRepository contentRepository,
            ModuleService moduleService,
            EnrollmentService enrollmentService) {

        this.contentRepository = contentRepository;
        this.moduleService = moduleService;
        this.enrollmentService = enrollmentService;
    }

    @Transactional(readOnly = true)
    public boolean isVisible(Long contentId) {
        return requireContent(contentId).isVisibleAt(Instant.now());
    }

    /** HTTP browsing only. The internal complete list remains available to Module callers. */
    @Transactional(readOnly = true)
    public List<Content> findContentsForUser(Long courseId, Long moduleId, Long userId) {
        CourseRole role = enrollmentService.activeCourseRoles(userId).get(courseId);
        if (role == null) {
            throw new AccessDeniedException("Active course enrollment required");
        }
        List<Content> contents = findContentsByModule(courseId, moduleId);
        if (role == CourseRole.TEACHER) {
            return contents;
        }
        Instant now = Instant.now();
        return contents.stream().filter(content -> content.isVisibleAt(now)).toList();
    }

    @Transactional
    public Content updateVisibility(Long contentId, Long teacherUserId, Instant visibleFrom) {
        Content content = visibilityContentForTeacher(contentId, teacherUserId);
        content.changeVisibleFrom(visibleFrom);
        return content;
    }

    @Transactional
    public Content updateVisibilityWindow(Long contentId, Long teacherUserId, Instant visibleFrom, Instant visibleUntil) {
        Content content = visibilityContentForTeacher(contentId, teacherUserId);
        content.changeVisibility(visibleFrom, visibleUntil);
        return content;
    }

    private Content visibilityContentForTeacher(Long contentId, Long teacherUserId) {
        // Reuse the content row lock used by submission acceptance and settings editing.
        Content content = lockSubmissionContent(contentId);
        enrollmentService.requireActiveTeacher(teacherUserId, content.getModule().getCourse().getId());
        return content;
    }

    // Create a StaticPage in a specific Course and Module.
    @Transactional
    public StaticPage createStaticPage(
            Long courseId,
            Long moduleId,
            String title,
            int position,
            String body) {

        if (moduleId == null) {
            throw new IllegalArgumentException(
                "Module ID must not be null"
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                "Content title must not be blank"
            );
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                "Content position must not be negative"
            );
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException(
                "Static page body must not be blank"
            );
        }

        Module module = moduleService
            .findModuleInCourse(courseId, moduleId)
            .orElseThrow(() -> new NoSuchElementException(
                "Module not found in course"
            ));

        StaticPage page = new StaticPage(
            module,
            title,
            position,
            body
        );

        if (contentRepository.existsByModule_IdAndPosition(moduleId, position)) {
            throw new ContentPositionConflictException("Content position is already occupied in this module");
        }

        return contentRepository.save(page);
    }

    // List all Content items in a specific Module.
    @Transactional(readOnly = true)
    public List<Content> findContentsByModule(
            Long courseId,
            Long moduleId) {

        if (moduleId == null) {
            throw new IllegalArgumentException(
                "Module ID must not be null"
            );
        }

        Module module = moduleService
            .findModuleInCourse(courseId, moduleId)
            .orElseThrow(() -> new NoSuchElementException(
                "Module not found in course"
            ));

        return contentRepository
            .findByModule_IdOrderByPositionAsc(moduleId);
    }

    @Transactional
    public void reorder(Long courseId, Long moduleId, List<Long> contentIds) {
        if (contentIds == null || contentIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Content IDs must be a non-null list of positive IDs");
        }
        List<Content> contents = findContentsByModule(courseId, moduleId);
        Map<Long, Content> byId = new HashMap<>();
        Set<Integer> occupiedPositions = new HashSet<>();
        for (Content content : contents) {
            byId.put(content.getId(), content);
            occupiedPositions.add(content.getPosition());
        }
        if (contentIds.size() != contents.size() || !new HashSet<>(contentIds).equals(byId.keySet())) {
            throw new IllegalArgumentException("Order must contain every Content in this module exactly once");
        }
        if (contents.isEmpty()) {
            return;
        }
        List<Content> ordered = contentIds.stream().map(byId::get).toList();

        // Temporary positions are outside both the current positions and final 0..n-1.
        // Flush each phase so swaps never violate the module/position unique constraint.
        int temporaryPosition = contents.size();
        for (Content content : ordered) {
            while (occupiedPositions.contains(temporaryPosition)) {
                temporaryPosition++;
            }
            content.changePosition(temporaryPosition++);
        }
        contentRepository.flush();
        for (int position = 0; position < ordered.size(); position++) {
            ordered.get(position).changePosition(position);
        }
        contentRepository.flush();
    }

    // Find any Content subtype by ID.
    // Shared lookup method for other feature services.
    @Transactional(readOnly = true)
    public Content requireContent(Long contentId) {

        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException(
                "Content ID must be positive"
            );
        }

        return contentRepository
            .findById(contentId)
            .orElseThrow(() -> new NoSuchElementException(
                "Content not found: " + contentId
            ));
    }

    // Acquire after the submission owner lock, and hold through submission commit.
    // Refresh reads current rules even when preflight/OSIV already loaded this entity.
    @Transactional(propagation = Propagation.MANDATORY)
    public Content lockSubmissionContent(Long contentId) {
        Content content = (Content) Hibernate.unproxy(requireContent(contentId));
        entityManager.refresh(content, LockModeType.PESSIMISTIC_WRITE);
        return content;
    }

    @Transactional
    public StaticPage updateStaticPage(Long contentId, String title, String body) {
        StaticPage page = findStaticPageById(contentId);
        page.updateDetails(title, body);
        return page;
    }

    @Transactional(readOnly = true)
    public Long staticPageCourseId(Long contentId) {
        return findStaticPageById(contentId).getModule().getCourse().getId();
    }

    // Internal lookup; HTTP access is checked by ContentController.
    // Find a Content item only if it is a StaticPage.
    @Transactional(readOnly = true)
    public StaticPage findStaticPageById(Long contentId) {

        Content content = requireContent(contentId);

        if (!(Hibernate.unproxy(content) instanceof StaticPage page)) {
            throw new NoSuchElementException(
                "Static page not found: " + contentId
            );
        }

        return page;
    }
}
