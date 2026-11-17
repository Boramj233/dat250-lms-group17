package no.hvl.dat250.lms.interaction.service;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.interaction.domain.StudentInteraction;
import no.hvl.dat250.lms.interaction.repository.StudentInteractionRepository;
import no.hvl.dat250.lms.user.domain.User;
import no.hvl.dat250.lms.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .findByStudent_IdAndContent_Id(studentId, contentId)
                .orElseGet(() -> {
                    User student = userService.getUser(studentId);
                    Content content = contentService.requireContent(contentId);

                    return new StudentInteraction(student, content);
                });

        interaction.markCompleted();

        return interactionRepository.save(interaction);
    }

    // if there are 0 incomplete required contents, return true
    @Transactional(readOnly = true)
    public boolean areRequiredContentCompleted(
            Long moduleId,
            Long studentId) {

        return interactionRepository.countIncompleteRequiredContent(
                moduleId,
                studentId
        ) == 0;
    }
}
