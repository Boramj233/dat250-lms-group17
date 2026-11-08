package no.hvl.dat250.lms.module.api.dto;

import no.hvl.dat250.lms.module.domain.Module;

public record ModuleResponse(
    Long id,
    Long courseId,
    String title,
    int position
) {

    public static ModuleResponse from(
            Module module,
            Long courseId) {

        return new ModuleResponse(
            module.getId(),
            courseId,
            module.getTitle(),
            module.getPosition()
        );
    }
}