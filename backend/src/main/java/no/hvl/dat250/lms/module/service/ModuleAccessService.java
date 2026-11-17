package no.hvl.dat250.lms.module.service;

import no.hvl.dat250.lms.interaction.service.StudentInteractionService;
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
    private final StudentInteractionService interactionService;

    public ModuleAccessService(
            ModuleRepository moduleRepository,
            StudentInteractionService interactionService) {

        this.moduleRepository = moduleRepository;
        this.interactionService = interactionService;
    }

    @Transactional(readOnly = true)
    public boolean isModuleCompleted(
            Long moduleId,
            Long studentId) {

        if (!moduleRepository.existsById(moduleId)) {
            throw new NoSuchElementException(
                    "Module not found" + moduleId
            );
        }

        return interactionService.areRequiredContentCompleted(
                moduleId,
                studentId
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
