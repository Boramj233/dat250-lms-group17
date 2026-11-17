package no.hvl.dat250.lms.module.api.dto;

import no.hvl.dat250.lms.module.domain.Module;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record ModuleResponse(
    Long id,
    Long courseId,
    String title,
    int position,
    Set<Long> dependencyIds,
    LocalDateTime availableFrom,
    LocalDateTime availableToAllFrom
) {

    public static ModuleResponse from(
            Module module,
            Long courseId) {

        Set<Long> dependencyIds = module.getDependencies()
                .stream()
                .map(Module::getId)
                .collect(Collectors.toSet());

        return new ModuleResponse(
            module.getId(),
            courseId,
            module.getTitle(),
            module.getPosition(),
            dependencyIds,
            module.getAvailableFrom(),
            module.getAvailableToAllFrom()
        );
    }
}