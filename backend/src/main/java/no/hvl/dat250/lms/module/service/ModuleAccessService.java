package no.hvl.dat250.lms.module.service;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.service.ContentService;
import no.hvl.dat250.lms.module.repository.ModuleRepository;
import no.hvl.dat250.lms.module.domain.Module;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
public class ModuleAccessService {

    private final ModuleRepository moduleRepository;
    private final ContentService contentService;

    public ModuleAccessService(
            ModuleRepository moduleRepository,
            ContentService contentService) {

        this.moduleRepository = moduleRepository;
        this.contentService = contentService;
    }

    @Transactional(readOnly = true)
    public boolean isModuleCompleted(
            Long moduleId,
            Long studentId) {

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new NoSuchElementException(
                        "module not found" + moduleId
                ));

        return module.getContents().stream()
                .filter(Content::isRequiredForModuleCompletion)
                .allMatch(content ->
                        contentService.isCompleted(
                                content.getId(),
                                studentId
                        )
                );
    }

    @Transactional(readOnly = true)
    public boolean isModuleVisible(
            Long moduleId,
            Long studentId) {

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new NoSuchElementException(
                        "module not found" + moduleId
                ));

        LocalDateTime now = LocalDateTime.now();

        // Available to everyone after this date
        if (module.getAvailableToAllFrom() != null
                && !now.isBefore(module.getAvailableToAllFrom())) {
            return true;
        }

        // Not available before the release date
        if (module.getAvailableFrom() != null
                && now.isBefore(module.getAvailableFrom())) {
            return false;
        }

        for (Module dependency : module.getDependencies()) {
            if (!isModuleCompleted(
                    dependency.getId(),
                    studentId)) {
                return false;
            }
        }

        return true;
    }

    @Transactional(readOnly = true)
    public void requireModuleVisible(
            Long moduleId,
            Long studentId) {

        if (!isModuleVisible(moduleId, studentId)) {
            throw new AccessDeniedException(
                    "Module prerequisites are not completed"
            );
        }
    }
}
