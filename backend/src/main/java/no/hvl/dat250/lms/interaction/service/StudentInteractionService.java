package no.hvl.dat250.lms.interaction.service;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.interaction.domain.StudentInteraction;
import no.hvl.dat250.lms.interaction.repository.StudentInteractionRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class StudentInteractionService {

    private final StudentInteractionRepository interactionRepository;
    private final UserService userService;
    private final ContentService contentService;

    public StudentInteractionService(
            StudentInteractionRepository interactionRepository,
            UserService userService,
            ContentService contentService) {

        this.interactionRepository = interactionRepository;
        this.userService = userService;
        this.contentService = contentService;
    }

    @Transactional
    public StudentInteraction completeContent(
            Long studentId,
            Long contentId) {

        StudentInteraction interaction = interactionRepository
                .findByStudentIdAndContentId(studentId, contentId)
                .orElseGet(() -> {
                    User student = userService.getUser(studentId);
                    Content content = contentService.requireContent(contentId);

                    return new StudentInteraction(student, content);
                });

        // TODO: BEFORE calling markCompleted(), verify:
        // 1. is the student an authenticated user?
        // 2. is the student enrolled at this course?
        // 3. is the content accessible?
        // 4. have the content-specific requirements been met?
        interaction.markCompleted();

        return interactionRepository.save(interaction);
    }
}
