package no.hvl.dat250.lms.module.service;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.interaction.repository.StudentInteractionRepository;
import no.hvl.dat250.lms.module.repository.ModuleRepository;
import no.hvl.dat250.lms.module.domain.Module;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ModuleAccessService {

    private final ModuleRepository moduleRepository;
    private final StudentInteractionRepository interactionRepository;

    public ModuleAccessService(
            ModuleRepository moduleRepository,
            StudentInteractionRepository interactionRepository) {

        this.moduleRepository = moduleRepository;
        this.interactionRepository = interactionRepository;
    }

    @Transactional(readOnly = true)
    public boolean isModuleCompleted(
            Long moduleId,
            Long studentId) {

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Module not found"
                ));

        // get all contents and check for mandatory later, or filter at get?
        // module.getDependencyContents();
        List<Content> contents = module.getContents();

        for (Content content : contents) {
            boolean completed = interactionRepository
                    .existsByStudentIdAndContentIdAndCompletedAtIsNotNull(
                            studentId,
                            content.getId()
                    );
            if (!completed) {
                return false;
            }
        }

        return true;
    }

    @Transactional(readOnly = true)
    public boolean isModuleVisible(
            Long moduleId,
            Long studentId) {

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new NoSuchElementException(
                        "module not found"
                ));

        for (Module dependency : module.getDependencies()) {
            if (!isModuleCompleted(
                    dependency.getId(),
                    studentId)) {
                return false;
            }
        }

        return true;
    }
}
