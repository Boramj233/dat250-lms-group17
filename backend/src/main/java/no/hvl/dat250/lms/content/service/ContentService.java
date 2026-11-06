package no.hvl.dat250.lms.content.service;

import java.util.List;
import java.util.NoSuchElementException;

import no.hvl.dat250.lms.content.domain.Content;
import no.hvl.dat250.lms.content.domain.StaticPage;
import no.hvl.dat250.lms.content.repository.ContentRepository;
import no.hvl.dat250.lms.module.domain.Module;
import no.hvl.dat250.lms.module.service.ModuleService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final ModuleService moduleService;

    public ContentService(ContentRepository contentRepository, ModuleService moduleService){
        this.contentRepository = contentRepository;
        this.moduleService = moduleService;
    }

    @Transactional
    public StaticPage createStaticPage(Long courseId, Long moduleId,  String title, int position, String body){

        if (moduleId == null) {
            throw new IllegalArgumentException(
                "Module ID must not be null"
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                "Content title must not be blank");
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                "Content position must not be negative");
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException(
                "Static page body must not be blank");
        }

        // Module module = moduleService.findModuleById(moduleId)
        //         .orElseThrow(() -> new NoSuchElementException(
        //             "Module not found: " + moduleId));

        Module module = moduleService
        .findModuleInCourse(courseId, moduleId)
        .orElseThrow(() -> new NoSuchElementException(
            "Module not found in course"
        ));

        StaticPage page = new StaticPage(module, title, position, body);

        return contentRepository.save(page);
    }

    @Transactional(readOnly = true)
    public List<Content> findContentsByModule(Long courseId, Long moduleId) {
        if (moduleId == null) {
            throw new IllegalArgumentException(
                "Module ID must not be null");
        }

        // moduleService.findModuleById(moduleId)
        //         .orElseThrow(() -> new NoSuchElementException(
        //             "Module not found: " + moduleId));

        Module module = moduleService
        .findModuleInCourse(courseId, moduleId)
        .orElseThrow(() -> new NoSuchElementException(
            "Module not found in course"
        ));

        return contentRepository
                .findByModule_IdOrderByPositionAsc(moduleId);


    }

}
